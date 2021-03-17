package de.embl.cba.elastixwrapper.transformix;

import bdv.util.BdvStackSource;
import de.embl.cba.bdv.utils.io.BdvImagePlusExport;
import de.embl.cba.elastixwrapper.settings.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;

import java.io.File;
import java.util.ArrayList;

public class TransformixWrapper {


    public static final String TRANSFORMIX_INPUT_FILENAME = "to_be_transformed";
    public static final String TRANSFORMIX_OUTPUT_FILENAME = "result";

    public TransformixWrapper() {

    }

    public void runTransformix()
    {
        createOrEmptyWorkingDir();

        ArrayList< String > channelFileNames = stageImageAsMhd(
                settings.movingImageFilePath, TRANSFORMIX_INPUT_FILENAME );

        for ( int c = 0; c < channelFileNames.size(); c++ )
            transformImageAndHandleOutput( channelFileNames, c );
    }

    private void transformImageAndHandleOutput( String executableShellScript,
                                                ArrayList< String > movingImageFileNames,
                                                int c )
    {
        // List< String > transformixCallArgs =
        //         getTransformixCallArgs(
        //                 movingImageFileNames.get( c ), executableShellScript );
        //
        // Utils.executeCommand( transformixCallArgs, settings.logService );
        // TODO - call transformix caller

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

        if ( settings.outputModality.equals( ElastixWrapperSettings.OUTPUT_MODALITY_SHOW_IMAGES ) )
        {
            result.show();
            result.setTitle( "transformed-ch" + c );
        }
        else
        {
            String outputFile = settings.outputFile.toString();
            outputFile = outputFile.replace( ".tif", "" );
            outputFile = outputFile.replace( ".xml", "" );

            if ( settings.outputModality.equals( ElastixWrapperSettings.OUTPUT_MODALITY_SAVE_AS_TIFF ) )
            {
                final String path = outputFile + "-ch" + c + ".tif";

                transformedImageFilePaths.add( path );

                settings.logService.info( "\nSaving transformed image: " + path );

                new FileSaver( result ).saveAsTiff( path );
            }
            else if ( settings.outputModality.equals( ElastixWrapperSettings.OUTPUT_MODALITY_SAVE_AS_BDV ) )
            {
                String path;
                if ( nChannels > 1 )
                    path = outputFile + "-ch" + c + ".xml";
                else
                    path = outputFile + ".xml";

                settings.logService.info( "\nSaving transformed image: " + path );

                BdvImagePlusExport.saveAsBdv( result, new File( path ) );
            }
        }

    }

    public ArrayList< ImagePlus > getTransformedImages()
    {
        if ( transformedImageFilePaths.size() == 0 )
            createTransformedImagesAndSaveAsTiff();

        ArrayList< ImagePlus > transformedImages = new ArrayList<>(  );

        for ( String path : transformedImageFilePaths )
            transformedImages.add( IJ.openImage( path ) );

        return transformedImages;
    }

    private void showTransformedImages()
    {
        for ( int index : settings.fixedToMovingChannel.values() )
        {
            ImagePlus imagePlus = IJ.openImage( transformedImageFilePaths.get( index ) );
            final BdvStackSource bdvStackSource = showImagePlusInBdv( imagePlus );
            bdvStackSource.setColor( colors.get( colorIndex++ )  );
            bdv = bdvStackSource.getBdvHandle();
        }
    }

}
