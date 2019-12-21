package com.kaaphi.logviewer.ui.util;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import com.kaaphi.logviewer.LogLine;

public class RowHeader extends JTable implements ChangeListener, PropertyChangeListener {
  public RowHeader(JTable table, TableModel model) {
    super(model);

    table.addPropertyChangeListener("rowHeight", this);
    setFocusable( false );
    setAutoCreateColumnsFromModel( false );

    TableColumn column = getColumnModel().getColumn(0);

    column.setCellRenderer(new RowNumberRenderer2());
    column.setPreferredWidth(60);
    setPreferredScrollableViewportSize(getPreferredSize());
    setRowHeight(table.getRowHeight());
    setRowMargin(0);
    setShowGrid(false);
    setSelectionModel(table.getSelectionModel());
  }

  @Override
  public void addNotify()
  {
    super.addNotify();

    Component c = getParent();

    //  Keep scrolling of the row table in sync with the main table.

    if (c instanceof JViewport)
    {
      JViewport viewport = (JViewport)c;
      viewport.addChangeListener( this );
    }
  }


  //
  //  Implement the ChangeListener
  //
  @Override
  public void stateChanged(ChangeEvent e)
  {
    //  Keep the scrolling of the row table in sync with main table

    JViewport viewport = (JViewport) e.getSource();
    JScrollPane scrollPane = (JScrollPane)viewport.getParent();
    scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
  }


  private static class RowNumberRenderer2 implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table,
        Object value, boolean isSelected, boolean hasFocus, int row,
        int column) {
      LogLine line = (LogLine)value;

      String label = String.format("%s: %d", line.getFile().getName(), line.getFileLineNumber());

      Component c = table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(table, line.getLineNumber(), isSelected, hasFocus, row, column); 

      ((JComponent)c).setToolTipText(label);

      DefaultTableCellRenderer tcr = (DefaultTableCellRenderer)c;
      tcr.setHorizontalAlignment(SwingConstants.RIGHT);

      return c;
    }

  }


  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    setRowHeight((Integer)evt.getNewValue());
  }

}
