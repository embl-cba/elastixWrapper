package registrationTools.dataStreamingTools;

import registrationTools.logging.IJLazySwingLogger;
import registrationTools.logging.Logger;
import registrationTools.utils.Utils;
import registrationTools.VirtualStackOfStacks.VirtualStackOfStacks;
import ij.IJ;
import ij.ImagePlus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Scanner;

import static java.awt.Desktop.getDesktop;
import static java.awt.Desktop.isDesktopSupported;

/**
 * Created by tischi on 11/04/17.
 */

public class DataStreamingToolsGUI extends JFrame implements ActionListener, FocusListener, ItemListener {

    JCheckBox cbLog = new JCheckBox("Verbose logging");
    JCheckBox cbLZW = new JCheckBox("LZW compression");
    JCheckBox cbSaveVolume = new JCheckBox("Save volume data");
    JCheckBox cbSaveProjection = new JCheckBox("Save xyz max-projection");

    JTextField tfBinning = new JTextField("1,1,1", 10);
    JTextField tfCropZMinMax = new JTextField("1,all", 5);
    JTextField tfCropTMinMax = new JTextField("1,all", 5);
    JTextField tfIOThreads = new JTextField("30", 2);
    JTextField tfRowsPerStrip = new JTextField("10", 3);

    JComboBox filterPatternComboBox = new JComboBox(new String[] {".*",".*_Target--.*",".*--LSEA00--.*",".*--LSEA01--.*"});
    JComboBox channelTimePatternComboBox = new JComboBox(new String[] {
            "None",
            Utils.LOAD_CHANNELS_FROM_FOLDERS,
            ".*_C<c>_T<t>.tif",
            "Cam_<c>_<t>.h5",
    });
    JComboBox hdf5DataSetComboBox = new JComboBox(new String[] {"None","Data","Data111","ITKImage/0/VoxelData","Data222","Data444"});
    JComboBox comboFileTypeForSaving = new JComboBox(Utils.FileType.values());

    final String BDV = "Big Data Viewer";
    JButton viewInBigDataViewer =  new JButton(BDV);

    final String SAVE = "Save";
    JButton save =  new JButton(SAVE);

    final String STREAMfromFolder = "Stream from folder";
    JButton streamFromFolder =  new JButton(STREAMfromFolder);

    final String STREAMfromInfoFile = "Stream from info file";
    JButton streamFromInfoFile =  new JButton(STREAMfromInfoFile);

    final String LOAD_FULLY_INTO_RAM = "Load current stream fully into RAM";
    JButton duplicateToRAM =  new JButton(LOAD_FULLY_INTO_RAM);

    final String CROPasNewStream = "Crop as new stream";
    JButton cropAsNewStream =  new JButton(CROPasNewStream);

    final String REPORTissue = "Report an issue";
    JButton reportIssue =  new JButton(REPORTissue);

    Logger logger = new IJLazySwingLogger();


    JFileChooser fc;

    public void StackStreamToolsGUI()
    {
    }

    public void showDialog()
    {


        JTabbedPane jtp = new JTabbedPane();

        String[] toolTipTexts = getToolTipFile("DataStreamingHelp.html");
        ToolTipManager.sharedInstance().setDismissDelay(10000000);

        // Checkboxes
        cbLog.setSelected(false);
        cbLog.addItemListener(this);
        cbLZW.setSelected(false);
        cbSaveVolume.setSelected(true);
        cbSaveProjection.setSelected(false);

        int i = 0, j = 0, k = 0;

        ArrayList<JPanel> mainPanels = new ArrayList();
        ArrayList<JPanel> panels = new ArrayList();

        // Streaming
        //

        mainPanels.add( new JPanel() );
        mainPanels.get(k).setLayout(new BoxLayout(mainPanels.get(k), BoxLayout.PAGE_AXIS));

        panels.add(new JPanel(new FlowLayout(FlowLayout.LEFT)));
        panels.get(j).add(new JLabel("STREAM FROM FOLDER"));
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        panels.get(j).add(new JLabel("Load files matching:"));
        panels.get(j).add(filterPatternComboBox);
        filterPatternComboBox.setEditable(true);
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        panels.get(j).add(new JLabel("Load multiple channels:"));
        channelTimePatternComboBox.setEditable(true);
        panels.get(j).add(channelTimePatternComboBox);
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        panels.get(j).add(new JLabel("Hdf5 data set:"));
        panels.get(j).add(hdf5DataSetComboBox);
        hdf5DataSetComboBox.setEditable(true);
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        streamFromFolder.setActionCommand(STREAMfromFolder);
        streamFromFolder.addActionListener(this);
        panels.get(j).add(streamFromFolder);
        mainPanels.get(k).add(panels.get(j++));

        mainPanels.get(k).add(new JSeparator(SwingConstants.HORIZONTAL));
        panels.add(new JPanel(new FlowLayout(FlowLayout.LEFT)));
        panels.get(j).add(new JLabel("STREAM FROM INFO FILE"));
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        streamFromInfoFile.setActionCommand(STREAMfromInfoFile);
        streamFromInfoFile.addActionListener(this);
        panels.get(j).add(streamFromInfoFile);
        mainPanels.get(k).add(panels.get(j++));

        mainPanels.get(k).add(new JSeparator(SwingConstants.HORIZONTAL));
        panels.add(new JPanel(new FlowLayout(FlowLayout.LEFT)));
        panels.get(j).add(new JLabel("LOAD FULLY"));
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        duplicateToRAM.setActionCommand(LOAD_FULLY_INTO_RAM);
        duplicateToRAM.addActionListener(this);
        panels.get(j).add(duplicateToRAM);
        mainPanels.get(k).add(panels.get(j++));

        jtp.add("Streaming", mainPanels.get(k++));

        // Cropping
        //

        mainPanels.add( new JPanel() );
        mainPanels.get(k).setLayout(new BoxLayout(mainPanels.get(k), BoxLayout.PAGE_AXIS));

        panels.add(new JPanel());
        panels.get(j).add(new JLabel("z-min, z-max [slices]:"));
        panels.get(j).add(tfCropZMinMax);
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        panels.get(j).add(new JLabel("t-min, t-max [frames]:"));
        panels.get(j).add(tfCropTMinMax);
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        cropAsNewStream.setActionCommand(CROPasNewStream);
        cropAsNewStream.addActionListener(this);
        panels.get(j).add(cropAsNewStream);
        mainPanels.get(k).add(panels.get(j++));

        jtp.add("Cropping", mainPanels.get(k++));

        // Saving
        //

        mainPanels.add(new JPanel());
        mainPanels.get(k).setLayout(new BoxLayout(mainPanels.get(k), BoxLayout.PAGE_AXIS));

        panels.add(new JPanel());
        panels.get(j).add(new JLabel("File type:"));
        panels.get(j).add(comboFileTypeForSaving);
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        panels.get(j).add(new JLabel("Binnings [pixels]: x1,y1,z1; x2,y2,z2; ... "));
        panels.get(j).add(tfBinning);
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        panels.get(j).add(cbSaveVolume);
        panels.get(j).add(cbSaveProjection);
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        panels.get(j).add(cbLZW);
        panels.get(j).add(new JLabel("LZW chunks [ny]"));
        panels.get(j).add(tfRowsPerStrip);
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        save.setActionCommand(SAVE);
        save.addActionListener(this);
        panels.get(j).add(save);
        mainPanels.get(k).add(panels.get(j++));

        jtp.add("Saving", mainPanels.get(k++));

        // Viewing
        //
        mainPanels.add( new JPanel() );
        mainPanels.get(k).setLayout(new BoxLayout(mainPanels.get(k), BoxLayout.PAGE_AXIS));

        panels.add(new JPanel());
        viewInBigDataViewer.setActionCommand(BDV);
        viewInBigDataViewer.addActionListener(this);
        panels.get(j).add(viewInBigDataViewer);
        mainPanels.get(k).add(panels.get(j++));

        jtp.add("Viewing", mainPanels.get(k++));

        // Misc
        //
        mainPanels.add( new JPanel() );
        mainPanels.get(k).setLayout(new BoxLayout(mainPanels.get(k), BoxLayout.PAGE_AXIS));

        panels.add(new JPanel());
        panels.get(j).add(new JLabel("I/O threads"));
        panels.get(j).add(tfIOThreads);
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        panels.get(j).add(cbLog);
        mainPanels.get(k).add(panels.get(j++));

        panels.add(new JPanel());
        reportIssue.setActionCommand(REPORTissue);
        reportIssue.addActionListener(this);
        panels.get(j).add(reportIssue);
        mainPanels.get(k).add(panels.get(j++));

        jtp.add("Misc", mainPanels.get(k++));

        // Show the GUI
        setTitle("Data Streaming Tools");
        add(jtp);
        setVisible(true);
        pack();

    }

    public void focusGained( FocusEvent e )
    {
        //
    }

    public void focusLost( FocusEvent e )
    {
        JTextField tf = (JTextField) e.getSource();
        if ( tf != null )
        {
            tf.postActionEvent();
        }
    }

    public void itemStateChanged( ItemEvent e )
    {
        Object source = e.getItemSelectable();
        if (source == cbLog)
        {
            if ( e.getStateChange() == ItemEvent.DESELECTED )
            {
                logger.setShowDebug(false);
            }
            else
            {
                logger.setShowDebug(true);
            }
        }
    }

    public void actionPerformed( ActionEvent e )
    {
        int i = 0;

        //
        // Get values from GUI
        //
        final DataStreamingTools dataStreamingTools = new DataStreamingTools();
        final String h5DataSet = (String)hdf5DataSetComboBox.getSelectedItem();
        final int nIOthreads = new Integer(tfIOThreads.getText());
        final int rowsPerStrip = new Integer(tfRowsPerStrip.getText());
        final String filterPattern = (String)filterPatternComboBox.getSelectedItem();
        final String channelPattern = (String) channelTimePatternComboBox.getSelectedItem();

        if (e.getActionCommand().equals(STREAMfromFolder)) {

            // Open from folder
            final String directory = IJ.getDirectory("Select a Directory");
            if (directory == null)
                return;

            Thread t1 = new Thread(new Runnable() {
                public void run() {
                    dataStreamingTools.openFromDirectory(directory, channelPattern, filterPattern, h5DataSet, nIOthreads);
                }
            });
            t1.start();

        }
        else if (e.getActionCommand().equals(BDV))
        {

            //
            // View current channel and time-point in BigDataViewer
            //
                /*
                final net.imagej.ImageJ ij = new net.imagej.ImageJ();

                final ImagePlus imp = IJ.getImage();
                VirtualStackOfStacks vss = (VirtualStackOfStacks) imp.getStack();
                ImagePlus impCT = vss.getFullFrame(imp.getT()-1, imp.getC()-1);
                final Img<UnsignedShortType> image = ImageJFunctions.wrapShort( impCT );
                BdvSource bdv = BdvFunctions.show(image, "time point "+imp.getT());
                Pair<? extends RealType,? extends RealType> minMax = ij.op().stats().minMax( image );
                bdv.setDisplayRange(minMax.getA().getRealDouble(), minMax.getB().getRealDouble());
                */
            logger.error("Currently not implemented.");

        }
        else if (e.getActionCommand().equals(STREAMfromInfoFile))
        {
            // Open from file
            //
            String filePath = IJ.getFilePath("Select *.ser file");
            if (filePath == null)
                return;
            File file = new File(filePath);
            ImagePlus imp = dataStreamingTools.openFromInfoFile(file.getParent() + "/", file.getName());
            imp.show();
            imp.setPosition(1, imp.getNSlices()/2, 1);
            imp.updateAndDraw();
            imp.resetDisplayRange();
        }
        else if (e.getActionCommand().equals(SAVE))
        {

            ImagePlus imp = IJ.getImage();
            if ( !Utils.hasVirtualStackOfStacks(imp) ) return;
            VirtualStackOfStacks vss = (VirtualStackOfStacks) imp.getStack();

            // Check that all image files have been parsed
            //
            if (vss.numberOfUnparsedFiles() > 0)
            {
                logger.error("There are still " + vss.numberOfUnparsedFiles() +
                        " files in the folder that have not been parsed yet.\n" +
                        "Please try again later (check ImageJ's status bar).");
                return;
            }

            fc = new JFileChooser(vss.getDirectory());
            int returnVal = fc.showSaveDialog(DataStreamingToolsGUI.this);
            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                final File file = fc.getSelectedFile();

                Utils.FileType fileType = (Utils.FileType) comboFileTypeForSaving.getSelectedItem();

                if ( fileType.equals(Utils.FileType.SERIALIZED_HEADERS) )
                {

                    // Save the info file
                    //
                    Thread t1 = new Thread(new Runnable() {
                        public void run()
                        {
                            logger.info("Saving: " + file.getAbsolutePath());
                            dataStreamingTools.writeFileInfosSer(vss.getFileInfosSer(), file.getAbsolutePath());
                        }
                    }); t1.start();

                }
                else if ( fileType.equals(Utils.FileType.TIFF)
                        || fileType.equals(Utils.FileType.HDF5) )
                {

                    final int ioThreads = new Integer(tfIOThreads.getText());

                    // Check that there is enough memory to hold the data in RAM while saving
                    //
                    if( ! Utils.checkMemoryRequirements(imp, Math.min(ioThreads, imp.getNFrames())) ) return;

                    String compression = "";
                    if(cbLZW.isSelected())
                        compression="LZW";

                    DataStreamingTools.saveVSSAsStacks(imp, tfBinning.getText(), cbSaveVolume.isSelected(),
                            cbSaveProjection.isSelected(),
                            file.getAbsolutePath(), fileType,
                            compression, rowsPerStrip, ioThreads);

                }

            }

        }
        else if (e.getActionCommand().equals(LOAD_FULLY_INTO_RAM))
        {

            if( ! Utils.checkMemoryRequirements(IJ.getImage()) ) return;
            if( ! Utils.hasVirtualStackOfStacks(IJ.getImage())) return;

            Thread t1 = new Thread(new Runnable() {
                public void run() {
                    ImagePlus impRAM = dataStreamingTools.loadVSSFullyIntoRAM(IJ.getImage(), nIOthreads);
                    if (impRAM != null)
                    {
                        impRAM.show();
                    }

                }
            }); t1.start();

        } else if (e.getActionCommand().equals(CROPasNewStream)) {

            //
            // Crop As New Stream
            //

            ImagePlus imp = IJ.getImage();
            if ( !Utils.hasVirtualStackOfStacks(imp) ) return;
            VirtualStackOfStacks vss = (VirtualStackOfStacks) imp.getStack();

            //
            // Check that all image files have been parsed
            //

            int numberOfUnparsedFiles = vss.numberOfUnparsedFiles();
            if(numberOfUnparsedFiles > 0) {
                logger.error("There are still " + numberOfUnparsedFiles +
                        " files in the folder that have not been parsed yet.\n" +
                        "Please try again later (check ImageJ's status bar).");
                return;
            }

            // get from gui
            //
            int[] zMinMax = getMinMaxFromTextField(imp, tfCropZMinMax, "z");
            int[] tMinMax = getMinMaxFromTextField(imp, tfCropTMinMax, "t");

            // check
            //
            if ( ! Utils.checkRange(imp, zMinMax[0], zMinMax[1], "z") ) return;
            if ( ! Utils.checkRange(imp, tMinMax[0], tMinMax[1], "t") ) return;

            // compute
            //
            ImagePlus imp2 = dataStreamingTools.getCroppedVSS(
                    imp, imp.getRoi(),
                    zMinMax[0] - 1, zMinMax[1] - 1,
                    tMinMax[0] - 1, tMinMax[1] - 1);

            // publish
            //
            if (imp2 != null)
            {
                Utils.show(imp2);
            }


        }  else if (e.getActionCommand().equals(REPORTissue)) {

            //
            // Report issue
            //

            String url = "https://github.com/tischi/imagej-open-stacks-as-virtualstack/issues";
            if (isDesktopSupported()) {
                try {
                    final URI uri = new URI(url);
                    getDesktop().browse(uri);
                } catch (URISyntaxException uriEx) {
                    logger.error(uriEx.toString());
                } catch (IOException ioEx) {
                    logger.error(ioEx.toString());
                }
            } else {
                logger.error("Could not open browser, please report issue here: \n" +
                        "https://github.com/tischi/imagej-open-stacks-as-virtualstack/issues");

            }

        }
    }

    private String[] getToolTipFile(String fileName) {

        ArrayList<String> toolTipTexts = new ArrayList<String>();

        //Get file from resources folder
        //ClassLoader classLoader = getClass().getClassLoader();
        //File file = new File(classLoader.getResource(fileName).getFile());

        //try {

        InputStream in = getClass().getResourceAsStream("/"+fileName);
        BufferedReader input = new BufferedReader(new InputStreamReader(in));
        Scanner scanner = new Scanner(input);

        StringBuilder sb = new StringBuilder("");


        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.equals("###")) {
                toolTipTexts.add(sb.toString());
                sb = new StringBuilder("");
            } else {
                sb.append(line);
            }

        }

        scanner.close();

        //} catch (IOException e) {

        //    logger.info("Did not find tool tip file 2.");
        //    e.printStackTrace();

        //}

        return(toolTipTexts.toArray(new String[0]));
    }

    private int[] getMinMaxFromTextField(ImagePlus imp, JTextField tf, String dimension)
    {
        String[] s = tf.getText().split(",");
        if(s.length != 2) {
            logger.error("Something went wrong parsing the min, max values.\n" +
                    "Please check that there are two comma separated values.");
            return null;
        }

        int min = new Integer(s[0]);
        int max = 0;
        if ( s[1].equals(("all")) )
        {
            if ( dimension.equals("z") )
                max = imp.getNSlices();
            else if ( dimension.equals("t") )
                max = imp.getNFrames();

        }
        else
        {
            max = new Integer(s[1]);
        }

        return new int[]{min,max};
    }

}
