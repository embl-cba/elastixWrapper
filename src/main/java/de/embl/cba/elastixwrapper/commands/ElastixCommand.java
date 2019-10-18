package de.embl.cba.elastixwrapper.commands;

import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.utils.Utils;
import ij.Prefs;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.DefaultLogger;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Elastix>Elastix" )
public class ElastixCommand implements Command
{
    public static final String NONE = "None";
    public static final String SHOW_OUTPUT_IN_IMAGEJ1 = "Show output in ImageJ1";
    public static final String SHOW_OUTPUT_IN_BDV = "Show output in Bdv";
    public static final String SAVE_TRANSFORMED_AS_TIFF = "Save transformed images in working directory as Tiff";

    @Parameter( label = "Elastix directory", style = "directory" )
    public File elastixDirectory;

    @Parameter( label = "Working directory", style = "directory" )
    public File workingDirectory;

    @Parameter( label = "Fixed image" )
    public File fixedImageFile;

    @Parameter( label = "Moving image" )
    public File movingImageFile;

    @Parameter( label = "Transformation type", choices = {
            ElastixSettings.TRANSLATION,
            ElastixSettings.EULER,
            ElastixSettings.SIMILARITY,
            ElastixSettings.AFFINE,
            ElastixSettings.SPLINE } )
    public String transformationType;

    @Parameter( label = "Grid spacing for BSpline transformation [voxels]", required = false )
    public String bSplineGridSpacing = "50,50,50";

    @Parameter( label = "Number of iterations" )
    public int numIterations = 1000;

    @Parameter( label = "Number of spatial samples" )
    public String numSpatialSamples = "10000";

    @Parameter( label = "Gaussian smoothing sigma [voxels]" )
    public String gaussianSmoothingSigmas = "10,10,10";

    @Parameter( label = "Transformation output file", style = "save" )
    public File transformationOutputFile;

    @Parameter( label = "Image output modality",
            choices = {
                    NONE,
                    SHOW_OUTPUT_IN_IMAGEJ1,
                    SAVE_TRANSFORMED_AS_TIFF
            } )
    public String outputModality;

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

    @Parameter( label = "Elastix parameters", choices =
            {
                    ElastixSettings.PARAMETERS_DEFAULT,
                    ElastixSettings.PARAMETERS_GIULIA
            })
    public String elastixParameters = ElastixSettings.PARAMETERS_DEFAULT;

    @Parameter( label = "Final resampler",
            choices = {
                    ElastixSettings.FINAL_RESAMPLER_LINEAR,
                    ElastixSettings.FINAL_RESAMPLER_NEAREST_NEIGHBOR
            } )
    public String finalResampler = ElastixSettings.FINAL_RESAMPLER_LINEAR;

    @Parameter( label = "Weights for multi channel images" )
    public String multiChannelWeights = "1.0,3.0,1.0,1.0,1.0,1.0";

    @Parameter
    public LogService logService;

    private ElastixWrapper elastixWrapper;

    public void run()
    {
        runElastix();
    }

    private void runElastix( )
    {
        ElastixSettings settings = getSettings();

        elastixWrapper = new ElastixWrapper( settings );

        elastixWrapper.runElastix();

        elastixWrapper.saveTransformationFile();

        if ( outputModality.equals( SHOW_OUTPUT_IN_BDV ))
        {
            elastixWrapper.reviewResults();
        }
        else if ( outputModality.equals( SHOW_OUTPUT_IN_IMAGEJ1 ))
        {
            elastixWrapper.reviewResultsInImageJ();
        }
        else if ( outputModality.equals( SAVE_TRANSFORMED_AS_TIFF ) )
        {
            elastixWrapper.createTransformedImagesAndSaveAsTiff();
        }
    }

    private ElastixSettings getSettings()
    {
        ElastixSettings settings = new ElastixSettings();

        settings.logService = logService;

        settings.elastixDirectory = elastixDirectory.toString();

        if ( ! new File( settings.elastixDirectory ).exists() )
        {
            Logger.error( "The elastix directory does not exist: " + settings.elastixDirectory );
            throw new UnsupportedOperationException( "Directory does not exist.");
        }

        settings.workingDirectory = workingDirectory.toString();

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
        settings.numWorkers = Prefs.getThreads();
        settings.downSamplingFactors = gaussianSmoothingSigmas;
        settings.bSplineGridSpacing = bSplineGridSpacing;
        settings.channelWeights = Utils.delimitedStringToDoubleArray( multiChannelWeights, "," );
        settings.finalResampler = finalResampler;
        settings.outputModality = outputModality;

        settings.transformationOutputFilePath = transformationOutputFile.getAbsolutePath();

        return settings;
    }
}

