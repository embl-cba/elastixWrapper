/**
 * Open [ Window > Console ] to see the log
 * 
 *
 */

import bdv.util.Bdv;
import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
import net.imagej.ImageJ;

ElastixSettings settings = new ElastixSettings();

final ImageJ ij = new ImageJ();
settings.logService = ij.log();
settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
settings.tmpDir = "/Users/tischer/Desktop/elastix-tmp";
settings.fixedImageFilePath = "/g/arendt/EM_6dpf_segmentation/platy-fibsem-datasets/registration/v1/sbem-target.tif";
settings.movingImageFilePath = "/g/arendt/EM_6dpf_segmentation/platy-fibsem-datasets/registration/v1/fibsem-target.tif";
settings.fixedMaskPath = ""; // /Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/ganglion-segmentation/spem-mask-ganglion.tif";
settings.initialTransformationFilePath = "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/ganglion-segmentation/amira-transform.txt";
settings.transformationType = ElastixSettings.AFFINE;
settings.downSamplingFactors = "10 10 10; 2, 2, 2";
// settings.movingMaskPath = "";
// settings.bSplineGridSpacing = "50 50 50";
settings.iterations = 1; // Set to 1000
settings.spatialSamples = "10000; 10000";
settings.channelWeights = [1.0, 1.0, 3.0, 1.0, 1.0];
// settings.finalResampler = ElastixSettings.FINAL_RESAMPLER_LINEAR;

final ElastixWrapper elastixWrapper = new ElastixWrapper( settings );
elastixWrapper.runElastix();

final Bdv bdv = elastixWrapper.reviewResults();
//bdv.close();
settings.logService.info( "Done!" );
