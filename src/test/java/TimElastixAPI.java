import bdv.util.Bdv;
import de.embl.cba.elastixwrapper.settings.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
import net.imagej.ImageJ;

public class TimElastixAPI
{
	public static void main( String[] args )
	{

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ElastixWrapperSettings settings = new ElastixWrapperSettings();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.tmpDir = "/Users/tischer/Desktop/elastix-tmp";
		settings.transformationType = ElastixWrapperSettings.AFFINE;
		settings.fixedImageFilePath = "/Users/tischer/Desktop/3dtemplate.tif";
		settings.movingImageFilePath = "/Users/tischer/Desktop/3dstg16.tif";

		/**
		 * You want to match the first channel (0) in the fixed image,
		 * - which has only one channel -
		 * to the second channel (1) in the moving image
		 * - which has two channels -
		 */
		settings.fixedToMovingChannel.put( 1, 1 );

		settings.downSamplingFactors = "10 10 10";
		// settings.fixedMaskPath = "";
		// settings.movingMaskPath = "";
		// settings.bSplineGridSpacing = "50 50 50";
		settings.iterations = 1000;
		settings.spatialSamples = "10000";
		// settings.channelWeights = new double[]{1.0, 3.0, 3.0, 1.0, 1.0};
		// settings.finalResampler = ElastixSettings.FINAL_RESAMPLER_LINEAR;

		final ElastixWrapper elastixWrapper = new ElastixWrapper( settings );
		elastixWrapper.runElastix();
		final Bdv bdv = elastixWrapper.reviewResults();
		//bdv.close();

		settings.logService.info( "Done!" );
	}

	private static String getImageFilePath( String relativePath )
	{
		return TimElastixAPI.class.getResource( relativePath ).getFile().toString();
	}

}
