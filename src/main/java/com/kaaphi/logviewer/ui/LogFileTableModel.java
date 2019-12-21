package com.kaaphi.logviewer.ui;

import java.util.LinkedList;
import javax.swing.table.AbstractTableModel;
import com.kaaphi.logviewer.LogFile;
import com.kaaphi.logviewer.LogLine;

public class LogFileTableModel extends AbstractTableModel {
  private LogFile logFile;

  public LogFileTableModel() {
    this.logFile = new LogFile(new LinkedList<LogLine>());
  }

  @Override
  public int getColumnCount() {
    return 1;
  }

  @Override
  public int getRowCount() {
    return logFile.size();
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    String row = logFile.getRow(rowIndex);
    return row;
  }

  public void applyFilter(LogFile.Filter filter) {
    logFile.applyFilter(filter);
    fireTableDataChanged();
  }

  public int getUnfilteredRowCount() {
    return logFile.unfilteredSize();
  }

  public int getUnfilteredRowIndex(int row) {
    return logFile.getFileRow(row);
  }

  public LogLine getLine(int row) {
    return logFile.getLine(row);
  }

  public int getFilteredRowIndex(int row) {
    return logFile.getFilteredRow(row);
  }

  public String getRow(int i) {
    return logFile.getRow(i);
  }

  public void setLogFile(LogFile file) {
    this.logFile = file;
    fireTableStructureChanged();
  }

  @Override
  public String getColumnName(int column) {
    return Integer.toString(column);
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return true;
  }

  @Override
  public Class<?> getColumnClass(int arg0) {
    return String.class;
  }





}
