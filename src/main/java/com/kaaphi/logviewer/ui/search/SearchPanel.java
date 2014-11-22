package com.kaaphi.logviewer.ui.search;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter.HighlightPainter;

import org.apache.log4j.Logger;

public class SearchPanel extends JTextField {
	private static final Logger log = Logger.getLogger(SearchPanel.class);
	
    private boolean reverse = false;
    private SearchSession search;
    private Set<SearchPanelListener> listeners;
    private boolean searchCanceled = false;
    private HighlightPainter searchFailedPainter = new DefaultHighlightPainter(Color.PINK);
    
    public SearchPanel(final SearchSession search) {
        this.search = search;
        
        this.listeners = new HashSet<SearchPanelListener>();
        
        this.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		search();
            }
        });
        
        this.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				updateSearch(e);
			}
			
			public void insertUpdate(DocumentEvent e) {
				updateSearch(e);
			}
			
			public void changedUpdate(DocumentEvent e) {
				updateSearch(e);
			}
			
			private void updateSearch(DocumentEvent e) {
				Document doc = e.getDocument();
				try {
					String text = doc.getText(0, doc.getLength());
					search.updateSearchString(text);
					search();
				} catch (BadLocationException e1) {
					log.warn("Shouldn't happen.", e1);
				}
			}
		});
        
        addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                cancelSearch();
            }
        });
        
        addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if(KeyEvent.VK_ESCAPE == e.getKeyCode()) {
                    cancelSearch();
                }
            }
        });
    }
    
    public void addSearchPanelListener(SearchPanelListener l) {
        listeners.add(l);
    }
    public void removeSearchPanelListener(SearchPanelListener l) {
        listeners.remove(l);
    }
    
    public void setDirection(boolean reverse) {
    	this.reverse = reverse;
    }
    
    public boolean isReverse() { return reverse; }
    
    public void search() {
    	if(reverse) {
    		if(!search.findPrevious()) {
    			flash();
    		}
    	} else {
    		if(!search.findNext()) {
    			flash();
    		}
    	}
    }
    
    private void flash() {
    	try {
    		final Object highlight = 
    			getHighlighter().addHighlight(0, getText().length(), searchFailedPainter);

    		new Thread() {
    			public void run() {
    				try {Thread.sleep(500);} catch (InterruptedException e) {}
    				SwingUtilities.invokeLater(new Runnable() {
    					public void run() {
    						getHighlighter().removeHighlight(highlight);
    					}
    				});
    			}
    		}.start();
    	} catch (BadLocationException e1) {
    		e1.printStackTrace();
    	}
    }
    
    private void cancelSearch() {
        if(!searchCanceled) {
            searchCanceled = true;
            for(SearchPanelListener l : listeners) {
                l.searchCanceled();
            }
        }
    }
    
    public static interface SearchPanelListener {
        public void searchCanceled();
    }
}
