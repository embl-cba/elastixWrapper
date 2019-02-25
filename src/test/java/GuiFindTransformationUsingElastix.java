import de.embl.cba.elastixwrapper.commands.ElastixCommand;
import net.imagej.ImageJ;

public class GuiFindTransformationUsingElastix
{
    // Main
    public static void main(final String... args) throws Exception
    {

        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run( ElastixCommand.class, true );

    }
}
