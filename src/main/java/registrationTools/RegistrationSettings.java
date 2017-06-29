package registrationTools;

import ij.gui.Roi;

/**
 * Created by tischi on 30/04/17.
 */
public class RegistrationSettings {

    String os;
    String method;
    Boolean recursive = false;
    int reference = 1, delta;
    int[] regRange = new int[]{1,1};
    int[] zRange = new int[]{1,1};
    double background = 0;
    Type type = Type.AFFINE;
    String folderElastix = "/Users/tischi/Downloads/elastix_macosx64_v4.8/";
    String folderTmp = "/Users/tischi/Desktop/tmp/";
    int iterations = 100;
    String spatialSamples = "100; 100";
    int workers = Runtime.getRuntime().availableProcessors();
    String resolutionPyramid = "10 10; 2 2";
    int bitDepth = 8;
    Roi roi = null;
    String bSplineGridSpacing = "30 30";

    public enum Type {
        TRANSLATION("Translation"),
        EULER("Euler"),
        AFFINE("Affine"),
        SPLINE("BSpline");

        private final String text;

        private Type(final String text) {
            this.text = text;
        }

        public String toString() {
            return text;
        }
    }
}
