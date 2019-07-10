package de.embl.cba.elastixwrapper.elastix;

import org.scijava.log.LogService;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tischi on 30/04/17.
 */
public class ElastixSettings
{

    // constants

    public static final String TRANSLATION = "Translation";
    public static final String EULER = "Euler";
    public static final String AFFINE = "Affine";
    public static final String SPLINE = "BSpline";
    public static final String SIMILARITY = "Similarity";

    public static final String FINAL_RESAMPLER_LINEAR = "FinalLinearInterpolator";
    public static final String FINAL_RESAMPLER_NEAREST_NEIGHBOR = "FinalNearestNeighborInterpolator";

    public static final String STAGING_FILE_TYPE = "mhd";

    public static final String PARAMETERS_HENNING = "Henning";
    public static final String PARAMETERS_DEFAULT = "Default";
    public static final String PARAMETERS_GIULIA = "CLEM";


    // variables

    public LogService logService;

    public String elastixDirectory = "/Users/tischi/Downloads/elastix_macosx64_v4.8/";
    public String workingDirectory = "/Users/tischi/Desktop/tmp/";

    public String fixedImageFilePath = "";
    public String movingImageFilePath = "";
    public String fixedMaskPath = "";
    public String movingMaskPath = "";

    public String parameterFilePath = "";
    public String initialTransformationFilePath = "";
    public String transformationFilePath = "";
    public String elastixParameters = PARAMETERS_DEFAULT;
    public String resultImageFileType = STAGING_FILE_TYPE;

    public int iterations = 1000;
    public String spatialSamples = "10000";
    public int numWorkers = Runtime.getRuntime().availableProcessors();
    public String downSamplingFactors = "10 10 10";
    public int movingImageBitDepth = 8;
    public String bSplineGridSpacing = "50 50 50";
    public String transformationType;

    public Map< Integer, Integer > fixedToMovingChannel = new HashMap<>(  );

    public int numChannels = 1;
    public double[] channelWeights = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,};

    public String finalResampler = FINAL_RESAMPLER_LINEAR;
    public String outputModality;
    public File outputFile;

    public ElastixSettings()
    {
    }

}
