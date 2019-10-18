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

package de.embl.cba.elastixwrapper.utils;

import ij.*;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import loci.common.services.ServiceFactory;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import ome.units.UNITS;
import org.scijava.log.LogService;

import java.io.*;
import java.util.List;

/**
 * Created by tischi on 06/11/16.
 */

public abstract class Utils {

    public static boolean verbose = false;
    public static String LOAD_CHANNELS_FROM_FOLDERS = "from sub-folders";

    public static double[] delimitedStringToDoubleArray(
            String s, String delimiter) {

        String[] sA = s.split(delimiter);
        double[] nums = new double[sA.length];
        for (int i = 0; i < nums.length; i++) {
            nums[i] = Double.parseDouble(sA[i]);
        }

        return nums;
    }

    public static int[] delimitedStringToIntegerArray(
            String s, String delimiter) {

        String[] sA = s.split(delimiter);
        int[] nums = new int[sA.length];
        for (int i = 0; i < nums.length; i++) {
            nums[i] = Integer.parseInt(sA[i]);
        }

        return nums;
    }

    public static void saveStringToFile( String text, String path  )
    {
        PrintWriter writer = null;
        try
        {
            writer = new PrintWriter( path, "UTF-8" );
        }
        catch ( FileNotFoundException e )
        {
            e.printStackTrace();
        }
        catch ( UnsupportedEncodingException e )
        {
            e.printStackTrace();
        }
        writer.write( text);
        writer.close();
    }


    public static void saveStringListToFile( List< String > parameters, String path )
    {
        try
        {
            FileWriter writer = new FileWriter( path );
            for (String str : parameters)
            {
                System.out.println( str );
                writer.write(str+"\n");
            }
            writer.close();
        }
        catch ( Exception e )
        {
            IJ.showMessage( "Writing file failed: " + path );
            System.out.print( e.toString() );
        }
    }

    public static String convertStreamToStr( InputStream is ) throws IOException {

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


    public static void executeCommand( List< String > args, LogService logService )
    {
        ProcessBuilder pb = new ProcessBuilder( args );

        String cmd = "";

        for ( String s : pb.command() )
        {
            cmd = cmd + " " + s;
        }

        logService.info("Command launched:" + cmd);

        int numAttempts = 0;
        int maxAttempts = 5;

        while ( numAttempts < maxAttempts )
        {
            try
            {
                pb.redirectErrorStream( true );
                final Process process = pb.start();
                InputStream myIS = process.getInputStream();
                String tempOut = convertStreamToStr( myIS );
                logService.info( tempOut );
                break;
            }
            catch ( Exception e )
            {
                logService.error( "Error occured during system call" );
                logService.error( "" + e );
                logService.error( "/nTrying again.../n" );
                waitOneSecond();
                numAttempts++;
            }
        }

    }

    public static void waitOneSecond()
    {
        try
        {
            Thread.sleep( 1000 );
        } catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }

	public static void convertToMask( ImagePlus imp, float threshold )
	{
		int width = imp.getWidth();
		int height = imp.getHeight();
		int size = width*height;
		int nSlices = imp.getStackSize();
		ImageStack stack1 = imp.getStack();
		ImageStack stack2 = new ImageStack(width, height);
		float value;
		ImageProcessor ip1, ip2;
		IJ.showStatus("Converting to mask");
		for (int i=1; i<=nSlices; i++) {
			IJ.showProgress(i, nSlices);
			String label = stack1.getSliceLabel(i);
			ip1 = stack1.getProcessor(i);
			ip2 = new ByteProcessor(width, height);
			for (int j=0; j<size; j++) {
				value = ip1.getf(j);
				if ( value>=threshold )
					ip2.set(j, 1);
				else
					ip2.set(j, 0);
			}
			stack2.addSlice(label, ip2);
		}
		imp.setStack(null, stack2);
		ImageStack stack = imp.getStack();
		stack.setColorModel( LookUpTable.createGrayscaleColorModel(!Prefs.blackBackground));
		imp.setStack(null, stack);
		if (imp.isComposite()) {
			CompositeImage ci = (CompositeImage)imp;
			ci.setMode(IJ.GRAYSCALE);
			ci.resetDisplayRanges();
			ci.updateAndDraw();
		}
		IJ.showStatus("");
	}
}
