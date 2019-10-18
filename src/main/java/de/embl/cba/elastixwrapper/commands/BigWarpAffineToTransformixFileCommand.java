package de.embl.cba.elastixwrapper.commands;

import de.embl.cba.elastixwrapper.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import itc.converters.AffineTransform3DToElastixAffine3D;
import itc.transforms.elastix.ElastixAffineTransform3D;
import itc.transforms.elastix.ElastixTransform;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.Arrays;

import static de.embl.cba.elastixwrapper.utils.Utils.getMillimeterVoxelSpacingUsingBF;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Elastix>Utils>Big Warp Affine to Transformix File" )
public class BigWarpAffineToTransformixFileCommand
{
	public static final String MILLIMETER = "millimeter";
	public static final String MICROMETER = "micrometer";
	public static final String NANOMETER = "nanometer";

	@Parameter( label = "Target image dimensions" )
	public File targetImageFile;

	@Parameter( label = "Big warp affine transform" )
	public String affineTransformString;

	@Parameter( label = "Big warp affine transform units", choices = { MILLIMETER, MICROMETER, NANOMETER }  )
	public String affineTransformUnit;

	@Parameter( label = "Transformix transformation output file", style = "save" )
	public File transformationOutputFile;

	public void run()
	{
		// configure converter, based on target image
		//
		final ImagePlus targetImp = IJ.openImage( targetImageFile.getAbsolutePath() );
		final Double[] targetImageVoxelSpacingsInMillimeter = getMillimeterVoxelSpacingUsingBF( targetImageFile );
		final Integer[] targetImageDimensionsInPixels = { targetImp.getWidth(), targetImp.getHeight(), targetImp.getNSlices() };

		final AffineTransform3DToElastixAffine3D converter = new AffineTransform3DToElastixAffine3D(
				ElastixTransform.FINAL_LINEAR_INTERPOLATOR,
				ElastixTransform.RESULT_IMAGE_PIXEL_TYPE_UNSIGNED_CHAR,
				targetImageVoxelSpacingsInMillimeter,
				targetImageDimensionsInPixels
		);

		// convert the big warp affine transform to elastix
		//

		// the big warp affine transform already is from fixed to moving, thus no inversion is needed
		AffineTransform3D affineTransform3D = new AffineTransform3D();
		affineTransform3D.set( asDoubles( affineTransformString) );

		// elastix works in millimeters, thus we need to convert the big warp affine to millimeters
		if ( affineTransformUnit.equals( MILLIMETER ) )
			affineTransform3D = affineTransform3D;
		else if ( affineTransformUnit.equals( MICROMETER ) )
			affineTransform3D = scaleAffineTransform3DCoordinateUnits( affineTransform3D, new double[]{ 1000, 1000, 1000 } );
		else if ( affineTransformUnit.equals( NANOMETER ) )
			affineTransform3D = scaleAffineTransform3DCoordinateUnits( affineTransform3D, new double[]{ 1000000, 1000000, 1000000 } );

		final ElastixAffineTransform3D elastixAffineTransform3D = converter.convert( affineTransform3D );
		elastixAffineTransform3D.save( transformationOutputFile.getAbsolutePath() );
	}


	public static AffineTransform3D scaleAffineTransform3DCoordinateUnits(
			AffineTransform3D transform,
			double[] scale )
	{

		AffineTransform3D scaledTransform = transform.copy();
		scaledTransform = scaledTransform.concatenate( new Scale( scale ) );

		final double[] inverse = Arrays.stream( scale ).map( x -> 1.0 / x ).toArray();
		scaledTransform = scaledTransform.preConcatenate( new Scale( inverse ) );

		return scaledTransform;
	}

	public static double[] asDoubles( String affineString )
	{
		affineString = affineString.replace( "3d-affine: (", "" );
		affineString = affineString.replace( ")", "" );
		affineString = affineString.replace( "(", "" );
		if ( affineString.contains( "," ))
			return Utils.delimitedStringToDoubleArray( affineString, "," );
		else
			return Utils.delimitedStringToDoubleArray( affineString, " " );
	}
}
