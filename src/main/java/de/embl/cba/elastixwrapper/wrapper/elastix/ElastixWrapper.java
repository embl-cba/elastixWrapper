package de.embl.cba.elastixwrapper.wrapper.elastix;

import bdv.util.*;
import de.embl.cba.elastixwrapper.commandline.ElastixCaller;
import de.embl.cba.elastixwrapper.commandline.settings.ElastixSettings;
import de.embl.cba.elastixwrapper.settings.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.settings.TransformixWrapperSettings;
import de.embl.cba.elastixwrapper.utils.Utils;
import de.embl.cba.elastixwrapper.wrapper.StagingManager;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.DefaultElastixParametersCreator;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParametersSettings;
import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapper;
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
    private ElastixWrapperSettings settings;
    private ElastixParametersSettings parametersSettings;
    private StagingManager stagingManager;

    private ArrayList< ARGBType > colors;
    private Bdv bdv;
    private int colorIndex;

    public ElastixWrapper(ElastixWrapperSettings settings, ElastixParametersSettings parametersSettings )
    {
        this.settings = settings;
        this.stagingManager = new StagingManager( settings );
    }

    public void runElastix()
    {
        processSettings();
        stagingManager.createOrEmptyWorkingDir();

        if (!stageImages()) {
            Utils.logErrorAndExit(settings, "There was an issue staging the images.\n " +
                    "Maybe the temporary working directory could not be generated.");
            return;
        }

        setMovingImageParameters();
        createElastixParameterFile();
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

        fixed = IJ.openImage( settings.fixedImageFilePath );

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
        TransformixWrapperSettings transformixWrapperSettings = createTransformixWrapperSettings(TransformixWrapperSettings.OutputModality.Save_as_tiff);
        TransformixWrapper transformixWrapper = new TransformixWrapper( transformixWrapperSettings );
        transformixWrapper.transformImagesAndHandleOutput();

        initColors();

        showFixedImagesInBdv();

        showMovingImages();

        showTransformedImages();

        return bdv;
    }

    private TransformixWrapperSettings createTransformixWrapperSettings(TransformixWrapperSettings.OutputModality outputModality) {
        TransformixWrapperSettings transformixWrapperSettings = new TransformixWrapperSettings( settings );
        transformixWrapperSettings.stagedMovingImageFilePaths = settings.stagedMovingImageFilePaths;
        transformixWrapperSettings.outputModality = outputModality;
        transformixWrapperSettings.transformationFilePath = stagingManager.getDefaultTransformationFilePath();
        // TODO - what is this used for?
        transformixWrapperSettings.outputFile = new File( settings.tmpDir + "transformed" );
        return transformixWrapperSettings;
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

    private void setMovingImageParameters() {
        ImagePlus imp = stagingManager.openImage( settings.movingImageFilePath );
        parametersSettings.movingImageBitDepth = imp.getBitDepth();
    }

    private void createElastixParameterFile()
    {
        settings.parameterFilePath = stagingManager.getDefaultParameterFilePath();
        System.out.println( "Parameter list type: " + settings.elastixParametersStyle );
        ElastixParameters parameters =
                new DefaultElastixParametersCreator( settings ).getElastixParameters( settings.elastixParametersStyle );

        parameters.writeParameterFile( settings.parameterFilePath );
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




}
