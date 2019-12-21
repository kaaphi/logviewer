package com.kaaphi.logviewer.ui.util;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class SimpleProgress extends JDialog {
  public SimpleProgress(JFrame owner) {
    super(owner, true);

    JProgressBar bar = new JProgressBar();
    bar.setIndeterminate(true);

    JPanel panel = new JPanel();
    panel.add(new JLabel("Loading..."));
    panel.add(bar);

    getContentPane().add(panel);
    pack();
  }

  public static void main(String[] args) {
    new SimpleProgress(null).setVisible(true);
  }
}
