package tests;

import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
import net.imagej.ImageJ;
import org.junit.Test;


public class TestElastix
{
	@Test
	public void registerEulerSingleChannelImage()
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ElastixSettings settings = new ElastixSettings();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.tmpDir = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/fluo01/tmp";
		settings.transformationType = ElastixSettings.EULER;
		settings.fixedImageFilePath = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/fluo01/ellipsoid-horizontal-dxyz200nm.tif";
		settings.movingImageFilePath = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/fluo01/ellipsoid-at45degrees-dxyz200nm.tif";
		settings.downSamplingFactors = "10 10";
		settings.fixedMaskPath = "";
		settings.movingMaskPath = "";

		final ElastixWrapper elastixWrapper = new ElastixWrapper( settings );
		elastixWrapper.runElastix();

		// Bdv
		elastixWrapper.reviewResults();

		// ImageJ
		elastixWrapper.reviewResultsInImageJ();

		// Save as Tiff
		elastixWrapper.createTransformedImagesAndSaveAsTiff();

		settings.logService.info( "Done!" );
	}


	public static void main( String[] args )
	{
		new TestElastix().registerEulerSingleChannelImage();
	}



}
