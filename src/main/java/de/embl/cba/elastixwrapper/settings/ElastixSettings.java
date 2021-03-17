package de.embl.cba.elastixwrapper.settings;

import java.util.ArrayList;

public class ElastixSettings extends Settings {

    public String parameterFilePath;
    public String initialTransformationFilePath;
    public ArrayList<String> fixedImageFilePaths;
    public ArrayList<String> movingImageFilePaths;
    public ArrayList<String> fixedMaskFilePaths;
    public ArrayList<String> movingMaskFilePaths;
}
