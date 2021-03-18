import de.embl.cba.elastixwrapper.settings.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.wrapper.elastix.ElastixWrapper;
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

		ElastixWrapperSettings settings = new ElastixWrapperSettings();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.tmpDir = "/Users/tischer/Desktop/elastix-tmp";
		settings.movingImageFilePath = inputImagePath;
		settings.transformationFilePath = "/Users/tischer/Desktop/transform.txt";

		final ElastixWrapper elastixWrapper = new ElastixWrapper( settings );
		elastixWrapper.runTransformix();

		MetaImage_Reader reader = new MetaImage_Reader();
		final ImagePlus transformed = reader.load(
				settings.tmpDir,
				ElastixWrapper.TRANSFORMIX_OUTPUT_FILENAME
						+ "." + settings.resultImageFileType,
				false );

		transformed.show();

		settings.logService.info( "Done!" );
	}

}
