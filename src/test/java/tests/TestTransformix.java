package tests;

import de.embl.cba.elastixwrapper.settings.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.wrapper.elastix.ElastixWrapper;
import net.imagej.ImageJ;
import org.junit.Test;

import java.io.File;

public class TestTransformix
{
	@Test
	public void registerSingleChannelImageAndSaveAsTiff()
	{
		ElastixWrapperSettings settings = new ElastixWrapperSettings();

		final ImageJ ij = new ImageJ();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.tmpDir = "/Users/tischer/Desktop/elastix-tmp";
		settings.movingImageFilePath = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/fluo01/ellipsoid-at45degrees-dxyz200nm.tif";
		settings.transformationFilePath = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/fluo01/tmp/TransformParameters.0.txt";

		settings.outputModality = ElastixWrapperSettings.OUTPUT_MODALITY_SAVE_AS_TIFF;
		settings.outputFile = new File( "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/fluo01/transformed-ellipsoid" );

		final ElastixWrapper elastixWrapper = new ElastixWrapper( settings );

		elastixWrapper.runTransformix();

		settings.logService.info( "Done!" );
	}

	public static void main( String[] args )
	{
		new TestTransformix().registerSingleChannelImageAndSaveAsTiff();
	}
}
