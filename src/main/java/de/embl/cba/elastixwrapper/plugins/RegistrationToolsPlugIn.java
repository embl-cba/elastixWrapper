package de.embl.cba.elastixwrapper.plugins;

import de.embl.cba.elastixwrapper.plugins.RegistrationToolsGUI;
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


}
