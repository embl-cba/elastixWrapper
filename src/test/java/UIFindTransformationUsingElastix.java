import de.embl.cba.elastixwrapper.commands.FindTransformationUsingElastix;
import net.imagej.ImageJ;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UIFindTransformationUsingElastix
{
    // Main
    public static void main(final String... args) throws Exception
    {

        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run( FindTransformationUsingElastix.class, true );

    }
}
