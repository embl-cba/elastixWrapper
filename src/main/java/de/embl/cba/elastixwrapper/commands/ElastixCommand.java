package de.embl.cba.elastixwrapper.commands;

import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import ij.IJ;
import ij.Prefs;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Elastix>Elastix" )
public class ElastixCommand implements Command
{
    public static final String SHOW_OUTPUT = "Show output";
    public static final String SAVE_TRANSFORMED_AS_TIFF =
            "Save transformed image as Tiff";
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
                    ElastixSettings.PARAMETERS_DEFAULT,
                    ElastixSettings.PARAMETERS_GIULIA
            })

    public String elastixParameters = ElastixSettings.PARAMETERS_DEFAULT;

    @Parameter( label = "Use fixed image mask" )
    public boolean useFixedMask;

    @Parameter( label = "Fixed image mask file", required = false )
    public File fixedMaskFile;

    @Parameter( label = "Use moving image mask" )
    public boolean useMovingMask;

    @Parameter( label = "Moving image mask file", required = false )
    public File movingMaskFile;

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
    public String numSpatialSamples = "10000";

    @Parameter( label = "Gaussian smoothing sigma [voxels]" )
    public String gaussianSmoothingSigmas = "10,10,10";

    @Parameter( label = "BSpline grid spacing [voxels]", required = false )
    public String bSplineGridSpacing = "50,50,50";

    @Parameter( label = "Final resampler",
            choices = {
                    ElastixSettings.FINAL_RESAMPLER_LINEAR,
                    ElastixSettings.FINAL_RESAMPLER_NEAREST_NEIGHBOR
            } )
    public String finalResampler = ElastixSettings.FINAL_RESAMPLER_LINEAR;

    @Parameter( label = "Output modality",
            choices = {
                    SHOW_OUTPUT,
                    SAVE_TRANSFORMED_AS_TIFF
            } )
    public String outputModality;

    @Parameter
    public LogService logService;

    private ElastixWrapper elastixWrapper;

    public void run()
    {
        runElastix();
        handleOutput();
    }

    private void runElastix( )
    {
        ElastixSettings settings = getSettings();

        elastixWrapper = new ElastixWrapper( settings );

        elastixWrapper.runElastix();
    }

    private void handleOutput( )
    {
        if ( outputModality.equals( SHOW_OUTPUT ) )
        {
            showOutput();
        }
        else if ( outputModality.equals( SAVE_TRANSFORMED_AS_TIFF ) )
        {
            elastixWrapper.createTransformedImagesAndSaveAsTiff();
        }
    }

    private void showOutput()
    {
        elastixWrapper.showInputImage();
        elastixWrapper.createTransformedImagesAndSaveAsTiff();
        elastixWrapper.showTransformedImages();
        elastixWrapper.showTransformationFile();
        IJ.run( "Synchronize Windows", "" );
    }

    private ElastixSettings getSettings()
    {
        ElastixSettings settings = new ElastixSettings();

        settings.logService = logService;

        settings.elastixDirectory = elastixDirectory.toString();
        if ( ! settings.elastixDirectory.endsWith( File.separator ) )
            settings.elastixDirectory += File.separator;

        settings.workingDirectory = workingDirectory.toString();
        if ( ! settings.workingDirectory.endsWith( File.separator ) )
            settings.workingDirectory += File.separator;

        if ( useInitialTransformation )
            settings.initialTransformationFilePath = initialTransformationFile.toString();
        else
            settings.initialTransformationFilePath = "";

        settings.elastixParameters = elastixParameters;

        if ( useFixedMask )
            settings.fixedMaskPath = fixedMaskFile.toString();
        else
            settings.fixedMaskPath = "";

        if ( useMovingMask )
            settings.movingMaskPath = movingMaskFile.toString();
        else
            settings.movingMaskPath = "";

        settings.fixedImageFilePath = fixedImageFile.toString();
        settings.movingImageFilePath = movingImageFile.toString();

        settings.transformationType = transformationType;
        settings.iterations = numIterations;
        settings.spatialSamples = numSpatialSamples;
        settings.workers = Prefs.getThreads();
        settings.resolutionPyramid = gaussianSmoothingSigmas;
        settings.bSplineGridSpacing = bSplineGridSpacing;

        // TODO: make this a UI
        settings.channelWeights = new double[]{1.0, 3.0, 3.0, 1.0, 1.0};

        settings.finalResampler = finalResampler;

        return settings;
    }


}

