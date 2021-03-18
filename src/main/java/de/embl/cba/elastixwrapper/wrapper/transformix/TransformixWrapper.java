package de.embl.cba.elastixwrapper.wrapper.transformix;

import bdv.util.Bdv;
import bdv.util.BdvStackSource;
import de.embl.cba.bdv.utils.io.BdvImagePlusExport;
import de.embl.cba.elastixwrapper.commandline.TransformixCaller;
import de.embl.cba.elastixwrapper.commandline.settings.TransformixSettings;
import de.embl.cba.elastixwrapper.settings.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.settings.TransformixWrapperSettings;
import de.embl.cba.elastixwrapper.settings.TransformixWrapperSettings.OutputModality;
import de.embl.cba.elastixwrapper.utils.Utils;
import de.embl.cba.elastixwrapper.wrapper.BdvManager;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;

import java.io.File;
import java.util.ArrayList;

public class TransformixWrapper {

    public static final String TRANSFORMIX_INPUT_FILENAME = "to_be_transformed";
    public static final String TRANSFORMIX_OUTPUT_FILENAME = "result";

    TransformixWrapperSettings settings;
    private ArrayList< String > transformedImageFilePaths;

    public TransformixWrapper( TransformixWrapperSettings settings ) {
        this.settings = settings;
        this.transformedImageFilePaths = new ArrayList<>(  );
    }

    public void runTransformix()
    {
        createOrEmptyWorkingDir();

        settings.stagedMovingImageFilePaths = stageImageAsMhd(
                settings.movingImageFilePath, TRANSFORMIX_INPUT_FILENAME );

        transformImagesAndHandleOutput();
    }

    public void transformImagesAndHandleOutput() {
        for ( int c = 0; c < settings.stagedMovingImageFilePaths.size(); c++ )
            transformImageAndHandleOutput( c );
    }

    private void transformImageAndHandleOutput( int movingFileIndex )
    {
        TransformixSettings transformixSettings = new TransformixSettings( settings, movingFileIndex );
        new TransformixCaller( transformixSettings ).callTransformix();

        String transformedImageFileName = TRANSFORMIX_OUTPUT_FILENAME
                + "."
                + settings.resultImageFileType;

        ImagePlus result = loadMetaImage(
                settings.tmpDir,
                transformedImageFileName );

        if ( result == null )
        {
            Utils.logErrorAndExit( settings,"The transformed image could not be loaded: "
                    + settings.tmpDir + File.separator + transformedImageFileName + "\n" +
                    "Please check the log: " + settings.tmpDir + File.separator + "elastix.log" );

        }

        if ( settings.outputModality.equals( OutputModality.Show_images ) )
        {
            result.show();
            result.setTitle( "transformed-ch" + movingFileIndex );
        }
        else
        {
            String outputFile = settings.outputFile.toString();
            outputFile = outputFile.replace( ".tif", "" );
            outputFile = outputFile.replace( ".xml", "" );

            if ( settings.outputModality.equals( OutputModality.Save_as_tiff ) )
            {
                final String path = outputFile + "-ch" + movingFileIndex + ".tif";

                transformedImageFilePaths.add( path );

                settings.logService.info( "\nSaving transformed image: " + path );

                new FileSaver( result ).saveAsTiff( path );
            }
            else if ( settings.outputModality.equals( OutputModality.Save_as_BigDataViewer_xml_h5 ) )
            {
                String path;
                if ( settings.stagedMovingImageFilePaths.size() > 1 )
                    path = outputFile + "-ch" + movingFileIndex + ".xml";
                else
                    path = outputFile + ".xml";

                settings.logService.info( "\nSaving transformed image: " + path );

                BdvImagePlusExport.saveAsBdv( result, new File( path ) );
            }
        }

    }

    // public ArrayList< ImagePlus > getTransformedImages()
    // {
    //     if ( transformedImageFilePaths.size() == 0 )
    //         createTransformedImagesAndSaveAsTiff();
    //
    //     ArrayList< ImagePlus > transformedImages = new ArrayList<>(  );
    //
    //     for ( String path : transformedImageFilePaths )
    //         transformedImages.add( IJ.openImage( path ) );
    //
    //     return transformedImages;
    // }

    public Bdv showTransformedImages( BdvManager bdvManager )
    {
        Bdv bdv = null;
        for ( String transformedImageFilePath : transformedImageFilePaths )
        {
            String baseName = new File( transformedImageFilePath ).getName();
            bdv = bdvManager.showMetaImageInBdv( settings.tmpDir, baseName );
        }
        return bdv;
    }

}
