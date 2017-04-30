package registrationTools.logging;

import ij.IJ;

import javax.swing.*;


public class IJLazySwingLogger implements Logger {

    private boolean showDebug = false;

    public IJLazySwingLogger() {
    }

    @Override
    public void setShowDebug(boolean showDebug)
    {
        this.showDebug = showDebug;
    }

    @Override
    public boolean isShowDebug()
    {
        return ( showDebug );
    }


    @Override
    public void info(String message){
        ijLazySwingLog(String.format("[INFO]: %s", message));
    }

    @Override
    public void progress( String message, String progress )
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                String[] logs = IJ.getLog().split("\n");
                if ( logs[logs.length-1].contains(message) )
                {
                    IJ.log(String.format("\\Update:[PROGRESS]: %s %s", message, progress));
                }
                else
                {
                    IJ.log(String.format("[PROGRESS]: %s %s", message, progress));
                }
            }
        });
    }

    @Override
    public void error(String _message){
        IJ.showMessage(String.format("[ERROR]: %s", _message));
    }

    @Override
    public void warning(String _message){
        ijLazySwingLog(String.format("[WARNING]: %s", _message));
    }

    @Override
    public void debug(String _message){
        if ( showDebug )
        {
            ijLazySwingLog(String.format("[DEBUG]: %s", _message));
        }
    }


    private void ijLazySwingLog(String message)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                IJ.log(message);
            }
        });
    }

}
