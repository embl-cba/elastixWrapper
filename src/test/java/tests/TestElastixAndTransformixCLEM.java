package tests;

import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
import net.imagej.ImageJ;
import org.junit.Test;

import java.io.File;


public class TestElastixAndTransformixCLEM
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

		settings.logService.info( "Done!" );
	}


	@Test
	public void transformTwoChannelFluo()
	{
		ElastixSettings settings = new ElastixSettings();

		final ImageJ ij = new ImageJ();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.workingDirectory = "/Users/tischer/Desktop/elastix-tmp";
		settings.movingImageFilePath = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/clem/fluo_red_green.tif";
		settings.transformationFilePath = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/clem/tmp/TransformParameters.0.txt";

		settings.outputModality = ElastixWrapper.OUTPUT_MODALITY_SAVE_AS_TIFF;
		settings.outputFile = new File( "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/clem/aligned" );

		final ElastixWrapper elastixWrapper = new ElastixWrapper( settings );

		elastixWrapper.runTransformix();

		settings.logService.info( "Done!" );
	}


	public static void main( String[] args )
	{
		new TestElastixAndTransformixCLEM().registerFluoToEM();
//		new TestElastixAndTransformixCLEM().transformTwoChannelFluo();
	}



}
