package tests;

import de.embl.cba.elastixwrapper.wrapper.elastix.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.wrapper.elastix.ElastixWrapper;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapper;
import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapperSettings;
import net.imagej.ImageJ;
import org.junit.Test;

import java.io.File;


public class TestElastixAndTransformixCLEM
{
	//@Test
	public void registerFluoToEM()
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ElastixWrapperSettings settings = new ElastixWrapperSettings();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.tmpDir = "/Users/tischer/Documents/elastixWrapper/src/test/resources/test-data/clem/tmp";
		settings.transformationType = ElastixParameters.TransformationType.BSpline;
		settings.fixedImageFilePath = "/Users/tischer/Documents/elastixWrapper/src/test/resources/test-data/clem/em.tif";
		settings.movingImageFilePath = "/Users/tischer/Documents/elastixWrapper/src/test/resources/test-data/clem/fluo_green.tif";
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

	//@Test
	public void transformTwoChannelFluo()
	{
		TransformixWrapperSettings settings = new TransformixWrapperSettings();

		final ImageJ ij = new ImageJ();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.tmpDir = "/Users/tischer/Desktop/elastix-tmp";
		settings.movingImageFilePath = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/clem/fluo_red_green.tif";
		settings.transformationFilePath = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/clem/tmp/TransformParameters.0.txt";

		settings.outputModality = TransformixWrapperSettings.OutputModality.Save_as_tiff;
		settings.outputFile = new File( "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/clem/aligned" );

		final TransformixWrapper transformixWrapper = new TransformixWrapper( settings );

		transformixWrapper.runTransformix();

		settings.logService.info( "Done!" );
	}


	public static void main( String[] args )
	{
		new TestElastixAndTransformixCLEM().registerFluoToEM();
		new TestElastixAndTransformixCLEM().transformTwoChannelFluo();
	}



}
