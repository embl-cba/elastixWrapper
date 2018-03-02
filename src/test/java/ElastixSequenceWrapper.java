import de.embl.cba.elastixwrapper.plugins.SequenceRegistrationPlugIn;
import ij.IJ;

public class ElastixSequenceWrapper
{
    public static void main(String[] args)
    {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = SequenceRegistrationPlugIn.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ij.ImageJ();

        // open an image
        IJ.openImage("/Users/tischer/Documents/fiji-plugin-elastixWrapper/src/test/resources/ellipsoid-horizontal-and-at45degrees.tif.zip").show();
        IJ.wait(2000);

        // run the plugin
        //

        SequenceRegistrationPlugIn sequenceRegistrationPlugIn = new SequenceRegistrationPlugIn();
        sequenceRegistrationPlugIn.run("");
    }
}
