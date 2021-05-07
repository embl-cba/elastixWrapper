package de.embl.cba.elastixwrapper.commandline;

import de.embl.cba.elastixwrapper.commandline.settings.Settings;
import de.embl.cba.elastixwrapper.utils.Utils;
import ij.IJ;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static de.embl.cba.elastixwrapper.utils.Utils.saveStringToFile;
import static org.scijava.util.PlatformUtils.*;

public class ExecutableShellScriptCreator {

    String elastixOrTransformix;
    Settings settings;

    public ExecutableShellScriptCreator( String elastixOrTransformix, Settings settings) {
        this.elastixOrTransformix = elastixOrTransformix;
        this.settings = settings;
    }

    public String createExecutableShellScript()
    {
        if ( isMac() || isLinux() )
        {
            String executablePath = settings.tmpDir
                    + File.separator + "run_" + elastixOrTransformix + ".sh";

            String binaryPath = settings.elastixDirectory + File.separator + "bin" + File.separator + elastixOrTransformix;

            if( ! new File( binaryPath ).exists() )
                Utils.logErrorAndExit( settings, "Elastix file does not exist: " + binaryPath );

            String shellScriptText = getScriptText();

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

    private String getScriptText()
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
}
