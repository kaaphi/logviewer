package com.kaaphi.logviewer.ui.search;

import java.awt.Color;
import java.awt.Rectangle;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import org.apache.log4j.Logger;

import com.kaaphi.logviewer.ui.LogDocument;
import com.kaaphi.logviewer.ui.LogDocument.LogLineElement;
import com.kaaphi.logviewer.ui.LogViewerConfiguration;

public class TypeAheadSearchSession {
	private static final Logger log = Logger.getLogger(TypeAheadSearchSession.class);
	
    private int currentElement= 0;
    private int currentIndex = 0;
    private JTextArea textArea;
    private LogDocument doc;
    private String searchString;
    private HighlightPainter painter;
    
    
    public TypeAheadSearchSession(JTextArea textArea, LogDocument doc) {
        super();
        this.textArea = textArea;
        this.doc = doc;
        setSearchHighlightColor(LogViewerConfiguration.getInstance().searchHighlight.get());
    }
    
    public void setSearchHighlightColor(Color c) {
    	this.painter = new DefaultHighlightPainter(c);
    }

    public void reset(String searchString) {
        this.searchString = searchString == null ? null : searchString.toLowerCase();
        currentElement= 0;
        currentIndex = 0;
        Highlighter highlighter = textArea.getHighlighter();
     	highlighter.removeAllHighlights();
    }
    
    public void updateSearchString(String searchString) {
    	if(this.searchString != null) {
    		currentIndex -= this.searchString.length();
    		this.searchString = searchString.toLowerCase();
    	} else {
    	    this.searchString = searchString.toLowerCase();
    	}
    }
    
    public void setStartingOffset(int offset) {
    	this.currentElement = doc.getDefaultRootElement().getElementIndex(offset);
    	Element e = doc.getDefaultRootElement().getElement(currentElement);
    	this.currentIndex = offset - e.getStartOffset();
    }
    
    public String getSearchString() {
        return searchString;
    }
    
    public boolean findPrevious() {
    	log.trace(String.format("%d %d", currentElement, currentIndex));
    	for(; currentElement >= 0; currentElement--) {
    		if((currentIndex = doFind(true)) >= 0) {
    			//currentIndex -= searchString.length();
    			currentIndex--;
    			return true;
    		} else {
    			log.trace("Failed to find anything");
    			currentIndex = Integer.MAX_VALUE;
    		}
    	}

    	//wrap back to end
    	currentElement = doc.getDefaultRootElement().getElementCount()-1;
		currentIndex = Integer.MAX_VALUE;

    	return false;
    }

    public boolean findNext() {
    	log.trace(String.format("%d %d", currentElement, currentIndex));
    	for(; currentElement < doc.getDefaultRootElement().getElementCount(); currentElement++) {
    		if((currentIndex = doFind(false)) >= 0) {
    			currentIndex += searchString.length();
    			return true;
    		} else {
    			log.trace("Failed to find anything");
    			currentIndex = 0;
    		}
    	}

    	//wrap back to top
    	currentElement = 0;
    	currentIndex = 0;
    	
    	return false;
    }
    
    private int doFind(boolean reverse) {
    	 int index;
         if((index = cellContains(currentElement, currentIndex, reverse)) >= 0) {
             scrollToCell(currentElement, index);
             return index;
         }
         
         return -1;
    }
    
    private void scrollToCell(int r, int i) {
        try {
            highlightSearch(r, i);
        } catch (BadLocationException e) {
            log.warn("Bad Location", e);
        }
    }
    
    private void highlightSearch(int r, int i) throws BadLocationException {
    	int offset = doc.getDefaultRootElement().getElement(r).getStartOffset() + i;
    	Rectangle rect1 = textArea.modelToView(offset);
    	Rectangle rect2 = textArea.modelToView(offset+searchString.length());
    	rect1.width = rect2.x - rect1.x;
    	textArea.scrollRectToVisible(rect1);
    	Highlighter highlighter = textArea.getHighlighter();
    	highlighter.removeAllHighlights();
    	try {
    		highlighter.addHighlight(offset, offset+searchString.length(), painter);
    	} catch (BadLocationException e) {
    		e.printStackTrace();
    	}
    }

    private int cellContains(int r, int i, boolean reverse) {
    	String row = ((LogLineElement)doc.getDefaultRootElement().getElement(r)).getLine().getLine();
    	int index = reverse ? 
    			row.toLowerCase().lastIndexOf(searchString, i) :
    				row.toLowerCase().indexOf(searchString, i);
    			if(index >= 0) {
    				return index;
    			}
    			return -1;
    }

}
