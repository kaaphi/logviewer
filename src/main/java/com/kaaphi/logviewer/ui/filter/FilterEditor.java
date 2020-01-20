package com.kaaphi.logviewer.ui.filter;

import com.kaaphi.logviewer.LogFile;
import java.awt.Font;


public interface FilterEditor {
  void setFilterFont(Font font);
  LogFile.Filter getFilter();
  boolean isFilterValid();
  void resetFilter();
}
