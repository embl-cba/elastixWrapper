package de.embl.cba.elastixwrapper.elastix;


import de.embl.cba.elastixwrapper.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ElastixParameters
{

    private ElastixSettings settings;
    List<String> parameters;

    public ElastixParameters( ElastixSettings settings )
    {
        this.settings = settings;
    }


    private void addChannelWeights()
    {
        String key = "MetricCHANNELWeight";

        for ( int c = 0; c < settings.fixedToMovingChannel.size(); ++c )
        {
            String channelKey = key.replace( "CHANNEL", ""+c  );
            String channelWeight = "" + settings.channelWeights[ c ];
            addParameter( channelKey, channelWeight, false, true  );
        }
    }

    private void addParameter(
            String key,
            String value,
            boolean isMultiChannelParameter,
            boolean isNumeric )
    {
        String keyValues = "(KEY VALUES)";

        keyValues = setKey( key, keyValues );
        keyValues = setValues( value, keyValues, isMultiChannelParameter, isNumeric );

        parameters.add( keyValues );
    }

    private String setValues( String value, String keyValues, boolean isMultiChannelParameter, boolean isNumeric )
    {
        String values = "";

        int n = isMultiChannelParameter ? settings.fixedToMovingChannel.size() : 1;

        for ( int c = 0; c < n; ++c )
            if ( isNumeric )
                values += value + " ";
            else
                values += "\"" + value + "\"" + " ";


        return keyValues.replace( "VALUES", values );

    }

    private String setKey( String key, String keyValues )
    {
        return keyValues.replace( "KEY", key );
    }


    public List<String> getDefaultParameters( )
    {
        parameters = new ArrayList<>();

        // User defined
        parameters.add( "(Transform \"" + settings.transformationType + "Transform\")" );
        parameters.add( "(MaximumNumberOfIterations " + settings.iterations + ")" );
        parameters.add( "(ImagePyramidSchedule " +
                settings.downSamplingFactors.replace( ";", " " ).replace( ",", " " ) + ")" );
        parameters.add( "(NumberOfSpatialSamples " +
                settings.spatialSamples.replace( ";", " " ) + ")" );
        parameters.add( "(FinalGridSpacingInVoxels " + settings.bSplineGridSpacing.replace( ",", " " ) + " )" );


        if ( settings.fixedToMovingChannel.size() > 1 )
        {
            addParameter(
                    "Registration",
                    "MultiMetricMultiResolutionRegistration",
                    false,
                    false );
            addChannelWeights();
        }
        else
        {
            parameters.add( "(Registration \"MultiResolutionRegistration\")" );
        }

        addParameter( "CheckNumberOfSamples", "false", false, false );

        // Pyramid
        parameters.add( "(NumberOfResolutions " + settings.downSamplingFactors.split( ";" ).length + ")" );
        addParameter( "FixedImagePyramid", "FixedSmoothingImagePyramid", true, false );
        addParameter( "MovingImagePyramid", "MovingSmoothingImagePyramid", true, false );

        // Initialisation
        parameters.add( "(AutomaticTransformInitialization \"true\")" );
        parameters.add( "(AutomaticTransformInitializationMethod \"CenterOfGravity\")" );

        // Samples
        addParameter( "ImageSampler", "RandomCoordinate", true, false );
        parameters.add( "(NewSamplesEveryIteration \"true\")" );

        if ( setResultImageBitDepth() ) return null;

        parameters.add("(DefaultPixelValue 0)");
        parameters.add("(Optimizer \"AdaptiveStochasticGradientDescent\")");

        parameters.add("(WriteTransformParametersEachIteration \"false\")");
        parameters.add("(WriteTransformParametersEachResolution \"false\")");
        parameters.add("(WriteResultImageAfterEachResolution \"false\")");
        parameters.add("(WritePyramidImagesAfterEachResolution \"false\")");
        parameters.add("(FixedInternalImagePixelType \"float\")");
        parameters.add("(MovingInternalImagePixelType \"float\")");
        parameters.add("(UseDirectionCosines \"false\")");

        addParameter( "Interpolator", "LinearInterpolator", true, false );
        parameters.add("(ResampleInterpolator \"" + settings.finalResampler + "\")");
        parameters.add("(AutomaticParameterEstimation \"true\")");
        parameters.add("(AutomaticScalesEstimation \"true\")");

//        if ( settings.transformationType.equals( ElastixSettings.SPLINE ) )
//        {
//            parameters.add("(UseRandomSampleRegion \"true\")");
//            final double sampleRegionSize = settings.imageWidthMillimeter / 5;
//            String sampleRegionSizeString = "";
//            for ( int d = 0; d < 3; d++ )
//                sampleRegionSizeString += sampleRegionSize + " ";
//            parameters.add("(SampleRegionSize " + sampleRegionSizeString +" )");
//        }

        // Metric
        addParameter(
                "Metric",
                "AdvancedMattesMutualInformation",
                true,
                false );

        parameters.add("(NumberOfHistogramBins 32)"); // needed for AdvancedMattesMutualInformation

        parameters.add("(HowToCombineTransforms \"Compose\")");
        parameters.add("(ErodeMask \"false\")");

        parameters.add("(WriteResultImage \"false\")");

        //parameters.add("(BSplineInterpolationOrder 1)");
        //parameters.add("(FinalBSplineInterpolationOrder 3)");
        //parameters.add("(WriteResultImage \"true\")");
        //parameters.add("(ResultImageFormat \"" + settings.resultImageFileType + "\")");

        return( parameters );
    }

    public List<String> getHenningStyleParameters()
    {
        parameters = new ArrayList<>();

        parameters.add("(CheckNumberOfSamples \"false\")");

        parameters.add("(Transform \"" + settings.transformationType + "Transform\")");
        parameters.add("(NumberOfResolutions " + settings.downSamplingFactors.split(";").length + ")");
        parameters.add("(MaximumNumberOfIterations " + settings.iterations + ")");
        parameters.add("(ImagePyramidSchedule " + settings.downSamplingFactors.replace(";"," ").replace(","," ")+")");
        parameters.add("(FinalGridSpacingInVoxels " + settings.bSplineGridSpacing.replace(",", " ") + " )");

        // Spatial Samples
        parameters.add("(NumberOfSpatialSamples " +
                settings.spatialSamples.
                        replace(";"," ").
                        replace("full","0")
                +")");

        // ImageSampler
        String imageSampler = "(ImageSampler ";
        for ( String s : settings.spatialSamples.split(";") )
        {
            imageSampler += s.equals("full") ? " \"Full\" " : " \"Random\" ";
        }
        imageSampler += ")";
        parameters.add(imageSampler);

        if ( setResultImageBitDepth() ) return null;

        parameters.add("(DefaultPixelValue 0)");
        parameters.add("(Optimizer \"AdaptiveStochasticGradientDescent\")");

        parameters.add("(Registration \"MultiResolutionRegistration\")");
        parameters.add("(WriteTransformParametersEachIteration \"false\")");
        parameters.add("(WriteTransformParametersEachResolution \"false\")");
        parameters.add("(WriteResultImageAfterEachResolution \"false\")");
        parameters.add("(WritePyramidImagesAfterEachResolution \"false\")");
        parameters.add("(FixedInternalImagePixelType \"float\")");
        parameters.add("(MovingInternalImagePixelType \"float\")");
        parameters.add("(UseDirectionCosines \"false\")");
        parameters.add("(Interpolator \"LinearInterpolator\")");
        parameters.add("(ResampleInterpolator \"FinalLinearInterpolator\")");
        parameters.add("(FixedImagePyramid \"FixedRecursiveImagePyramid\")");
        parameters.add("(MovingImagePyramid \"MovingRecursiveImagePyramid\")");
        parameters.add("(AutomaticParameterEstimation \"true\")");
        parameters.add("(AutomaticScalesEstimation \"true\")");
        parameters.add("(Metric \"AdvancedMeanSquares\")");
        parameters.add("(AutomaticTransformInitialization \"false\")");
        parameters.add("(HowToCombineTransforms \"Compose\")");
        parameters.add("(ErodeMask \"false\")");
        parameters.add("(NewSamplesEveryIteration \"true\")");

        parameters.add("(BSplineInterpolationOrder 1)");
        parameters.add("(FinalBSplineInterpolationOrder 3)");
        parameters.add("(WriteResultImage \"true\")");
        parameters.add("(ResultImageFormat \"" + settings.resultImageFileType + "\")");

        return( parameters );
    }

    private boolean setResultImageBitDepth()
    {
        if ( settings.movingImageBitDepth == 8 )
        {
            parameters.add( "(ResultImagePixelType \"unsigned char\")" );
        }
        else if ( settings.movingImageBitDepth == 16 )
        {
            parameters.add( "(ResultImagePixelType \"unsigned short\")" );
        }
        else
        {
            Utils.logErrorAndExit( settings,"Bit depth " + settings.movingImageBitDepth + " not supported.");
            return true;
        }
        return false;
    }

    public List<String> getGiuliaMizzonStyleParameters()
    {
        parameters = new ArrayList<>();

        if ( settings.fixedToMovingChannel.size() > 1 )
        {
            addParameter( "Registration", "MultiMetricMultiResolutionRegistration", false, false );
            addChannelWeights();
        }
        else
        {
            parameters.add("(Registration \"MultiResolutionRegistration\")");
        }

        addParameter( "CheckNumberOfSamples", "false", false, false );

        parameters.add("(Transform \"" + settings.transformationType + "Transform\")");
        parameters.add("(MaximumNumberOfIterations " + settings.iterations + ")");

        // Pyramid
        parameters.add("(NumberOfResolutions " + settings.downSamplingFactors.split(";").length + ")");
        parameters.add("(ImagePyramidSchedule " + settings.downSamplingFactors.replace(";"," ").replace(","," ")+")");

        // TODO: different pyramids for fixed and moving due to different resolution?
        // or re-save fluorescence image with same resolution as fixed image?
        addParameter( "FixedImagePyramid", "FixedSmoothingImagePyramid", true, false );
        addParameter( "MovingImagePyramid", "MovingSmoothingImagePyramid", true, false );
        parameters.add("(FinalGridSpacingInVoxels " + settings.bSplineGridSpacing.replace(",", " ") + " )");

        // Samples
        parameters.add("(NumberOfSpatialSamples " + settings.spatialSamples.replace(";"," ") +")");
        addParameter( "ImageSampler", "RandomCoordinate", true, false );
        parameters.add("(NewSamplesEveryIteration \"true\")");

        if ( setResultImageBitDepth() ) return null;

        parameters.add("(DefaultPixelValue 0)");
        parameters.add("(Optimizer \"AdaptiveStochasticGradientDescent\")");

        parameters.add("(WriteTransformParametersEachIteration \"false\")");
        parameters.add("(WriteTransformParametersEachResolution \"false\")");
        parameters.add("(WriteResultImageAfterEachResolution \"false\")");
        parameters.add("(WritePyramidImagesAfterEachResolution \"false\")");
        parameters.add("(FixedInternalImagePixelType \"float\")");
        parameters.add("(MovingInternalImagePixelType \"float\")");
        parameters.add("(UseDirectionCosines \"false\")");

        addParameter( "Interpolator", "LinearInterpolator", true, false );
        parameters.add("(ResampleInterpolator \"FinalLinearInterpolator\")");
        parameters.add("(AutomaticParameterEstimation \"true\")");
        parameters.add("(AutomaticScalesEstimation \"true\")");

        // Metric
        addParameter( "Metric", "AdvancedMattesMutualInformation", true, false );
        parameters.add("(NumberOfHistogramBins 32)"); // needed for AdvancedMattesMutualInformation

        parameters.add("(HowToCombineTransforms \"Compose\")");
        parameters.add("(ErodeMask \"false\")");

        //parameters.add("(BSplineInterpolationOrder 1)");
        //parameters.add("(FinalBSplineInterpolationOrder 3)");
        //parameters.add("(WriteResultImage \"true\")");
        //parameters.add("(ResultImageFormat \"" + settings.resultImageFileType + "\")");

        return( parameters );
    }

}
