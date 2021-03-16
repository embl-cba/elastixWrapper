package de.embl.cba.elastixwrapper.elastix;

import bdv.util.*;
import de.embl.cba.bdv.utils.io.BdvImagePlusExport;
import de.embl.cba.elastixwrapper.logging.Logger;
import de.embl.cba.elastixwrapper.utils.Utils;
import de.embl.cba.metaimage_io.MetaImage_Reader;
import de.embl.cba.metaimage_io.MetaImage_Writer;
import ij.*;
import ij.io.FileSaver;
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
import java.util.Map;

import static de.embl.cba.elastixwrapper.utils.Utils.saveStringToFile;
import static org.scijava.util.PlatformUtils.*;

public class ElastixWrapper
{
    public static final String ELASTIX = "elastix";
    public static final String TRANSFORMIX = "transformix";
    public static final String ELASTIX_FIXED_IMAGE_NAME = "fixed";
    public static final String ELASTIX_MOVING_IMAGE_NAME = "moving";
    public static final String ELASTIX_FIXED_MASK_IMAGE_NAME = "fixedMask";
    public static final String ELASTIX_MOVING_MASK_IMAGE_NAME = "movingMask";

    public static final String MHD = ".mhd";
    public static final String FIXED = "f";
    public static final String MOVING = "m";
    public static final String ELASTIX_OUTPUT_FILENAME = "result.0";
    public static final String TRANSFORMIX_INPUT_FILENAME = "to_be_transformed";
    public static final String TRANSFORMIX_OUTPUT_FILENAME = "result";
    public static final String RAW = ".raw";

    ElastixSettings settings;

    private ArrayList< String > fixedImageFileNames;
    private ArrayList< String > movingImageFileNames;
    private ArrayList< String > fixedMaskFileNames;
    private ArrayList< String > movingMaskFileNames;
    private ArrayList< String > transformedImageFilePaths;

    private int movingImageBitDepth;
    private ArrayList< ARGBType > colors;
    private Bdv bdv;
    private int colorIndex;
    private int nChannels;
    private ArrayList< String > elastixTmpFilenames;

    public ElastixWrapper( ElastixSettings settings )
    {
        this.settings = settings;
        this.transformedImageFilePaths = new ArrayList<>(  );
    }

    public void runElastix()
    {
        processSettings();

        createOrEmptyWorkingDir();

        if ( ! stageImages() )
        {
            Utils.logErrorAndExit( settings, "There was an issue staging the images.\n " +
                    "Maybe the temporary working directory could not be generated." );
            return;
        }

        callElastix();
    }

    private void processSettings()
    {
        if ( ! settings.elastixDirectory.endsWith( File.separator ) )
            settings.elastixDirectory += File.separator;

        if ( ! settings.tmpDir.endsWith( File.separator ) )
            settings.tmpDir += File.separator;
    }

    public void runTransformix()
    {
        createOrEmptyWorkingDir();

        ArrayList< String > channelFileNames = stageImageAsMhd(
                settings.movingImageFilePath, TRANSFORMIX_INPUT_FILENAME );

        String executableShellScript = createExecutableShellScript( TRANSFORMIX );

        for ( int c = 0; c < channelFileNames.size(); c++ )
            transformImageAndHandleOutput( executableShellScript, channelFileNames, c );
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
        createTransformedImagesAndSaveAsTiff();

        initColors();

        showFixedImagesInBdv();

        showMovingImages();

        showTransformedImages();

        return bdv;
    }

    private void showTransformedImages()
    {
        for ( int index : settings.fixedToMovingChannel.values() )
        {
            ImagePlus imagePlus = IJ.openImage( transformedImageFilePaths.get( index ) );
            final BdvStackSource bdvStackSource = showImagePlusInBdv( imagePlus );
            bdvStackSource.setColor( colors.get( colorIndex++ )  );
            bdv = bdvStackSource.getBdvHandle();
        }
    }

    private void showMovingImages()
    {
        for ( int index : settings.fixedToMovingChannel.values() )
        {
            ImagePlus imagePlus = loadMetaImage(
                    settings.tmpDir,
                    movingImageFileNames.get( index ) );
            final BdvStackSource bdvStackSource = showImagePlusInBdv( imagePlus );
            bdvStackSource.setColor( colors.get( colorIndex++ )  );
            bdv = bdvStackSource.getBdvHandle();
        }
    }

    private void showFixedImagesInBdv(  )
    {
        for ( int index : settings.fixedToMovingChannel.keySet() )
        {
            ImagePlus imagePlus = loadMetaImage(
                    settings.tmpDir,
                    fixedImageFileNames.get( index ) );
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
        final ImagePlus templateImp = IJ.openImage( settings.fixedImageFilePath );

        return showImagePlusInBdv( templateImp );
    }

    public void createTransformedImagesAndSaveAsTiff()
    {
        settings.outputModality = ElastixSettings.OUTPUT_MODALITY_SAVE_AS_TIFF;
        settings.outputFile = new File( settings.tmpDir + "transformed" );

        settings.transformationFilePath =
                getPath( "TransformParameters.0.txt" );

        String executableShellScript = createExecutableShellScript( TRANSFORMIX );

        for ( int c = 0; c < movingImageFileNames.size(); ++c )
            transformImageAndHandleOutput( executableShellScript, movingImageFileNames, c );
    }

	public void reviewResultsInImageJ()
	{
		settings.outputModality = ElastixSettings.OUTPUT_MODALITY_SHOW_IMAGES;
		settings.outputFile = new File( settings.tmpDir + "transformed" );

		settings.transformationFilePath =
				getPath( "TransformParameters.0.txt" );

		String executableShellScript = createExecutableShellScript( TRANSFORMIX );

		showInputImagePlus();

		for ( int c = 0; c < movingImageFileNames.size(); ++c )
			transformImageAndHandleOutput( executableShellScript, movingImageFileNames, c );

	}

    private void transformImageAndHandleOutput( String executableShellScript,
                                                ArrayList< String > movingImageFileNames,
                                                int c )
    {
        List< String > transformixCallArgs =
                getTransformixCallArgs(
                        movingImageFileNames.get( c ), executableShellScript );

        Utils.executeCommand( transformixCallArgs, settings.logService );

        String transformedImageFileName = TRANSFORMIX_OUTPUT_FILENAME
                + "."
                + settings.resultImageFileType;

        ImagePlus result = loadMetaImage(
                settings.tmpDir,
                transformedImageFileName );

        if ( result == null )
        {
            Utils.logErrorAndExit( settings,"The transformed image could not be loaded: "
                    + settings.tmpDir + File.separator + transformedImageFileName + "\n" +
                    "Please check the log: " + settings.tmpDir + File.separator + "elastix.log" );

        }

        if ( settings.outputModality.equals( ElastixSettings.OUTPUT_MODALITY_SHOW_IMAGES ) )
        {
            result.show();
            result.setTitle( "transformed-ch" + c );
        }
        else
        {
            String outputFile = settings.outputFile.toString();
            outputFile = outputFile.replace( ".tif", "" );
            outputFile = outputFile.replace( ".xml", "" );

            if ( settings.outputModality.equals( ElastixSettings.OUTPUT_MODALITY_SAVE_AS_TIFF ) )
            {
                final String path = outputFile + "-ch" + c + ".tif";

                transformedImageFilePaths.add( path );

                settings.logService.info( "\nSaving transformed image: " + path );

                new FileSaver( result ).saveAsTiff( path );
            }
            else if ( settings.outputModality.equals( ElastixSettings.OUTPUT_MODALITY_SAVE_AS_BDV ) )
            {
                String path;
                if ( nChannels > 1 )
                    path = outputFile + "-ch" + c + ".xml";
                else
                    path = outputFile + ".xml";

                settings.logService.info( "\nSaving transformed image: " + path );

                BdvImagePlusExport.saveAsBdv( result, new File( path ) );
            }
        }

    }

    public ArrayList< ImagePlus > getTransformedImages()
    {
        if ( transformedImageFilePaths.size() == 0 )
            createTransformedImagesAndSaveAsTiff();

        ArrayList< ImagePlus > transformedImages = new ArrayList<>(  );

        for ( String path : transformedImageFilePaths )
            transformedImages.add( IJ.openImage( path ) );

        return transformedImages;
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
        fixedImageFileNames = stageImageAsMhd(
                settings.fixedImageFilePath,
                ELASTIX_FIXED_IMAGE_NAME );

        movingImageFileNames = stageImageAsMhd(
                settings.movingImageFilePath,
                ELASTIX_MOVING_IMAGE_NAME );

        if ( ! settings.fixedMaskPath.equals( "" ) )
            fixedMaskFileNames = stageImageAsMhd(
                    settings.fixedMaskPath,
                    ELASTIX_FIXED_MASK_IMAGE_NAME );

        if ( ! settings.movingMaskPath.equals( "" ) )
            movingMaskFileNames = stageImageAsMhd(
                    settings.movingMaskPath,
                    ELASTIX_MOVING_MASK_IMAGE_NAME );

        settings.numChannels = fixedImageFileNames.size();

        if ( settings.fixedToMovingChannel.size() == 0 )
        {
            // use all channels for registration
            for ( int c = 0; c < settings.numChannels; c++ )
                settings.fixedToMovingChannel.put( c, c );
        }

        return true;
    }

    private void callElastix()
    {
        settings.logService.info( "Running elastix... (please wait)" );

        setParameters();

        List< String > args = createElastixCallArgs();

        Utils.executeCommand( args, settings.logService );

        settings.logService.info( "...done!" );
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
        settings.movingImageBitDepth = movingImageBitDepth;
        settings.parameterFilePath = getDefaultParameterFilePath();

        ElastixParameters parameters = new ElastixParameters( settings );

        final List< String > parameterList;

        System.out.println( "Parameter list type: " + settings.elastixParameters);

        if ( settings.elastixParameters.equals( ElastixSettings.PARAMETERS_HENNING ) )
        {
            parameterList = parameters.getHenningStyleParameters();
        }
        else if ( settings.elastixParameters.equals( ElastixSettings.PARAMETERS_GIULIA ) )
        {
            parameterList = parameters.getGiuliaMizzonStyleParameters();
        }
        else if ( settings.elastixParameters.equals( ElastixSettings.PARAMETERS_DEFAULT ) )
        {
            parameterList = parameters.getDefaultParameters( );
        }
        else
        {
            IJ.error( "Could not generate parameter list." );
            parameterList = null;
        }

        System.out.println( "Number of parameters: " + parameterList.size() );
        System.out.println( "Writing parameter file: " + settings.parameterFilePath  );
        Utils.saveStringListToFile( parameterList, settings.parameterFilePath );
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
        return filenameWithExtension;
    }

    private ArrayList< String > stageImageAsMhd( String imagePath, String filename )
    {
        ImagePlus imp = IJ.openImage( imagePath );

        if ( imp == null )
        {
            System.err.println( "[ERROR] The image could not be loaded: "
                    + imagePath );
            if ( settings.headless )
                System.exit( 1 );
        }

        if ( filename.equals( ELASTIX_MOVING_IMAGE_NAME ) )
            movingImageBitDepth = imp.getBitDepth();

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
        ArrayList< String > fileNames = new ArrayList<>( );

        for ( int channelIndex = 0; channelIndex < imp.getNChannels(); ++channelIndex )
        {
            ImagePlus channelImage = getChannel( imp, channelIndex );

            fileNames.add(
                    stageImagePlusAsMhd(
                        channelImage, getChannelFilename( filename, channelIndex ) ) );
        }

        return fileNames;
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

    private List< String > getTransformixCallArgs( String filenameMoving, String executableShellScript )
    {

        List<String> args = new ArrayList<>();
        args.add( executableShellScript );
        args.add( "-out" );
        args.add( settings.tmpDir );
        args.add( "-in" );
        args.add( getPath( filenameMoving ) );
        args.add( "-tp" );
        args.add( settings.transformationFilePath );
        args.add( "-threads" );
        args.add( "" + settings.numWorkers );

        return args;
    }

    private List< String > createElastixCallArgs( )
    {
        List<String> args = new ArrayList<>();
        args.add( createExecutableShellScript( ELASTIX ) );
        args.add( "-out" );
        args.add( settings.tmpDir );

        addImagesAndMasksToArguments( args );

        args.add( "-p" );
        args.add( settings.parameterFilePath );
        args.add( "-threads" );
        args.add( "" + settings.numWorkers );

        if ( ! settings.initialTransformationFilePath.equals( "" ) )
        {
            args.add( "-t0" );
            args.add( settings.initialTransformationFilePath );
        }

        return args;
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

    private String createExecutableShellScript( String elastixOrTransformix )
    {
        if ( isMac() || isLinux() )
        {
            String executablePath = settings.tmpDir
                    + File.separator + "run_" + elastixOrTransformix + ".sh";

            String binaryPath = settings.elastixDirectory + File.separator + "bin" + File.separator + elastixOrTransformix;

            if( ! new File( binaryPath ).exists() )
                Utils.logErrorAndExit( settings, "Elastix file does not exist: " + binaryPath );

            String shellScriptText = getScriptText( elastixOrTransformix );

            saveStringToFile( shellScriptText, executablePath );

            makeExecutable( executablePath );

            return executablePath;

        }
        else if ( isWindows() )
        {
            setElastixSystemPathForWindowsOS();

            String binaryPath = settings.elastixDirectory + File.separator + elastixOrTransformix + ".exe";

            if ( ! new File( binaryPath ).exists() )
                Utils.logErrorAndExit( settings, "Elastix file does not exist: " + binaryPath );

            return binaryPath;
        }
        else
        {
            Utils.logErrorAndExit( settings, "Could not detect operating system!" );
            return null;
        }

    }

    private String getScriptText( String elastixOrTransformix )
    {
        String shellScriptText = "";
        shellScriptText += "#!/bin/bash\n";
        shellScriptText += "ELASTIX_PATH=" + settings.elastixDirectory + "\n";

        if ( isMac() )
        {
            shellScriptText += "export DYLD_LIBRARY_PATH=$ELASTIX_PATH/lib/\n";
        }
        else if ( isLinux() )
        {
            shellScriptText += "export LD_LIBRARY_PATH=$ELASTIX_PATH/lib/\n";
        }

        shellScriptText += "$ELASTIX_PATH/bin/" + elastixOrTransformix +" $@\n";
        return shellScriptText;
    }

    private void setElastixSystemPathForWindowsOS()
    {
        ProcessBuilder pb = new ProcessBuilder();
        Map<String, String> env = pb.environment();
        env.put( "PATH", settings.elastixDirectory + ":$PATH");
    }

    private void makeExecutable( String executablePath )
    {
        try
        {
            Utils.waitOneSecond();
            Runtime.getRuntime().exec("chmod +x " + executablePath );
            Utils.waitOneSecond();
        }
        catch ( IOException e )
        {
            IJ.log( "Could not make file executable: " + executablePath );
            e.printStackTrace();
        }
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
