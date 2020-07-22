import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
 
settings = ElastixSettings();
settings.logService = ij.log();
settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
settings.tmpDir = "/Users/tischer/Desktop/elastix-tmp";
settings.fixedImageFilePath = "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/ganglion-segmentation/spem-seg-ganglion.tif";
settings.movingImageFilePath = "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/ganglion-segmentation/fib-sem-seg-ganglion.tif";
settings.fixedMaskPath = "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/ganglion-segmentation/spem-mask-ganglion.tif";
settings.initialTransformationFilePath = "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/ganglion-segmentation/amira-transform.txt";
settings.transformationType = ElastixSettings.AFFINE;
settings.downSamplingFactors = "10 10 10; 2, 2, 2";
# settings.movingMaskPath = "";
# settings.bSplineGridSpacing = "50 50 50";
settings.iterations = 1;
settings.spatialSamples = "10000; 10000";
settings.channelWeights = [1.0, 1.0, 3.0, 1.0, 1.0];
# settings.finalResampler = ElastixSettings.FINAL_RESAMPLER_LINEAR;

elastixWrapper = ElastixWrapper( settings );
elastixWrapper.runElastix();

elastixWrapper.reviewResults();	
