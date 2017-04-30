package registrationTools.imageFilter;

import ij.ImagePlus;

/**
 * Created by tischi on 21/04/17.
 */
public class DoNotFilter implements ImageFilter {

    public ImagePlus filter(ImagePlus imp)
    {
        // do nothing
        return imp;
    }

}
