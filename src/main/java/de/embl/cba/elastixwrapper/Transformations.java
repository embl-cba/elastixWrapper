package de.embl.cba.elastixwrapper;

import java.util.List;

/**
 * Created by tischi on 30/04/17.
 */
public class Transformations<T>  {

    Object[] transformations;

    Transformations(int length)
    {
        transformations = new Object[length];
    }

    public void set(T e, int i)
    {
        transformations[i] = e;
    }

    public T get(int i)
    {
        return( (T) transformations[i] );
    }

}
