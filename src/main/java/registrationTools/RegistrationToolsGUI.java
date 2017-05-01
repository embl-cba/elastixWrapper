package registrationTools;

import ij.IJ;
import ij.ImagePlus;

/**
 * Created by tischi on 30/04/17.
 */
public class RegistrationToolsGUI {

    public final static String IMAGEPLUS = "from ImageJ";
    public final static String ELASTIX = "Elastix";


    /**
     *
     * input: current image, folder
     * output: new image, folder
     *
     * method: Elastix
     *
     * Elastix settings: ...
     *
     * actions: [Run registration]
     *
     */

    public void showDialog()
    {
        testRun();
    }

    public void testRun()
    {

        ImagePlus imp = IJ.openImage("/Users/tischi/Documents/fiji-plugin-registrationTools/example-data/2d-movie--affine.tif");
        imp.show();

        RegistrationSettings settings = new RegistrationSettings();
        settings.method = RegistrationToolsGUI.ELASTIX;
        settings.first = 0;
        settings.referenceFrame = 5;
        settings.last = imp.getNFrames() - 1;
        settings.snake = true;
        settings.tmpDir = "/Users/tischi/Desktop/tmp/";

        String inputImages = RegistrationToolsGUI.IMAGEPLUS;
        String outputImages = RegistrationToolsGUI.IMAGEPLUS;

        RegistrationTools registrationTools = new RegistrationTools(inputImages, outputImages, settings);
        registrationTools.run();

    }

}
