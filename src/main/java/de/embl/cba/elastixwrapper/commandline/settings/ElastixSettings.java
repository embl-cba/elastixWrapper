package de.embl.cba.elastixwrapper.commandline.settings;

import de.embl.cba.elastixwrapper.wrapper.elastix.ElastixWrapperSettings;

import java.util.ArrayList;

public class ElastixSettings extends Settings {

    public String parameterFilePath;
    public String initialTransformationFilePath;
    // if size >1 will do multi-image registration
    // Order of fixed, moving and masks must be the same
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

        fixedImageFilePaths = new ArrayList<>();
        fixedMaskFilePaths = new ArrayList<>();
        movingImageFilePaths = new ArrayList<>();
        movingMaskFilePaths = new ArrayList<>();

        for ( int fixedChannelIndex : settings.fixedToMovingChannel.keySet() ) {
            int movingChannelIndex = settings.fixedToMovingChannel.get(fixedChannelIndex);
            fixedImageFilePaths.add( settings.stagedFixedImageFilePaths.get(fixedChannelIndex) );
            if (settings.stagedFixedMaskFilePaths != null ) {
                fixedMaskFilePaths.add(settings.stagedFixedMaskFilePaths.get(fixedChannelIndex));
            }
            movingImageFilePaths.add( settings.stagedMovingImageFilePaths.get(movingChannelIndex) );
            if ( settings.stagedMovingMaskFilePaths != null ) {
                movingMaskFilePaths.add(settings.stagedMovingMaskFilePaths.get(movingChannelIndex));
            }
        }
    }
}
