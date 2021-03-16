package de.embl.cba.elastixwrapper.elastix;

import de.embl.cba.elastixwrapper.utils.Utils;
import ij.IJ;
import org.scijava.log.LogService;
import org.scijava.log.StderrLogService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.embl.cba.elastixwrapper.elastix.ElastixWrapper.*;
import static de.embl.cba.elastixwrapper.utils.Utils.saveStringToFile;
import static org.scijava.util.PlatformUtils.*;

public class ElastixCaller {

    LogService logService;
    String elastixDirectory;
    String tmpDir;
    String parameterFilePath;
    String initialTransformationFilePath;
    ArrayList<String> fixedImageFilePaths;
    ArrayList<String> movingImageFilePaths;
    ArrayList<String> fixedMaskFilePaths;
    ArrayList<String> movingMaskFilePaths;
    int numWorkers;

    String executableShellScript;


    public ElastixCaller( ElastixSettings settings ) {
        this.elastixDirectory = settings.elastixDirectory;
        this.tmpDir = settings.tmpDir;
        this.parameterFilePath = settings.parameterFilePath;
        this.logService = settings.logService;

        executableShellScript = createExecutableShellScript( ELASTIX );

    }

    public ElastixCaller( String elastixDirectory, String tmpDir, String parameterFilePath,
                          String initialTransformationFilePath, ArrayList<String> fixedImageFilePaths,
                          ArrayList<String> movingImageFilePaths, ArrayList<String> fixedMaskFilePaths,
                          ArrayList<String> movingMaskFilePaths ) {
        this.elastixDirectory = elastixDirectory;
        this.tmpDir = tmpDir;
        this.parameterFilePath = parameterFilePath;
        this.initialTransformationFilePath = initialTransformationFilePath;
        this.fixedImageFilePaths = fixedImageFilePaths;
        this.movingImageFilePaths = movingImageFilePaths;
        this.fixedMaskFilePaths = fixedMaskFilePaths;
        this.movingMaskFilePaths = movingMaskFilePaths;
        this.logService = new StderrLogService();

        executableShellScript = createExecutableShellScript( ELASTIX )
    }


    public void callElastix()
    {
        logService.info( "Running elastix... (please wait)" );

        // parameters should already be set when we get here

        // setParameters();

        List< String > args = createElastixCallArgs();

        Utils.executeCommand( args, logService );

        logService.info( "...done!" );
    }

    private List< String > createElastixCallArgs( )
    {
        List<String> args = new ArrayList<>();
        args.add( executableShellScript );
        args.add( "-out" );
        args.add( tmpDir );

        addImagesAndMasksToArguments( args );

        args.add( "-p" );
        args.add( parameterFilePath );
        args.add( "-threads" );
        args.add( "" + numWorkers );

        if ( ! initialTransformationFilePath.equals( "" ) )
        {
            args.add( "-t0" );
            args.add( initialTransformationFilePath );
        }

        return args;
    }

    private void addImagesAndMasksToArguments( List< String > args )
    {
        addImagesToArguments( args, FIXED, fixedImageFilePaths );

        addImagesToArguments( args, MOVING, movingImageFilePaths );

        if ( fixedMaskFilePaths != null )
            addImagesToArguments( args, "fMask", fixedMaskFilePaths );

        if ( movingMaskFilePaths != null )
            addImagesToArguments( args, "mMask", movingMaskFilePaths );
    }

    private void addImagesToArguments( List< String > args,
                                       String fixedOrMoving,
                                       ArrayList< String > filePaths )
    {
        int elastixChannelIndex = 0;
        for ( String filePath : filePaths ) {
            if (filePaths.size() == 1)
                args.add("-" + fixedOrMoving);
            else
                args.add("-" + fixedOrMoving + elastixChannelIndex);

            args.add(filePath);

            elastixChannelIndex++;
        }
    }

    private String createExecutableShellScript( String elastixOrTransformix )
    {
        if ( isMac() || isLinux() )
        {
            String executablePath = tmpDir
                    + File.separator + "run_" + elastixOrTransformix + ".sh";

            String binaryPath = elastixDirectory + File.separator + "bin" + File.separator + elastixOrTransformix;

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

            String binaryPath = elastixDirectory + File.separator + elastixOrTransformix + ".exe";

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
        shellScriptText += "ELASTIX_PATH=" + elastixDirectory + "\n";

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
        env.put( "PATH", elastixDirectory + ":$PATH");
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

}
