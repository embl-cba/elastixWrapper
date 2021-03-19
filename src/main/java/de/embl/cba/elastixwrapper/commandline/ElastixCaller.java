package de.embl.cba.elastixwrapper.commandline;

import de.embl.cba.elastixwrapper.commandline.settings.ElastixSettings;
import de.embl.cba.elastixwrapper.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ElastixCaller {

    public static final String ELASTIX = "elastix";
    public static final String FIXED = "f";
    public static final String MOVING = "m";

    ElastixSettings settings;
    String executableShellScript;

    public ElastixCaller( ElastixSettings settings ) {
        this.settings = settings;
        executableShellScript = new ExecutableShellScriptCreator( ELASTIX, settings ).createExecutableShellScript();
    }

    public void callElastix()
    {
        settings.logService.info( "Running elastix... (please wait)" );

        // parameters should already be set when we get here

        // setParameters();

        List< String > args = createElastixCallArgs();

        Utils.executeCommand( args, settings.logService );

        settings.logService.info( "...done!" );
    }

    private List< String > createElastixCallArgs( )
    {
        List<String> args = new ArrayList<>();
        args.add( executableShellScript );
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
        addImagesToArguments( args, FIXED, settings.fixedImageFilePaths );

        addImagesToArguments( args, MOVING, settings.movingImageFilePaths );

        if ( settings.fixedMaskFilePaths != null )
            addImagesToArguments( args, "fMask", settings.fixedMaskFilePaths );

        if ( settings.movingMaskFilePaths != null )
            addImagesToArguments( args, "mMask", settings.movingMaskFilePaths );
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



}
