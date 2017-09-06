package registrationTools;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;
import registrationTools.logging.IJLazySwingLogger;
import registrationTools.logging.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by tischi on 30/04/17.
 */
public class RegistrationTools {

    ImagePlus imp, impOut = null;
    String inputImages;
    String outputImages;
    RegistrationSettings settings;
    Logger logger = new IJLazySwingLogger();

    public RegistrationTools(String inputImages,
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

        if ( outputImages.equals(RegistrationToolsGUI.IMAGEPLUS) )
        {
            impOut = imp.duplicate();
            IJ.run(impOut, "Select All", "");
            IJ.run(impOut, "Clear", "stack");
            IJ.run(impOut, "Select None", "");
            impOut.setTitle("Registered-" + imp.getTitle());
            impOut.show();
        }

        if ( settings.method.equals(RegistrationToolsGUI.ELASTIX) )
        {
            Elastix registration = new Elastix();
            registration.setReference();
            registration.setParameters();
            new Thread(new Runnable() {
                        public void run() {
                            registration.run();
                        }}).start();

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

    public class Elastix
    {
        String nameReferenceImage = "reference";
        String nameMovingImage = "moving";
        String nameMaskImage = "mask";

        String fileType = ".mhd";

        String pathParameterFile = settings.folderTmp + "elastix_parameters.txt";
        String pathMaskImage = null;


        public Elastix()
        {
            createOrEmptyTmpDir();

            if ( (settings.roi != null) ||
                    (settings.zRange[0] > 1)  ||
                        (settings.zRange[1] < imp.getNSlices()))
            {
                createMaskFile();
            }
        }

        private void createMaskFile()
        {
            Duplicator duplicator = new Duplicator();
            ImagePlus impMask = duplicator.run(imp, 1, 1, 1, imp.getNSlices(), 1, 1);
            IJ.setBackgroundColor(0, 0, 0);
            IJ.run(impMask, "Select All", "");
            IJ.run(impMask, "Clear", "stack");
            IJ.run(impMask, "Select None", "");
            IJ.run(impMask, "8-bit", "");
            for (int i = settings.zRange[0]; i <= settings.zRange[1]; i++) {
                impMask.setSlice(i);
                ImageProcessor ip = impMask.getProcessor();
                ip.setColor(1);
                ip.setRoi(settings.roi);
                ip.fill();
            }


            pathMaskImage = settings.folderTmp + nameMaskImage + fileType;

            if ( fileType.equals(".mhd") )
            {
                MetaImage_Writer writer = new MetaImage_Writer();
                writer.save(impMask, settings.folderTmp, nameMaskImage + fileType);
            }
            else if ( fileType.equals(".tif") )
            {
                IJ.saveAs(impMask, "Tiff", pathMaskImage);
            }

            // show mask to user
            /*
            IJ.run(impMask, "Multiply...", "value=255 stack");
            impMask.setTitle("Mask");
            impMask.show();
            */

        }

        private void createOrEmptyTmpDir()
        {
            File directory = new File(settings.folderTmp);
            if (! directory.exists() )
            {
                directory.mkdir();
            }
            else
            {
                for(File file : directory.listFiles())
                    if ( !file.isDirectory() )
                        file.delete();
            }

        }

        public void run()
        {

            int numImages = getNumberOfImages();
            String[] transformations = new String[numImages];
            String fixedImage, movingImage;

            // forward
            if( settings.regRange[1] > settings.reference )
            {
                Registerer registerer = new Registerer(
                        Math.max(settings.reference, settings.regRange[0]),
                        settings.regRange[1],
                        settings.delta
                );
                registerer.run();
            }

            // backward
            if( settings.regRange[0] < settings.reference )
            {
                Registerer registerer = new Registerer(
                        Math.min(settings.reference, settings.regRange[1]),
                        settings.regRange[0],
                        settings.delta
                );
                registerer.run();
            }

        }

        public void setReference()
        {
            if ( inputImages.equals(RegistrationToolsGUI.IMAGEPLUS) )
            {
                saveFrame(settings.folderTmp, nameReferenceImage,
                          settings.reference,
                          settings.background);
            }
        }

        public void setParameters()
        {
            saveParameters(getParametersHenningNo5());
        }

        public void saveParameters(List<String> parameters)
        {
            try
            {
                FileWriter writer = new FileWriter(pathParameterFile);
                for (String str : parameters)
                {
                    writer.write(str+"\n");
                }
                writer.close();
            }
            catch (Exception e)
            {
                logger.error(e.toString());
            }
        }

        public List<String> getParametersHenningNo5()
        {
            List<String> parameters = new ArrayList<>();

            parameters.add("(CheckNumberOfSamples \"false\")");

            parameters.add("(Transform \"" + settings.type + "Transform\")");
            parameters.add("(NumberOfResolutions "+settings.resolutionPyramid.split(";").length+")");
            parameters.add("(MaximumNumberOfIterations "+settings.iterations +")");
            parameters.add("(ImagePyramidSchedule "+settings.resolutionPyramid.replace(";"," ")+")");
            parameters.add("(FinalGridSpacingInVoxels "+settings.bSplineGridSpacing+" )");

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
                logger.error("Bit depth " + settings.bitDepth + " not supported.");
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
            parameters.add("(ResultImageFormat \""+fileType.replace(".","")+"\")");

            return(parameters);
        }

        public void saveFrame(String folder, String file, int t, double background)
        {

            Duplicator duplicator = new Duplicator();
            ImagePlus imp2 = duplicator.run(imp, 1, 1, 1, imp.getNSlices(), t, t);

            if ( background > 0 )
            {
                IJ.run(imp2, "Subtract...", "value="+background+" stack");
            }

            if ( fileType.equals(".mhd") )
            {
                MetaImage_Writer writer = new MetaImage_Writer();
                writer.save(imp2, folder, file + fileType);
            }
            else if ( fileType.equals(".tif") )
            {
                IJ.saveAs(imp2, "Tiff", folder + file + fileType);
            }
        }

        class Registerer implements Runnable {
            int s, e, d;
            IntStream range;
            Boolean firstTransformation = true;


            public Registerer(int s, int e, int d)
            {
                this.s = s;
                this.e = e;
                this.d = d;

                if ( e < s )
                {
                    // revert order
                    range = IntStream.rangeClosed(e, s)
                            .map(i -> s - i + e );
                }
                else
                {
                    // keep order
                    range = IntStream.rangeClosed(s, e)
                            .map(i -> i);
                }

            }

            public void run()
            {
                // TODO: check out "Parallel()"
                String pathTransformation = null;

                for ( int t : range.toArray() )
                {
                    logger.info("ref: " + settings.reference +
                            " reg: " + t +
                            " t0: " + pathTransformation);

                    pathTransformation = transform( t, pathTransformation );

                    //applyTransformation(t, transformation);
                    putTransformedImageToImagePlus(t,
                            RegistrationToolsGUI.IMAGEPLUS);
                }


                //applyTransformation(getImagePath(inputImages, i),
                //        getImagePath(outputImages, i),
                //        transformations[i]);


            }

            public String transform(int t, String pathTransformation)
            {
                saveFrame(settings.folderTmp, nameMovingImage,
                          t, settings.background);

                if ( settings.recursive)
                {
                    if ( firstTransformation )
                    {
                        sysCallElastix(settings.folderTmp+nameReferenceImage+fileType,
                                       settings.folderTmp+nameMovingImage+fileType,
                                       null);
                        firstTransformation = false;
                    }
                    else
                    {
                        sysCallElastix(settings.folderTmp + "result.0" + fileType,
                                       settings.folderTmp+nameMovingImage+fileType,
                                       pathTransformation);
                    }

                    try
                    {
                        pathTransformation = settings.folderTmp + "IntitialTransformParameters."+t+".txt";
                        copyFile(settings.folderTmp + "TransformParameters.0.txt",
                                 pathTransformation);
                    }
                    catch (Exception e)
                    {
                        logger.error(e.toString());
                    }

                }
                else
                {
                    // simply always register against reference
                    sysCallElastix(settings.folderTmp+nameReferenceImage+fileType,
                            settings.folderTmp+nameMovingImage+fileType,
                            null);
                }

                return ( pathTransformation );
            }

            private void copyFile(String source, String dest) throws IOException {
                Path FROM = Paths.get(source);
                Path TO = Paths.get(dest);
                //overwrite existing file, if exists
                CopyOption[] options = new CopyOption[]{
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                };
                Files.copy(FROM, TO, options);
            }

            private String sysCallElastix(String pathReferenceImage,
                                          String pathMovingImage,
                                          String pathInitialTransformation)
            {


                ProcessBuilder pb = new ProcessBuilder();

                List<String> args = new ArrayList<>();
                if ( settings.os.equals("Mac") )
                {
                    logger.info("Settings for Mac");
                    Map<String, String> env = pb.environment();
                    env.put( "DYLD_LIBRARY_PATH", settings.folderElastix + "lib" + ":$DYLD_LIBRARY_PATH");
                    logger.info("DYLD_LIBRARY_PATH = " + env.get("DYLD_LIBRARY_PATH"));
                    logger.info( "elastix binary = " + settings.folderElastix + "bin/elastix");
                    args.add(settings.folderElastix + "bin/elastix"); // command name
                }
                else if ( settings.os.equals("Linux") )
                {
                    logger.info("Settings for Linux");
                    Map<String, String> env = pb.environment();
                    args.add(settings.folderElastix + "bin/run_elastix.sh"); // command name
                }
                else if ( settings.os.equals("Windows") )
                {
                    logger.info("Setting system variables for Windows OS");
                    Map<String, String> env = pb.environment();
                    //logger.info(env.get("PATH"));
                    env.put("PATH", settings.folderElastix + ":$PATH");
                    //logger.info(env.get("PATH"));
                    args.add(settings.folderElastix + "elastix.exe"); // command name
                }

                args.add("-p");
                args.add(pathParameterFile);
                args.add("-out");
                args.add(settings.folderTmp);
                args.add("-f");
                args.add(pathReferenceImage);
                args.add("-m");
                args.add(pathMovingImage);
                args.add("-threads");
                args.add(""+settings.workers);

                if ( pathInitialTransformation != null )
                {
                    args.add("-t0");
                    args.add(pathInitialTransformation);
                }

                if ( pathMaskImage != null )
                {
                    args.add("-fMask");
                    args.add(pathMaskImage);
                }

                pb = new ProcessBuilder(args);

                String cmd = "";
                for (String s : pb.command() )
                {
                    cmd = cmd + " " + s;
                }
                logger.info("Command launched:" + cmd);

                try
                {
                    pb.redirectErrorStream(true);
                    final Process process = pb.start();
                    InputStream myIS = process.getInputStream();
                    String tempOut = convertStreamToStr(myIS);
                    logger.info(tempOut);
                    //p.waitFor();
                }
                catch (Exception e)
                {
                    logger.error("" + e);
                }

                return("");
            }

            public String convertStreamToStr(InputStream is) throws IOException {

                if (is != null) {
                    Writer writer = new StringWriter();

                    char[] buffer = new char[1024];
                    try {
                        Reader reader = new BufferedReader(new InputStreamReader(is,
                                "UTF-8"));
                        int n;
                        while ((n = reader.read(buffer)) != -1) {
                            writer.write(buffer, 0, n);
                        }
                    } finally {
                        is.close();
                    }
                    return writer.toString();
                }
                else {
                    return "";
                }
            }

            public void applyTransformation(int t, String transformation)
            {

            }


            public void putTransformedImageToImagePlus(int t, String outputImage)
            {

                ImagePlus impTmp = null;

                if ( outputImage.equals(RegistrationToolsGUI.IMAGEPLUS) )
                {

                    File file = new File(settings.folderTmp + "result.0" + fileType);
                    if (! file.exists() )
                    {
                        logger.error("Elastix output file not found: "+settings.folderTmp + "result.0" + fileType +
                        "\nPlease check the Log window.");
                        //"\nPlease check the log file: "+settings.folderTmp + "elastix.log");

                    }

                    if ( fileType.equals(".tif") )
                    {
                        impTmp = IJ.openImage(settings.folderTmp + "result.0" + fileType);
                    }
                    else if ( fileType.equals(".mhd") )
                    {
                        MetaImage_Reader reader = new MetaImage_Reader();
                        impTmp = reader.load(settings.folderTmp, "result.0" + fileType, false);
                    }

                    ImageStack stackTmp = impTmp.getStack();
                    ImageStack stackOut = impOut.getStack();
                    int iOut = impOut.getStackIndex(1, 1, t);
                    for ( int i = 0; i < stackTmp.size(); i++ )
                    {
                        stackOut.setProcessor( stackTmp.getProcessor(i + 1), iOut++ );
                    }

                    impOut.updateAndDraw();
                    impOut.setT(t);
                }
            }

        }

    }

}
