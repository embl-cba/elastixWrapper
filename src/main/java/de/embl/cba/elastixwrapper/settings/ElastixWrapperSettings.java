package de.embl.cba.elastixwrapper.settings;

import de.embl.cba.elastixwrapper.elastix.DefaultElastixParametersCreator;
import org.scijava.log.LogService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ElastixWrapperSettings extends ElastixSettings
{
    public static final String STAGING_FILE_TYPE = "mhd";

    // before staging - do we need to keep this??
    public String initialFixedImageFilePath = "";
    public String initialMovingImageFilePath = "";
    public String intialFixedMaskPath = "";
    public String initialMovingMaskPath = "";

    public DefaultElastixParametersCreator.ParameterStyle elastixParameters = DefaultElastixParametersCreator.ParameterStyle.Default;
    public String resultImageFileType = STAGING_FILE_TYPE;
    public Map< Integer, Integer > fixedToMovingChannel = new HashMap<>(  );
    public int numChannels = 1;
    public double[] channelWeights = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,};
    public double imageWidthMillimeter;

}
