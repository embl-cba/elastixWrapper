package de.embl.cba.elastixwrapper.commands;

import de.embl.cba.bdv.utils.io.BdvWriter;
import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixUtils;
import de.embl.cba.elastixwrapper.metaimage.MetaImage_Reader;
import de.embl.cba.elastixwrapper.utils.CommandUtils;
import ij.ImagePlus;
import ij.io.FileSaver;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;

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
            CommandUtils.OUTPUT_MODALITY_SHOW_IMAGE,
            CommandUtils.OUTPUT_MODALITY_SAVE_AS_TIFF,
            CommandUtils.OUTPUT_MODALITY_SAVE_AS_BDV
    } )
    public String outputModality;

    @Parameter( label = "Output file (without extension!)", style = "save" )
    public File outputFile;

    @Parameter( label = "Number of threads" )
    int numThreads = 1;

    public void run()
    {
        ElastixWrapper wrapper = runTransformix();
        handleOutput( wrapper );
    }

    private void handleOutput( ElastixWrapper wrapper )
    {
        ArrayList< ImagePlus > results = wrapper.getTransformedImages();

        for ( int c = 0; c < results.size(); ++c )
        {
            ImagePlus result = results.get( c );

            if ( outputModality.equals( CommandUtils.OUTPUT_MODALITY_SHOW_IMAGE ) )
            {
                result.show();
                result.setTitle( "transformed-ch" + c );
            }
            else if ( outputModality.equals(
                    CommandUtils.OUTPUT_MODALITY_SAVE_AS_TIFF ) )
            {
                new FileSaver( result ).saveAsTiff( outputFile.toString() + "-ch" + c + ".tif" );
            }
            else if ( outputModality.equals(
                    CommandUtils.OUTPUT_MODALITY_SAVE_AS_BDV ) )
            {
                BdvWriter.saveAsBdv( result, new File( outputFile.toString() + "-ch" + c + ".xml" ) );
            }
        }
    }


    private ImagePlus openResultImage( ElastixSettings settings )
    {
        ImagePlus result;

        if ( settings.resultImageFileType.equals(
                ElastixSettings.STAGING_FILE_TYPE ) )
        {
            MetaImage_Reader reader = new MetaImage_Reader();
            result = reader.load(
                    settings.workingDirectory,
                    ElastixUtils.DEFAULT_TRANSFORMIX_OUTPUT_FILENAME
                            + "." + settings.resultImageFileType,
                    false );
        }
        else
        {
            result = null;
        }

        return result;
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

        return settings;
    }
}
