import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixWrapper;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.img.display.imagej.ImageJFunctions;

import java.util.ArrayList;

public class TimElastixAPI
{
	public static void main( String[] args )
	{

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ElastixSettings settings = new ElastixSettings();

		settings.logService = ij.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8" ;
		settings.workingDirectory = "/Users/tischer/Desktop/elastix-tmp";
		settings.transformationType = ElastixSettings.EULER;
		settings.fixedImageFilePath = "/Users/tischer/Desktop/tim-elastix/template.tif";
		settings.movingImageFilePath = "/Users/tischer/Desktop/tim-elastix/bUnwarpJ_pass.tif";

		/**
		 * You want to match the first channel (0) in the fixed image,
		 * - which has only one channel -
		 * to the second channel (1) in the moving image
		 * - which has two channels -
		 */
		settings.fixedToMovingChannel.put( 0, 1 );

		settings.downSamplingFactors = "10 10 10";
		// settings.fixedMaskPath = "";
		// settings.movingMaskPath = "";
		// settings.bSplineGridSpacing = "50 50 50";
		// settings.iterations = 1000;
		// settings.spatialSamples = 10000;
		// settings.channelWeights = new double[]{1.0, 3.0, 3.0, 1.0, 1.0};
		// settings.finalResampler = ElastixSettings.FINAL_RESAMPLER_LINEAR;

		final ElastixWrapper elastixWrapper = new ElastixWrapper( settings );
		elastixWrapper.runElastix();

		final ImagePlus templateImp = IJ.openImage( settings.fixedImageFilePath );
		final Bdv bdv = BdvFunctions.show(
				ImageJFunctions.wrap( templateImp ),
				templateImp.getTitle(),
				BdvOptions.options().is2D() ).getBdvHandle();

		final ArrayList< ImagePlus > transformedImages = elastixWrapper.getTransformedImages();

		for ( ImagePlus transformedImage : transformedImages )
		{
			BdvFunctions.show(
					ImageJFunctions.wrap( transformedImage  ),
					transformedImage.getTitle(),
					BdvOptions.options().is2D().addTo( bdv )
					);
		}

		settings.logService.info( "Done!" );
	}

	private static String getImageFilePath( String relativePath )
	{
		return TimElastixAPI.class.getResource( relativePath ).getFile().toString();
	}

}
