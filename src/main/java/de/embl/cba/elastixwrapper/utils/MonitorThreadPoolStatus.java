package de.embl.cba.elastixwrapper.utils;

import de.embl.cba.elastixwrapper.logging.IJLazySwingLogger;
import de.embl.cba.elastixwrapper.logging.Logger;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by tischi on 11/04/17.
 */
public class MonitorThreadPoolStatus {

    private static Logger logger = new IJLazySwingLogger();

    public static void showProgressAndWaitUntilDone(List<Future> futures,
                                                    String message,
                                                    int updateFrequencyMilliseconds) {
        int done = 0;
        while( done != futures.size() )
        {
            done = 0;
            for ( Future f : futures )
            {
                if (f.isDone() ) done++;
            }

            logger.progress( message,  done + "/" + futures.size() );

            try {
                Thread.sleep(updateFrequencyMilliseconds);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

        }

    }

}
