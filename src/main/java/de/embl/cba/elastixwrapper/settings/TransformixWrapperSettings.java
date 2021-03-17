package de.embl.cba.elastixwrapper.settings;

import java.io.File;

public class TransformixWrapperSettings extends TransformixSettings {

    public enum OutputModality
    {
        Show_images,
        Save_as_tiff,
        Save_as_BigDataViewer_xml_h5
    }

    public OutputModality outputModality;
    public File outputFile;
    public File outputDirectory;
    public String transformationOutputFilePath;
}
