package com.kaaphi.logviewer.ui.filter;

import com.kaaphi.logviewer.LogFile;


public interface FilterEditor {
  public LogFile.Filter getFilter();
  public boolean isFilterValid();
  public void resetFilter();
}
