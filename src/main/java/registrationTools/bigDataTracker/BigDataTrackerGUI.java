package registrationTools.bigDataTracker;

import registrationTools.VirtualStackOfStacks.VirtualStackOfStacks;
import registrationTools.logging.IJLazySwingLogger;
import registrationTools.logging.Logger;
import registrationTools.utils.Utils;
import ij.ImagePlus;
import ij.gui.Roi;
import javafx.geometry.Point3D;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Scanner;

public class BigDataTrackerGUI implements ActionListener, FocusListener
{
    JFrame frame;

    int nt = 10;
    int iterationsCenterOfMass = 6;
    Point3D objectSize = new Point3D(50,50,10);;
    double trackingFactor = 2.5;
    Point3D subSamplingXYZ = new Point3D(1,1,1);
    int subSamplingT = 1;
    private String resizeFactor = "1.0";
    private int background = 0;
    String trackingMethod = "center of mass";
    BigDataTracker bigDataTracker;
    TrackTablePanel trackTablePanel;
    String TRACKING_LENGTH = "Length [frames]";
    Integer TRACKING_LENGTH_ID = 3;
    String[] defaults = {
            String.valueOf((int) objectSize.getX()) + "," + (int) objectSize.getY() + "," +String.valueOf((int) objectSize.getZ()),
            String.valueOf(trackingFactor),
            String.valueOf((int) subSamplingXYZ.getX() + "," + (int) subSamplingXYZ.getY() + "," + (int) subSamplingXYZ.getZ() + "," + subSamplingT),
            String.valueOf(nt),
            String.valueOf(background),
            String.valueOf(resizeFactor)
    };

    ImagePlus imp;

    Logger logger = new IJLazySwingLogger();

    String[] texts = {
            "Object size: x,y,z [pixels]",
            "Tracking window size [factor]",
            "dx, dy, dz, dt [pixels, frames]",
            TRACKING_LENGTH,
            "Background value [gray values]",
            "Resize objects by [factor]"
    };

    String[] buttonActions = {
            "Set x&y from ROI",
            "Set z",
            "Track selected object",
            "Stop tracking",
            "Show table",
            "Save table",
            "Clear all tracks",
            "View as new stream",
            "Report issue"
    };


    String[] comboNames = {
            "Method"
    };

    String[][] comboChoices = {
            {"center of mass","correlation"}
    };

    JTextField[] textFields = new JTextField[texts.length];

    JLabel[] labels = new JLabel[texts.length];

    int previouslySelectedZ = -1;

    public BigDataTrackerGUI(ImagePlus imp)
    {
        if ( !Utils.hasVirtualStackOfStacks(imp) ) return;
        nt = imp.getNFrames();
        background = (int) imp.getProcessor().getMin();
        this.imp = imp;
        this.bigDataTracker = new BigDataTracker(imp);
        trackTablePanel = new TrackTablePanel(bigDataTracker.getTrackTable(), imp);
        setDefaults();
    }

    public void setDefaults()
    {

        String[] defaults = {
                String.valueOf((int) objectSize.getX()) + "," + (int) objectSize.getY() + "," +String.valueOf((int) objectSize.getZ()),
                String.valueOf(trackingFactor),
                String.valueOf((int) subSamplingXYZ.getX() + "," + (int) subSamplingXYZ.getY() + "," + (int) subSamplingXYZ.getZ() + "," + subSamplingT),
                String.valueOf(nt),
                String.valueOf(background),
                String.valueOf(resizeFactor)
        };

        this.defaults = defaults;
    }

    public void showDialog()
    {

        frame = new JFrame("Big Data Tracker");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Container c = frame.getContentPane();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        String[] toolTipTexts = getToolTipFile("TrackAndCropHelp.html");
        int iToolTipText = 0;

        //
        // Configure all TextFields
        //
        for (int i = 0; i < textFields.length; i++, iToolTipText++)
        {
            textFields[i] = new JTextField(12);
            textFields[i].setActionCommand(texts[i]);
            textFields[i].addActionListener(this);
            textFields[i].addFocusListener(this);
            textFields[i].setText(defaults[i]);
            textFields[i].setToolTipText(toolTipTexts[iToolTipText]);
            labels[i] = new JLabel(texts[i] + ": ");
            labels[i].setLabelFor(textFields[i]);
        }

        //
        // Buttons
        //
        JButton[] buttons = new JButton[buttonActions.length];

        for (int i = 0; i < buttons.length; i++, iToolTipText++) {
            buttons[i] = new JButton(buttonActions[i]);
            buttons[i].setActionCommand(buttonActions[i]);
            buttons[i].addActionListener(this);
            buttons[i].setToolTipText(toolTipTexts[iToolTipText]);
        }

        //
        // ComboBoxes
        //
        JComboBox[] comboBoxes = new JComboBox[comboNames.length];
        JLabel[] comboLabels = new JLabel[comboNames.length];

        for (int i = 0; i < comboChoices.length; i++, iToolTipText++) {
            comboBoxes[i] = new JComboBox(comboChoices[i]);
            comboBoxes[i].setActionCommand(comboNames[i]);
            comboBoxes[i].addActionListener(this);
            comboBoxes[i].setToolTipText(toolTipTexts[iToolTipText]);
            comboLabels[i] = new JLabel(comboNames[i] + ": ");
            comboLabels[i].setLabelFor(comboBoxes[i]);
        }

        //
        // Panels
        //
        int i = 0;
        ArrayList<JPanel> panels = new ArrayList<JPanel>();
        int iPanel = 0;
        int k = 0;
        int iComboBox = 0;
        //
        // TRACKING
        //
        panels.add(new JPanel(new FlowLayout(FlowLayout.LEFT)));
        panels.get(iPanel).add(new JLabel("TRACKING"));
        c.add(panels.get(iPanel++));
        // Object size
        panels.add(new JPanel(new FlowLayout(FlowLayout.RIGHT)));
        panels.get(iPanel).add(buttons[i++]);
        panels.get(iPanel).add(buttons[i++]);
        panels.get(iPanel).add(labels[k]);
        panels.get(iPanel).add(textFields[k++]);
        c.add(panels.get(iPanel++));
        // Window size
        panels.add(new JPanel(new FlowLayout(FlowLayout.RIGHT)));
        panels.get(iPanel).add(labels[k]);
        panels.get(iPanel).add(textFields[k++]);
        c.add(panels.get(iPanel++));
        // Subsampling
        panels.add(new JPanel(new FlowLayout(FlowLayout.RIGHT)));
        panels.get(iPanel).add(labels[k]);
        panels.get(iPanel).add(textFields[k++]);
        c.add(panels.get(iPanel++));
        // Length
        panels.add(new JPanel(new FlowLayout(FlowLayout.RIGHT)));
        panels.get(iPanel).add(labels[k]);
        panels.get(iPanel).add(textFields[k++]);
        c.add(panels.get(iPanel++));
        // Method
        panels.add(new JPanel(new FlowLayout(FlowLayout.RIGHT)));
        panels.get(iPanel).add(comboLabels[iComboBox]);
        panels.get(iPanel).add(comboBoxes[iComboBox++]);
        c.add(panels.get(iPanel++));
        // Background value (this will be subtracted from the image)
        panels.add(new JPanel(new FlowLayout(FlowLayout.RIGHT)));
        panels.get(iPanel).add(labels[k]);
        panels.get(iPanel).add(textFields[k++]);
        c.add(panels.get(iPanel++));
        // ObjectTracker button
        panels.add(new JPanel(new FlowLayout(FlowLayout.CENTER)));
        panels.get(iPanel).add(buttons[i++]);
        c.add(panels.get(iPanel++));
        // ObjectTracker cancel button
        panels.add(new JPanel(new FlowLayout(FlowLayout.CENTER)));
        panels.get(iPanel).add(buttons[i++]);
        c.add(panels.get(iPanel++));

        //
        // RESULTS TABLE
        //
        c.add(new JSeparator(SwingConstants.HORIZONTAL));
        panels.add(new JPanel(new FlowLayout(FlowLayout.LEFT)));
        panels.get(iPanel).add(new JLabel("RESULTS TABLE"));
        c.add(panels.get(iPanel++));
        // Table buttons
        panels.add(new JPanel(new FlowLayout(FlowLayout.CENTER)));
        panels.get(iPanel).add(buttons[i++]);
        panels.get(iPanel).add(buttons[i++]);
        panels.get(iPanel).add(buttons[i++]);
        c.add(panels.get(iPanel++));
        //
        // CROPPING
        //
        c.add(new JSeparator(SwingConstants.HORIZONTAL));
        panels.add(new JPanel(new FlowLayout(FlowLayout.LEFT)));
        panels.get(iPanel).add(new JLabel("VIEW TRACKED OBJECTS"));
        c.add(panels.get(iPanel++));

        panels.add(new JPanel(new FlowLayout(FlowLayout.CENTER)));
        panels.get(iPanel).add(labels[k]);
        panels.get(iPanel).add(textFields[k++]);
        panels.get(iPanel).add(buttons[i++]);
        c.add(panels.get(iPanel++));
        //
        // MISCELLANEOUS
        //
        c.add(new JSeparator(SwingConstants.HORIZONTAL));
        panels.add(new JPanel(new FlowLayout(FlowLayout.LEFT)));
        panels.get(iPanel).add(new JLabel("MISCELLANEOUS"));
        c.add(panels.get(iPanel++));

        panels.add(new JPanel());
        panels.get(iPanel).add(buttons[i++]);
        c.add(panels.get(iPanel++));

        //
        // Show the GUI
        //
        frame.pack();
        frame.setLocation(imp.getWindow().getX() + imp.getWindow().getWidth(), imp.getWindow().getY());
        frame.setVisible(true);

    }

    public void changeTextField(int i, String text) {
        textFields[i].setText(text);
    }

    public void focusGained(FocusEvent e) {
        //
    }

    public void focusLost(FocusEvent e) {
        JTextField tf = (JTextField) e.getSource();
        if (!(tf == null)) {
            tf.postActionEvent();
        }
    }

    public void actionPerformed(ActionEvent e) {

        int i = 0, j = 0, k = 0;
        JFileChooser fc;

        if ( !Utils.hasVirtualStackOfStacks(imp) ) return;
        VirtualStackOfStacks vss = (VirtualStackOfStacks) imp.getStack();

        if (e.getActionCommand().equals(buttonActions[i++])) {

            //
            // Set nx, ny
            //

            Roi r = imp.getRoi();

            if(r==null || !r.getTypeAsString().equals("Rectangle")) {
                logger.error("Please put a rectangular selection on the image");
                return;
            }

            objectSize = new Point3D((int)r.getFloatWidth(), (int)r.getFloatHeight(), objectSize.getZ() );
            changeTextField(0, "" + (int) objectSize.getX() + "," + (int) objectSize.getY() + "," +
                    (int) objectSize.getZ());

        } else if (e.getActionCommand().equals(buttonActions[i++])) {
            //
            //  Set nz
            //

            int z = imp.getZ()-1;
            if (previouslySelectedZ==-1) {
                // first time do nothing
            } else {
                int nz = Math.abs(z - previouslySelectedZ);
                objectSize = new Point3D(objectSize.getX(), objectSize.getY(), nz);
                changeTextField(0, "" + (int) objectSize.getX() + "," + (int) objectSize.getY() + "," +
                        (int) objectSize.getZ());
            }
            previouslySelectedZ = z;

        } else if (e.getActionCommand().equals(buttonActions[i++])) {

            //
            // track selected object
            //

            // check roi
            //
            Roi roi = imp.getRoi();
            if (roi == null || ! (roi.getTypeAsString().equals("Point") || roi.getTypeAsString().equals("Rectangle")) ) {
                logger.error("Please use ImageJ's Point selection tool on image: '"
                        + imp.getTitle() + "'");
                return;
            }

            // configure the tracking
            //

            TrackingSettings trackingSettings = new TrackingSettings();

            trackingSettings.trackingMethod = trackingMethod;
            trackingSettings.iterationsCenterOfMass = (int) Math.ceil(Math.pow(trackingFactor, 2));
            trackingSettings.channel = imp.getC() - 1;
            trackingSettings.trackStartROI = roi;
            trackingSettings.objectSize = objectSize;
            trackingSettings.subSamplingXYZ = subSamplingXYZ;
            trackingSettings.subSamplingT = subSamplingT;
            trackingSettings.trackingFactor = trackingFactor;
            trackingSettings.background = background;
            trackingSettings.nt = (((imp.getT()-1) + nt) > imp.getNFrames()) ?
                    imp.getNFrames() - (imp.getT()-1) : nt;

            // give feedback
            //
            if ( trackingSettings.nt  != nt )
            {
                logger.warning("Requested track length too long => shortened.");
            }

            // do it
            //
            bigDataTracker.trackObject(trackingSettings);

        }
        else if ( e.getActionCommand().equals(buttonActions[i++]) )
        {

            //
            // Cancel Tracking
            //

            bigDataTracker.cancelTracking();

        }

        else if ( e.getActionCommand().equals(buttonActions[i++]) )
        {

            //
            // Show Table
            //

            showTrackTable();

        }
        else if (e.getActionCommand().equals(buttonActions[i++]))
        {

            //
            // Save Table
            //

            TableModel model = bigDataTracker.getTrackTable().getTable().getModel();
            if(model == null) {
                logger.error("There are no tracks yet.");
                return;
            }
            fc = new JFileChooser(vss.getDirectory());
            if (fc.showSaveDialog(this.frame) == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                bigDataTracker.getTrackTable().saveTrackTable(file);
            }

        }
        else if ( e.getActionCommand().equals(buttonActions[i++]) )
        {

            bigDataTracker.clearAllTracks();


        } else if (e.getActionCommand().equals(buttonActions[i++])) {

            //
            // View Object tracks
            //

            showTrackedObjects();


        } else if (e.getActionCommand().equals(buttonActions[i++])) {

            //
            // Report issue
            //

            if (Desktop.isDesktopSupported()) {
                try {
                    final URI uri = new URI("https://github.com/tischi/imagej-open-stacks-as-virtualstack/issues");
                    Desktop.getDesktop().browse(uri);
                } catch (URISyntaxException uriEx) {
                    logger.error(uriEx.toString());
                } catch (IOException ioEx) {
                    logger.error(ioEx.toString());
                }
            } else { /* TODO: error handling */ }

        } else if (e.getActionCommand().equals(texts[k++])) {
            //
            // ObjectTracker object size
            //
            JTextField source = (JTextField) e.getSource();
            String[] sA = source.getText().split(",");
            objectSize = new Point3D(new Integer(sA[0]), new Integer(sA[1]), new Integer(sA[2]));
        }
        else if (e.getActionCommand().equals(texts[k++]))
        {
            //
            // ObjectTracker factor
            //
            JTextField source = (JTextField) e.getSource();
            trackingFactor = new Double(source.getText());
        }
        else if (e.getActionCommand().equals(texts[k++]))
        {
            //
            // ObjectTracker sub-sampling
            //
            JTextField source = (JTextField) e.getSource();
            String[] sA = source.getText().split(",");
            subSamplingXYZ = new Point3D(new Integer(sA[0]), new Integer(sA[1]), new Integer(sA[2]));
            subSamplingT = new Integer(sA[3]);
        }
        else if ( e.getActionCommand().equals(texts[k++]) )
        {
            //
            // Track length
            //
            JTextField source = (JTextField) e.getSource();
            nt = new Integer(source.getText());
        }
        else if ( e.getActionCommand().equals(texts[k++]) )
        {
            //
            // Image background value
            //
            JTextField source = (JTextField) e.getSource();
            background = new Integer(source.getText());
        }
        else if (e.getActionCommand().equals(texts[k++]))
        {
            //
            // Cropping factor
            //
            JTextField source = (JTextField) e.getSource();
            resizeFactor = source.getText();
        }
        else if (e.getActionCommand().equals(comboNames[j++]))
        {
            //
            // ObjectTracker method
            //
            JComboBox cb = (JComboBox)e.getSource();
            trackingMethod = (String)cb.getSelectedItem();
        }
    }

    private String[] getToolTipFile(String fileName) {
        ArrayList<String> toolTipTexts = new ArrayList<String>();

        //Get file from resources folder
        InputStream in = getClass().getResourceAsStream("/"+fileName);
        BufferedReader input = new BufferedReader(new InputStreamReader(in));
        Scanner scanner = new Scanner(input);
        StringBuilder sb = new StringBuilder("");

        while ( scanner.hasNextLine() )
        {
            String line = scanner.nextLine();
            if(line.equals("###")) {
                toolTipTexts.add(sb.toString());
                sb = new StringBuilder("");
            } else {
                sb.append(line);
            }

        }

        scanner.close();


        return(toolTipTexts.toArray(new String[0]));
    }

    public void showTrackTable()
    {
        trackTablePanel.showTable();
    }

    public void showTrackedObjects() {

        ArrayList<ImagePlus> imps = bigDataTracker.getViewsOnTrackedObjects(resizeFactor);


        if( imps == null )
        {
            logger.info("The cropping failed!");
        }
        else
        {
            for (ImagePlus imp : imps)
            {
                Utils.show(imp);
            }
        }
    }

}
