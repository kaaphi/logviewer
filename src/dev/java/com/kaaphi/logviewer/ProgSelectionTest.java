package com.kaaphi.logviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

public class ProgSelectionTest extends JPanel {
	public ProgSelectionTest() {
		super(new BorderLayout());
		
		final JTextField field = new JTextField(40);
		field.setText("one two three four five six seven eight nine ten");
		JTextField button = new JTextField("Select");
		button.addActionListener(new ActionListener() {
			int start = 1;
			Object oldHighlight;
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//field.requestFocus();
				Highlighter highlight = field.getHighlighter();
				try {
					if(oldHighlight != null)
						highlight.removeHighlight(oldHighlight);
					oldHighlight = highlight.addHighlight(start++, start+2, new DefaultHighlighter.DefaultHighlightPainter(Color.BLUE));
				} catch (BadLocationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		
		add(field, BorderLayout.NORTH);
		add(button, BorderLayout.SOUTH);
	}
	
	public static void main(String[] args) throws Exception  {
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				JFrame frame = new JFrame("Log Viewer");
				frame.getContentPane().add(new ProgSelectionTest());				
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frame.pack();
				frame.setVisible(true);
			}
		});
	}
	
	
}
