package com.kaaphi.logviewer.ui.search;

import java.awt.Color;
import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter.HighlightPainter;

import org.apache.log4j.Logger;

import com.kaaphi.logviewer.ui.LogFileTableModel;
import com.kaaphi.logviewer.ui.LogViewerConfiguration;

public class SearchSession {
	private static final Logger log = Logger.getLogger(SearchSession.class);
	
    private int currentRow = 0;
    private int currentColumn = 0;
    private int currentIndex = 0;
    private JTable table;
    private LogFileTableModel model;
    private JTextField editorField;
    private String searchString;
    private HighlightPainter painter;
    
    
    public SearchSession(JTable table, LogFileTableModel model, JTextField editorField) {
        super();
        this.table = table;
        this.model = model;
        this.editorField = editorField;
        setSearchHighlightColor(LogViewerConfiguration.getInstance().searchHighlight.get());
    }
    
    public void setSearchHighlightColor(Color c) {
    	this.painter = new DefaultHighlightPainter(c);
    }

    public void reset(String searchString) {
        this.searchString = searchString == null ? null : searchString.toLowerCase();
        currentRow = 0;
        currentColumn = 0;
        currentIndex = 0;
        Highlighter highlighter = editorField.getHighlighter();
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
    
    public void setStartingRow(int row) {
        this.currentRow = row;
    }
    
    public String getSearchString() {
        return searchString;
    }
    
    public boolean findPrevious() {
    	log.trace(String.format("%d %d %d", currentRow, currentColumn, currentIndex));
    	for(; currentRow >= 0; currentRow--) {
    		for(; currentColumn >= 0; currentColumn--) {
    			if((currentIndex = doFind(true)) >= 0) {
    				//currentIndex -= searchString.length();
    				currentIndex--;
    				return true;
    			} else {
    				log.trace("Failed to find anything");
    				currentIndex = Integer.MAX_VALUE;
    			}
    		}
    		currentColumn = table.getColumnCount()-1;
    	}

    	//wrap back to end
    	currentRow = model.getRowCount()-1;
    	currentColumn = model.getColumnCount()-1;
		currentIndex = Integer.MAX_VALUE;

    	return false;
    }

    public boolean findNext() {
        log.trace(String.format("%d %d %d", currentRow, currentColumn, currentIndex));
    	for(; currentRow < model.getRowCount(); currentRow++) {
    		for(; currentColumn < model.getColumnCount(); currentColumn++) {
    			if((currentIndex = doFind(false)) >= 0) {
    				currentIndex += searchString.length();
    				return true;
    			} else {
    				log.trace("Failed to find anything");
    				currentIndex = 0;
    			}
    		}
    		currentColumn = 0;
    	}

    	//wrap back to top
    	currentRow = 0;
    	currentColumn = 0;
    	currentIndex = 0;
    	
    	return false;
    }
    
    private int doFind(boolean reverse) {
    	 int index;
         if((index = cellContains(currentRow, currentColumn, currentIndex, reverse)) >= 0) {
             scrollToCell(currentRow, currentColumn, index);
             return index;
         }
         
         return -1;
    }
    
    private void scrollToCell(int r, int c, int i) {
        /*
        TableColumnModel cModel = table.getColumnModel();
        int x = 0;
        for(int j = 0; j < c; j++) {
            x += cModel.getColumn(j).getPreferredWidth();
        }
        int width = cModel.getColumn(c).getPreferredWidth();
        Rectangle rect = new Rectangle(x,
                table.getRowHeight()*(r),
                width,
                table.getRowHeight());
        table.scrollRectToVisible(rect);
        */
        
        try {
            highlightSearch(r, c, i);
        } catch (BadLocationException e) {
            log.warn("Bad Location", e);
        }
    }
    
    private void highlightSearch(int r, int c, int i) throws BadLocationException {
    	 if(table.editCellAt(r, c)) {
    	    Rectangle rect = editorField.getUI().modelToView(editorField, i);
    	    rect.y = table.getRowHeight()*r;
    	    rect.width = editorField.getUI().modelToView(editorField, i+searchString.length()).x - rect.x;
    	    table.scrollRectToVisible(rect);
         	Highlighter highlighter = editorField.getHighlighter();
         	highlighter.removeAllHighlights();
         	try {
         	    highlighter.addHighlight(i, i+searchString.length(), painter);
 			} catch (BadLocationException e) {
 				e.printStackTrace();
 			}
         } else {
             log.debug("COULDN'T EDIT!");
         }
    }

    private int cellContains(int r, int c, int i, boolean reverse) {
    	String row = model.getRow(r);
    	int index = reverse ? 
    			row.toLowerCase().lastIndexOf(searchString, i) :
    				row.toLowerCase().indexOf(searchString, i);
    			if(index >= 0) {
    				return index;
    			}
    			return -1;
    }

}
