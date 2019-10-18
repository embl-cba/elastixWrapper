package de.embl.cba.elastixwrapper.commands;

import de.embl.cba.elastixwrapper.utils.Utils;
import itc.converters.BigWarpAffineToElastixAffineTransform3D;
import itc.transforms.elastix.ElastixAffineTransform3D;
import itc.transforms.elastix.ElastixTransform;
import loci.common.services.ServiceFactory;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import net.imglib2.realtransform.AffineTransform3D;
import ome.units.UNITS;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

import static de.embl.cba.elastixwrapper.utils.Utils.getMillimeterVoxelSpacingAndPixelDimensions;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Elastix>Utils>Big Warp Affine to Transformix File" )
public class BigWarpAffineToTransformixFileCommand implements Command
{
	public static final String MILLIMETER = "millimeter";
	public static final String MICROMETER = "micrometer";
	public static final String NANOMETER = "nanometer";

	@Parameter( label = "Target image dimensions (from image file)" )
	public File targetImageFile;

	@Parameter( label = "Big warp affine transform" )
	public String affineTransformString;

	@Parameter( label = "Big warp affine transform units", choices = { MILLIMETER, MICROMETER, NANOMETER }  )
	public String affineTransformUnit;

	@Parameter( label = "Transformix transformation output file", style = "save" )
	public File transformationOutputFile;

	@Parameter( label = "Interpolation", choices = { ElastixTransform.FINAL_LINEAR_INTERPOLATOR, ElastixTransform.FINAL_NEAREST_NEIGHBOR_INTERPOLATOR } )
	public String interpolation;
	private Double[] voxelSpacingsMillimeter;
	private Integer[] dimensionsPixels;
	private Integer bitDepth;

	public void run()
	{
		setTargetImageProperties( targetImageFile );

		AffineTransform3D affineTransform3D = new AffineTransform3D();
		affineTransform3D.set( affineStringAsDoubles( affineTransformString ) );

		final ElastixAffineTransform3D elastixAffineTransform3D =
				new BigWarpAffineToElastixAffineTransform3D().convert(
						affineTransform3D,
						voxelSpacingsMillimeter,
						dimensionsPixels,
						bitDepth,
						interpolation,
						affineTransformUnit );

		elastixAffineTransform3D.save( transformationOutputFile.getAbsolutePath() );
	}

	public static double[] affineStringAsDoubles( String affineString )
	{
		affineString = affineString.replace( "3d-affine: (", "" );
		affineString = affineString.replace( ")", "" );
		affineString = affineString.replace( "(", "" );
		if ( affineString.contains( "," ))
			return Utils.delimitedStringToDoubleArray( affineString, "," );
		else
			return Utils.delimitedStringToDoubleArray( affineString, " " );
	}


	private void setTargetImageProperties( File file )
	{
		// create OME-XML metadata store
		ServiceFactory factory = null;
		try
		{
			factory = new ServiceFactory();
			OMEXMLService service = factory.getInstance( OMEXMLService.class );
			IMetadata meta = service.createOMEXMLMetadata();

			// create format reader
			IFormatReader reader = new ImageReader();
			reader.setMetadataStore( meta );

			// initialize file
			reader.setId( file.getAbsolutePath() );
			reader.setSeries(0);

			String unit = meta.getPixelsPhysicalSizeX( 0 ).unit().getSymbol();
			voxelSpacingsMillimeter = new Double[ 3 ];
			voxelSpacingsMillimeter[ 0 ] = meta.getPixelsPhysicalSizeX( 0 ).value( UNITS.MILLIMETRE ).doubleValue();
			voxelSpacingsMillimeter[ 1 ] = meta.getPixelsPhysicalSizeX( 0 ).value( UNITS.MILLIMETRE ).doubleValue();
			voxelSpacingsMillimeter[ 2 ] = meta.getPixelsPhysicalSizeX( 0 ).value( UNITS.MILLIMETRE ).doubleValue();

			dimensionsPixels = new Integer[ 3 ];
			dimensionsPixels[ 0 ] = meta.getPixelsSizeX( 0 ).getValue();
			dimensionsPixels[ 1 ] = meta.getPixelsSizeY( 0 ).getValue();
			dimensionsPixels[ 2 ] = meta.getPixelsSizeZ( 0 ).getValue();

			bitDepth = meta.getPixelsSignificantBits( 0 ).getValue();
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}
}
