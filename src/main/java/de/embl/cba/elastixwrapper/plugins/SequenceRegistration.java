package de.embl.cba.elastixwrapper.plugins;

import de.embl.cba.elastixwrapper.elastix.ElastixSettings;
import de.embl.cba.elastixwrapper.elastix.ElastixTransformationParameters;
import de.embl.cba.elastixwrapper.logging.IJLazySwingLogger;
import de.embl.cba.elastixwrapper.logging.Logger;
import de.embl.cba.elastixwrapper.metaimage.MetaImage_Reader;
import de.embl.cba.elastixwrapper.metaimage.MetaImage_Writer;
import de.embl.cba.elastixwrapper.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by tischi on 30/04/17.
 */
public class SequenceRegistration
{

    ImagePlus imp, impOut = null;
    String inputImages;
    String outputImages;
    ElastixSettings settings;
    Logger logger = new IJLazySwingLogger();

    public SequenceRegistration( String inputImages, String outputImages, ElastixSettings registrationSettings )
    {
        this.inputImages = inputImages;
        this.outputImages = outputImages;
        this.settings = registrationSettings;

        if ( inputImages.equals( SequenceRegistrationGUI.IMAGEPLUS) )
        {
            this.imp = IJ.getImage();
        }

    }

    public void run()
    {

        if ( outputImages.equals( SequenceRegistrationGUI.IMAGEPLUS ) )
        {
            impOut = imp.duplicate();
            IJ.run(impOut, "Select All", "");
            IJ.run(impOut, "Clear", "stack");
            IJ.run(impOut, "Select None", "");
            impOut.setTitle("Registered-" + imp.getTitle());
            impOut.show();
        }

        if ( settings.transformationType.equals( SequenceRegistrationGUI.ELASTIX ) )
        {
            Elastix registration = new Elastix();
            registration.setReference();
            registration.setParameters();
            new Thread(new Runnable() { public void run() {
                            registration.run();
                        }}).start();

        }


    }

    public int getNumberOfImages()
    {
        if( inputImages.equals( SequenceRegistrationGUI.IMAGEPLUS) )
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

        String parameterFilePath = settings.workingDirectory + "elastix_parameters.txt";
        String pathMaskImage = null;


        public Elastix()
        {
            createOrEmptyTmpDir();

            if ( (settings.roi != null) || (settings.zRange[0] > 1)  || (settings.zRange[1] < imp.getNSlices()))
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


            pathMaskImage = settings.workingDirectory + nameMaskImage + fileType;

            if ( fileType.equals(".mhd") )
            {
                MetaImage_Writer writer = new MetaImage_Writer();
                writer.save(impMask, settings.workingDirectory, nameMaskImage + fileType);
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
            File directory = new File( settings.workingDirectory );
            if (! directory.exists() )
            {
                directory.mkdir();
            }
            else
            {
                for( File file : directory.listFiles() )
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
            if ( inputImages.equals( SequenceRegistrationGUI.IMAGEPLUS ) )
            {
                saveFrame(settings.workingDirectory, nameReferenceImage, settings.reference, settings.background);
            }
        }

        public void setParameters()
        {
            Utils.saveStringListToFile( ElastixTransformationParameters.getParametersHenning( settings ), parameterFilePath );
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


            public Registerer( int s, int e, int d )
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
                    putTransformedImageToImagePlus( t, SequenceRegistrationGUI.IMAGEPLUS );
                }


                //applyTransformation(getImagePath(inputImages, i),
                //        getImagePath(outputImages, i),
                //        transformations[i]);


            }

            public String transform( int t, String pathTransformation )
            {
                saveFrame( settings.workingDirectory, nameMovingImage, t, settings.background );

                if ( settings.recursive)
                {
                    if ( firstTransformation )
                    {
                        sysCallElastix(settings.workingDirectory + nameReferenceImage + fileType,
                                       settings.workingDirectory + nameMovingImage + fileType,
                                       null);
                        firstTransformation = false;
                    }
                    else
                    {
                        sysCallElastix(settings.workingDirectory + "result.0" + fileType,
                                       settings.workingDirectory +nameMovingImage+fileType,
                                       pathTransformation);
                    }

                    try
                    {
                        pathTransformation = settings.workingDirectory + "IntitialTransformParameters."+t+".txt";
                        copyFile(settings.workingDirectory + "TransformParameters.0.txt",
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
                    sysCallElastix(settings.workingDirectory +nameReferenceImage+fileType,
                            settings.workingDirectory +nameMovingImage+fileType,
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

            private String sysCallElastix( String pathReferenceImage, String pathMovingImage, String pathInitialTransformation )
            {
                ProcessBuilder pb = new ProcessBuilder();

                List<String> args = new ArrayList<>();
                if ( settings.os.equals("Mac") )
                {
                    logger.info("Settings for Mac");
                    args.add(settings.elastixDirectory + "run_elastix.sh");
                }
                else if ( settings.os.equals("Linux") )
                {
                    logger.info("Settings for Linux");
                    args.add(settings.elastixDirectory + "run_elastix.sh");
                }
                else if ( settings.os.equals("Windows") )
                {
                    logger.info("Setting system variables for Windows OS");
                    Map<String, String> env = pb.environment();
                    //logger.info(env.get("PATH"));
                    env.put("PATH", settings.elastixDirectory + ":$PATH");
                    //logger.info(env.get("PATH"));
                    args.add(settings.elastixDirectory + "elastix.exe"); // command name
                }

                args.add("-p");
                args.add( parameterFilePath );
                args.add("-out");
                args.add(settings.workingDirectory );
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
                    String tempOut = Utils.convertStreamToStr( myIS );
                    logger.info(tempOut);
                    //p.waitFor();
                }
                catch (Exception e)
                {
                    logger.error("" + e);
                }

                return("");
            }



            public void applyTransformation(int t, String transformation)
            {

            }


            public void putTransformedImageToImagePlus(int t, String outputImage)
            {

                ImagePlus impTmp = null;

                if ( outputImage.equals( SequenceRegistrationGUI.IMAGEPLUS) )
                {

                    File file = new File(settings.workingDirectory + "result.0" + fileType);
                    if (! file.exists() )
                    {
                        logger.error("Elastix output file not found: "+settings.workingDirectory + "result.0" + fileType +
                        "\nPlease check the Log window.");
                        //"\nPlease check the log file: "+settings.workingDirectory + "elastix.log");

                    }

                    if ( fileType.equals(".tif") )
                    {
                        impTmp = IJ.openImage(settings.workingDirectory + "result.0" + fileType);
                    }
                    else if ( fileType.equals(".mhd") )
                    {
                        MetaImage_Reader reader = new MetaImage_Reader();
                        impTmp = reader.load(settings.workingDirectory, "result.0" + fileType, false);
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
