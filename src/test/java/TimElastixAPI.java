import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
import net.imagej.ImageJ;

public class TimElastixAPI
{
	public static void main( String[] args )
	{

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ElastixSettings settings = new ElastixSettings();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.workingDirectory = "/Users/tischer/Desktop/elastix-tmp";
		settings.transformationType = ElastixSettings.AFFINE;
		settings.fixedImageFilePath = "/Users/tischer/Desktop/tim-elastix/template.tif";
		settings.movingImageFilePath = "/Users/tischer/Desktop/tim-elastix/bUnwarpJ_pass.tif";

		/**
		 * You want to match the first channel (0) in the fixed image,
		 * - which has only one channel -
		 * to the second channel (1) in the moving image
		 * - which has two channels -
		 */
		settings.fixedToMovingChannel.put( 0, 1 );

		settings.downSamplingFactors = "10 10";
		// settings.fixedMaskPath = "";
		// settings.movingMaskPath = "";
		// settings.bSplineGridSpacing = "50 50 50";
		settings.iterations = 1000;
		settings.spatialSamples = "3000";
		// settings.channelWeights = new double[]{1.0, 3.0, 3.0, 1.0, 1.0};
		// settings.finalResampler = ElastixSettings.FINAL_RESAMPLER_LINEAR;

		final ElastixWrapper elastixWrapper = new ElastixWrapper( settings );
		elastixWrapper.runElastix();
		elastixWrapper.createTransformedImagesAndSaveAsTiff();
		//elastixWrapper.reviewResults();

		settings.logService.info( "Done!" );
	}

	private static String getImageFilePath( String relativePath )
	{
		return TimElastixAPI.class.getResource( relativePath ).getFile().toString();
	}

}
