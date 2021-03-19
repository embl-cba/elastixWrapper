package de.embl.cba.elastixwrapper.wrapper.elastix;

import bdv.util.*;
import de.embl.cba.elastixwrapper.commandline.ElastixCaller;
import de.embl.cba.elastixwrapper.commandline.settings.ElastixSettings;
import de.embl.cba.elastixwrapper.utils.Utils;
import de.embl.cba.elastixwrapper.wrapper.BdvManager;
import de.embl.cba.elastixwrapper.wrapper.StagingManager;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.DefaultElastixParametersCreator;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapper;
import de.embl.cba.elastixwrapper.wrapper.transformix.TransformixWrapperSettings.OutputModality;
import ij.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static de.embl.cba.elastixwrapper.wrapper.StagingManager.*;

public class ElastixWrapper
{
    private ElastixWrapperSettings settings;
    private StagingManager stagingManager;

    public ElastixWrapper( ElastixWrapperSettings settings )
    {
        this.settings = settings;
        this.stagingManager = new StagingManager( settings );
    }

    public void runElastix()
    {
        processSettings();
        stagingManager.createOrEmptyWorkingDir();

        if (!stageImages()) {
            Utils.logErrorAndExit(settings, "There was an issue staging the images.\n " +
                    "Maybe the temporary working directory could not be generated.");
            return;
        }

        setMovingImageParameters();
        createElastixParameterFile();
        // TODO - check channels added in right order
        ElastixSettings elastixSettings = new ElastixSettings( settings );
        new ElastixCaller( elastixSettings ).callElastix();
    }

    private void processSettings()
    {
        if ( ! settings.elastixDirectory.endsWith( File.separator ) )
            settings.elastixDirectory += File.separator;

        if ( ! settings.tmpDir.endsWith( File.separator ) )
            settings.tmpDir += File.separator;
    }

    private boolean stageImages()
    {
        settings.stagedFixedImageFilePaths = stagingManager.stageImageAsMhd(
                settings.fixedImageFilePath,
                ELASTIX_FIXED_IMAGE_NAME );

        settings.stagedMovingImageFilePaths = stagingManager.stageImageAsMhd(
                settings.movingImageFilePath,
                ELASTIX_MOVING_IMAGE_NAME );

        if ( ! settings.fixedMaskPath.equals( "" ) )
            settings.stagedFixedMaskFilePaths = stagingManager.stageImageAsMhd(
                    settings.fixedMaskPath,
                    ELASTIX_FIXED_MASK_IMAGE_NAME );

        if ( ! settings.movingMaskPath.equals( "" ) )
            settings.stagedMovingMaskFilePaths = stagingManager.stageImageAsMhd(
                    settings.movingMaskPath,
                    ELASTIX_MOVING_MASK_IMAGE_NAME );

        setFixedToMovingChannel();

        return true;
    }

    private void setFixedToMovingChannel() {
        if ( settings.fixedToMovingChannel.size() == 0 )
        {
            // use all channels for registration
            for ( int c = 0; c < settings.stagedFixedImageFilePaths.size(); c++ )
                settings.fixedToMovingChannel.put( c, c );
        }
    }

    private void setMovingImageParameters() {
        ImagePlus imp = stagingManager.openImage( settings.movingImageFilePath );
        settings.movingImageBitDepth = imp.getBitDepth();
    }

    private void createElastixParameterFile()
    {
        settings.parameterFilePath = stagingManager.getDefaultParameterFilePath();
        System.out.println( "Parameter list type: " + settings.elastixParametersStyle );
        ElastixParameters parameters =
                new DefaultElastixParametersCreator( settings ).getElastixParameters( settings.elastixParametersStyle );

        if ( parameters == null ) {
            Utils.logErrorAndExit( settings, "Parameter file could not be created - image bit depth might not be supported" );
        }

        parameters.writeParameterFile( settings.parameterFilePath );
    }

    /**
     * Shows the fixed, moving and transformed moving images
     * in BigDataViewer.
     *
     * @return {@code Bdv} BigDataViewer handle, enabling, e.g., bdv.close()
     */
    public Bdv reviewResults()
    {
        TransformixWrapper transformixWrapper = createTransformixWrapper( OutputModality.Save_as_tiff );
        transformixWrapper.transformImagesAndHandleOutput();

        BdvManager bdvManager = new BdvManager();
        showFixedImagesInBdv( bdvManager );
        showMovingImages( bdvManager );
        return transformixWrapper.showTransformedImages( bdvManager );
    }

    public void reviewResultsInImageJ()
    {
        TransformixWrapper transformixWrapper = createTransformixWrapper( OutputModality.Show_images );
        showInputImagePlus();
        transformixWrapper.transformImagesAndHandleOutput();
    }

    public void createTransformedImagesAndSaveAsTiff()
    {
        TransformixWrapper transformixWrapper = createTransformixWrapper( OutputModality.Save_as_tiff );
        transformixWrapper.transformImagesAndHandleOutput();
    }

    private TransformixWrapper createTransformixWrapper( OutputModality outputModality ) {
        settings.outputModality = outputModality;
        settings.transformationFilePath = stagingManager.getDefaultTransformationFilePath();
        settings.outputFile = new File( stagingManager.getPath( "transformed" ) );
        return new TransformixWrapper( settings );
    }

    private Bdv showMovingImages( BdvManager bdvManager )
    {
        Bdv bdv = null;
        for ( String movingImageFilePath : settings.stagedMovingImageFilePaths )
        {
            String baseName = new File( movingImageFilePath ).getName();
            bdv = bdvManager.showMetaImageInBdv( settings.tmpDir, baseName );
        }
        return bdv;
    }

    private Bdv showFixedImagesInBdv( BdvManager bdvManager )
    {
        Bdv bdv = null;
        for ( String fixedImagePath : settings.stagedFixedImageFilePaths )
        {
            String baseName = new File( fixedImagePath ).getName();
            bdv = bdvManager.showMetaImageInBdv( settings.tmpDir, baseName );
        }
        return bdv;
    }

    private BdvStackSource showFixedInBdv( BdvManager bdvManager )
    {
        final ImagePlus templateImp = IJ.openImage( settings.fixedImageFilePath );
        return bdvManager.showImagePlusInBdv( templateImp );
    }

    public void showTransformationFile()
    {
        IJ.open( stagingManager.getDefaultTransformationFilePath() );
    }

    public void saveTransformationFile()
    {
        final File transformation =
                new File( stagingManager.getDefaultTransformationFilePath() );

        File copied = new File( settings.transformationOutputFilePath );

        try
        {
            FileUtils.copyFile( transformation, copied);
        } catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    public void showInputImagePlus( )
    {
        ImagePlus fixed;

        fixed = IJ.openImage( settings.fixedImageFilePath );

        fixed.show();

        fixed.setTitle( "fixed" );

        // TODO: The macro recording does not work when using IJ.run(..) inside the plugin
        // if ( fixed.getNChannels() > 1 ) IJ.run("Split Channels" );
    }

    // TODO - CHECK MUST BE ADDED IN CHANNEL ORDER, used to pick which channelf rom fixedtomovingchannels??


    // private void addImagesToArguments( List< String > args,
    //                                    String fixedOrMoving,
    //                                    ArrayList< String > fileNames )
    // {
    //     int elastixChannelIndex = 0;
    //     for ( int fixedChannelIndex : settings.fixedToMovingChannel.keySet() )
    //     {
    //         if (  settings.fixedToMovingChannel.size() == 1 )
    //             args.add( "-" + fixedOrMoving );
    //         else
    //             args.add( "-" + fixedOrMoving + elastixChannelIndex );
    //
    //         final String filename = getFileName(
    //                 fixedOrMoving, fileNames, fixedChannelIndex );
    //
    //         args.add( getPath( filename ) );
    //
    //         elastixChannelIndex++;
    //     }
    // }
    //
    // private String getFileName(
    //         String fixedOrMoving,
    //         ArrayList< String > filenames,
    //         int fixedChannelIndex )
    // {
    //     int filenameIndex;
    //
    //     if ( fixedOrMoving.equals( FIXED ) )
	// 		filenameIndex = fixedChannelIndex;
	// 	else // Moving
	// 		filenameIndex = parametersSettings.fixedToMovingChannel.get( fixedChannelIndex );
    //
    //     return filenames.get( filenameIndex );
    // }




}
