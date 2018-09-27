package de.embl.cba.elastixwrapper.commands;

import de.embl.cba.elastixwrapper.elastix.ElastixBinaryRunner;
import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import ij.Prefs;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Elastix>Compute Transformation (elastix)" )
public class FindTransformationUsingElastixCommand implements Command
{
    public static final String PLUGIN_NAME = "Register two image files";

    @Parameter( label = "Elastix directory", style = "directory" )
    public File elastixDirectory;

    @Parameter( label = "Working directory", style = "directory" )
    public File workingDirectory;

    @Parameter( label = "Fixed image" )
    public File fixedImageFile;

    @Parameter( label = "Moving image" )
    public File movingImageFile;

    @Parameter( label = "Elastix parameters", choices =
            {
                    ElastixSettings.PARAMETERS_HENNING,
                    ElastixSettings.PARAMETERS_DETLEV,
                    ElastixSettings.PARAMETERS_GIULIA
            })

    public String elastixParameters;

    @Parameter( label = "Use fixed image mask" )
    public boolean useMask;

    @Parameter( label = "Fixed image mask file", required = false )
    public File maskFile;

    @Parameter( label = "Use initial transformation" )
    public boolean useInitialTransformation;

    @Parameter( label = "Initial transformation file", required = false )
    public File initialTransformationFile;

    @Parameter( label = "Transformation type", choices = {
            ElastixSettings.TRANSLATION,
            ElastixSettings.EULER,
            ElastixSettings.SIMILARITY,
            ElastixSettings.AFFINE,
            ElastixSettings.SPLINE } )

    public String transformationType;

    @Parameter( label = "Number of iterations" )
    public int numIterations = 1000;

    @Parameter( label = "Number of spatial samples" )
    public String numSpatialSamples = "10000;10000";

    @Parameter( label = "Resolution pyramid" )
    public String resolutionPyramid = "10,10,10;1,1,1";

    @Parameter( label = "BSpline grid spacing [voxels]", required = false )
    public String bSplineGridSpacing = "50,50,50";

//    @Parameter( label = "Output modality", choices = {
//            CommandUtils.OUTPUT_MODALITY_SHOW_AS_INDIVIDUAL_IMAGES,
//            CommandUtils.OUTPUT_MODALITY_DO_NOT_SHOW_IMAGES
//    } )
//    public String outputModality;

    @Parameter
    public LogService logService;

    @Parameter
    public ThreadService threadService;

    public void run()
    {
        runElastix( );
    }

    private void runElastix( )
    {
        ElastixSettings settings = getSettingsFromUI();
        ElastixBinaryRunner elastixBinaryRunner = new ElastixBinaryRunner( settings );
        elastixBinaryRunner.run();
    }


    private ElastixSettings getSettingsFromUI()
    {
        ElastixSettings settings = new ElastixSettings();

        settings.logService = logService;

        settings.workingDirectory = workingDirectory.toString();

        if ( ! settings.workingDirectory.endsWith( "/" ) )
        {
            settings.workingDirectory += "/";
        }

        settings.elastixDirectory = elastixDirectory.toString();

        if ( useInitialTransformation )
        {
            settings.initialTransformationFilePath = initialTransformationFile.toString();
        }
        else
        {
            settings.initialTransformationFilePath = "";
        }

//        settings.outputModality = outputModality;

        settings.elastixParameters = elastixParameters;

        if ( useMask )
        {
            settings.maskImageFilePath = maskFile.toString();
        }
        else
        {
            settings.maskImageFilePath = "";
        }

        settings.fixedImageFilePath = fixedImageFile.toString();
        settings.movingImageFilePath = movingImageFile.toString();

        settings.transformationType = transformationType;
        settings.iterations = numIterations;
        settings.spatialSamples = numSpatialSamples;
        settings.workers = Prefs.getThreads(); // TODO
        settings.resolutionPyramid = resolutionPyramid;
        settings.bSplineGridSpacing = bSplineGridSpacing;

        // TODO: make this a UI
        settings.channelWeights = new double[]{1.0, 1.0, 1.0, 1.0, 1.0};

        return settings;
    }


}

