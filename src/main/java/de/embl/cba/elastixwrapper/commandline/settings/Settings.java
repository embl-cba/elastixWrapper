package de.embl.cba.elastixwrapper.commandline.settings;

import org.scijava.log.LogService;

public class Settings {
    public LogService logService;
    public String elastixDirectory = "/Users/tischi/Downloads/elastix_macosx64_v4.8/";
    public String tmpDir;
    public int numWorkers = Runtime.getRuntime().availableProcessors();
    public boolean headless = false;
}
