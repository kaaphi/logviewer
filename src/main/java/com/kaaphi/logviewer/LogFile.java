package com.kaaphi.logviewer;

import java.util.AbstractList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import com.kaaphi.logviewer.ui.filter.ComplexContainsFilter;


public class LogFile implements Iterable<String> {
  private static final Logger log = Logger.getLogger(LogFile.class);

  private static final Comparator<LogLine> offsetComparator = new Comparator<LogLine>() {
    @Override
    public int compare(LogLine o1, LogLine o2) {
      return o1.getStartIndex() - o2.getStartIndex();
    }
  };

  private List<LogLine> logLines;
  private List<LogLine> viewList;
  private int[] view;
  private int[] model;
  private int viewSize;
  private int viewCharacterLength;
  private FilterListener listener;


  public LogFile(List<LogLine> logLines) {
    this.logLines = logLines;
    this.view = new int[logLines.size()];
    this.model = new int[logLines.size()];
    this.viewList = new ViewList();

    resetFilter();
  }

  public void setListener(FilterListener listener) {
    this.listener = listener;
  }

  public String getRow(int i) {
    int index = view[i];
    return logLines.get(index).getLine();
  }

  public LogLine getLine(int i) {
    return logLines.get(view[i]);
  }

  public int getFileRow(int filteredRow) {
    return view[filteredRow];
  }

  public int getFilteredRow(int fileRow) {
    return model[fileRow];
  }


  public int size() { 
    return viewSize;
  }

  public int characterLength() {
    return viewCharacterLength;
  }

  public int unfilteredSize() {
    return logLines.size();
  }

  @Override
  public Iterator<String> iterator() {
    return new Iterator<String>() {
      int index = 0;
      int max = size();

      @Override
      public boolean hasNext() {
        return index < max;
      }
      @Override
      public String next() {
        return getRow(index++);
      }
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public List<LogLine> unfilteredLines() {
    return logLines;
  }

  public List<LogLine> lines() {
    return viewList;
  }

 public int getLineIndex(int offset) {
    LogLine key = new LogLine("", null, -1, -1);
    key.setStartIndex(offset);
    int index = Collections.binarySearch(lines(), key, offsetComparator);
    if(index < 0) {
      index = -index - 2;
    }

    return index;			
  }

  public void applyFilter(Filter filter) {
    try {
      if(listener != null) listener.filteringStarted();

      int i = 0;
      int j = 0;
      int size = logLines.size();
      int startIndex = 0;
      for(LogLine line : logLines) {
        if(filter == null || filter.filter(line.getRawLine())) {
          int viewIdx = j++;
          view[viewIdx] = i;
          model[i] = viewIdx; 
          startIndex = line.setStartIndex(startIndex);
        } else {
          model[i] = -j;
          line.setStartIndex(-startIndex);
        }
        i++;
        if((i % 1000 == 0) && (listener != null) && (!listener.filterUpdate(i, size))) {
          resetFilter();
          return;
        }
      }
      viewSize = j;
      viewCharacterLength = startIndex;
    } finally {
      if(listener != null) listener.filteringEnded();
    }
  }

  private void resetFilter() {
    int i = 0;
    viewCharacterLength = 0;
    for(LogLine line : logLines) {
      view[i] = i;
      model[i] = i;
      i++;
      viewCharacterLength = line.setStartIndex(viewCharacterLength);
    }
    viewSize = view.length;
  }

  public static interface Filter {
    public boolean filter(String line);
  }

  public static Filter createRegexFilter(String regex) {
    final Pattern pattern = Pattern.compile(regex);

    return new Filter() {
      @Override
      public boolean filter(String line) {
        return pattern.matcher(line).find();
      }
    };
  }

  public static Filter createContainsFilter(String str) {
    return new ComplexContainsFilter(str);
  }

  private class ViewList extends AbstractList<LogLine> implements RandomAccess {
    @Override
    public LogLine get(int index) {
      return getLine(index);
    }

    @Override
    public int size() {
      return LogFile.this.size();
    }

  }
}
