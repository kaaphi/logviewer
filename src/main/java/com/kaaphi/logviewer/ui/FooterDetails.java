package com.kaaphi.logviewer.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import com.kaaphi.logviewer.FilterListener;


public class FooterDetails extends JPanel implements DocumentListener,FilterListener,CaretListener {
  private static final String LABEL_NAME = "label";
  private static final String PROGRESS_NAME = "filter_progress";
  private JLabel label;
  private JProgressBar filterProgress;
  private JButton cancelFilterButton;
  private CardLayout layout;
  private boolean continueFiltering = true; 
  private int rowCount;
  private int totalRows;
  private int selectionSize;
  private LogDocument doc;

  public FooterDetails(LogDocument doc) {
    layout = new CardLayout();
    setLayout(layout);
    label = new JLabel();
    label.setHorizontalAlignment(JLabel.CENTER);
    filterProgress = new JProgressBar();
    cancelFilterButton = new JButton("Cancel");
    cancelFilterButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        continueFiltering = false;
      }
    });

    JPanel progressPanel = new JPanel(new BorderLayout());
    progressPanel.add(filterProgress, BorderLayout.CENTER);
    progressPanel.add(cancelFilterButton, BorderLayout.EAST);


    add(label, LABEL_NAME);
    add(progressPanel, PROGRESS_NAME);
    setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    layout.show(this, LABEL_NAME);

    this.doc = doc;
    doc.addDocumentListener(this);
  }

  @Override
  public void filteringStarted() {
    continueFiltering = true;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        layout.show(FooterDetails.this, PROGRESS_NAME);
      }
    });
  }

  @Override
  public void filteringEnded() {
    continueFiltering = true;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        layout.show(FooterDetails.this, LABEL_NAME);
      }
    });
  }

  @Override
  public boolean filterUpdate(final int linesProcessed, final int totalLines) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        filterProgress.setMinimum(0);
        filterProgress.setMaximum(totalLines);
        filterProgress.setValue(linesProcessed);
      }
    });
    return continueFiltering;
  }

  /*
	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(!e.getValueIsAdjusting()) {
			ListSelectionModel lsm = (ListSelectionModel)e.getSource();
			selectionSize = 0;
			int max = lsm.getMaxSelectionIndex();
			for(int i = lsm.getMinSelectionIndex(); i <= max; i++) {
				if(lsm.isSelectedIndex(i)) selectionSize++;
			}
			updateLabel();
		}
	}
   */

  private void updateLabel() {
    if(selectionSize > 0) {
      label.setText(String.format("%d/%d (%d)", rowCount, totalRows, selectionSize));
    } else {
      label.setText(String.format("%d/%d", rowCount, totalRows));
    }
  }


  @Override
  public void insertUpdate(DocumentEvent e) {
    change(e);
  }


  @Override
  public void removeUpdate(DocumentEvent e) {
    change(e);
  }

  private void change(DocumentEvent e) {
    LogDocument d =  (LogDocument)e.getDocument();

    rowCount = d.getLogFile().size();
    totalRows =d.getLogFile().unfilteredSize();
    updateLabel();
  }


  @Override
  public void changedUpdate(DocumentEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void caretUpdate(CaretEvent e) {
    if(e.getDot() != e.getMark()) {
      int idx1 = doc.getDefaultRootElement().getElementIndex(e.getDot());
      int idx2 = doc.getDefaultRootElement().getElementIndex(e.getMark());

      int startIdx, endIdx;
      int startOffset, endOffset;
      if(idx1 == idx2) {
        selectionSize = 0;
      } else { 
        if(idx1 < idx2) {
          startIdx = idx1;
          endIdx = idx2;
          startOffset = e.getDot();
          endOffset = e.getMark();							
        } else {
          startIdx = idx2;
          endIdx = idx1;
          startOffset = e.getMark();	
          endOffset =  e.getDot();
        }

        Element start = doc.getDefaultRootElement().getElement(startIdx);
        Element end = doc.getDefaultRootElement().getElement(endIdx);
        if(startOffset == start.getEndOffset()) {
          startIdx++;
        }
        if(endOffset == end.getStartOffset()) {
          endIdx --;
        }

        selectionSize = (endIdx - startIdx)+1;
      }
    } else {
      selectionSize = 0;
    }

    updateLabel();
  }

}
