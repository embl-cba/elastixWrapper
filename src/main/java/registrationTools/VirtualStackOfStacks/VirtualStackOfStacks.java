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

package registrationTools.VirtualStackOfStacks;

/**
 * Created by tischi on 27/10/16.
 */

import registrationTools.logging.IJLazySwingLogger;
import registrationTools.logging.Logger;
import registrationTools.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.process.ImageProcessor;
import javafx.geometry.Point3D;

// todo: replace all == with "equals"
// TODO: extend VirtualStack rather than ImageStack ?

/**
 This class represents an array of disk-resident image stacks.
 */
public class VirtualStackOfStacks extends ImageStack {
    int nSlices;
    private int nX, nY, nZ, nC, nT;
    int bitDepth = 0;
    FileInfoSer[][][] infos;  // channel, t, z
    String fileType = "tiff"; // h5
    String directory = "";
    String[] channelFolders;
    String[][][] fileList;
    String h5DataSet;

    Logger logger = new IJLazySwingLogger();

    /** Creates a new, empty virtual stack of required size */
    public VirtualStackOfStacks(String directory, String[] channelFolders, String[][][] fileList, int nC, int nT, int nX, int nY, int nZ, int bitDepth, String fileType, String h5DataSet) {
        super();

        this.directory = directory;
        this.nC = nC;
        this.nT = nT;
        this.nZ = nZ;
        this.nX = nX;
        this.nY = nY;
        this.bitDepth = bitDepth;
        this.nSlices = nC*nT*nZ;
        this.fileType = fileType;
        this.channelFolders = channelFolders;
        this.fileList = fileList;
        this.infos = new FileInfoSer[nC][nT][];
        this.h5DataSet = h5DataSet;

        if( logger.isShowDebug() ) {
            logStatus();
        }

    }

    public VirtualStackOfStacks(String directory, FileInfoSer[][][] infos) {
        super();

        this.infos = infos;
        this.directory = directory;
        nC = infos.length;
        nT = infos[0].length;
        bitDepth = infos[0][0][0].bytesPerPixel*8;

        if(infos[0][0][0].isCropped) {
            nX = (int) infos[0][0][0].pCropSize[0];
            nY = (int) infos[0][0][0].pCropSize[1];
            nZ = (int) infos[0][0][0].pCropSize[2];
        } else {
            nX = (int) infos[0][0][0].width;
            nY = (int) infos[0][0][0].height;
            nZ = (int) infos[0][0].length;
        }

        nSlices = nC*nT*nZ;

        if(infos[0][0][0].fileName.endsWith(".h5"))
            this.fileType = "h5";
        if(infos[0][0][0].fileName.endsWith(".tif"))
            this.fileType = "tif";

        if( logger.isShowDebug() ) {
            logStatus();
        }

    }

    public void logStatus() {
              logger.info("# VirtualStackOfStacks");
              logger.info("fileType: " + fileType);
              logger.info("x: " + nX);
              logger.info("y: " + nY);
              logger.info("z: " + nZ);
              logger.info("channel: " + nC);
              logger.info("t: " + nT);
    }

    public FileInfoSer[][][] getFileInfosSer() {
        return(infos);
    }

    public String getDirectory() {
        return directory;
    }

    public int numberOfUnparsedFiles() {
        int numberOfUnparsedFiles = 0;
        for(int c = 0; c < nC; c++ )
            for(int t = 0; t < nT; t++)
                if (infos[c][t] == null)
                    numberOfUnparsedFiles++;

        return numberOfUnparsedFiles;
    }

    /** Adds an image stack from file infos */
    public void setStackFromFile(int t, int c) {
        FileInfo[] info = null;
        FileInfoSer[] infoSer = null;
        FastTiffDecoder ftd;

        long startTime = System.currentTimeMillis();

        try {


            if ( fileType.equals("tif stacks") ) {

                ftd = new FastTiffDecoder(directory + channelFolders[c], fileList[c][t][0]);
                info = ftd.getTiffInfo();

                // convert FileInfo[] to FileInfoSer[]
                infoSer = new FileInfoSer[nZ];
                for (int z = 0; z < nZ; z++) {
                    infoSer[z] = new FileInfoSer(info[z]);
                    infoSer[z].fileName = fileList[c][t][z]; // relative path to main directory
                    infoSer[z].directory = channelFolders[c] + "/"; // relative path to main directory
                    infoSer[z].fileTypeString = fileType;
                }

            } else if (fileType.equals("leica single tif")) {

                infoSer = new FileInfoSer[nZ];

                //
                // open all IFDs from all files and convert to FileInfoSer
                // (this is necessary if they are compressed in any way)
                //
                for (int z = 0; z < nZ; z++) {

                    ftd = new FastTiffDecoder(directory + channelFolders[c], fileList[c][t][z]);
                    info = ftd.getTiffInfo();
                    infoSer[z] = new FileInfoSer(info[0]); // just duplicate from first file
                    infoSer[z].directory = channelFolders[c] + "/"; // relative path to main directory
                    infoSer[z].fileName = fileList[c][t][z];
                    infoSer[z].fileTypeString = fileType;

                }


            } else if (fileType.equals("h5")) {

                //
                // construct a FileInfoSer
                // todo: this could be much leaner
                // e.g. the nX, nY and bit depth
                //
                infoSer = new FileInfoSer[nZ];
                for (int z = 0; z < nZ; z++) {
                    infoSer[z] = new FileInfoSer();
                    infoSer[z].fileName = fileList[c][t][z];
                    infoSer[z].directory = channelFolders[c] + "/";
                    infoSer[z].width = nX;
                    infoSer[z].height = nY;
                    infoSer[z].bytesPerPixel = 2; // todo: how to get the bit-depth from the info?
                    infoSer[z].h5DataSet = h5DataSet;
                    infoSer[z].fileTypeString = fileType;
                }

            } // h5

        } catch(Exception e) {

             logger.error("Error: " + e.toString());

        }

        this.infos[c][t] = infoSer;

    }

    /** Does nothing. */
    public void addSlice(String sliceLabel, Object pixels) {
    }

    /** Does nothing.. */
    public void addSlice(String sliceLabel, ImageProcessor ip) {
    }

    /** Does noting. */
    public void addSlice(String sliceLabel, ImageProcessor ip, int n) {
    }

    /** Overrides the super method */
    public int getBitDepth()
    {
        return( bitDepth );
    }

    /** Does noting. */
    public void deleteSlice(int n) {
        /*
        if (n<1 || n>nSlices)
            throw new IllegalArgumentException("Argument out of range: "+n);
        if (nSlices<1)
            return;
        for (int i=n; i<nSlices; i++)
            infos[i-1] = infos[i];
        infos[nSlices-1] = null;
        nSlices--;
        */
    }

    /** Deletes the last slice in the stack. */
    public void deleteLastSlice() {
        /*if (nSlices>0)
            deleteSlice(nSlices);
            */
    }

    /** Returns the pixel array for the specified slice, were 1<=n<=nslices. */
    public Object getPixels(int n) {
        ImageProcessor ip = getProcessor(n);
        if (ip!=null)
            return ip.getPixels();
        else
            return null;
    }

    /** Assigns a pixel array to the specified slice,
     were 1<=n<=nslices. */
    public void setPixels(Object pixels, int n) {
    }

    /** Returns an ImageProcessor for the specified slice,
     were 1<=n<=nslices. Returns null if the stack is empty.
    N is computed by IJ assuming the czt ordering, with
    n = ( channel + z*nC + t*nZ*nC ) + 1
    */
    public ImageProcessor getProcessor(int n) {
        // recompute channel,z,t
        n -= 1;
        int c = (n % nC);
        int z = ((n-c)%(nZ*nC))/nC;
        int t = (n-c-z*nC)/(nZ*nC);

        ImagePlus imp;

        if( logger.isShowDebug() ) {
              logger.info("# VirtualStackOfStacks.getProcessor");
              logger.info("requested slice [one-based]: " + (n + 1));
              logger.info("channel [one-based]: " + (c + 1));
              logger.info("z [one-based]: " + (z + 1));
              logger.info("t [one-based]: " + (t + 1));
              logger.info("opening file: " + directory + infos[c][t][z].directory + infos[c][t][z].fileName);
        }

        Point3D po, ps;
        if(infos[c][t] == null) {
            setStackFromFile(t, c);
        }

        FileInfoSer fi = infos[c][t][0];

        po = new Point3D(0,0,z);

        if(fi.isCropped) {
            // offset for cropping is added in  getDataCube
            ps = new Point3D(fi.pCropSize[0],fi.pCropSize[1],1);
        } else {
            ps = new Point3D(fi.width,fi.height,1);
        }

        // imp = new OpenerExtension().readDataCube(directory, infos[channel][t], dz, po, ps);
        imp = getDataCube(t, c, po, ps, new Point3D(1, 1, 1), 0);

        return imp.getProcessor();

    }

    public boolean isCropped() {
        return(infos[0][0][0].isCropped);
    }

    public Point3D getCropOffset() {
        return(new Point3D(infos[0][0][0].pCropOffset[0], infos[0][0][0].pCropOffset[1], infos[0][0][0].pCropOffset[2]));
    }

    public Point3D getCropSize() {
        return(new Point3D(infos[0][0][0].pCropSize[0], infos[0][0][0].pCropSize[1], infos[0][0][0].pCropSize[2]));
    }

    public ImagePlus getFullFrame(int t, int c) {
        return(getFullFrame(t, c, new Point3D(1,1,1)));
    }

    public ImagePlus getFullFrame(int t, int c, Point3D pSubSample) {

        Point3D po, ps;
        po = new Point3D(0, 0, 0);
        if(infos[0][0][0].isCropped) {
            // offset is added by getDataCube
            ps = infos[0][0][0].getCropSize();
        } else {
            ps = new Point3D(nX, nY, nZ);
        }

        ImagePlus imp = getDataCube(t, c, po, ps, pSubSample, 0);
        if( (int)pSubSample.getX()>1 || (int)pSubSample.getY()>1) {
            return(resizeWidthAndHeight(imp,(int)pSubSample.getX(),(int)pSubSample.getY()));
        } else {
            return(imp);
        }

    }

    public ImagePlus getDataCube(int t, int c, Point3D po, Point3D ps, Point3D pSubSample, int background) {

        ImagePlus impLoaded = null;

        if ( logger.isShowDebug() ) {
              logger.info("# VirtualStackOfStacks.getDataCube");
              logger.info("t: " + t);
              logger.info("channel: " + c);
        }

        FileInfoSer fi = infos[c][t][0];

        if (fi.isCropped) {
            po = po.add(fi.getCropOffset());
        }

        if (infos[c][t] == null) {
            // file info not yet loaded => get it!
            setStackFromFile(t, c);
        }

        int dz = (int) pSubSample.getZ();

        // compute ranges to be loaded
        int ox = (int) (po.getX() + 0.5);
        int oy = (int) (po.getY() + 0.5);
        int oz = (int) (po.getZ() + 0.5);
        int sx = (int) (ps.getX() + 0.5);
        int sy = (int) (ps.getY() + 0.5);
        int sz = (int) (ps.getZ() + 0.5);

        // adjust ranges for loading to stay within the image bounds

        //
        // adjust for negative offsets
        //

        // set negative offsets to zero
        int ox2 = (ox < 0) ? 0 : ox;
        int oy2 = (oy < 0) ? 0 : oy;
        int oz2 = (oz < 0) ? 0 : oz;

        // adjust the loaded sizes accordingly
        int sx2 = sx - (ox2-ox);
        int sy2 = sy - (oy2-oy);
        int sz2 = sz - (oz2-oz);

        //
        // adjust for too large loading ranges due to high offsets
        // - note: ox2=ox and sx2=sx if ox was positive
        int nX = fi.width;
        int nY = fi.height;
        int nZ = infos[c][t].length;

        sx2 = (ox2+sx2 > nX) ? nX-ox2 : sx2;
        sy2 = (oy2+sy2 > nY) ? nY-oy2 : sy2;
        sz2 = (oz2+sz2 > nZ) ? nZ-oz2 : sz2;

        //
        // check memory requirements
        //

        long numPixels = (long) sx2 * sy2 * sz2;
        int numStacks = 1;
        int bitDepth = this.getBitDepth();

        if( ! Utils.checkMemoryRequirements(numPixels, bitDepth, numStacks) ) return(null);

        //
        // load the requested data into RAM
        //

        if( sx2>0 && sy2>0 && sz2>0 )
        {
            Point3D po2 = new Point3D(ox2, oy2, oz2);
            Point3D ps2 = new Point3D(sx2, sy2, sz2);
            impLoaded = new OpenerExtension().readDataCube(directory, infos[c][t], dz, po2, ps2);

            if (impLoaded == null)
            {
                logger.info("Error: loading failed!");
                return null;
            }

        }

        // put the potentially smaller loaded stack into the full stack

        int finalStackOffsetX = (ox2 - ox);
        int finalStackOffsetY = (oy2 - oy);
        int finalStackOffsetZ = (oz2 - oz);

        if (dz > 1) { // adapt for sub-sampling in z
            sz = (int) (1.0 * sz / dz + 0.5); // final stack size
            finalStackOffsetZ = (int) (1.0 * finalStackOffsetZ / dz); // final stack offset
        }

        ImageStack finalStack = ImageStack.create(sx, sy, sz, fi.bytesPerPixel*8);

        if( sx2>0 && sy2>0 && sz2>0 )
        { // something was actually loaded

            // subtract an image background
            // this helps both the center of mass and the correlation
            if( background > 0 )
            {
                // an alternative here is to automatically determine the background, options are
                // - subtract mean intensity (if the cropping region around the object is not much larger
                // than the object itself this will highlight the bright regions in the object)
                // int mean = computeMean16bit(impLoaded.getStack());
                // logger.info("subtracting mean: " + mean);
                // - compute some lower percentile of the region
                IJ.run(impLoaded, "Subtract...", "value=" + background + " stack");
            }

            // put the loaded stack into a larger stack
            // this deals with out-of-bounds issues
            ImageStack loadedStack = impLoaded.getStack();
            for ( int z = 1; z <= loadedStack.size(); z++ ) // getProcessor is one-based
            {
                ImageProcessor ip = loadedStack.getProcessor(z);
                ImageProcessor ip2 = ip.createProcessor(sx, sy);
                ip2.insert(ip, finalStackOffsetX, finalStackOffsetY);
                if( z + finalStackOffsetZ > finalStack.size() )
                {
                     logger.error("Error due to z-subsampling");
                }
                finalStack.setProcessor(ip2, z + finalStackOffsetZ);
            }
        }

        // todo: if this is called from "getProcessor" I need different logic because
        // it should only return one plane

        ImagePlus finalImp = new ImagePlus("", finalStack);

        // subsample in x and y
        if ((int) pSubSample.getX() > 1 || (int) pSubSample.getY() > 1) {
            return (resizeWidthAndHeight(finalImp, (int) pSubSample.getX(), (int) pSubSample.getY()));
        } else {
            return (finalImp);
        }
    }

    public int computeMean16bit(ImageStack stack) {

        //long startTime = System.currentTimeMillis();
        double sum = 0.0;
        int i;
        int width = stack.getWidth();
        int height = stack.getHeight();
        int depth = stack.getSize();
        int xMin = 0;
        int xMax = (width-1);
        int yMin = 0;
        int yMax = (height-1);
        int zMin = 0;
        int zMax = (depth-1);

        for( int z=zMin; z<=zMax; z++ )
        {
            short[] pixels = (short[]) stack.getProcessor(z+1).getPixels();
            for ( int y = yMin; y<=yMax; y++ )
            {
                i = y * width + xMin;
                for ( int x = xMin; x <= xMax; x++ )
                {
                    sum += (pixels[i] & 0xffff);
                    i++;
                }
            }
        }

        return((int) sum/(width*height*depth));

    }

    public ImagePlus resizeWidthAndHeight(ImagePlus imp, int dx, int dy) {
        int nSlices = imp.getStackSize();
        int nx = imp.getWidth(), ny = imp.getHeight();
        ImagePlus imp2 = imp.createImagePlus();
        ImageStack stack1 = imp.getStack();
        ImageStack stack2 = new ImageStack(nx/dx, ny/dy);
        ImageProcessor ip1, ip2;
        int method = ImageProcessor.BILINEAR; // ImageProcessor.NEAREST_NEIGHBOR;
        if (nx == 1 || ny == 1)
            method = ImageProcessor.NONE;
        for (int i = 1; i <= nSlices; i++) {
            ip1 = stack1.getProcessor(i);
            ip1.setInterpolationMethod(method);
            ip2 = ip1.resize(nx/dx, ny/dy, false);
            if (ip2 != null)
                stack2.addSlice("", ip2);
        }
        imp2.setStack("", stack2);
        return(imp2);
    }

    public int getSize() {
        return nSlices;
    }

    public int getWidth() {
        return nX;
    }

    public int getHeight() {
        return nY;
    }

    public int getDepth() {
        return nZ;
    }

    public int getChannels() {
        return nC;
    }

    public int getFrames() {
        return nT;
    }


    /** Returns the file name of the Nth image. */
    public String getSliceLabel(int n) {
        //int nFile;
        //nFile = (n-1) / nZ;
        //return infos[nFile][0].fileName;
        return "slice label";
    }

    /** Returns null. */
    public Object[] getImageArray() {
        return null;
    }

    /** Does nothing. */
    public void setSliceLabel(String label, int n) {
    }

    /** Always return true. */
    public boolean isVirtual() {
        return true; // do we need this?
    }

    /** Does nothing. */
    public void trim() {
    }

}


/*
    public void deleteSlice(int n) {
        if (n<1 || n>nSlices)
            throw new IllegalArgumentException("Argument out of range: "+n);
        if (nSlices<1)
            return;
        for (int i=n; i<nSlices; i++)
            infos[i-1] = infos[i];
        infos[nSlices-1] = null;
        nSlices--;
    }

    /** Deletes the last slice in the stack.
    public void deleteLastSlice() {
        if (nSlices>0)
            deleteSlice(nSlices);
    }*/

/*
// todo: put the conversion from centerRadii to offsetSize into this function
    public ImagePlus getCubeByTimeCenterAndRadii(int t, int channel, Point3D psub, Point3D pc, Point3D pr) {

        if( logger.isShowDebug() ) {
            logger.info("# VirtualStackOfStacks.getCroppedFrameCenterRadii");
            logger.info("t: "+t);
            logger.info("channel: "+channel);
            }

        FileInfoSer fi = infos[0][0][0];

        if(fi.isCropped) {
            // load cropped slice
            pc = pc.add(fi.getCropOffset());
        }

        if(infos[channel][t] == null) {
            // file info not yet loaded => get it!
            setStackFromFile(t, channel);
        }

        ImagePlus imp = new OpenerExtension().openCroppedStackCenterRadii(directory, infos[channel][t], (int) psub.getZ(), pc, pr);

        if (imp==null) {
            logger.info("Error: loading failed!");
            return null;
        } else {
            return imp;
        }
    }
    */