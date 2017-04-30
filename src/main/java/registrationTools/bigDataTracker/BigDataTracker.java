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


package registrationTools.bigDataTracker;

import registrationTools.dataStreamingTools.DataStreamingTools;
import registrationTools.logging.IJLazySwingLogger;
import registrationTools.logging.Logger;
import registrationTools.utils.Utils;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import javafx.geometry.Point3D;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



// todo: expose the number of iterations for the center of mass to the gui
// todo: correlation tracking: it did not show an error when no object was selected

// todo: add unique track trackID to tracktable, track, and imageName of cropped track

// todo: multi-point selection tool
// todo: track-jTableSpots: load button

    // todo: make frames around buttons that belong together


public class BigDataTracker {

    Logger logger = new IJLazySwingLogger();

    private ImagePlus imp;
    private ArrayList<Track> tracks = new ArrayList<Track>();
    private TrackTable trackTable;
    private ExecutorService es = Executors.newCachedThreadPool();

    public boolean interruptTrackingThreads = false;

    public BigDataTracker(ImagePlus imp) {
        this.imp = imp;
        this.trackTable = new TrackTable();
    }

    public TrackTable getTrackTable() {
        return trackTable;
    }

    public ArrayList<Track> getTracks() {
        return tracks;
    }

    public ImagePlus getImp() {
        return imp;
    }

    public void clearAllTracks()
    {
        this.getTrackTable().clear();
        this.getTracks().clear();
        this.getImp().setOverlay(new Overlay());
    }

    public ArrayList<ImagePlus> getViewsOnTrackedObjects(String croppingFactor) {

        ArrayList<ImagePlus> imps = new ArrayList<>();

        for( Track track : tracks) {

            Point3D pCropSize;

            //
            // convert track center coordinates to bounding box offsets
            //
            ArrayList<Point3D> trackOffsets = new ArrayList<>();
            Map<Integer, Point3D> locations = track.getLocations();

            if( croppingFactor.equals("all") ) {

                // the object was used to "drift correct" the whole image
                // thus, show the whole image
                Point3D pImageSize = new Point3D(imp.getWidth(), imp.getHeight(), imp.getNSlices());
                Point3D pImageCenter = pImageSize.multiply(0.5);
                Point3D offsetToImageCenter = locations.get(track.getTmin()).subtract(pImageCenter);
                for( Point3D position : locations.values() )
                {
                    Point3D correctedImageCenter = position.subtract(offsetToImageCenter);
                    trackOffsets.add(Utils.computeOffsetFromCenterSize(correctedImageCenter,
                            pImageSize));
                }

                pCropSize = pImageSize;

            }
            else
            {
                //  crop around the object

                double croppingFactorValue;

                try
                {
                    croppingFactorValue = Double.parseDouble(croppingFactor);
                }
                catch(NumberFormatException nfe)
                {
                    logger.error("Please either enter 'all' or a number as the Cropping Factor");
                    return null;
                }

                pCropSize = track.getObjectSize().multiply(croppingFactorValue);

                for( Point3D position : locations.values() )
                {
                    trackOffsets.add(Utils.computeOffsetFromCenterSize(position, pCropSize));
                }

            }

            ImagePlus impObjectTrack = DataStreamingTools.getCroppedVSS(imp,
                    trackOffsets.toArray(new Point3D[trackOffsets.size()]),
                    pCropSize, track.getTmin(), track.getTmax());
            impObjectTrack.setTitle("Track_"+track.getID());

            imps.add(impObjectTrack);

        }

        return imps;

    }

    public synchronized void addLocationToOverlay(final Track track, int t) {

        int rx = (int) track.getObjectSize().getX()/2;
        int ry = (int) track.getObjectSize().getY()/2;
        int rz = (int) track.getObjectSize().getZ()/2;

        Roi roi;
        Overlay o = imp.getOverlay();
        if(o==null) {
            o = new Overlay();
            imp.setOverlay(o);
        }

        int x = (int) track.getPosition(t).getX();
        int y = (int) track.getPosition(t).getY();
        int z = (int) track.getPosition(t).getZ();
        int c = (int) track.getC();

        int rrx, rry;
        for(int iz=0; iz<imp.getNSlices(); iz++) {
            rrx = Math.max(rx/(Math.abs(iz-z)+1),1);
            rry = Math.max(ry/(Math.abs(iz-z)+1),1);
            roi = new Roi(x - rrx, y - rry, 2 * rrx + 1, 2 * rry + 1);
            roi.setPosition(c+1, iz+1, t+1);
            o.add(roi);
        }
    }

    public synchronized Track addNewTrack(TrackingSettings trackingSettings) {

        int trackID = tracks.size(); // TODO: something else here as ID?
        tracks.add(new Track(trackingSettings, trackID));
        return(tracks.get(tracks.size()-1));

    }

    public void trackObject(TrackingSettings trackingSettings)
    {
        interruptTrackingThreads = false;
        ObjectTracker objectTracker = new ObjectTracker(this, trackingSettings, logger);
        es.execute(objectTracker);

    }

    public void cancelTracking()
    {
        logger.info("Stopping all tracking...");
        interruptTrackingThreads = true;
    }

}


/*
    public int addTrackStartWholeDataSet(ImagePlus imp) {
        int t;

        int ntTracking = nt;
        t = imp.getT()-1;

        if(t+nt > imp.getNFrames()) {
            logger.error("Your track would be longer than the movie!\n" +
                    "Please\n- reduce the 'Track length', or\n- move the time slider to an earlier time point.");
            return(-1);
        }

        totalTimePointsToBeTracked += ntTracking;
        int newTrackID = tracks.size();
        //info("added new track start; trackID = "+newTrackID+"; starting [frame] = "+t+"; length [frames] = "+ntTracking);
        tracks.add(new Track(ntTracking));
        tracks.get(newTrackID).addLocation(new Point3D(0, 0, imp.getZ()-1), t, imp.getC()-1);

        return(newTrackID);

    }
*/


/*

    public int getNumberOfUncompletedTracks() {
        int uncomplete = 0;
        for(Track t:tracks) {
            if(!t.completed)
                uncomplete++;
        }
        return uncomplete;
    }


 */