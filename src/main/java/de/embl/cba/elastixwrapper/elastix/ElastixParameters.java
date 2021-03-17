package de.embl.cba.elastixwrapper.elastix;


import de.embl.cba.elastixwrapper.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ElastixParameters
{
    public enum TransformationType
    {
        Translation,
        Euler,
        Similarity,
        BSpline
    }

    public static final String FINAL_RESAMPLER_LINEAR = "FinalLinearInterpolator";
    public static final String FINAL_RESAMPLER_NEAREST_NEIGHBOR = "FinalNearestNeighborInterpolator";

    List<String> parameters;

    public ElastixParameters( TransformationType transformationType )
    {
        addParameter("Transform", transformationType.toString() + "Transform", false, false );
    }

    public void addParameter(
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

}
