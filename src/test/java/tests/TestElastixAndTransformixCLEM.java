package tests;

import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
import net.imagej.ImageJ;
import org.junit.Test;


public class TestElastixCLEM
{
	@Test
	public void registerFluoToEM()
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ElastixSettings settings = new ElastixSettings();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.workingDirectory = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/clem/tmp";
		settings.transformationType = ElastixSettings.SPLINE;
		settings.fixedImageFilePath = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/clem/em.tif";
		settings.movingImageFilePath = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/clem/fluo_green.tif";
		settings.downSamplingFactors = "2 2";
		settings.bSplineGridSpacing = "100 100";
		settings.fixedMaskPath = "";
		settings.movingMaskPath = "";

		final ElastixWrapper elastixWrapper = new ElastixWrapper( settings );
		elastixWrapper.runElastix();

		// Bdv
		elastixWrapper.reviewResults();

		// Save as Tiff
		// elastixWrapper.createTransformedImagesAndSaveAsTiff();

		settings.logService.info( "Done!" );
	}


	public static void main( String[] args )
	{
		new TestElastixCLEM().registerFluoToEM();
	}



}
