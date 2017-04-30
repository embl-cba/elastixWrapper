package registrationTools.bigDataTracker;

import registrationTools.VirtualStackOfStacks.VirtualStackOfStacks;
import registrationTools.imageFilter.DoNotFilter;
import registrationTools.imageFilter.ImageFilter;
import registrationTools.logging.Logger;
import registrationTools.utils.Utils;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import javafx.geometry.Point3D;
import mpicbg.imglib.algorithm.fft.PhaseCorrelation;
import mpicbg.imglib.algorithm.fft.PhaseCorrelationPeak;
import mpicbg.imglib.image.ImagePlusAdapter;

import java.util.ArrayList;


class ObjectTracker implements Runnable
{
    BigDataTracker bigDataTracker;
    Logger logger;
    TrackingSettings trackingSettings;
    ImageFilter imageFilter;

    public ObjectTracker(BigDataTracker bigDataTracker,
                         TrackingSettings trackingSettings,
                         Logger logger)
    {
        this.bigDataTracker = bigDataTracker;
        this.trackingSettings = trackingSettings;
        this.logger = logger;

        // filter image (mainly good for improving the correlation)
        //

        // don't filter
        imageFilter = new DoNotFilter();

        // FFT BandPass
        //imageFilter = new FFTBandPass();
        //((FFTBandPass)imageFilter).sizeMax = 10;
        //((FFTBandPass)imageFilter).sizeMin = 5;

    }

    public void run() {

        long startTime, stopTime, elapsedReadingTime, elapsedProcessingTime;
        ImagePlus imp0, imp1;
        Point3D p0offset;
        Point3D p1offset;
        Point3D pShift;
        Point3D pLocalShift;
        Point3D pSize;
        //boolean subtractMean = true;

        // obtain all the info about the track
        Track track = bigDataTracker.addNewTrack(trackingSettings);
        int tStart = trackingSettings.trackStartROI.getTPosition();
        int channel = trackingSettings.channel;
        int nt = trackingSettings.nt;
        int dt = trackingSettings.subSamplingT;
        Point3D pStart = new Point3D(
                trackingSettings.trackStartROI.getXBase(),
                trackingSettings.trackStartROI.getYBase(),
                trackingSettings.trackStartROI.getImage().getZ() - 1);

        ImagePlus imp = bigDataTracker.getImp();
        VirtualStackOfStacks vss = (VirtualStackOfStacks) imp.getStack();
        TrackTable trackTable = bigDataTracker.getTrackTable();

        //
        // track first time-point by center of mass
        //

        // get selected track coordinates
        // load more data than the user selected
        pSize = track.getObjectSize().multiply(trackingSettings.trackingFactor);
        p0offset = Utils.computeOffsetFromCenterSize(pStart, pSize);

        // read data
        //
        startTime = System.currentTimeMillis();
        imp0 = vss.getDataCube(tStart, channel, p0offset, pSize,
                trackingSettings.subSamplingXYZ, trackingSettings.background);
        elapsedReadingTime = System.currentTimeMillis() - startTime;

        // filter the image to ease the tracking
        //
        imp0 = imageFilter.filter(imp0);

        // iteratively compute the shift of the center of mass relative to the center of the image stack
        // using only half the image size for iteration
        startTime = System.currentTimeMillis();
        pShift = compute16bitShiftUsingIterativeCenterOfMass(imp0.getStack(),
                trackingSettings.trackingFactor,
                trackingSettings.iterationsCenterOfMass);
        elapsedProcessingTime = System.currentTimeMillis() - startTime;

        // correct for sub-sampling
        //
        pShift = Utils.multiplyPoint3dComponents(pShift,
                trackingSettings.subSamplingXYZ);

        // add track location for first image
        //
        Point3D pUpdate = Utils.computeCenterFromOffsetSize(p0offset.add(pShift), pSize);


        // store results
        //
        publishResult(track, trackTable, logger, pUpdate, tStart, nt,
                elapsedReadingTime, elapsedProcessingTime);

        //
        // compute shifts for following time-points
        //

        boolean finish = false;
        int tMax = tStart + nt - 1;
        int tPrevious = tStart;
        int tNow;
        int tMaxUpdate;

        //  Important notes for the logic:
        //  - p0offset has to be the position where the previous images was loaded
        //  - p1offset has to be the position where the current image was loaded

        for (int t = tStart + dt; t < tStart + nt + dt; t = t + dt) {

            tNow = t;

            if(tNow >= tMax) {
                // due to the sub-sampling in t the addition of dt
                // can cause the frame to be outside of
                // the tracking range => load the last frame
                tNow = tMax;
                finish = true;
            }

            // load next image at the same position where the previous image has been loaded (p0offset)
            // plus the computed shift (pShift), basically a linear motion model
            // but curate this position according to the image bounds
            //info("Position where previous image was loaded: " + p0offset);
            //info("Position where previous image was loaded plus shift: " + p0offset.add(pShift));
            //p1offset = DataStreamingTools.curatePositionOffsetSize(imp, p0offset.add(pShift), pSize);
            p1offset = p0offset.add(pShift);
            //info("Curatep0offset.add(pShift)d position where this image is loaded: " + p1offset);

            // load image
            startTime = System.currentTimeMillis();
            imp1 = vss.getDataCube(tNow, channel, p1offset, pSize,
                    trackingSettings.subSamplingXYZ, trackingSettings.background);
            elapsedReadingTime = System.currentTimeMillis() - startTime;

            // filter image
            //
            imp1 = imageFilter.filter(imp1);


            if (trackingSettings.trackingMethod.equals("correlation") ) {

                logger.debug("measuring drift using correlation...");

                // compute shift relative to previous time-point
                startTime = System.currentTimeMillis();
                pShift = computeShiftUsingPhaseCorrelation(imp1, imp0);
                stopTime = System.currentTimeMillis();
                elapsedProcessingTime = stopTime - startTime;

                // correct for sub-sampling
                pShift = Utils.multiplyPoint3dComponents(pShift, trackingSettings.subSamplingXYZ);
                //info("Correlation ObjectTracker Shift: "+pShift);


                // take into account the different loading positions of this and the previous image
                pShift = pShift.add(p1offset.subtract(p0offset));
                //info("Correlation ObjectTracker Shift including image shift: "+pShift);

                if( logger.isShowDebug() )  logger.info("actual final shift is " + pShift.toString());

            }
            else if ( trackingSettings.trackingMethod.equals("center of mass") )
            {

                if( logger.isShowDebug() )  logger.info("measuring drift using center of mass...");

                // compute the different of the center of mass
                // to the geometric center of imp1
                startTime = System.currentTimeMillis();
                //info("timepoint: "+t);
                pLocalShift = compute16bitShiftUsingIterativeCenterOfMass(imp1.getStack(),
                        trackingSettings.trackingFactor,
                        trackingSettings.iterationsCenterOfMass);
                stopTime = System.currentTimeMillis();
                elapsedProcessingTime = stopTime - startTime;

                // correct for sub-sampling
                pLocalShift = Utils.multiplyPoint3dComponents(pLocalShift, trackingSettings.subSamplingXYZ);
                //info("Center of Mass Local Shift: "+pLocalShift);

                if( logger.isShowDebug() )
                {
                    logger.info("local shift after correction for sub-sampling is " + pLocalShift.toString());
                }

                // the drift corrected position in the global coordinate system is: p1offset.add(pLocalShift)
                // in center coordinates this is: computeCenterFromOffsetSize(p1offset.add(pShift),pSize)
                // relative to previous tracking position:
                //info(""+track.getXYZ(tPrevious).toString());
                //info(""+computeCenterFromOffsetSize(p1offset.add(pLocalShift),pSize).toString());
                //info(""+p1offset.add(pLocalShift).toString());
                pShift = Utils.computeCenterFromOffsetSize(
                        p1offset.add(pLocalShift), pSize).subtract(track.getPosition(tPrevious - tStart));

                if( logger.isShowDebug() )  logger.info("actual shift is "+pShift.toString());

            }


            // compute time-points between this and the previous one (inclusive)
            // using linear interpolation

            tMaxUpdate = tNow;
            if(finish) tMaxUpdate = tNow; // include last data point

            for (int tUpdate = tPrevious + 1; tUpdate <= tMaxUpdate; tUpdate++) {

                Point3D pPrevious = track.getPosition(tPrevious - tStart);
                double interpolation = (double) (tUpdate - tPrevious) / (double) (tNow - tPrevious);
                pUpdate = pPrevious.add(pShift.multiply(interpolation));

                publishResult(track, trackTable, logger, pUpdate, tUpdate, nt, elapsedReadingTime, elapsedProcessingTime);

            }

            tPrevious = tNow;
            imp0 = imp1;
            p0offset = p1offset; // store the position where this image was loaded

            if(finish) return;
            if(bigDataTracker.interruptTrackingThreads)
            {
                logger.info("Tracking of track " + track.getID() + " interrupted.");
                return;
            }

        }


    }

    private void publishResult(Track track, TrackTable trackTable, Logger logger, Point3D location,
                               int t, int nt,
                               long elapsedReadingTime, long elapsedProcessingTime)
    {

        track.addLocation(t, location);

        trackTable.addRow(new Object[]{
                String.format("%1$04d", track.getID()) + "_" + String.format("%1$05d", t),
                (float) location.getX(), (float) location.getY(), (float) location.getZ(), t, track.getID()
        });

        bigDataTracker.addLocationToOverlay(track, t);

        // TODO: make this somehow a logger.progress
        logger.info("Track ID: " + track.getID() +
                "; Time points tracked: " + (t - track.getTmin() + 1) + "/" + nt +
                "; reading [ms] = " + elapsedReadingTime +
                "; processing [ms] = " + elapsedProcessingTime);


    }


    private Point3D compute16bitShiftUsingIterativeCenterOfMass(ImageStack stack,
                                                                double trackingFactor,
                                                                int iterations) {
        Point3D pMin, pMax;

        // compute stack center and tracking radii
        // at each iteration, the center of mass is only computed for a subset of the data cube
        // this subset iteratively shifts every iteration according to the results of the center of mass computation
        Point3D pStackSize = new Point3D(stack.getWidth(), stack.getHeight(), stack.getSize() );
        Point3D pStackCenter = Utils.computeCenterFromOffsetSize(new Point3D(0,0,0), pStackSize);
        Point3D pCenter = pStackCenter;
        double trackingFraction;
        for(int i=0; i<iterations; i++) {
            // trackingFraction = 1/trackingFactor is the user selected object size, because we are loading
            // a portion of the data, which is trackingFactor times larger than the object size
            // below formula makes the region in which the center of mass is compute go from 1 to 1/trackingfactor
            trackingFraction = 1.0 - Math.pow(1.0*(i+1)/iterations,1.0/4.0)*(1.0-1.0/trackingFactor);
            pMin = pCenter.subtract(pStackSize.multiply(trackingFraction / 2)); // div 2 because it is radius
            pMax = pCenter.add(pStackSize.multiply(trackingFraction / 2));
            pCenter = compute16bitCenterOfMass(stack, pMin, pMax);
            //info("i "+i+" trackingFraction "+trackingFraction+" pCenter "+pCenter.toString());
        }
        return(pCenter.subtract(pStackCenter));
    }

    private Point3D computeShiftUsingPhaseCorrelation(ImagePlus imp1, ImagePlus imp0) {
        if( logger.isShowDebug() )   logger.info("PhaseCorrelation phc = new PhaseCorrelation(...)");
        PhaseCorrelation phc = new PhaseCorrelation(ImagePlusAdapter.wrap(imp1), ImagePlusAdapter.wrap(imp0), 5, true);
        if( logger.isShowDebug() )   logger.info("phc.process()... ");
        phc.process();
        // get the first peak that is not a clean 1.0,
        // because 1.0 cross-correlation typically is an artifact of too much shift into black areas of both images
        ArrayList<PhaseCorrelationPeak> pcp = phc.getAllShifts();
        float ccPeak = 0;
        int iPeak = 0;
        for(iPeak = pcp.size()-1; iPeak>=0; iPeak--) {
            ccPeak = pcp.get(iPeak).getCrossCorrelationPeak();
            if (ccPeak < 0.999) break;
        }
        //info(""+ccPeak);
        int[] shift = pcp.get(iPeak).getPosition();
        return(new Point3D(shift[0],shift[1],shift[2]));
    }

    private Point3D compute16bitCenterOfMass(ImageStack stack, Point3D pMin, Point3D pMax) {


        final String centeringMethod = "center of mass";

        //long startTime = System.currentTimeMillis();
        double sum = 0.0, xsum = 0.0, ysum = 0.0, zsum = 0.0;
        int i, v;
        int width = stack.getWidth();
        int height = stack.getHeight();
        int depth = stack.getSize();
        int xmin = 0 > (int) pMin.getX() ? 0 : (int) pMin.getX();
        int xmax = (width-1) < (int) pMax.getX() ? (width-1) : (int) pMax.getX();
        int ymin = 0 > (int) pMin.getY() ? 0 : (int) pMin.getY();
        int ymax = (height-1) < (int) pMax.getY() ? (height-1) : (int) pMax.getY();
        int zmin = 0 > (int) pMin.getZ() ? 0 : (int) pMin.getZ();
        int zmax = (depth-1) < (int) pMax.getZ() ? (depth-1) : (int) pMax.getZ();

        // compute one-based, otherwise the numbers at x=0,y=0,z=0 are lost for the center of mass

        if (centeringMethod.equals("center of mass")) {
            for (int z = zmin + 1; z <= zmax + 1; z++) {
                ImageProcessor ip = stack.getProcessor(z);
                short[] pixels = (short[]) ip.getPixels();
                for (int y = ymin + 1; y <= ymax + 1; y++) {
                    i = (y - 1) * width + xmin; // zero-based location in pixel array
                    for (int x = xmin + 1; x <= xmax + 1; x++) {
                        v = pixels[i] & 0xffff;
                        // v=0 is ignored automatically in below formulas
                        sum += v;
                        xsum += x * v;
                        ysum += y * v;
                        zsum += z * v;
                        i++;
                    }
                }
            }
        }

        if (centeringMethod.equals("centroid")) {
            for (int z = zmin + 1; z <= zmax + 1; z++) {
                ImageProcessor ip = stack.getProcessor(z);
                short[] pixels = (short[]) ip.getPixels();
                for (int y = ymin + 1; y <= ymax + 1; y++) {
                    i = (y - 1) * width + xmin; // zero-based location in pixel array
                    for (int x = xmin + 1; x <= xmax + 1; x++) {
                        v = pixels[i] & 0xffff;
                        if (v > 0) {
                            sum += 1;
                            xsum += x;
                            ysum += y;
                            zsum += z;
                        }
                        i++;
                    }
                }
            }
        }

        // computation is one-based; result should be zero-based
        double xCenter = (xsum / sum) - 1;
        double yCenter = (ysum / sum) - 1;
        double zCenter = (zsum / sum) - 1;

        //long stopTime = System.currentTimeMillis(); long elapsedTime = stopTime - startTime;  logger.info("center of mass in [ms]: " + elapsedTime);

        return(new Point3D(xCenter,yCenter,zCenter));
    }

    private int compute16bitMean(ImageStack stack) {

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

        for(int z=zMin; z<=zMax; z++) {
            short[] pixels = (short[]) stack.getProcessor(z+1).getPixels();
            for (int y = yMin; y<=yMax; y++) {
                i = y * width + xMin;
                for (int x = xMin; x <= xMax; x++) {
                    sum += (pixels[i] & 0xffff);
                    i++;
                }
            }
        }

        return((int) sum/(width*height*depth));

    }

    private Point3D compute16bitMaximumLocation(ImageStack stack) {
        long startTime = System.currentTimeMillis();
        int vmax = 0, xmax = 0, ymax = 0, zmax = 0;
        int i, v;
        int width = stack.getWidth();
        int height = stack.getHeight();
        int depth = stack.getSize();

        for(int z=1; z <= depth; z++) {
            ImageProcessor ip = stack.getProcessor(z);
            short[] pixels = (short[]) ip.getPixels();
            i = 0;
            for (int y = 1; y <= height; y++) {
                i = (y-1) * width;
                for (int x = 1; x <= width; x++) {
                    v = pixels[i] & 0xffff;
                    if (v > vmax) {
                        xmax = x;
                        ymax = y;
                        zmax = z;
                        vmax = v;
                    }
                    i++;
                }
            }
        }

        long stopTime = System.currentTimeMillis(); long elapsedTime = stopTime - startTime;
        logger.info("center of mass in [ms]: " + elapsedTime);

        return(new Point3D(xmax,ymax,zmax));
    }

}

