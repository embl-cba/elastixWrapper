package registrationTools;

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
                RegistrationToolsGUI.showDialog();
            }
        });

    }
}