package de.embl.cba.elastixwrapper.elastix;

import de.embl.cba.elastixwrapper.utils.Utils;
import org.scijava.log.LogService;

import java.util.ArrayList;
import java.util.List;

import static de.embl.cba.elastixwrapper.elastix.ElastixWrapper.TRANSFORMIX;

public class TransformixCaller {

    LogService logService;
    String elastixDirectory;
    String tmpDir;
    String transformationFilePath;
    String movingImageFilePath;
    int numWorkers;

    String executableShellScript;

    public TransformixCaller() {
        executableShellScript = createExecutableShellScript( TRANSFORMIX );
    }

    public void callTransformix()
    {
        logService.info( "Running transformix... (please wait)" );

        List< String > args = createTransformixCallArgs();

        Utils.executeCommand( args, logService );

        logService.info( "...done!" );
    }

    private List< String > createTransformixCallArgs()
    {

        List<String> args = new ArrayList<>();
        args.add( executableShellScript );
        args.add( "-out" );
        args.add( tmpDir );
        args.add( "-in" );
        args.add( movingImageFilePath );
        args.add( "-tp" );
        args.add( transformationFilePath );
        args.add( "-threads" );
        args.add( "" + numWorkers );

        return args;
    }
}
