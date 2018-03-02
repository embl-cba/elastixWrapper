package de.embl.cba.elastixwrapper.elastix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.embl.cba.elastixwrapper.metaimage.MetaImage_Writer;
import de.embl.cba.elastixwrapper.utils.Utils;
import ij.IJ;
import ij.ImagePlus;

import static de.embl.cba.elastixwrapper.utils.Utils.saveStringToFile;
import static org.scijava.util.PlatformUtils.isLinux;
import static org.scijava.util.PlatformUtils.isMac;
import static org.scijava.util.PlatformUtils.isWindows;

public class ElastixBinaryRunner
{
    public static String ELASTIX = "elastix";
    public static String TRANSFORMIX = "transformix";
    public static String TRANSFORMIX_INPUT_IMAGE_NAME = "toBeTransformed";

    ElastixSettings settings;

    public ElastixBinaryRunner( ElastixSettings settings )
    {
        this.settings = settings;
    }

    public void runElastix()
    {
        createOrEmptyWorkingDir();

        stageImageAsMhd( settings.fixedImageFilePath );

        stageImageAsMhd( settings.movingImageFilePath );

        settings.parameterFilePath = getDefaultParameterFilePath();

        Utils.saveStringListToFile( ElastixTransformationParameters.getParametersHenningNo5( settings ), settings.parameterFilePath );

        setElastixSystemPathForWindowsOS();

        List< String > args = getElastixCallArgs();

        Utils.executeCommand( args, settings.logService );

    }
    public void runTransformix()
    {
        createOrEmptyWorkingDir();

        stageImageAsMhd( settings.movingImageFilePath );

        setElastixSystemPathForWindowsOS();

        List< String > args = getTransformixCallArgs();

        Utils.executeCommand( args, settings.logService );

    }

    private String getDefaultParameterFilePath()
    {
        return settings.workingDirectory + File.separator + "elastix_parameters.txt";
    }


    private void stageImagePlusAsMhd( ImagePlus imp, String workingDirectory, String filename )
    {
        MetaImage_Writer writer = new MetaImage_Writer();
        writer.save( imp, workingDirectory, filename + ".mhd" );
    }

    private void stageImageAsMhd( String imagePath )
    {
        ImagePlus imp = IJ.openImage( imagePath );
        stageImagePlusAsMhd( imp, settings.workingDirectory, TRANSFORMIX_INPUT_IMAGE_NAME );
    }

    private List< String > getTransformixCallArgs( )
    {
        List<String> args = new ArrayList<>();
        args.add( createExecutableShellScript( TRANSFORMIX ) );
        args.add( "-out" );
        args.add( settings.workingDirectory );
        args.add( "-in" );
        args.add( settings.movingImageFilePath );
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
        args.add( "-f" );
        args.add( settings.fixedImageFilePath );
        args.add( "-m" );
        args.add( settings.movingImageFilePath );
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

    private void setElastixSystemPathForWindowsOS()
    {
        if ( isWindows() )
        {
            ProcessBuilder pb = new ProcessBuilder();
            Map<String, String> env = pb.environment();
            env.put( "PATH", settings.elastixDirectory + ":$PATH");
        }
    }

    private String createExecutableShellScript( String elastixOrTransformix )
    {

        if ( isMac() || isLinux() )
        {
            String executablePath = settings.workingDirectory + File.separator + "run_" + elastixOrTransformix + ".sh";
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
            saveStringToFile( shellScriptText, executablePath );

            makeExecutable( executablePath );

            return executablePath;

        }
        else if ( isWindows() )
        {
            return settings.elastixDirectory + elastixOrTransformix + ".exe";
        }
        else
        {
            settings.logService.error( "Could not detect operating system!" );

            return null;
        }

    }

    private void makeExecutable( String executablePath )
    {
        try
        {
            Runtime.getRuntime().exec("chmod u+x " + executablePath );
        }
        catch ( IOException e )
        {
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
                if ( !file.isDirectory() )
                    file.delete();
        }
    }


}
