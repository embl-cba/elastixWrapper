package tests;

import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapper;
import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapperSettings;
import net.imagej.ImageJ;
import org.junit.Test;

import java.io.File;

public class TestTransformix
{
	@Test
	public void registerSingleChannelImageAndSaveAsTiff()
	{
		TransformixWrapperSettings settings = new TransformixWrapperSettings();

		final ImageJ ij = new ImageJ();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.tmpDir = "/Users/tischer/Desktop/elastix-tmp";
		settings.movingImageFilePath = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/fluo01/ellipsoid-at45degrees-dxyz200nm.tif";
		settings.transformationFilePath = "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/fluo01/tmp/TransformParameters.0.txt";

		settings.outputModality = TransformixWrapperSettings.OutputModality.Save_as_tiff;
		settings.outputFile = new File( "/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/test-data/fluo01/transformed-ellipsoid" );

		final TransformixWrapper transformixWrapper = new TransformixWrapper( settings );

		transformixWrapper.runTransformix();

		settings.logService.info( "Done!" );
	}

	public static void main( String[] args )
	{
		new TestTransformix().registerSingleChannelImageAndSaveAsTiff();
	}
}
