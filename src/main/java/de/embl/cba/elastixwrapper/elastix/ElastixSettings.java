package de.embl.cba.elastixwrapper.elastix;

import org.scijava.log.LogService;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: make a simpler settings for transformix
 */
public class ElastixSettings
{
    public static final String STAGING_FILE_TYPE = "mhd";

    public static final String OUTPUT_MODALITY_SHOW_IMAGES
            = "Show images";
    public static final String OUTPUT_MODALITY_SAVE_AS_TIFF
            = "Save as Tiff";
    public static final String OUTPUT_MODALITY_SAVE_AS_BDV
            = "Save as BigDataViewer .xml/.h5";

    // variables
    //
    public LogService logService;

    public String elastixDirectory = "/Users/tischi/Downloads/elastix_macosx64_v4.8/";
    public String tmpDir = "/Users/tischi/Desktop/tmp/";

    public String fixedImageFilePath = "";
    public String movingImageFilePath = "";
    public String fixedMaskPath = "";
    public String movingMaskPath = "";

    public String parameterFilePath = "";
    public String initialTransformationFilePath = "";
    public String transformationFilePath = "";
    public String elastixParameters = PARAMETERS_DEFAULT;
    public String resultImageFileType = STAGING_FILE_TYPE;

    public int numWorkers = Runtime.getRuntime().availableProcessors();
    public Map< Integer, Integer > fixedToMovingChannel = new HashMap<>(  );

    public int numChannels = 1;
    public double[] channelWeights = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,};

    public String outputModality;
    public File outputFile;
	public File outputDirectory;
    public String transformationOutputFilePath;
    public double imageWidthMillimeter;
    public boolean headless = false;

	public ElastixSettings()
    {
    }

}
