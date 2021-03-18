package de.embl.cba.elastixwrapper.wrapper.elastix;

import bdv.util.*;
import de.embl.cba.elastixwrapper.commandline.ElastixCaller;
import de.embl.cba.elastixwrapper.commandline.settings.ElastixSettings;
import de.embl.cba.elastixwrapper.settings.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.utils.Utils;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.DefaultElastixParametersCreator;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import de.embl.cba.metaimage_io.MetaImage_Reader;
import de.embl.cba.metaimage_io.MetaImage_Writer;
import ij.*;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ElastixWrapper
{
    public static final String ELASTIX_FIXED_IMAGE_NAME = "fixed";
    public static final String ELASTIX_MOVING_IMAGE_NAME = "moving";
    public static final String ELASTIX_FIXED_MASK_IMAGE_NAME = "fixedMask";
    public static final String ELASTIX_MOVING_MASK_IMAGE_NAME = "movingMask";

    public static final String MHD = ".mhd";
    public static final String RAW = ".raw";

    public static final String ELASTIX_OUTPUT_FILENAME = "result.0";
    public static final String TRANSFORMIX_INPUT_FILENAME = "to_be_transformed";
    public static final String TRANSFORMIX_OUTPUT_FILENAME = "result";

    private ElastixWrapperSettings settings;
    private ArrayList< String > transformedImageFilePaths;

    private ArrayList< ARGBType > colors;
    private Bdv bdv;
    private int colorIndex;
    private int nChannels;
    private ArrayList< String > elastixTmpFilenames;

    public ElastixWrapper( ElastixWrapperSettings settings )
    {
        this.settings = settings;
        this.transformedImageFilePaths = new ArrayList<>(  );
    }

    public void runElastix()
    {
        processSettings();

        createOrEmptyWorkingDir();

        if ( settings.stageImages ) {
            if (!stageImages()) {
                Utils.logErrorAndExit(settings, "There was an issue staging the images.\n " +
                        "Maybe the temporary working directory could not be generated.");
                return;
            }
        } else {
            updateSettingsForUnstagedImages();
        }

        setParameters();
        ElastixSettings elastixSettings = new ElastixSettings( settings );
        new ElastixCaller( elastixSettings ).callElastix();
    }

    private void processSettings()
    {
        if ( ! settings.elastixDirectory.endsWith( File.separator ) )
            settings.elastixDirectory += File.separator;

        if ( ! settings.tmpDir.endsWith( File.separator ) )
            settings.tmpDir += File.separator;
    }

    public void showTransformationFile()
    {
        IJ.open( settings.tmpDir + "TransformParameters.0.txt");
    }

    public void saveTransformationFile()
    {
        final File transformation =
                new File( settings.tmpDir + "TransformParameters.0.txt" );

        File copied = new File( settings.transformationOutputFilePath );

        try
        {
            FileUtils.copyFile( transformation, copied);
        } catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    public void showInputImagePlus( )
    {
        ImagePlus fixed;

        fixed = IJ.openImage( settings.initialFixedImageFilePath );

        fixed.show();

        fixed.setTitle( "fixed" );

        // TODO: The macro recording does not work when using IJ.run(..) inside the plugin
        // if ( fixed.getNChannels() > 1 ) IJ.run("Split Channels" );
    }


    /**
     * Shows the fixed, moving and transformed moving images
     * in BigDataViewer.
     *
     * @return {@code Bdv} BigDataViewer handle, enabling, e.g., bdv.close()
     */
    public Bdv reviewResults()
    {
        createTransformedImagesAndSaveAsTiff();

        initColors();

        showFixedImagesInBdv();

        showMovingImages();

        showTransformedImages();

        return bdv;
    }

    private void showMovingImages()
    {
        for ( int index : settings.fixedToMovingChannel.values() )
        {
            String baseName = new File( settings.movingImageFilePaths.get(index) ).getName();
            ImagePlus imagePlus = loadMetaImage(
                    settings.tmpDir,
                    baseName );
            final BdvStackSource bdvStackSource = showImagePlusInBdv( imagePlus );
            bdvStackSource.setColor( colors.get( colorIndex++ )  );
            bdv = bdvStackSource.getBdvHandle();
        }
    }

    private void showFixedImagesInBdv(  )
    {
        for ( int index : settings.fixedToMovingChannel.keySet() )
        {
            String baseName = new File( settings.fixedImageFilePaths.get(index) ).getName();
            ImagePlus imagePlus = loadMetaImage(
                    settings.tmpDir,
                    baseName );
            final BdvStackSource bdvStackSource = showImagePlusInBdv( imagePlus );
            bdvStackSource.setColor( colors.get( colorIndex++ )  );
            bdv = bdvStackSource.getBdvHandle();
        }
    }

    private BdvStackSource showImagePlusInBdv(
            ImagePlus imp )
    {
        final Calibration calibration = imp.getCalibration();

        if ( imp.getNSlices() > 1 )
        {
            final double[] calib = {
                    calibration.pixelWidth,
                    calibration.pixelHeight,
                    calibration.pixelDepth
            };
            return BdvFunctions.show(
                    ( RandomAccessibleInterval ) ImageJFunctions.wrapReal( imp ),
                    imp.getTitle(),
                    BdvOptions.options().addTo( bdv ).axisOrder( AxisOrder.XYZ ).sourceTransform( calib ) );
        }
        else
        {

            final double[] calib = {
                    calibration.pixelWidth,
                    calibration.pixelHeight
            };

            return BdvFunctions.show(
                    ( RandomAccessibleInterval ) ImageJFunctions.wrapReal( imp ),
                    imp.getTitle(),
                    BdvOptions.options().addTo( bdv ).is2D().sourceTransform( calib ) );
        }
    }

    private void initColors()
    {
        colors = new ArrayList<>();
        colors.add( new ARGBType( ARGBType.rgba( 000, 255, 000, 255 ) ) );
        colors.add( new ARGBType( ARGBType.rgba( 255, 000, 255, 255 ) ) );
        colors.add( new ARGBType( ARGBType.rgba( 255, 000, 000, 255 ) ) );
        colors.add( new ARGBType( ARGBType.rgba( 000, 000, 255, 255 ) ) );
        colors.add( new ARGBType( ARGBType.rgba( 000, 255, 255, 255 ) ) );
        colors.add( new ARGBType( ARGBType.rgba( 255, 255, 000, 255 ) ) );

        colors.add( new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) ) );
        colors.add( new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) ) );
        colors.add( new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) ) );
        colors.add( new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) ) );
        colors.add( new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) ) );
        colors.add( new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) ) );
        colors.add( new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) ) );
        colors.add( new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) ) );
        colorIndex = 0;
    }

    private BdvStackSource showFixedInBdv()
    {
        final ImagePlus templateImp = IJ.openImage( settings.initialFixedImageFilePath );

        return showImagePlusInBdv( templateImp );
    }

    public void createTransformedImagesAndSaveAsTiff()
    {
        settings.outputModality = ElastixWrapperSettings.OUTPUT_MODALITY_SAVE_AS_TIFF;
        settings.outputFile = new File( settings.tmpDir + "transformed" );

        settings.transformationFilePath =
                getPath( "TransformParameters.0.txt" );

        String executableShellScript = createExecutableShellScript( TRANSFORMIX );

        for ( int c = 0; c < movingImageFileNames.size(); ++c )
            transformImageAndHandleOutput( executableShellScript, movingImageFileNames, c );
    }

	public void reviewResultsInImageJ()
	{
		settings.outputModality = ElastixWrapperSettings.OUTPUT_MODALITY_SHOW_IMAGES;
		settings.outputFile = new File( settings.tmpDir + "transformed" );

		settings.transformationFilePath =
				getPath( "TransformParameters.0.txt" );

		String executableShellScript = createExecutableShellScript( TRANSFORMIX );

		showInputImagePlus();

		for ( int c = 0; c < movingImageFileNames.size(); ++c )
			transformImageAndHandleOutput( executableShellScript, movingImageFileNames, c );

	}





    private String getPath( String fileName )
    {
        return settings.tmpDir + File.separator + fileName;
    }


    public static String createTransformedImageTitle( int channel )
    {
        return "C" + channel + "-transformed";
    }


    public ImagePlus loadMetaImage( String directory, String filename )
    {
        MetaImage_Reader reader = new MetaImage_Reader();
        return reader.load( directory, filename, false );
    }

    private boolean stageImages()
    {
        settings.fixedImageFilePaths = stageImageAsMhd(
                settings.initialFixedImageFilePath,
                ELASTIX_FIXED_IMAGE_NAME );

        settings.movingImageFilePaths = stageImageAsMhd(
                settings.initialMovingImageFilePath,
                ELASTIX_MOVING_IMAGE_NAME );

        if ( ! settings.initialFixedMaskPath.equals( "" ) )
            settings.fixedMaskFilePaths = stageImageAsMhd(
                    settings.initialFixedMaskPath,
                    ELASTIX_FIXED_MASK_IMAGE_NAME );

        if ( ! settings.initialMovingMaskPath.equals( "" ) )
            settings.movingMaskFilePaths = stageImageAsMhd(
                    settings.initialMovingMaskPath,
                    ELASTIX_MOVING_MASK_IMAGE_NAME );

        setFixedToMovingChannel();

        return true;
    }

    private void setFixedToMovingChannel() {
        settings.numChannels = settings.fixedImageFilePaths.size();

        if ( settings.fixedToMovingChannel.size() == 0 )
        {
            // use all channels for registration
            for ( int c = 0; c < settings.numChannels; c++ )
                settings.fixedToMovingChannel.put( c, c );
        }
    }

    private void updateSettingsForUnstagedImages() {
        // with no staging, our final file paths are == to the initial ones
        settings.fixedImageFilePaths.add( settings.initialFixedImageFilePath );
        settings.movingImageFilePaths.add( settings.initialMovingImageFilePath );
        if ( settings.initialFixedMaskPath != null ) {
            settings.fixedMaskFilePaths.add( settings.initialFixedMaskPath );
        }

        if ( settings.initialMovingMaskPath != null ) {
            settings.movingMaskFilePaths.add( settings.initialMovingMaskPath );
        }

        // need bit depth to create parameter file
        ImagePlus imp = openImage( settings.initialMovingImageFilePath );
        settings.movingImageBitDepth = imp.getBitDepth();
        setFixedToMovingChannel();
    }

    private ImagePlus openImage ( String imagePath ) {
        ImagePlus imp = IJ.openImage( imagePath );

        if ( imp == null )
        {
            System.err.println( "[ERROR] The image could not be loaded: "
                    + imagePath );
            if ( settings.headless )
                System.exit( 1 );
        }

        return imp;
    }

    private boolean checkChannelNumber( int nChannelsFixedImage, int nChannelsMovingImage )
    {

        if ( nChannelsFixedImage != nChannelsMovingImage )
        {
            Utils.logErrorAndExit( settings, "Number of channels " +
                    "in fixed and moving image do not match." );
            return false;
        }
        return true;
    }

    private void setParameters()
    {
        settings.parameterFilePath = getDefaultParameterFilePath();
        System.out.println( "Parameter list type: " + settings.elastixParametersStyle);
        ElastixParameters parameters =
                new DefaultElastixParametersCreator( settings ).getElastixParameters( settings.elastixParametersStyle );

        parameters.writeParameterFile( settings.parameterFilePath );
    }


    private String getDefaultParameterFilePath()
    {
        return getPath( "elastix_parameters.txt" );
    }

    private String stageImagePlusAsMhd( ImagePlus imp, String filename )
    {
        if ( filename.contains( ELASTIX_FIXED_MASK_IMAGE_NAME )
                || filename.contains( ELASTIX_MOVING_MASK_IMAGE_NAME ) )
            Utils.convertToMask( imp, 0.1F );

        MetaImage_Writer writer = new MetaImage_Writer();
        String filenameWithExtension = filename + MHD;
        settings.logService.info( "Staging image as mhd: " + filenameWithExtension );
        writer.save( imp, settings.tmpDir, filenameWithExtension );
        settings.imageWidthMillimeter = writer.getImageWidthMillimeter();
        return new File( settings.tmpDir, filenameWithExtension ).getAbsolutePath();
    }

    private ArrayList< String > stageImageAsMhd( String imagePath, String filename )
    {
        ImagePlus imp = openImage( imagePath );

        if ( filename.equals( ELASTIX_MOVING_IMAGE_NAME ) )
            settings.movingImageBitDepth = imp.getBitDepth();

        nChannels = imp.getNChannels();

        if ( nChannels > 1 )
        {
            return stageMultiChannelImagePlusAsMhd( imp, filename );
        }
        else
        {
            ArrayList< String > filenames = new ArrayList<>();
            filenames.add( stageImagePlusAsMhd( imp, filename ) );
            return filenames;
        }
    }


    private ArrayList< String > stageMultiChannelImagePlusAsMhd( ImagePlus imp, String filename )
    {
        ArrayList< String > filePaths = new ArrayList<>( );

        for ( int channelIndex = 0; channelIndex < imp.getNChannels(); ++channelIndex )
        {
            ImagePlus channelImage = getChannel( imp, channelIndex );

            filePaths.add(
                    stageImagePlusAsMhd(
                        channelImage, getChannelFilename( filename, channelIndex ) ) );
        }

        return filePaths;
    }

    private String getChannelFilename( String filename, int channelIndex )
    {
        return filename + "-C" + channelIndex;
    }

    private ImagePlus getChannel( ImagePlus imp, int channel )
    {
        Duplicator duplicator = new Duplicator();

        return duplicator.run(
                imp,
                channel + 1,
                channel + 1,
                1,
                imp.getNSlices(),
                1,
                1 );
    }

    private void addImagesAndMasksToArguments( List< String > args )
    {
        addImagesToArguments( args, FIXED, fixedImageFileNames );

        addImagesToArguments( args, MOVING, movingImageFileNames );

        if ( fixedMaskFileNames != null )
            addImagesToArguments( args, "fMask", fixedMaskFileNames );

        if ( movingMaskFileNames != null )
            addImagesToArguments( args, "mMask", movingMaskFileNames );
    }

    private void addImagesToArguments( List< String > args,
                                       String fixedOrMoving,
                                       ArrayList< String > fileNames )
    {
        int elastixChannelIndex = 0;
        for ( int fixedChannelIndex : settings.fixedToMovingChannel.keySet() )
        {
            if (  settings.fixedToMovingChannel.size() == 1 )
                args.add( "-" + fixedOrMoving );
            else
                args.add( "-" + fixedOrMoving + elastixChannelIndex );

            final String filename = getFileName(
                    fixedOrMoving, fileNames, fixedChannelIndex );

            args.add( getPath( filename ) );

            elastixChannelIndex++;
        }
    }

    private String getFileName(
            String fixedOrMoving,
            ArrayList< String > filenames,
            int fixedChannelIndex )
    {
        int filenameIndex;

        if ( fixedOrMoving.equals( FIXED ) )
			filenameIndex = fixedChannelIndex;
		else // Moving
			filenameIndex = settings.fixedToMovingChannel.get( fixedChannelIndex );

        return filenames.get( filenameIndex );
    }

    private void createOrEmptyWorkingDir()
    {
        settings.logService.info( "Temporary directory is: " + settings.tmpDir );

        setElastixTmpFilenames();

        File directory = new File( settings.tmpDir );

        if (! directory.exists() )
            directory.mkdir();
        else
            for ( String filename : elastixTmpFilenames )
            {
                final File file = new File( settings.tmpDir + File.separator + filename );
                if ( file.exists() )
                    file.delete();
            }
    }

    private void setElastixTmpFilenames()
    {
        final ArrayList< String > elastixTmpFilenameStumps = new ArrayList<>();
        elastixTmpFilenameStumps.add( ELASTIX_FIXED_IMAGE_NAME );
        elastixTmpFilenameStumps.add( ELASTIX_MOVING_IMAGE_NAME );
        elastixTmpFilenameStumps.add( ELASTIX_MOVING_MASK_IMAGE_NAME );
        elastixTmpFilenameStumps.add( ELASTIX_FIXED_MASK_IMAGE_NAME );
        elastixTmpFilenameStumps.add( ELASTIX_OUTPUT_FILENAME  );
        elastixTmpFilenameStumps.add( TRANSFORMIX_OUTPUT_FILENAME );
        elastixTmpFilenameStumps.add( TRANSFORMIX_INPUT_FILENAME );

        elastixTmpFilenames = new ArrayList<>();
        for ( String filenameStump : elastixTmpFilenameStumps )
            addTmpImage( filenameStump );

        for ( int c = 0; c < 10; c++ )
            for ( String filenameStump : elastixTmpFilenameStumps )
                addTmpImage( getChannelFilename( filenameStump, c ) );
    }

    private void addTmpImage( String filename )
    {
        elastixTmpFilenames.add( filename + MHD );
        elastixTmpFilenames.add( filename + RAW );
    }


}
