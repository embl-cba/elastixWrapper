import de.embl.cba.elastixwrapper.wrapper.StagingManager;
import de.embl.cba.elastixwrapper.wrapper.elastix.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.wrapper.elastix.ElastixWrapper;
import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapper;
import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapperSettings;
import de.embl.cba.metaimage_io.MetaImage_Reader;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

public class ExampleTransformixAPI
{
	public static void main( String[] args )
	{

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final String inputImagePath =
				"/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/FIB segmentation/muscle.tif";

		IJ.open( inputImagePath );

		TransformixWrapperSettings settings = new TransformixWrapperSettings();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.tmpDir = "/Users/tischer/Desktop/elastix-tmp";
		settings.movingImageFilePath = inputImagePath;
		settings.transformationFilePath = "/Users/tischer/Desktop/transform.txt";

		final TransformixWrapper transformixWrapper = new TransformixWrapper( settings );
		transformixWrapper.runTransformix();

		MetaImage_Reader reader = new MetaImage_Reader();
		final ImagePlus transformed = reader.load(
				settings.tmpDir,
				StagingManager.TRANSFORMIX_OUTPUT_FILENAME
						+ "." + StagingManager.STAGING_FILE_TYPE,
				false );

		transformed.show();

		settings.logService.info( "Done!" );
	}

}
