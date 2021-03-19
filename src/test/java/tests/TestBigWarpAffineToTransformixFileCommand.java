package tests;

import itc.commands.BigWarpAffineToTransformixFileCommand;
import de.embl.cba.elastixwrapper.wrapper.elastix.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.wrapper.elastix.ElastixWrapper;
import net.imagej.ImageJ;
import org.junit.Test;

import java.io.File;

public class TestBigWarpAffineToTransformixFileCommand
{
	private static boolean interactive;

	@Test
	public void run()
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		final BigWarpAffineToTransformixFileCommand command = new BigWarpAffineToTransformixFileCommand();
		command.affineTransformString = "3d-affine: (0.2631659750292986, 0.2728152883093439, -0.8421807709743863, 246.3565248635497, -0.5994842563176209, 0.7014132763310869, 0.039887406048063165, 128.0280556665454, 0.6513948564132443, 0.5352982320152232, 0.37695292383424855, -147.204085589971)";
		command.targetImageFile = new File( getClass().getClassLoader().getResource("test-data/sbem.ome.tif").getFile() );
		command.transformationOutputFile = new File( command.targetImageFile.getParent() + File.separator + "transformation.txt" );
		command.affineTransformUnit = BigWarpAffineToTransformixFileCommand.MICROMETER;
		command.run();

		ElastixWrapperSettings settings = new ElastixWrapperSettings();

		settings.logService = imageJ.log();
		settings.elastixDirectory = "/Applications/elastix_macosx64_v4.8";
		settings.tmpDir = "/Users/tischer/Desktop/elastix-tmp/";
		settings.movingImageFilePath = getClass().getClassLoader().getResource("test-data/xray.ome.tif").getFile();
		settings.transformationFilePath = command.transformationOutputFile.getAbsolutePath();
		settings.numWorkers = 4;
		settings.outputModality = ElastixWrapperSettings.OUTPUT_MODALITY_SHOW_IMAGES;
		// settings.outputFile = outputFile;

		ElastixWrapper elastixWrapper = new ElastixWrapper( settings );
		elastixWrapper.runTransformix();
	}

	public static void main( String[] args )
	{
		interactive = true;
		new TestBigWarpAffineToTransformixFileCommand().run();
	}
}
