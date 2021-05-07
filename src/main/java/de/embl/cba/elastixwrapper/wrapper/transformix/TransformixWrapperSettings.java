package de.embl.cba.elastixwrapper.wrapper.transformix;

import de.embl.cba.elastixwrapper.commandline.settings.Settings;

import java.io.File;
import java.util.ArrayList;

public class TransformixWrapperSettings extends Settings {

    public enum OutputModality
    {
        Show_images,
        Save_as_tiff,
        Save_as_bdv
    }

    public static final String OUTPUT_MODALITY_SHOW_IMAGES
            = "Show images";
    public static final String OUTPUT_MODALITY_SAVE_AS_TIFF
            = "Save as Tiff";
    public static final String OUTPUT_MODALITY_SAVE_AS_BDV
            = "Save as BigDataViewer .xml/.h5";

    public TransformixWrapperSettings() {}

    public String transformationFilePath;

    // before staging
    public String movingImageFilePath = "";
    // after staging - in order of channels
    public ArrayList<String> stagedMovingImageFilePaths;

    public OutputModality outputModality;
    public File outputFile;
}
