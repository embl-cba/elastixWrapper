package registrationTools;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import javax.swing.*;

public class RegistrationToolsPlugIn_ implements PlugIn {

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
        Class<?> clazz = RegistrationToolsPlugIn_.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length
                ());
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ij.ImageJ();

        //ImagePlus imp1 = IJ.openImage("C:\\Users\\tischer\\Desktop\\series--z4---t179-231.tif"); imp1.show();

        //ImagePlus imp2 = IJ.openImage("/Users/tischi/Documents/fiji-plugin-registrationTools/example-data/3d-movie" +
        //        "--embryo.tif"); imp2.show();

        //ImagePlus imp2 = IJ.openImage("/Users/tischi/Documents/fiji-plugin-registrationTools/example-data/2d-movie--affine--crop.tif"); imp2.show();

        ImagePlus imp2 = IJ.openImage("/Users/tischi/Desktop/BIAS2017-Registration/Cell-45.tif"); imp2.show();
        IJ.wait(2000);


        // Run the plugin
        //

        RegistrationToolsPlugIn_ registrationToolsPlugIn = new RegistrationToolsPlugIn_();
        registrationToolsPlugIn.run("");
    }

}
