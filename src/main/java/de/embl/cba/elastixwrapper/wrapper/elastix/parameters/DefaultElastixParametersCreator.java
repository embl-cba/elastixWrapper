package de.embl.cba.elastixwrapper.wrapper.elastix.parameters;

import de.embl.cba.elastixwrapper.settings.ElastixWrapperSettings;
import de.embl.cba.elastixwrapper.utils.Utils;
import ij.IJ;

public class DefaultElastixParametersCreator {

    public enum ParameterStyle
    {
        Default,
        Henning,
        CLEM
    }

    ElastixWrapperSettings settings;

    public DefaultElastixParametersCreator( ElastixWrapperSettings settings )
    {
        this.settings = settings;
    }

    public ElastixParameters getElastixParameters(ParameterStyle style ) {

        ElastixParameters elastixParameters;
        if ( style.equals( ParameterStyle.Henning ) )
        {
            elastixParameters = getHenningStyleParameters();
        }
        else if ( style.equals( ParameterStyle.CLEM ) )
        {
            elastixParameters = getGiuliaMizzonStyleParameters();
        }
        else if ( style.equals( ParameterStyle.Default ) )
        {
            elastixParameters = getDefaultParameters( );
        }
        else
        {
            IJ.error( "Could not generate parameter list." );
            return null;
        }

        return elastixParameters;
    }



    private ElastixParameters getDefaultParameters( )
    {
        ElastixParameters parameters = new ElastixParameters( settings.transformationType );
        setCommonParameters( parameters );

        parameters.addParameter( "NumberOfSpatialSamples", settings.spatialSamples.replace( ";", " " ), false, true);

        if ( settings.fixedToMovingChannel.size() > 1 )
        {
            parameters.addParameter(
                    "Registration",
                    "MultiMetricMultiResolutionRegistration",
                    false,
                    false );
            addChannelWeights( parameters );
        }
        else
        {
            parameters.addParameter( "Registration", "MultiResolutionRegistration", false, false );
        }

        // Pyramid
        parameters.addParameter( "FixedImagePyramid", "FixedSmoothingImagePyramid", true, false );
        parameters.addParameter( "MovingImagePyramid", "MovingSmoothingImagePyramid", true, false );

        // Initialisation
        parameters.addParameter( "AutomaticTransformInitialization", "true", false, false);
        parameters.addParameter( "AutomaticTransformInitializationMethod", "CenterOfGravity", false, false);

        // Samples
        parameters.addParameter( "ImageSampler", "RandomCoordinate", true, false );

        parameters.addParameter("ResampleInterpolator", settings.finalResampler, false, false);
        parameters.addParameter("WriteResultImage", "false", false, false);

       // if ( settings.transformationType.equals( ElastixSettings.SPLINE ) )
       // {
       //     parameters.add("(UseRandomSampleRegion \"true\")");
       //     final double sampleRegionSize = settings.imageWidthMillimeter / 5;
       //     String sampleRegionSizeString = "";
       //     for ( int d = 0; d < 3; d++ )
       //         sampleRegionSizeString += sampleRegionSize + " ";
       //     parameters.add("(SampleRegionSize " + sampleRegionSizeString +" )");
       // }

        //parameters.add("(BSplineInterpolationOrder 1)");
        //parameters.add("(FinalBSplineInterpolationOrder 3)");
        //parameters.add("(WriteResultImage \"true\")");
        //parameters.add("(ResultImageFormat \"" + settings.resultImageFileType + "\")");

        return( parameters );
    }

    // private ElastixParameters getHenningStyleParameters()
    // {
    //     ElastixParameters parameters = new ElastixParameters( transformationType );
    //
    //     // Spatial Samples
    //     parameters.addParameter("NumberOfSpatialSamples",
    //             spatialSamples.replace(";"," ").replace("full","0"),
    //             false,
    //             true);
    //
    //     // ImageSampler
    //     String imageSampler = "";
    //     for ( String s : spatialSamples.split(";") )
    //     {
    //         imageSampler += s.equals("full") ? "Full" : "Random";
    //     }
    //     parameters.addParameter("ImageSampler", imageSampler, false, false);
    //
    //     parameters.addParameter("Registration", "MultiResolutionRegistration", false, false);
    //
    //     parameters.addParameter("Interpolator", "LinearInterpolator", false, false);
    //     parameters.addParameter("ResampleInterpolator", "FinalLinearInterpolator", false, false);
    //     parameters.addParameter("FixedImagePyramid", "FixedRecursiveImagePyramid", false, false);
    //     parameters.addParameter("MovingImagePyramid", "MovingRecursiveImagePyramid", false, false);
    //     parameters.addParameter("AutomaticParameterEstimation",  "true", false, false);
    //     parameters.addParameter("AutomaticScalesEstimation", "true", false, false);
    //     parameters.addParameter("Metric", "AdvancedMeanSquares", false, false);
    //     parameters.addParameter("AutomaticTransformInitialization", "false", false, false);
    //     parameters.addParameter("HowToCombineTransforms", "Compose", false, false);
    //     parameters.addParameter("ErodeMask", "false", false, false);
    //
    //     parameters.addParameter("BSplineInterpolationOrder", "1", false, true);
    //     parameters.addParameter("FinalBSplineInterpolationOrder", "3", false, true);
    //     parameters.addParameter("WriteResultImage", "true", false, false);
    //     parameters.addParameter("ResultImageFormat", resultImageFileType, false, false);
    //
    //     return( parameters );
    // }



    private ElastixParameters getGiuliaMizzonStyleParameters()
    {
        ElastixParameters parameters = new ElastixParameters( settings.transformationType );
        setCommonParameters( parameters );

        if ( settings.fixedToMovingChannel.size() > 1 )
        {
            parameters.addParameter( "Registration", "MultiMetricMultiResolutionRegistration", false, false );
            addChannelWeights( parameters );
        }
        else
        {
            parameters.addParameter("Registration", "MultiResolutionRegistration", false, false);
        }

        // Pyramid
        // TODO: different pyramids for fixed and moving due to different resolution?
        // or re-save fluorescence image with same resolution as fixed image?
        parameters.addParameter( "FixedImagePyramid", "FixedSmoothingImagePyramid", true, false );
        parameters.addParameter( "MovingImagePyramid", "MovingSmoothingImagePyramid", true, false );

        // Samples
        parameters.addParameter("NumberOfSpatialSamples", settings.spatialSamples.replace(";"," "), false, false);
        parameters.addParameter( "ImageSampler", "RandomCoordinate", true, false );


        parameters.addParameter( "Interpolator", "LinearInterpolator", true, false );
        parameters.addParameter("ResampleInterpolator", "FinalLinearInterpolator", false, false);

        //parameters.add("(BSplineInterpolationOrder 1)");
        //parameters.add("(FinalBSplineInterpolationOrder 3)");
        //parameters.add("(WriteResultImage \"true\")");
        //parameters.add("(ResultImageFormat \"" + settings.resultImageFileType + "\")");

        return( parameters );
    }

    private void setCommonParameters( ElastixParameters parameters ) {
        parameters.addParameter( "MaximumNumberOfIterations", Integer.toString(settings.iterations), false, true );
        parameters.addParameter( "CheckNumberOfSamples", "false", false, false );
        parameters.addParameter("NumberOfResolutions" , Integer.toString(settings.downSamplingFactors.split(";").length), false, true);
        parameters.addParameter("ImagePyramidSchedule", settings.downSamplingFactors.replace(";"," ").replace(","," "), false, true);
        parameters.addParameter("FinalGridSpacingInVoxels", settings.bSplineGridSpacing.replace(",", " "), false,true);
        parameters.addParameter("NewSamplesEveryIteration", "true", false, false);
        parameters.addParameter("DefaultPixelValue",  "0", false, true);
        parameters.addParameter("Optimizer", "AdaptiveStochasticGradientDescent", false, false);
        parameters.addParameter("WriteTransformParametersEachIteration", "false", false, false);
        parameters.addParameter("WriteTransformParametersEachResolution", "false", false, false);
        parameters.addParameter("WriteResultImageAfterEachResolution", "false", false, false);
        parameters.addParameter("WritePyramidImagesAfterEachResolution", "false", false, false);
        parameters.addParameter("FixedInternalImagePixelType", "float", false, false);
        parameters.addParameter("MovingInternalImagePixelType", "float", false, false);
        parameters.addParameter("UseDirectionCosines", "false", false, false);
        parameters.addParameter("AutomaticParameterEstimation", "true", false, false);
        parameters.addParameter("AutomaticScalesEstimation", "true", false, false);
        parameters.addParameter( "Metric", "AdvancedMattesMutualInformation", true, false );
        parameters.addParameter("NumberOfHistogramBins", "32", false, true); // needed for AdvancedMattesMutualInformation
        parameters.addParameter("HowToCombineTransforms", "Compose", false, false);
        parameters.addParameter("ErodeMask", "false", false, false);

        if ( setResultImageBitDepth( parameters ) ) return null;
    }

    private void addChannelWeights( ElastixParameters elastixParameters )
    {
        String key = "MetricCHANNELWeight";

        for ( int c = 0; c < settings.fixedToMovingChannel.size(); ++c )
        {
            String channelKey = key.replace( "CHANNEL", ""+c  );
            String channelWeight = "" + settings.channelWeights[ c ];
            elastixParameters.addParameter( channelKey, channelWeight, false, true  );
        }
    }

    private boolean setResultImageBitDepth( ElastixParameters parameters )
    {
        if ( settings.movingImageBitDepth == 8 )
        {
            parameters.addParameter( "ResultImagePixelType", "unsigned char", false, false );
        }
        else if ( settings.movingImageBitDepth == 16 )
        {
            parameters.addParameter( "ResultImagePixelType", "unsigned short", false, false );
        }
        else
        {
            Utils.logErrorAndExit( settings,"Bit depth " + settings.movingImageBitDepth + " not supported.");
            return true;
        }
        return false;
    }

}
