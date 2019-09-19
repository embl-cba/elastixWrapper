package de.embl.cba.elastixwrapper.commands;

import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
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

    @Parameter( label = "Working directory", style = "directory" )
    public File workingDirectory;

    @Parameter( label = "Image" )
    public File inputImageFile;

    @Parameter( label = "Transformation" )
    public File transformationFile;

    @Parameter( label = "Output modality", choices = {
            ElastixSettings.OUTPUT_MODALITY_SHOW_IMAGES,
            ElastixSettings.OUTPUT_MODALITY_SAVE_AS_TIFF,
            ElastixSettings.OUTPUT_MODALITY_SAVE_AS_BDV
    } )
    public String outputModality;

    @Parameter( label = "Output file", style = "save" )
    public File outputFile;

    @Parameter( label = "Number of threads" )
    int numThreads = 1;

    public void run()
    {
        runTransformix();
    }

    private ElastixWrapper runTransformix()
    {
        ElastixSettings settings = getSettingsFromUI();
        ElastixWrapper elastixWrapper = new ElastixWrapper( settings );
        elastixWrapper.runTransformix();
        return elastixWrapper;
    }

    private ElastixSettings getSettingsFromUI()
    {
        ElastixSettings settings = new ElastixSettings();

        settings.logService = logService;

        settings.elastixDirectory = elastixDirectory.toString();
        settings.workingDirectory = workingDirectory.toString();
        settings.movingImageFilePath = inputImageFile.toString();
        settings.transformationFilePath = transformationFile.toString();
        settings.numWorkers = numThreads;
        settings.outputModality = outputModality;
        settings.outputFile = outputFile;

        return settings;
    }
}
