package de.embl.cba.elastixwrapper.commandline.settings;

import de.embl.cba.elastixwrapper.settings.ElastixWrapperSettings;

import java.util.ArrayList;

public class ElastixSettings extends Settings {

    public String parameterFilePath;
    public String initialTransformationFilePath;
    public ArrayList<String> fixedImageFilePaths;
    public ArrayList<String> movingImageFilePaths;
    public ArrayList<String> fixedMaskFilePaths;
    public ArrayList<String> movingMaskFilePaths;

    public ElastixSettings() {}

    public ElastixSettings( ElastixWrapperSettings settings ) {
        logService = settings.logService;
        elastixDirectory = settings.elastixDirectory;
        tmpDir = settings.tmpDir;
        numWorkers = settings.numWorkers;
        headless = settings.headless;
        parameterFilePath = settings.parameterFilePath;
        initialTransformationFilePath = settings.initialTransformationFilePath;
        fixedImageFilePaths = settings.stagedFixedImageFilePaths;
        movingImageFilePaths = settings.stagedMovingImageFilePaths;
        fixedMaskFilePaths = settings.stagedFixedMaskFilePaths;
        movingMaskFilePaths = settings.stagedMovingMaskFilePaths;
    }
}
