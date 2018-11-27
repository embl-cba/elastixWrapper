package de.embl.cba.elastixwrapper.commands;

import de.embl.cba.elastixwrapper.elastix.ElastixAndTransformixBinaryRunner;
import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixUtils;
import de.embl.cba.elastixwrapper.metaimage.MetaImage_Reader;
import de.embl.cba.elastixwrapper.utils.CommandUtils;
import ij.ImagePlus;
import ij.Prefs;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Elastix>Apply Transformation (transformix)" )
public class ApplyTransformationUsingTransformix implements Command
{
    public static final String PLUGIN_NAME = "Apply transformation to one image file";

    @Parameter( label = "Elastix directory", style = "directory" )
    public File elastixDirectory;
    public static final String ELASTIX_DIRECTORY = "elastixDirectory";

    @Parameter( label = "Working directory", style = "directory" )
    public File workingDirectory;
    public static final String WORKING_DIRECTORY = "workingDirectory";

    @Parameter( label = "Image" )
    public File inputImageFile;
    public static final String INPUT_IMAGE_FILE = "inputImageFile";

    @Parameter( label = "Transformation" )
    public File transformationFile;
    public static final String TRANSFORMATION_FILE = "transformationFile";

    @Parameter( label = "Output modality", choices = { CommandUtils.OUTPUT_MODALITY_SHOW_IMAGE } )
    public String outputModality;
    public static final String OUTPUT_MODALITY = "outputModality";

    @Parameter
    public LogService logService;

    public void run()
    {
        ElastixSettings settings = runTransformix();
        handleOutput( settings );
    }

    private void handleOutput( ElastixSettings settings )
    {
        if ( outputModality.equals( CommandUtils.OUTPUT_MODALITY_SHOW_IMAGE ) )
        {
            ImagePlus result;

            if ( settings.resultImageFileType.equals( ElastixSettings.RESULT_IMAGE_FILE_TYPE_MHD ) )
            {
                MetaImage_Reader reader = new MetaImage_Reader();
                result = reader.load( settings.workingDirectory, ElastixUtils.DEFAULT_TRANSFORMIX_OUTPUT_FILENAME + "." + settings.resultImageFileType, false );
            }
            else
            {
                result = null;
            }

            if ( outputModality.equals( CommandUtils.OUTPUT_MODALITY_SHOW_IMAGE ) )
            {
                result.show();
            }
        }
    }

    private ElastixSettings runTransformix()
    {
        ElastixSettings settings = getSettingsFromUI();
        ElastixAndTransformixBinaryRunner elastixAndTransformixBinaryRunner = new ElastixAndTransformixBinaryRunner( settings );
        elastixAndTransformixBinaryRunner.runTransformix();
        return settings;
    }

    private ElastixSettings getSettingsFromUI()
    {
        ElastixSettings settings = new ElastixSettings();

        settings.logService = logService;

        settings.elastixDirectory = elastixDirectory.toString();
        settings.workingDirectory = workingDirectory.toString();
        settings.movingImageFilePath = inputImageFile.toString();
        settings.transformationFilePath = transformationFile.toString();

        settings.workers = Prefs.getThreads(); // TODO

        return settings;
    }
}
