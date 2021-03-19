package de.embl.cba.elastixwrapper.wrapper.elastix;

import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.DefaultElastixParametersCreator.ParameterStyle;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapperSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters.FINAL_RESAMPLER_LINEAR;

public class ElastixWrapperSettings extends TransformixWrapperSettings
{
    // before staging
    public String fixedImageFilePath = "";
    public String fixedMaskPath = "";
    public String movingMaskPath = "";

    // after staging
    public ArrayList<String> stagedFixedImageFilePaths;
    public ArrayList<String> stagedFixedMaskFilePaths;
    public ArrayList<String> stagedMovingMaskFilePaths;

    public String initialTransformationFilePath;
    public String parameterFilePath;

    // path to copy calculated transformation to
    public String transformationOutputFilePath;

    // minimal settings needed to generate defaults with DefaultElastixParametersCreator
    public ParameterStyle elastixParametersStyle = ParameterStyle.Default;
    public ElastixParameters.TransformationType transformationType;
    public int iterations = 1000;
    public String spatialSamples = "10000";
    public String downSamplingFactors = "10 10 10";
    public String bSplineGridSpacing = "50 50 50";
    public String finalResampler = FINAL_RESAMPLER_LINEAR;
    public int movingImageBitDepth = 8;
    public Map< Integer, Integer > fixedToMovingChannel = new HashMap<>(  );
    public double[] channelWeights = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,};
}
