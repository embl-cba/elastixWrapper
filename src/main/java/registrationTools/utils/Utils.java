/*
 * #%L
 * Data streaming, tracking and cropping tools
 * %%
 * Copyright (C) 2017 Christian Tischer
 *
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package registrationTools.utils;

import registrationTools.VirtualStackOfStacks.VirtualStackOfStacks;
import registrationTools.logging.IJLazySwingLogger;
import registrationTools.logging.Logger;
import ij.IJ;
import ij.ImagePlus;
import javafx.geometry.Point3D;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by tischi on 06/11/16.
 */

public class Utils {

    public static boolean verbose = false;
    public static String LOAD_CHANNELS_FROM_FOLDERS = "from sub-folders";

    static Logger logger = new IJLazySwingLogger();

    public enum FileType {
        TIFF("Tiff"),
        HDF5("Hdf5"),
        SERIALIZED_HEADERS("Serialized headers");
        private final String text;
        private FileType(String s) {
            text = s;
        }
        @Override
        public String toString() {
            return text;
        }
    }


    public static boolean checkRange(ImagePlus imp, int min, int max, String dimension)

    {
        // setup
        //

        int Min = 0, Max = 0;

        if ( dimension.equals("z") )
        {
            Min = 1;
            Max = imp.getNSlices();
        }
        else if ( dimension.equals("t") )
        {
            Min = 1;
            Max = imp.getNFrames();
        }

        // check
        //

        if (min < Min)
        {
            logger.error(""+dimension+" minimum must be >= " + Min + "; please change the value.");
            return false;
        }

        if (max > Max)
        {
            logger.error(""+dimension+" maximum must be <= " + Max + "; please change the value.");
            return false;
        }


        return true;

    }

    public static Point3D computeOffsetFromCenterSize(Point3D pCenter, Point3D pSize) {
        return(pCenter.subtract(pSize.subtract(1, 1, 1).multiply(0.5)));
    }

    public static Point3D computeCenterFromOffsetSize(Point3D pOffset, Point3D pSize) {
        // center of width 7 is 0,1,2,*3*,4,5,6
        // center of width 6 is 0,1,2,*2.5*,3,4,5
        return(pOffset.add(pSize.subtract(1, 1, 1).multiply(0.5)));
    }

    public static Point3D multiplyPoint3dComponents(Point3D p0, Point3D p1) {

        double x = p0.getX() * p1.getX();
        double y = p0.getY() * p1.getY();
        double z = p0.getZ() * p1.getZ();

        return (new Point3D(x,y,z));

    }

    public static void show(ImagePlus imp)
    {
        imp.show();
        imp.setPosition(1, imp.getNSlices() / 2, 1);
        IJ.wait(200);
        imp.resetDisplayRange();
        imp.updateAndDraw();
    }

    public static VirtualStackOfStacks getVirtualStackOfStacks(ImagePlus imp) {
        VirtualStackOfStacks vss = null;
        try {
            vss = (VirtualStackOfStacks) imp.getStack();
            return (vss);
        } catch (Exception e) {
             logger.error("This is only implemented for images opened with the Data Streaming Tools plugin.");
            return (null);
        }
    }

    public static double[] delimitedStringToDoubleArray(String s, String delimiter) {

        String[] sA = s.split(delimiter);
        double[] nums = new double[sA.length];
        for (int i = 0; i < nums.length; i++) {
            nums[i] = Double.parseDouble(sA[i]);
        }

        return nums;
    }

    public static int[] delimitedStringToIntegerArray(String s, String delimiter) {

        String[] sA = s.split(delimiter);
        int[] nums = new int[sA.length];
        for (int i = 0; i < nums.length; i++) {
            nums[i] = Integer.parseInt(sA[i]);
        }

        return nums;
    }


    public static void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
              logger.info("" + pair.getKey() + " = " + pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
    }


    public static boolean hasVirtualStackOfStacks(ImagePlus imp) {

        if( ! (imp.getStack() instanceof VirtualStackOfStacks) ) {
             logger.error("Wrong image type. " +
                     "This method is only implemented for images opened via " +
                     "the Data Streaming Tools plugin.");
            return false;
        }
        else
        {
            return true;
        }

    }


    public static boolean checkMemoryRequirements(ImagePlus imp)
    {
        long numPixels = (long)imp.getWidth()*imp.getHeight()*imp.getNSlices()*imp.getNChannels()*imp.getNFrames();
        boolean ok = checkMemoryRequirements(numPixels, imp.getBitDepth(), 1);
        return(ok);
    }


    public static boolean checkMemoryRequirements(ImagePlus imp, int nThreads)
    {
        long numPixels = (long)imp.getWidth()*imp.getHeight()*imp.getNSlices();
        boolean ok = checkMemoryRequirements(numPixels, imp.getBitDepth(), nThreads);
        return(ok);
    }

    public static boolean checkMemoryRequirements(long numPixels, int bitDepth, int nThreads)
    {
        //
        // check that the data cube is not too large for the java indexing
        //
        long maxSize = (1L<<31) - 1;
        if( numPixels > maxSize )
        {

              logger.info("Warning: " + "The size of one requested data cube is " + numPixels + " (larger than 2^31)\n");
            //logger.error("The size of one requested data cube is "+numPixels +" (larger than 2^31)\n" +
            //        "and can thus not be loaded as one java array into RAM.\n" +
            //        "Please crop a smaller region.");
            //return(false);
        }

        //
        // check that the data cube(s) fits into the RAM
        //
        double GIGA = 1000000000.0;
        long freeMemory = IJ.maxMemory() - IJ.currentMemory();
        double maxMemoryGB = IJ.maxMemory()/GIGA;
        double freeMemoryGB = freeMemory/GIGA;
        double requestedMemoryGB = numPixels*bitDepth/8*nThreads/GIGA;

        if( requestedMemoryGB > freeMemoryGB )
        {
             logger.error("The size of the requested data cube(s) is " + requestedMemoryGB + " GB.\n" +
                     "The free memory is only " + freeMemoryGB + " GB.\n" +
                     "Please consider cropping a smaller region \n" +
                     "and/or reducing the number of I/O threads \n" +
                     "(you are currently using " + nThreads + ").");
            return(false);
        }
        else
        {
            if( requestedMemoryGB > 0.1 ) {
                logger.info("Memory [GB]: Max=" + maxMemoryGB + "; Free=" + freeMemoryGB + "; Requested=" +
                        requestedMemoryGB);
            }

        }



        return(true);

    }



}
