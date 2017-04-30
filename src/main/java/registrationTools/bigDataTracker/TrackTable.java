package registrationTools.bigDataTracker;

import registrationTools.logging.IJLazySwingLogger;
import registrationTools.logging.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by tischi on 14/04/17.
 */
class TrackTable  {

    Logger logger = new IJLazySwingLogger();

    private JTable table;

    public TrackTable() {
        String[] columnNames = {"ID_T",
                "X",
                "Y",
                "Z",
                "T",
                "ID"
                //,
                //"t_TotalSum",
                //"t_ReadThis",
                //"t_ProcessThis"
        };

        DefaultTableModel model = new DefaultTableModel(columnNames,0);
        table = new JTable(model);
      }

    public synchronized void addRow(final Object[] row)
    {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(row);
    }

    public void clear() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
    }

    public JTable getTable() {
        return table;
    }

    public void saveTrackTable(File file) {
        try{
            TableModel model = this.getTable().getModel();
            FileWriter excel = new FileWriter(file);

            for(int i = 0; i < model.getColumnCount(); i++){
                excel.write(model.getColumnName(i) + "\t");
            }
            excel.write("\n");

            for(int i=0; i< model.getRowCount(); i++) {
                for(int j=0; j < model.getColumnCount(); j++) {
                    excel.write(model.getValueAt(i,j).toString()+"\t");
                }
                excel.write("\n");
            }
            excel.close();

        } catch(IOException e) { logger.error(e.toString()); }
    }
}
