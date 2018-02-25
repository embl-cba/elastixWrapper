package de.embl.cba.elastixwrapper.commands;

import de.embl.cba.elastixwrapper.ElastixBinaryRunner;
import de.embl.cba.elastixwrapper.ElastixSettings;
import ij.Prefs;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import java.io.File;

import static de.embl.cba.elastixwrapper.commands.ApplyTransformationUsingTransformix.PLUGIN_NAME;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Elastix>" + PLUGIN_NAME )
public class FindTransformationUsingElastix implements Command
{
    public static final String PLUGIN_NAME = "Find transformation";

    @Parameter( label = "Elastix directory" )
    public File elastixDirectory;
    public static final String ELASTIX_DIRECTORY = "elastixDirectory";

    @Parameter( label = "Working directory" )
    public File workingDirectory;
    public static final String WORKING_DIRECTORY = "workingDirectory";

    @Parameter( label = "Fixed Image" )
    public File fixedImageFile;
    public static final String FIXED_IMAGE_FILE = "fixedImageFile";

    @Parameter( label = "Moving Image" )
    public File movingImageFile;
    public static final String MOVING_IMAGE_FILE = "movingImageFile";

    @Parameter( label = "Transformation type", choices = {
            ElastixSettings.TRANSLATION,
            ElastixSettings.EULER,
            ElastixSettings.AFFINE,
            ElastixSettings.SPLINE } )
    public String transformationType;
    public static final String TRANSFORMATION_TYPE = "transformationType";
    
    @Parameter( label = "Number of iterations" )
    public int numIterations = 1000;
    public static final String NUMBER_OF_ITERATIONS = "numIterations";

    @Parameter( label = "Number of spatial samples" )
    public String numSpatialSamples = "3000;3000";
    public static final String NUMBER_OF_SPATIAL_SAMPLES = "numSpatialSamples";

    @Parameter( label = "Resolution pyramid" )
    public String resolutionPyramid = "10,10,10;1,1,1";
    public static final String RESOLUTION_PYRAMID = "resolutionPyramid";

    @Parameter( label = "Spline grid spacing", required = false )
    public String bSplineGridSpacing = "50,50,50";
    public static final String SPLINE_GRID_SPACING = "bSplineGridSpacing";

    @Parameter
    public LogService logService;

    @Parameter
    public ThreadService threadService;

    public void run()
    {
        ElastixSettings settings = getSettingsFromUI();
        ElastixBinaryRunner elastixBinaryRunner = new ElastixBinaryRunner( settings );
        elastixBinaryRunner.runElastix();
    }

    private ElastixSettings getSettingsFromUI()
    {
        ElastixSettings settings = new ElastixSettings();

        settings.logService = logService;

        settings.workingDirectory = workingDirectory.toString();
        settings.elastixDirectory = elastixDirectory.toString();

        settings.fixedImageFilePath = fixedImageFile.toString();
        settings.movingImageFilePath = movingImageFile.toString();

        settings.transformationType = transformationType;
        settings.iterations = numIterations;
        settings.spatialSamples = numSpatialSamples;
        settings.workers = Prefs.getThreads(); // TODO
        settings.resolutionPyramid = resolutionPyramid;
        settings.bSplineGridSpacing = bSplineGridSpacing;

        return settings;
    }


}

