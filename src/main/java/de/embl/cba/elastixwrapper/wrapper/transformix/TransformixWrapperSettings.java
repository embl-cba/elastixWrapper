package de.embl.cba.elastixwrapper.wrapper.transformix;

import de.embl.cba.elastixwrapper.commandline.settings.Settings;

import java.io.File;
import java.util.ArrayList;

public class TransformixWrapperSettings extends Settings {

    public enum OutputModality
    {
        Show_images,
        Save_as_tiff,
        Save_as_BigDataViewer_xml_h5
    }

    public TransformixWrapperSettings() {}

    public String transformationFilePath;

    // before staging
    public String movingImageFilePath = "";
    // after staging
    public ArrayList<String> stagedMovingImageFilePaths;

    public OutputModality outputModality;
    public File outputFile;
}
