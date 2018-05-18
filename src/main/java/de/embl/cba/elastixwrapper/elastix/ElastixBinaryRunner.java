package de.embl.cba.elastixwrapper.elastix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.embl.cba.elastixwrapper.metaimage.MetaImage_Reader;
import de.embl.cba.elastixwrapper.metaimage.MetaImage_Writer;
import de.embl.cba.elastixwrapper.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;

import static de.embl.cba.elastixwrapper.utils.Utils.saveStringToFile;
import static org.scijava.util.PlatformUtils.isLinux;
import static org.scijava.util.PlatformUtils.isMac;
import static org.scijava.util.PlatformUtils.isWindows;

public class ElastixBinaryRunner
{
    public static String ELASTIX = "elastix";
    public static String TRANSFORMIX = "transformix";
    public static String ELASTIX_FIXED_IMAGE_NAME = "fixed";
    public static String ELASTIX_MOVING_IMAGE_NAME = "moving";
    public static String MHD_SUFFIX = ".mhd";
    public static String DEFAULT_TRANSFORMIX_INPUT_IMAGE_NAME = "to_be_transformed";

    ElastixSettings settings;

    private ArrayList< String > fixedImageFilenames;
    private ArrayList< String > movingImageFilenames;

    public ElastixBinaryRunner( ElastixSettings settings )
    {
        this.settings = settings;
    }

    public void runElastix()
    {

        createOrEmptyWorkingDir();

        if ( stageImages() ) return;

        callElastix();

        callTransformix();

        mergeOutputChannels();

    }

    private void mergeOutputChannels()
    {
        if ( settings.numChannels > 1 )
        {
            String mergeCmd = "";

            for ( int c = 1; c <= settings.numChannels; ++c )
            {
                mergeCmd += "c" + c + "=" + getResultImageTitle( c ) + " ";
            }
            mergeCmd += "create";

            IJ.run( "Merge Channels...", mergeCmd );

            IJ.getImage().setTitle( "result" );
        }
    }

    private void callTransformix()
    {
        settings.transformationFilePath = settings.workingDirectory + File.separator + "TransformParameters.0.txt";

        String executableShellScript = createExecutableShellScript( TRANSFORMIX );

        for ( int c = 1; c <= settings.numChannels; ++c )
        {
            List< String > transformixCallArgs = getTransformixCallArgs( movingImageFilenames.get( c - 1 ), executableShellScript );
            Utils.executeCommand( transformixCallArgs, settings.logService );
            String imageTitle = getResultImageTitle( c );
            showMhd( ElastixUtils.DEFAULT_TRANSFORMIX_OUTPUT_FILENAME, imageTitle );
        }
    }

    private String getResultImageTitle( int channel )
    {
        return "C" + channel + "-result";
    }

    private void showMhd( String filename, String imageTitle )
    {
        MetaImage_Reader reader = new MetaImage_Reader();
        ImagePlus result = reader.load( settings.workingDirectory,  filename + "." + settings.resultImageFileType, false );
        result.show();
        result.setTitle( imageTitle );
    }

    private boolean stageImages()
    {
        fixedImageFilenames = stageImageAsMhd( settings.fixedImageFilePath, ELASTIX_FIXED_IMAGE_NAME );

        movingImageFilenames = stageImageAsMhd( settings.movingImageFilePath, ELASTIX_MOVING_IMAGE_NAME );

        if ( ! checkChannelNumber( fixedImageFilenames.size(), movingImageFilenames.size() ) ) return true;

        settings.numChannels = fixedImageFilenames.size();
        return false;
    }

    private void callElastix()
    {
        setParameters();

        List< String > args = getElastixCallArgs();

        Utils.executeCommand( args, settings.logService );
    }

    private boolean checkChannelNumber( int nChannelsFixedImage, int nChannelsMovingImage )
    {
        if ( nChannelsFixedImage != nChannelsMovingImage )
        {
            settings.logService.error( "Number of channels in fixed and moving image do not match." );
            return false;
        }
        return true;
    }

    private void setParameters()
    {
        settings.parameterFilePath = getDefaultParameterFilePath();

        ElastixTransformationParameters parameters = new ElastixTransformationParameters( settings );

        if ( settings.elastixParameters.equals( ElastixSettings.PARAMETERS_HENNING ) )
        {
            Utils.saveStringListToFile( parameters.getHenningStyle( ), settings.parameterFilePath );
        }
        else if ( settings.elastixParameters.equals( ElastixSettings.PARAMETERS_DETLEV ) )
        {
            Utils.saveStringListToFile( parameters.getDetlevStyle( ), settings.parameterFilePath );
        }
    }

    public void runTransformix()
    {
        createOrEmptyWorkingDir();

        ArrayList< String > fileNames = stageImageAsMhd( settings.movingImageFilePath, DEFAULT_TRANSFORMIX_INPUT_IMAGE_NAME );

        String executableShellScript = createExecutableShellScript( TRANSFORMIX );

        List< String > transformixCallArgs = getTransformixCallArgs( fileNames.get( 0 ), executableShellScript );

        Utils.executeCommand( transformixCallArgs, settings.logService );

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

    private ArrayList< String > stageMultiChannelImagePlusAsMhd( ImagePlus imp, String filename )
    {
        ArrayList< String > filenames = new ArrayList<>( );

        for ( int channel = 1; channel <= imp.getNChannels(); ++channel )
        {
            Duplicator duplicator = new Duplicator();
            ImagePlus channelImage = duplicator.run( imp, channel, channel, 1 ,imp.getNSlices(), 1, 1 );
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
        args.add( "" + settings.workers );

        return args;
    }

    private List< String > getElastixCallArgs( )
    {
        List<String> args = new ArrayList<>();
        args.add( createExecutableShellScript( ELASTIX ) );
        args.add( "-out" );
        args.add( settings.workingDirectory );

        addFixedAndMovingImages( args );

        args.add( "-p" );
        args.add( settings.parameterFilePath );
        args.add( "-threads" );
        args.add( "" + settings.workers );

        if ( ! settings.initialTransformationFilePath.equals( "" ) )
        {
            args.add( "-t0" );
            args.add( settings.initialTransformationFilePath );
        }

        if ( ! settings.maskImageFilePath.equals( "" ) )
        {
            args.add( "-fMask" );
            args.add( settings.maskImageFilePath );
        }

        return args;
    }

    private void addFixedAndMovingImages( List< String > args )
    {
        addImages( args, "f", fixedImageFilenames );
        addImages( args, "m", movingImageFilenames );
    }

    private void addImages( List< String > args, String fixedOrMoving, ArrayList< String > filenames )
    {
        for ( int c = 0; c < settings.numChannels; ++c )
        {
            if ( settings.numChannels == 1 )
            {
                args.add( "-" + fixedOrMoving );
            }
            else
            {
                args.add( "-" + fixedOrMoving + c );
            }

            args.add( settings.workingDirectory + File.separator + filenames.get( c ) );
        }
    }

    private String createExecutableShellScript( String elastixOrTransformix )
    {

        if ( isMac() || isLinux() )
        {
            String executablePath = settings.workingDirectory + File.separator + "run_" + elastixOrTransformix + ".sh";

            String shellScriptText = getScriptText( elastixOrTransformix );

            saveStringToFile( shellScriptText, executablePath );

            makeExecutable( executablePath );

            return executablePath;

        }
        else if ( isWindows() )
        {
            setElastixSystemPathForWindowsOS();
            return settings.elastixDirectory + elastixOrTransformix + ".exe";
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
