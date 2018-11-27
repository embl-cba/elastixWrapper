package de.embl.cba.elastixwrapper.elastix;

import ij.gui.Roi;
import org.scijava.log.LogService;

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

    public static final String RESULT_IMAGE_FILE_TYPE_MHD = "mhd";

    public static final String PARAMETERS_HENNING = "Henning";
    public static final String PARAMETERS_DETLEV = "Default";
    public static final String PARAMETERS_GIULIA = "CLEM";


    // variables

    public LogService logService;

    public String elastixDirectory = "/Users/tischi/Downloads/elastix_macosx64_v4.8/";
    public String workingDirectory = "/Users/tischi/Desktop/tmp/";

    public String fixedImageFilePath;
    public String movingImageFilePath;
    public String parameterFilePath;
    public String initialTransformationFilePath = "";
    public String maskImageFilePath;

    public String outputModality;

    public String elastixParameters;

    public String transformationFilePath;

    public String resultImageFileType = RESULT_IMAGE_FILE_TYPE_MHD;

    public String os;

    public Boolean recursive = false;
    public int reference = 1, delta;
    public int[] regRange = new int[]{1,1};
    public int[] zRange = new int[]{1,1};
    public double background = 0;
    public int iterations = 100;
    public String spatialSamples = "100; 100";
    public int workers = Runtime.getRuntime().availableProcessors();
    public String resolutionPyramid = "10 10; 2 2";
    public int movingImageBitDepth = 8;
    public Roi roi = null;
    public String bSplineGridSpacing = "30 30";

    public String transformationType;

    public int numChannels = 1;
    public double[] channelWeights;

    public String finalResampler;
}
