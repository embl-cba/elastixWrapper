package de.embl.cba.elastixwrapper.wrapper.elastix.parameters;

import de.embl.cba.elastixwrapper.commandline.settings.Settings;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;

import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters.FINAL_RESAMPLER_LINEAR;

public class ElastixParametersSettings extends Settings {
    // minimal settings needed to generate defaults with DefaultElastixParametersCreator
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
