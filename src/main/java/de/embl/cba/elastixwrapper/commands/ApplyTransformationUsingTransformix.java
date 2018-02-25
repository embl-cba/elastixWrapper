package de.embl.cba.elastixwrapper.commands;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

import static de.embl.cba.elastixwrapper.commands.ApplyTransformationUsingTransformix.PLUGIN_NAME;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Elastix>" + PLUGIN_NAME )
public class ApplyTransformationUsingTransformix implements Command
{
    public static final String PLUGIN_NAME = "Apply transformation";

    @Parameter( label = "Elastix directory" )
    public File elastixDirectory;
    public static final String ELASTIX_DIRECTORY = "elastixDirectory";

    @Parameter( label = "Image" )
    public File inputImageFile;
    public static final String INPUT_IMAGE_FILE = "inputImageFile";

    @Parameter( label = "Transformation" )
    public File transformationFile;
    public static final String TRANSFORMATION_FILE = "transformationFile";

    @Parameter
    public LogService logService;

    public void run()
    {

    }
}
