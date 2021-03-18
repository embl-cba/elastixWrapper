import de.embl.cba.elastixwrapper.settings.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.wrapper.elastix.ElastixWrapper;

import net.imagej.ImageJ;

public class ExampleElastixAPI
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ElastixWrapperSettings settings = new ElastixWrapperSettings();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.tmpDir = "/Users/tischer/Desktop/elastix-tmp";
		settings.transformationType = ElastixWrapperSettings.EULER;
		settings.fixedImageFilePath = getImageFilePath( "test-data/fluo01/ellipsoid-horizontal-dxyz200nm.tif" );
		settings.movingImageFilePath = getImageFilePath( "test-data/fluo01/ellipsoid-at45degrees-dxyz200nm.tif" );
		settings.downSamplingFactors = "10 10 10";
		settings.fixedMaskPath = "";
		settings.movingMaskPath = "";
		// settings.bSplineGridSpacing = "50 50 50";
		// settings.iterations = 1000;
		// settings.spatialSamples = 10000;
		// settings.channelWeights = new double[]{1.0, 3.0, 3.0, 1.0, 1.0};
		// settings.finalResampler = ElastixSettings.FINAL_RESAMPLER_LINEAR;

		final ElastixWrapper elastixWrapper = new ElastixWrapper( settings );
		elastixWrapper.runElastix();
		elastixWrapper.reviewResults();

		settings.logService.info( "Done!" );
	}

	private static String getImageFilePath( String relativePath )
	{
		return ExampleElastixAPI.class.getResource( relativePath ).getFile().toString();
	}

}
