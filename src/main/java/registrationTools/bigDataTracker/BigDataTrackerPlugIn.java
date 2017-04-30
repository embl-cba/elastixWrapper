package registrationTools.bigDataTracker;

import ij.IJ;
import ij.plugin.PlugIn;

import javax.swing.*;

public class BigDataTrackerPlugIn implements PlugIn {

    @Override
    public void run(String s)
    {
        BigDataTrackerGUI bigDataTrackerGUI = new BigDataTrackerGUI(IJ.getImage());

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                bigDataTrackerGUI.showDialog();
            }
        });

    }
}

