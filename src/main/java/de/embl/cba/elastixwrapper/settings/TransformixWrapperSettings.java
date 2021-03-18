package de.embl.cba.elastixwrapper.settings;

import de.embl.cba.elastixwrapper.commandline.settings.TransformixSettings;

import java.io.File;

public class TransformixWrapperSettings {

    public enum OutputModality
    {
        Show_images,
        Save_as_tiff,
        Save_as_BigDataViewer_xml_h5
    }

    public OutputModality outputModality;
    public File outputFile;
    
    // surely this == tmpdir?
    public File outputDirectory;
    // ?? how determined
    public String transformationOutputFilePath;

    public boolean stageImages;
    public boolean cleanTmpDir;
}
