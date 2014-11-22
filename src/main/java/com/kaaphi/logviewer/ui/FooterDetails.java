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
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.kaaphi.logviewer.FilterListener;


public class FooterDetails extends JPanel implements TableModelListener,FilterListener,ListSelectionListener {
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
	
    public FooterDetails() {
    	layout = new CardLayout();
    	setLayout(layout);
        label = new JLabel();
        label.setHorizontalAlignment(JLabel.CENTER);
        filterProgress = new JProgressBar();
        cancelFilterButton = new JButton("Cancel");
        cancelFilterButton.addActionListener(new ActionListener() {
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
    }
    
    
    @Override
    public void tableChanged(TableModelEvent e) {
        LogFileTableModel model = (LogFileTableModel) e.getSource();
        
        rowCount = model.getRowCount();
        totalRows = model.getUnfilteredRowCount();
        updateLabel();
    }


	@Override
	public void filteringStarted() {
		continueFiltering = true;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				layout.show(FooterDetails.this, PROGRESS_NAME);
			}
		});
	}

	@Override
	public void filteringEnded() {
		continueFiltering = true;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				layout.show(FooterDetails.this, LABEL_NAME);
			}
		});
	}

	@Override
	public boolean filterUpdate(final int linesProcessed, final int totalLines) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				filterProgress.setMinimum(0);
				filterProgress.setMaximum(totalLines);
				filterProgress.setValue(linesProcessed);
			}
		});
		return continueFiltering;
	}


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
	
	private void updateLabel() {
		if(selectionSize > 0) {
			label.setText(String.format("%d/%d (%d)", rowCount, totalRows, selectionSize));
		} else {
			label.setText(String.format("%d/%d", rowCount, totalRows));
		}
	}

}
