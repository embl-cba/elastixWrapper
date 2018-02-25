package de.embl.cba.elastixwrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.embl.cba.elastixwrapper.utils.Utils;
import de.embl.cba.utils.logging.Logger;
import ij.IJ;
import ij.ImagePlus;
import org.scijava.log.LogService;

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

        Utils.saveStringListToFile( ElastixTransformationParameters.getParametersHenningNo5( settings ), getDefaultParameterFilePath() );

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
        prepareAndAddBinaryCall( TRANSFORMIX, args );
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
        prepareAndAddBinaryCall( ELASTIX, args );
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

    private void prepareAndAddBinaryCall( String elastixOrTransformix, List< String > args )
    {

        if ( isMac() || isLinux() )
        {
            String runTransformixPath = settings.elastixDirectory + File.separator + "runTransformix.sh";
            String runTransformixText = "";
            runTransformixText += "#!/bin/bash\n";
            runTransformixText += "ELASTIX_PATH="+settings.elastixDirectory+"\n";

            if ( isMac() )
            {
                runTransformixText += "export DYLD_LIBRARY_PATH=$ELASTIX_PATH/lib/\n";
            }
            else if ( isLinux() )
            {
                runTransformixText += "export LD_LIBRARY_PATH=$ELASTIX_PATH/lib/\n";
            }

            runTransformixText += "$ELASTIX_PATH/bin/" + elastixOrTransformix +" $@\n";
            saveStringToFile( runTransformixText, runTransformixPath );
            args.add( runTransformixPath );
        }
        else if ( isWindows() )
        {
            args.add(settings.elastixDirectory + "transformix.exe"); // command name
        }
        else
        {
            settings.logService.error( "Could not detect operating system!" );
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
