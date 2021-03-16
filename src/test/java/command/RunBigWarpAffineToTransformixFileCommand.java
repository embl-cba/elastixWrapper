package command;

import itc.commands.BigWarpAffineToTransformixFileCommand;
import net.imagej.ImageJ;

public class RunBigWarpAffineToTransformixFileCommand
{
    public static void main(final String... args)
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run( BigWarpAffineToTransformixFileCommand.class, true );
    }
}
