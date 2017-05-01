package registrationTools;

/**
 * Created by tischi on 30/04/17.
 */
public class RegistrationSettings {

    String method;
    String tmpDir;
    Boolean snake = false;
    int referenceFrame, first, last, delta;
    Type type = Type.AFFINE;
    String folderElastix = "/Users/tischi/Downloads/elastix_macosx64_v4.8/bin/";


    public enum Type {
        TRANSLATION("Translation"),
        EULER("Euler"),
        AFFINE("Affine");

        private final String text;

        private Type(final String text) {
            this.text = text;
        }

        public String toString() {
            return text;
        }
    }
}
