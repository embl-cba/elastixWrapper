package de.embl.cba.elastixwrapper.commands;

import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapperSettings;
import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapperSettings.OutputModality;
import de.embl.cba.elastixwrapper.utils.Utils;
import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapper;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Elastix>Transformix" )
public class TransformixCommand implements Command
{

    @Parameter
    public LogService logService;

    @Parameter( label = "Elastix installation directory",
            style = "directory" )
    public File elastixDirectory;

    @Parameter( label = "Temporary directory for intermediate files", style = "directory" )
    public File tmpDir = new File( System.getProperty("java.io.tmpdir") );

    @Parameter( label = "Image" )
    public File inputImageFile;

    @Parameter( label = "Transformation" )
    public File transformationFile;

    @Parameter( label = "Output modality", choices = {
            TransformixWrapperSettings.OUTPUT_MODALITY_SHOW_IMAGES,
            TransformixWrapperSettings.OUTPUT_MODALITY_SAVE_AS_TIFF,
            TransformixWrapperSettings.OUTPUT_MODALITY_SAVE_AS_BDV
    } )
    public String outputModality;

    @Parameter( label = "Output file", style = "save", required = false )
    public File outputFile;

    @Parameter( label = "Number of threads" )
    int numThreads = 1;

    public void run()
    {
        runTransformix();
    }

    private void runTransformix()
    {
        TransformixWrapperSettings settings = getSettingsFromUI();
        TransformixWrapper transformixWrapper = new TransformixWrapper( settings );
        transformixWrapper.runTransformix();
    }

    private TransformixWrapperSettings getSettingsFromUI()
    {
        TransformixWrapperSettings settings = new TransformixWrapperSettings();
        settings.logService = logService;
        settings.elastixDirectory = elastixDirectory.toString();
        settings.tmpDir = tmpDir.toString();
        settings.movingImageFilePath = inputImageFile.toString();
        settings.transformationFilePath = transformationFile.toString();
        settings.numWorkers = numThreads;

        switch ( outputModality ) {
            case TransformixWrapperSettings.OUTPUT_MODALITY_SHOW_IMAGES:
                settings.outputModality = OutputModality.Show_images;
                break;
            case TransformixWrapperSettings.OUTPUT_MODALITY_SAVE_AS_TIFF:
                settings.outputModality = OutputModality.Save_as_tiff;
                break;
            case TransformixWrapperSettings.OUTPUT_MODALITY_SAVE_AS_BDV:
                settings.outputModality = OutputModality.Save_as_bdv;
                break;
        }

        settings.outputFile = outputFile;

        if ( !settings.outputModality.equals( OutputModality.Show_images ) )
            if ( outputFile == null )
                Utils.logErrorAndExit( settings,"Please specify an output file.");

        return settings;
    }
}
