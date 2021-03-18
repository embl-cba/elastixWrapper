package de.embl.cba.elastixwrapper.settings;

import de.embl.cba.elastixwrapper.commandline.settings.Settings;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.DefaultElastixParametersCreator.ParameterStyle;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import org.scijava.log.LogService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters.FINAL_RESAMPLER_LINEAR;

public class ElastixWrapperSettings extends Settings
{
    public static final String STAGING_FILE_TYPE = "mhd";

    // public LogService logService;
    // public String elastixDirectory;
    // public String tmpDir;
    // public int numWorkers;
    // public boolean headless;

    // before staging
    public String fixedImageFilePath = "";
    public String movingImageFilePath = "";
    public String fixedMaskPath = "";
    public String movingMaskPath = "";

    // after staging
    public ArrayList<String> stagedFixedImageFilePaths;
    public ArrayList<String> stagedMovingImageFilePaths;
    public ArrayList<String> stagedFixedMaskFilePaths;
    public ArrayList<String> stagedMovingMaskFilePaths;

    // always the same
    public String initialTransformationFilePath;
    public String parameterFilePath;

    // public ParameterStyle elastixParametersStyle = ParameterStyle.Default;
    // path to copy calculated transformation to
    public String transformationOutputFilePath;
    public TransformixWrapperSettings.OutputModality outputModality;

    // TODO - split off into elastix parmeter settings???
    // public ElastixParameters.TransformationType transformationType;
    // public int iterations = 1000;
    // public String spatialSamples = "10000";
    // public String downSamplingFactors = "10 10 10";
    // public String bSplineGridSpacing = "50 50 50";
    // public String finalResampler = FINAL_RESAMPLER_LINEAR;
    // public double[] channelWeights = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,};
    //
    // // determined directly from images
    // public int numChannels = 1;
    // public double imageWidthMillimeter;
    // public int movingImageBitDepth = 8;
    // public Map< Integer, Integer > fixedToMovingChannel = new HashMap<>(  );


}
