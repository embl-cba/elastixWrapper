package registrationTools;

import ij.IJ;
import ij.ImagePlus;

/**
 * Created by tischi on 30/04/17.
 */
public class RegistrationTools {

    ImagePlus imp;
    String inputImages;
    String outputImages;
    RegistrationSettings registrationSettings

    public void RegistrationTools(String inputImages,
                                  String outputImages,
                                  RegistrationSettings registrationSettings)
    {
        this.inputImages = inputImages;
        this.outputImages = outputImages;
        this.registrationSettings = registrationSettings;

        if ( inputImages.equals(RegistrationToolsGUI.CURRENT_IMAGE) )
        {
            this.imp = IJ.getImage();
        }

    }

    public void run()
    {
        if ( registrationSettings.method.equals(RegistrationToolsGUI.ELASTIX) )
        {
            Elastix registration = new Elastix();
        }

        registration.run();

    }
    
    public int getNumberOfImages()
    {
        if( inputImages.equals(RegistrationToolsGUI.CURRENT_IMAGE) )
        {
            return imp.getNFrames();
        }
        else
        {
            // check how many time-points there are in the folder...
        }
    }


    public class Elastix
    {
        Elastix()
        {
        }

        public void run()
        {

            int numImages = getNumberOfImages();
            String[] transformations = new String[numImages];

            // Compute transformation


            // Apply transformation

        }

    }


}
