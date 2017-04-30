package registrationTools.bigDataTracker;

import ij.gui.Roi;
import javafx.geometry.Point3D;

/**
 * Created by tischi on 14/04/17.
 */
public class TrackingSettings {

    public String trackingMethod;
    public Roi trackStartROI;
    public Point3D objectSize;
    public Point3D subSamplingXYZ;
    public int subSamplingT;
    public int iterationsCenterOfMass;
    public int nt;
    public int channel;
    public double trackingFactor;
    public int background;

}
