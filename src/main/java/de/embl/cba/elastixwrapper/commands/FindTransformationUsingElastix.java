package de.embl.cba.elastixwrapper.commands;

import de.embl.cba.elastixwrapper.elastix.ElastixBinaryRunner;
import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.utils.CommandUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.ChannelSplitter;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Elastix>Compute Transformation (elastix)" )
public class FindTransformationUsingElastix implements Command
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
                    ElastixSettings.PARAMETERS_DETLEV
            })

    public String elastixParameters;

    @Parameter( label = "Use initial transformation" )
    public boolean useInitialTransformation;

    @Parameter( label = "Initial transformation", required = false )
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

    @Parameter( label = "Output modality", choices = {
            CommandUtils.OUTPUT_MODALITY_SHOW_AS_INDIVIDUAL_IMAGES,
            CommandUtils.OUTPUT_MODALITY_SHOW_AS_COMPOSITE_IMAGE
    } )
    public String outputModality;

    @Parameter
    public LogService logService;

    @Parameter
    public ThreadService threadService;

    public void run()
    {
        ElastixSettings settings = runElastix();

        showInput( settings );


        IJ.run("Merge Channels...", "c2=C1-fixed c6=C1-result create ignore");
        IJ.getImage().setTitle( "channel01-fixed-moving" );
        IJ.run("Merge Channels...", "c2=C2-fixed c6=C2-result create ignore");
        IJ.getImage().setTitle( "channel02-fixed-moving" );

    }

    private ElastixSettings runElastix()
    {
        ElastixSettings settings = getSettingsFromUI();
        ElastixBinaryRunner elastixBinaryRunner = new ElastixBinaryRunner( settings );
        elastixBinaryRunner.runElastix();
        return settings;
    }

    private void showInput( ElastixSettings settings )
    {
        if ( outputModality.equals( CommandUtils.OUTPUT_MODALITY_SHOW_AS_INDIVIDUAL_IMAGES ) || outputModality.equals( CommandUtils.OUTPUT_MODALITY_SHOW_AS_COMPOSITE_IMAGE ) )
        {
            ImagePlus fixed;

            fixed = IJ.openImage( fixedImageFile.toString() );
            //moving = IJ.openImage( movingImageFile.toString() );

            fixed.show();
            fixed.setTitle( "fixed" );

            if ( fixed.getNChannels() > 1 )
            {
                IJ.run("Split Channels");
            }


            //moving.show();
            //moving.setTitle( "moving" );

            /*
            if ( outputModality.equals( CommandUtils.OUTPUT_MODALITY_SHOW_AS_COMPOSITE_IMAGE ) )
            {
                IJ.run( fixed, "Merge Channels...", "c2=fixed c6=result create" );
            }*/

        }
    }

    private ElastixSettings getSettingsFromUI()
    {
        ElastixSettings settings = new ElastixSettings();

        settings.logService = logService;

        settings.workingDirectory = workingDirectory.toString();
        settings.elastixDirectory = elastixDirectory.toString();

        if ( useInitialTransformation )
        {
            settings.initialTransformationFilePath = initialTransformationFile.toString();
        }
        else
        {
            settings.initialTransformationFilePath = "";
        }

        settings.elastixParameters = elastixParameters;

        settings.maskImageFilePath = "";

        settings.fixedImageFilePath = fixedImageFile.toString();
        settings.movingImageFilePath = movingImageFile.toString();

        settings.transformationType = transformationType;
        settings.iterations = numIterations;
        settings.spatialSamples = numSpatialSamples;
        settings.workers = Prefs.getThreads(); // TODO
        settings.resolutionPyramid = resolutionPyramid;
        settings.bSplineGridSpacing = bSplineGridSpacing;

        settings.channelWeights = new double[]{0.125, 0.125, 0.125, 0.125, 0.125};

        return settings;
    }


}

