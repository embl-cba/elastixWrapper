package de.embl.cba.elastixwrapper.transformix;

import de.embl.cba.elastixwrapper.settings.TransformixSettings;
import de.embl.cba.elastixwrapper.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static de.embl.cba.elastixwrapper.utils.CommandLineUtils.createExecutableShellScript;

public class TransformixCaller {

    public static final String TRANSFORMIX = "transformix";
    TransformixSettings settings;
    String executableShellScript;

    public TransformixCaller( TransformixSettings settings ) {
        this.settings = settings;
        executableShellScript = createExecutableShellScript( TRANSFORMIX, settings );
    }

    public void callTransformix()
    {
        settings.logService.info( "Running transformix... (please wait)" );

        List< String > args = createTransformixCallArgs();

        Utils.executeCommand( args, settings.logService );

        settings.logService.info( "...done!" );
    }

    private List< String > createTransformixCallArgs()
    {

        List<String> args = new ArrayList<>();
        args.add( executableShellScript );
        args.add( "-out" );
        args.add( settings.tmpDir );
        args.add( "-in" );
        args.add( settings.movingImageFilePath );
        args.add( "-tp" );
        args.add( settings.transformationFilePath );
        args.add( "-threads" );
        args.add( "" + settings.numWorkers );

        return args;
    }
}
