package registrationTools;

import ij.IJ;
import ij.ImagePlus;
import registrationTools.logging.IJLazySwingLogger;
import registrationTools.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by tischi on 30/04/17.
 */
public class RegistrationTools {

    ImagePlus imp;
    String inputImages;
    String outputImages;
    RegistrationSettings settings;
    Logger logger = new IJLazySwingLogger();

    public void RegistrationTools(String inputImages,
                                  String outputImages,
                                  RegistrationSettings registrationSettings)
    {
        this.inputImages = inputImages;
        this.outputImages = outputImages;
        this.settings = registrationSettings;

        if ( inputImages.equals(RegistrationToolsGUI.IMAGEPLUS) )
        {
            this.imp = IJ.getImage();
        }

    }

    public void run()
    {
        if ( settings.method.equals(RegistrationToolsGUI.ELASTIX) )
        {
            Elastix registration = new Elastix();
            registration.setReference();
            registration.setParameters();
            registration.run();

        }


    }

    public int getNumberOfImages()
    {
        if( inputImages.equals(RegistrationToolsGUI.IMAGEPLUS) )
        {
            return imp.getNFrames();
        }
        else
        {
            return 0;
            // check how many time-points there are in the folder...
        }
    }

    public String getImagePath(int i)
    {
        return "";
    }

    public class Elastix {

        public void run()
        {

            int numImages = getNumberOfImages();
            String[] transformations = new String[numImages];
            String fixedImage, movingImage;

            if (settings.snake)
            {
                // forward
                Registerer forwardRegisterer = new Registerer(
                        settings.reference + 1,
                        settings.last,
                        settings.delta
                );
                forwardRegisterer.run();

                // backward
                Registerer backwardRegisterer = new Registerer(
                        settings.reference + 1,
                        settings.first,
                        settings.delta
                );
                backwardRegisterer.run();

            }
        }

        public void setReference()
        {

        }

        public void setParameters()
        {

        }

        class Registerer implements Runnable {
            int s, e, d;
            IntStream range;

            Registerer(int s, int e, int d)
            {
                this.s = s;
                this.e = e;
                this.d = d;

                if ( s < e )
                {
                    range = IntStream.rangeClosed(s, e)
                            .map(i -> i );
                }
                else
                {
                    range = IntStream.range(s, e)
                            .map(i -> s - i + e - 1);
                }

            }

            public void run()
            {
                // TODO: check out "Parallel()"
                range.forEach(
                    i -> {
                        logger.info("reg "+i);
                    }
                );

                //applyTransformation(getImagePath(inputImages, i),
                //        getImagePath(outputImages, i),
                //        transformations[i]);


            }

            public void computeTransformation(String fixedImage,
                                              String movingImage)

            {

            }


            public void applyTransformation(String inputImage,
                                            String outputImage,
                                            String transformation)

            {

            }


            public void showTransformation(String inputImage,
                                           String outputImage,
                                           String transformation)

            {

            }


        }

    }

}
