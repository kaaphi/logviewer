package com.kaaphi.logviewer.ui;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter.HighlightPainter;
import org.apache.log4j.Logger;
import com.kaaphi.logviewer.ui.LogDocument.LogLineElement;

public class SequenceHighlighter implements ChangeListener {
  private static final Logger log = Logger.getLogger(SequenceHighlighter.class);

  private JTextArea textArea;
  private JViewport viewport;
  //private List<Object> highlights = new LinkedList<>();
  private Map<String,Sequence> sequences = new HashMap<>();

  private NavigableMap<Integer, Map<Sequence, Set<Object>>> highlights = new TreeMap<>();

  private int currentStartIdx = -1;
  private int currentEndIdx = -1;

  public SequenceHighlighter(JViewport viewPort, JTextArea textArea) {
    this.viewport = viewPort;
    this.textArea = textArea;
    textArea.getDocument().addDocumentListener(new DocumentListener() {

      @Override
      public void removeUpdate(DocumentEvent e) {
        dataChanged();
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        dataChanged();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        dataChanged();
      }
    });
  }

  @Override
  public void stateChanged(ChangeEvent evt) {
    updateHighlights();
  }

  public void toggleSequence(String text, Color color) {
    if(text == null || text.isEmpty()) {
      return;
    }
    Sequence seq = sequences.remove(text);
    if(seq == null || !seq.getColor().equals(color)) {
      sequences.put(text, new Sequence(text, color));
    }
    dataChanged();
  }

  public void clearSequences(Color color) {
    sequences.values().removeIf(seq -> seq.getColor().equals(color));
    dataChanged();
  }

  public void clearAllSequences() {
    sequences.clear();
    dataChanged();
  }

  private void dataChanged() {
    log.trace("data change, resetting highlights");
    clearHighlights(highlights);
    currentStartIdx = -1;
    currentEndIdx = -1;
    updateHighlights();
  }

  public void updateHighlights() {
    Rectangle rect = viewport.getViewRect();
    log.trace("Rectangle: " + rect);
    log.trace("Size: " + textArea.getSize());
    log.trace(String.format("Valid: area=%b; viewport=%b",  textArea.isValid(), viewport.isValid()));
    if(!viewport.isVisible() || !textArea.isValid()) {
      return;
    }
    int rowStartOffset = textArea.viewToModel( new Point(0, rect.y) );
    int endOffset = textArea.viewToModel( new Point(0, rect.y + rect.height) );

    int startIdx = textArea.getDocument().getDefaultRootElement().getElementIndex(rowStartOffset);
    int endIdx = textArea.getDocument().getDefaultRootElement().getElementIndex(endOffset);
    log.trace(String.format("Viewable rows: %d-%d", startIdx, endIdx));

    if(!sequences.isEmpty()) {
      if(currentStartIdx >= 0) {
        if(currentStartIdx < startIdx) {
          clearHighlights(currentStartIdx, Math.min(currentEndIdx, startIdx));					
        } else if(startIdx < currentStartIdx) {
          highlightRows(startIdx, Math.min(currentStartIdx, endIdx));
        }

        if(currentEndIdx > endIdx) {
          clearHighlights(Math.max(currentStartIdx, endIdx), currentEndIdx);
        } else if(endIdx > currentEndIdx) {
          highlightRows(Math.max(currentEndIdx, startIdx), endIdx);
        }
      } else {
        //first run, 
        highlightRows(startIdx, endIdx);
      }
    }

    currentStartIdx = startIdx;
    currentEndIdx = endIdx;
  }

  private void clearHighlights(int startIdx, int endIdx) {
    log.trace(String.format("Clearing rows %d-%d", startIdx, endIdx));

    if(startIdx < endIdx) {
      clearHighlights(highlights.subMap(startIdx, true, endIdx, true));
    }
  }

  private void clearHighlights(Map<Integer, Map<Sequence, Set<Object>>> toRemove) {
    toRemove.values().stream()
    .map(Map::values)
    .flatMap(Collection::stream)
    .flatMap(Set::stream)
    .forEach(textArea.getHighlighter()::removeHighlight);

    toRemove.clear();
  }

  private void highlightRows(int startIdx, int endIdx) {
    if(startIdx < 0 || startIdx >= endIdx) {
      return;
    }		
    log.trace(String.format("Highlighting rows %d-%d", startIdx, endIdx));

    for(int i = startIdx; i <= endIdx; i++) {
      LogLineElement e = (LogLineElement) textArea.getDocument().getDefaultRootElement().getElement(i);
      String lineText = e.getLine().getLine();
      for(Sequence seq : sequences.values()) {
        HighlightPainter painter = new DefaultHighlightPainter(seq.getColor());
        int idx = -1;
        while((idx = lineText.indexOf(seq.getText(),idx+1)) >= 0) {
          log.trace(String.format("highlighting text %s in row %d at idx %d.", seq.getText(), i, idx));
          try {
            Object tag = textArea.getHighlighter().addHighlight(e.getStartOffset() + idx, e.getStartOffset() + idx + seq.getText().length(), painter);
            highlights
            .computeIfAbsent(i, key -> new HashMap<>())
            .computeIfAbsent(seq, key -> new HashSet<>())
            .add(tag);
          } catch (BadLocationException ble) {
            log.error("highlight fail", ble);
          }				
        }	
      }
    }	
  }

  public static class Sequence {
    private final String text;
    private final Color color;
    public Sequence(String text, Color color) {
      super();
      this.text = text;
      this.color = color;
    }
    public String getText() {
      return text;
    }
    public Color getColor() {
      return color;
    }
    @Override
    public int hashCode() {
      return Objects.hash(text);
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof Sequence))
        return false;
      Sequence other = (Sequence) obj;
      return Objects.equals(text, other.text);
    }

  }

}
