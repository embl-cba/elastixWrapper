package de.embl.cba.elastixwrapper.elastix;

import de.embl.cba.elastixwrapper.metaimage.MetaImage_Reader;
import de.embl.cba.elastixwrapper.metaimage.MetaImage_Writer;
import de.embl.cba.elastixwrapper.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.FileSaver;
import ij.plugin.Duplicator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.embl.cba.elastixwrapper.elastix.ElastixUtils.DEFAULT_TRANSFORMIX_OUTPUT_FILENAME;
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

    public static final String MHD_SUFFIX = ".mhd";
    public static final String DEFAULT_TRANSFORMIX_INPUT_IMAGE_NAME = "to_be_transformed";
    public static final String FIXED = "f";
    public static final String MOVING = "m";

    ElastixSettings settings;

    private ArrayList< String > fixedImageFilenames;
    private ArrayList< String > movingImageFilenames;
    private ArrayList< String > fixedMaskFilenames;
    private ArrayList< String > movingMaskFilenames;

    private int movingImageBitDepth;


    public ElastixWrapper( ElastixSettings settings )
    {
        this.settings = settings;
    }

    public void runElastix()
    {
        processSettings();

        createOrEmptyWorkingDir();

        if ( ! stageImages() ) return;

        callElastix();
    }

    private void processSettings()
    {
        if ( ! settings.elastixDirectory.endsWith( File.separator ) )
            settings.elastixDirectory += File.separator;

        if ( ! settings.workingDirectory.endsWith( File.separator ) )
            settings.workingDirectory += File.separator;
    }

    public void runTransformix()
    {
        createOrEmptyWorkingDir();

        ArrayList< String > fileNames = stageImageAsMhd( settings.movingImageFilePath, DEFAULT_TRANSFORMIX_INPUT_IMAGE_NAME );

        String executableShellScript = createExecutableShellScript( TRANSFORMIX );

        List< String > transformixCallArgs = getTransformixCallArgs( fileNames.get( 0 ), executableShellScript );

        Utils.executeCommand( transformixCallArgs, settings.logService );

    }

    public void showTransformationFile()
    {
        IJ.open( settings.workingDirectory + "TransformParameters.0.txt");
    }


    public void showInputImage( )
    {
        ImagePlus fixed;

        fixed = IJ.openImage( settings.fixedImageFilePath );

        fixed.show();

        fixed.setTitle( "fixed" );

        if ( fixed.getNChannels() > 1 ) IJ.run("Split Channels" );
    }

    private void mergeAndShowOutputChannels()
    {

        // TODO: does not work

        if ( settings.numChannels > 1 )
        {
            String mergeCmd = "";

            for ( int c = 1; c <= settings.numChannels; ++c )
            {
                mergeCmd += "c" + c + "=" + createTransformedImageTitle( c ) + " ";
            }

            mergeCmd += "create";

            IJ.run( "Merge Channels...", mergeCmd );

            IJ.getImage().setTitle( "result" );
        }
    }

    public void createTransformedImagesAndSaveAsTiff()
    {
        settings.transformationFilePath =
                settings.workingDirectory + File.separator + "TransformParameters.0.txt";

        String executableShellScript = createExecutableShellScript( TRANSFORMIX );

        for ( int c = 1; c <= settings.numChannels; ++c )
        {
            List< String > transformixCallArgs =
                    getTransformixCallArgs(
                            movingImageFilenames.get( c - 1 ), executableShellScript );
            Utils.executeCommand( transformixCallArgs, settings.logService );
            ImagePlus result = loadResultImage(
                    settings.workingDirectory,
                    DEFAULT_TRANSFORMIX_OUTPUT_FILENAME,
                    settings.resultImageFileType );

            final String path = settings.workingDirectory
                    + File.separator
                    + createTransformedImageTitle( c ) + ".tif";

            settings.logService.info( "\nSaving transformed image: " + path );

            new FileSaver( result ).saveAsTiff( path );
        }
    }

    public ArrayList< ImagePlus > openTransformedImages()
    {
        ArrayList< ImagePlus > transformedImages = new ArrayList<>(  );

        for ( int c = 1; c <= settings.numChannels; ++c )
        {
            final ImagePlus imagePlus = IJ.openImage( settings.workingDirectory
                    + File.separator + createTransformedImageTitle( c ) + ".tif" );

            transformedImages.add( imagePlus );
        }

        return transformedImages;
    }


    private String createTransformedImageTitle( int channel )
    {
        return "C" + channel + "-moving-aligned";
    }


    public ImagePlus loadResultImage(
            String directory, String filename, String fileType )
    {
        MetaImage_Reader reader = new MetaImage_Reader();
        return reader.load( directory,  filename + "." + fileType, false );
    }

    private boolean stageImages()
    {
        fixedImageFilenames = stageImageAsMhd(
                settings.fixedImageFilePath,
                ELASTIX_FIXED_IMAGE_NAME );

        movingImageFilenames = stageImageAsMhd(
                settings.movingImageFilePath,
                ELASTIX_MOVING_IMAGE_NAME );

        if ( ! settings.fixedMaskPath.equals( "" ) )
            fixedMaskFilenames = stageImageAsMhd(
                    settings.fixedMaskPath,
                    ELASTIX_FIXED_MASK_IMAGE_NAME );

        if ( ! settings.movingMaskPath.equals( "" ) )
            movingMaskFilenames = stageImageAsMhd(
                    settings.movingMaskPath,
                    ELASTIX_MOVING_MASK_IMAGE_NAME );

        if ( ! checkChannelNumber( fixedImageFilenames.size(), movingImageFilenames.size() ) )
            return false;

        settings.numChannels = fixedImageFilenames.size();

        return true;
    }

    private void callElastix()
    {
        setParameters();

        List< String > args = createElastixCallArgs();

        Utils.executeCommand( args, settings.logService );
    }

    private boolean checkChannelNumber( int nChannelsFixedImage, int nChannelsMovingImage )
    {

        if ( nChannelsFixedImage != nChannelsMovingImage )
        {
            settings.logService.error( "Number of channels " +
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

        if ( settings.elastixParameters.equals( ElastixSettings.PARAMETERS_HENNING ) )
        {
            Utils.saveStringListToFile( parameters.getHenningStyleParameters( ), settings.parameterFilePath );
        }
        else if ( settings.elastixParameters.equals( ElastixSettings.PARAMETERS_GIULIA ) )
        {
            Utils.saveStringListToFile( parameters.getGiuliaMizzonStyleParameters(), settings.parameterFilePath );
        }
        else if ( settings.elastixParameters.equals( ElastixSettings.PARAMETERS_DEFAULT ) )
        {
            Utils.saveStringListToFile( parameters.getDetlevStyleParameters( ), settings.parameterFilePath );
        }


    }


    private String getDefaultParameterFilePath()
    {
        return settings.workingDirectory + File.separator + "elastix_parameters.txt";
    }

    private String stageImagePlusAsMhd( ImagePlus imp, String filename )
    {
        MetaImage_Writer writer = new MetaImage_Writer();
        String filenameWithExtension = filename + MHD_SUFFIX;
        writer.save( imp, settings.workingDirectory, filenameWithExtension );
        return filenameWithExtension;
    }

    private ArrayList< String > stageImageAsMhd( String imagePath, String filename )
    {
        ImagePlus imp = IJ.openImage( imagePath );

        if ( imp == null )
        {
            settings.logService.error( "Could not open image file: " + imagePath );
        }

        if ( filename.equals( ELASTIX_MOVING_IMAGE_NAME ) )
        {
            movingImageBitDepth = imp.getBitDepth();
        }

        if ( imp.getNChannels() > 1 )
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

    public static void convertToMask( ImagePlus imp )
    {
        IJ.setRawThreshold( imp, 0.5, Double.MAX_VALUE, null );
        Prefs.blackBackground = true;
        IJ.run( imp, "Convert to Mask", "method=Default background=Dark black" );
        IJ.run( imp, "Divide...", "value=255 stack" );
        IJ.wait( 100 );
    }

    private ArrayList< String > stageMultiChannelImagePlusAsMhd( ImagePlus imp, String filename )
    {
        ArrayList< String > filenames = new ArrayList<>( );

        for ( int channel = 1; channel <= imp.getNChannels(); ++channel )
        {
            Duplicator duplicator = new Duplicator();

            ImagePlus channelImage = duplicator.run(
                    imp,
                    channel,
                    channel,
                    1,
                    imp.getNSlices(),
                    1,
                    1 );

            if ( filename.equals( ELASTIX_FIXED_MASK_IMAGE_NAME ) )
            {
                convertToMask( channelImage );
            }

            filenames.add( stageImagePlusAsMhd( channelImage, filename + "-C" + channel ) );
        }

        return filenames;
    }

    private List< String > getTransformixCallArgs( String filenameMoving, String executableShellScript )
    {

        List<String> args = new ArrayList<>();
        args.add( executableShellScript );
        args.add( "-out" );
        args.add( settings.workingDirectory );
        args.add( "-in" );
        args.add( settings.workingDirectory + File.separator + filenameMoving );
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
        args.add( settings.workingDirectory );

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
        addImages( args, FIXED, fixedImageFilenames );

        addImages( args, MOVING, movingImageFilenames );

        if ( fixedMaskFilenames != null )
            addImages( args, "fMask", fixedMaskFilenames );

        if ( movingMaskFilenames != null )
            addImages( args, "mMask", movingMaskFilenames );
    }

    private void addImages( List< String > args,
                            String fixedOrMoving,
                            ArrayList< String > filenames )
    {

        if ( settings.fixedToMovingChannel.size() == 0 )
            for ( int c = 0; c < filenames.size(); c++ )
                settings.fixedToMovingChannel.put( c, c );

        int elastixChannelIndex = 0;
        for ( int fixedChannelIndex :  settings.fixedToMovingChannel.keySet() )
        {
            if (  settings.fixedToMovingChannel.size() == 1 )
                args.add( "-" + fixedOrMoving );
            else
                args.add( "-" + fixedOrMoving + elastixChannelIndex );

            final String filename = getFileName(
                    fixedOrMoving, filenames, fixedChannelIndex );

            args.add( settings.workingDirectory + File.separator + filename );

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
            String executablePath = settings.workingDirectory
                    + File.separator + "run_" + elastixOrTransformix + ".sh";

            String shellScriptText = getScriptText( elastixOrTransformix );

            saveStringToFile( shellScriptText, executablePath );

            makeExecutable( executablePath );

            return executablePath;

        }
        else if ( isWindows() )
        {
            setElastixSystemPathForWindowsOS();

            String binaryPath = settings.elastixDirectory + File.separator + elastixOrTransformix + ".exe";

            if( ! new File( binaryPath ).exists() )
            {
                IJ.showMessage( "Elastix file does not exist: " +  binaryPath );
            }

            return binaryPath;
        }
        else
        {
            settings.logService.error( "Could not detect operating system!" );
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
        createOrEmptyDir( settings.workingDirectory );
    }


    private static void createOrEmptyDir( String directoryString )
    {
        File directory = new File( directoryString );

        if (! directory.exists() )
        {
            directory.mkdir();
        }
        else
        {
            for( File file : directory.listFiles() )
            {
                if ( !file.isDirectory() )
                {
                    file.delete();
                }
            }
        }
    }


}
