import de.embl.cba.elastixwrapper.RegistrationToolsPlugIn;
import ij.IJ;

public class TestDetlevNuclei
{
    public static void main(String[] args)
    {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = RegistrationToolsPlugIn.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ij.ImageJ();

        // open an image
        IJ.openImage("/Volumes/cba/tischer/projects/detlev-arendt-clem-registration--data/Nuclei-ProsPr6-asMovie-scale0.2.tif").show();
        IJ.wait(2000);

        // run the plugin
        //
        RegistrationToolsPlugIn registrationToolsPlugIn = new RegistrationToolsPlugIn();
        registrationToolsPlugIn.run("");
    }
}
