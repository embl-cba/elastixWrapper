package users.rachel;

import bdv.util.Bdv;
import de.embl.cba.elastixwrapper.settings.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
import net.imagej.ImageJ;

public class RachelParapodRegistration
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ElastixWrapperSettings settings = new ElastixWrapperSettings();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.tmpDir = "/Users/tischer/Desktop/elastix-tmp";

		settings.fixedImageFilePath = "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/ganglion-segmentation/spem-seg-ganglion.tif";

		settings.movingImageFilePath = "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/ganglion-segmentation/fib-sem-seg-ganglion.tif";

		settings.fixedMaskPath = "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/ganglion-segmentation/spem-mask-ganglion.tif";

		settings.initialTransformationFilePath = "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/ganglion-segmentation/amira-transform.txt";

		settings.transformationType = ElastixWrapperSettings.AFFINE;
		settings.downSamplingFactors = "10 10 10; 2, 2, 2";
		// settings.movingMaskPath = "";
		// settings.bSplineGridSpacing = "50 50 50";
		settings.iterations = 1000;
		settings.spatialSamples = "10000; 10000";
		settings.channelWeights = new double[]{1.0, 1.0, 3.0, 1.0, 1.0};
		// settings.finalResampler = ElastixSettings.FINAL_RESAMPLER_LINEAR;

		final ElastixWrapper elastixWrapper = new ElastixWrapper( settings );
		elastixWrapper.runElastix();

		final Bdv bdv = elastixWrapper.reviewResults();
		//bdv.close();

		settings.logService.info( "Done!" );
	}

	private static String getImageFilePath( String relativePath )
	{
		return RachelParapodRegistration.class.getResource( relativePath ).getFile().toString();
	}

}
