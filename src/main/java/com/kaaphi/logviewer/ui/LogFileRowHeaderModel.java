package com.kaaphi.logviewer.ui;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

public class LogFileRowHeaderModel extends AbstractTableModel implements TableModelListener {
  private LogFileTableModel model;

  public LogFileRowHeaderModel(LogFileTableModel model) {
    this.model = model;
    model.addTableModelListener(this);
  }

  @Override
  public int getColumnCount() {
    return 1;
  }

  @Override
  public int getRowCount() {
    return model.getRowCount();
  }

  @Override
  public Object getValueAt(int r, int c) {
    return model.getLine(r);
  }

  @Override
  public void tableChanged(TableModelEvent e) {
    fireTableChanged(new TableModelEvent(this, e.getFirstRow(), e.getLastRow(), 0, e.getType()));
  }

}
