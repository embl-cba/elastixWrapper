/**
 * Open this script in Fiji and click [ Run ]
 * 
 * Open [ Window > Console ] to see the log
 * 
 */

import bdv.util.Bdv;
import de.embl.cba.elastixwrapper.settings.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.wrapper.elastix.ElastixWrapper;
import net.imagej.ImageJ;


ElastixWrapperSettings settings = new ElastixWrapperSettings();

final ImageJ ij = new ImageJ();
settings.logService = ij.log();
settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
settings.tmpDir = "/Users/tischer/Desktop/elastix-tmp";
settings.fixedImageFilePath = "/g/arendt/EM_6dpf_segmentation/platy-fibsem-datasets/registration/examples/fixed-image-sbem-seg-ganglion.tif";
settings.movingImageFilePath = "/g/arendt/EM_6dpf_segmentation/platy-fibsem-datasets/registration/examples/moving-image-sbem-seg-ganglion.tif";
settings.fixedMaskPath = "/g/arendt/EM_6dpf_segmentation/platy-fibsem-datasets/registration/examples/fixed-image-mask.tif";
settings.initialTransformationFilePath = "g/arendt/EM_6dpf_segmentation/platy-fibsem-datasets/registration/examples/amira-transform.txt";
settings.transformationType = ElastixWrapperSettings.AFFINE;
settings.downSamplingFactors = "10 10 10; 2, 2, 2";
// settings.movingMaskPath = "";
// settings.bSplineGridSpacing = "50 50 50";
settings.iterations = 1; // Set to 1000
settings.spatialSamples = "10000; 10000";
settings.channelWeights = [1.0, 1.0, 3.0, 1.0, 1.0]; // not used in this example
// settings.finalResampler = ElastixSettings.FINAL_RESAMPLER_LINEAR;

final ElastixWrapper elastixWrapper = new ElastixWrapper( settings );
elastixWrapper.runElastix();

final Bdv bdv = elastixWrapper.reviewResults();
//bdv.close();
settings.logService.info( "Done!" );
