package registrationTools;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import javax.swing.*;

public class RegistrationToolsPlugIn implements PlugIn {

    @Override
    public void run(String s)
    {
        RegistrationToolsGUI registrationToolsGUI = new RegistrationToolsGUI();

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                registrationToolsGUI.showDialog();
            }
        });

    }


    // main method for debugging
    // throws ImgIOException
    public static void main(String[] args)
    {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = RegistrationToolsPlugIn.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length
                ());
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ij.ImageJ();

        ImagePlus imp = IJ.openImage("/Users/tischi/Documents/fiji-plugin-registrationTools/example-data/2d-movie" +
                "--affine.tif");
        imp.show();

        // set the plugins.dir property to make the plugin appear in the Plugins menu
        RegistrationToolsPlugIn registrationToolsPlugIn = new RegistrationToolsPlugIn();
        registrationToolsPlugIn.run("");
    }

}
