package de.embl.cba.elastixwrapper.elastix;


import java.util.ArrayList;
import java.util.List;

public class ElastixTransformationParameters
{

    private ElastixSettings settings;
    List<String> parameters;

    public ElastixTransformationParameters( ElastixSettings settings )
    {
        this.settings = settings;
    }


    private void addChannelWeights()
    {
        String key = "MetricCHANNELWeight";

        for ( int c = 0; c < settings.numChannels; ++c )
        {
            String channelKey = key.replace( "CHANNEL", ""+c  );
            String channelWeight = "" + settings.channelWeights[ c ];
            addParameter( channelKey, channelWeight, false, true  );
        }
    }

    private void addParameter( String key, String value, boolean isMultiChannelParameter, boolean isNumeric )
    {
        String keyValues = "(KEY VALUES)";

        keyValues = setKey( key, keyValues );
        keyValues = setValues( value, keyValues, isMultiChannelParameter, isNumeric );

        parameters.add( keyValues );
    }

    private String setValues( String value, String keyValues, boolean isMultiChannelParameter, boolean isNumeric )
    {
        String values = "";

        int n = isMultiChannelParameter ? settings.numChannels : 1;

        for ( int c = 0; c < n; ++c )
        {
            if ( isNumeric )
            {
                values += value + " ";
            }
            else
            {
                values += "\"" + value + "\"" + " ";
            }

        }

        return keyValues.replace( "VALUES", values );

    }

    private String setKey( String key, String keyValues )
    {
        return keyValues.replace( "KEY", key );
    }


    public List<String> getDetlevStyle( )
    {
        parameters = new ArrayList<>();

        if ( settings.numChannels > 1 )
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
        parameters.add("(NumberOfResolutions " + settings.resolutionPyramid.split(";").length + ")");
        parameters.add("(ImagePyramidSchedule " + settings.resolutionPyramid.replace(";"," ").replace(","," ")+")");
        addParameter( "FixedImagePyramid", "FixedSmoothingImagePyramid", true, false );
        addParameter( "MovingImagePyramid", "MovingSmoothingImagePyramid", true, false );
        parameters.add("(FinalGridSpacingInVoxels " + settings.bSplineGridSpacing.replace(",", " ") + " )");

        // Initialisation
        parameters.add("(AutomaticTransformInitialization \"true\")");
        parameters.add("(AutomaticTransformInitializationMethod \"CenterOfGravity\")");

        // Samples
        parameters.add("(NumberOfSpatialSamples " + settings.spatialSamples.replace(";"," ") +")");
        addParameter( "ImageSampler", "RandomCoordinate", true, false );
        parameters.add("(NewSamplesEveryIteration \"true\")");

        if ( settings.bitDepth == 8 )
            parameters.add("(ResultImagePixelType \"unsigned char\")");
        else if ( settings.bitDepth == 16 )
            parameters.add("(ResultImagePixelType \"unsigned short\")");
        else
        {
            settings.logService.error("Bit depth " + settings.bitDepth + " not supported.");
            return null;
        }

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


    public List<String> getHenningStyle()
    {
        parameters = new ArrayList<>();

        parameters.add("(CheckNumberOfSamples \"false\")");

        parameters.add("(Transform \"" + settings.transformationType + "Transform\")");
        parameters.add("(NumberOfResolutions " + settings.resolutionPyramid.split(";").length + ")");
        parameters.add("(MaximumNumberOfIterations " + settings.iterations + ")");
        parameters.add("(ImagePyramidSchedule " + settings.resolutionPyramid.replace(";"," ").replace(","," ")+")");
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

        if ( settings.bitDepth == 8 )
            parameters.add("(ResultImagePixelType \"unsigned char\")");
        else if ( settings.bitDepth == 16 )
            parameters.add("(ResultImagePixelType \"unsigned short\")");
        else
        {
            settings.logService.error("Bit depth " + settings.bitDepth + " not supported.");
            return null;
        }

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

}
