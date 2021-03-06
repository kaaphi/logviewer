package com.kaaphi.logviewer.ui.search;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.kaaphi.logviewer.LogFile;
import com.kaaphi.logviewer.LogLine;

public class SearchSession {
  private LogFile file;
  private Map<Integer, MatchedLine> matchingLines;


  public void search(Searcher searcher) {
    matchingLines.clear();

    for(LogLine line : file.unfilteredLines()) {
      int[] found;
      int i = 0;
      MatchedLine match = null;
      while((found = searcher.findNext(i, line.getLine())) != null) {
        i = found[0]+found[1];
        found[0] += line.getStartIndex();
        if(match == null) match = new MatchedLine(line);
        match.matches.add(found);
      }

      if(match != null) {
        matchingLines.put(line.getLineNumber(), match);
      }
    }
  }

  private static class MatchedLine {
    private List<int[]> matches;

    public MatchedLine(LogLine line) {
      this.matches = new LinkedList<>();
    }
  }

  private static interface Searcher {
    public int[] findNext(int start, String line);
  }
}
