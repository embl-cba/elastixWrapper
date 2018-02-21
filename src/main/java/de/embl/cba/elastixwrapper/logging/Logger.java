package de.embl.cba.elastixwrapper.logging;

public interface Logger {

    /**
     * whether or not to show debug messages
     *
     * @param message
     */
    void setShowDebug(boolean showDebug);


    /**
     * whether or not to show debug messages
     *
     * @param message
     */
    boolean isShowDebug();

    /**
     * prints messages that are merely for information, such as progress of computations
     *
     * @param message
     */
    void info(String message);


    /**
     * prints messages that are merely for information, such as progress of computations
     *
     * @param message
     */
    void progress(String message, String progress);

    /**
     * shows important messages that should not be overlooked by the user
     *
     * @param message
     */
    void error(String message);

    /**
     * shows messages that contain warnings
     *
     * @param message
     */
    void warning(String message);

    /**
     * shows messages that contain information for debugging
     *
     * @param message
     */
    void debug(String message);


}
