package com.kaaphi.logviewer.ui.filter;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import com.kaaphi.logviewer.LogFile.Filter;

public class FiltersPanel extends AbstractFilterEditor implements ActionListener {
  private RegexFilterEditor filterEditor;

  public FiltersPanel() {
    setLayout(new BorderLayout());
    filterEditor = new RegexFilterEditor();

    JButton apply = new JButton("Apply");
    apply.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        fireActionPerformed();
      }
    });

    JButton reset = new JButton("Reset");
    reset.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        resetFilter();
        fireActionPerformed();
      }
    });

    JPanel buttons  = new JPanel(new GridLayout(1,2));
    buttons.add(apply);
    buttons.add(reset);

    add(filterEditor, BorderLayout.CENTER);
    add(buttons, BorderLayout.EAST);

    filterEditor.addActionListener(this);
  }



  @Override
  public void actionPerformed(ActionEvent e) {
    fireActionPerformed();
  }



  @Override
  public Filter getFilter() {
    return filterEditor.getFilter();
  }



  @Override
  public boolean isFilterValid() {
    return filterEditor.isFilterValid();
  }



  @Override
  public void resetFilter() {
    filterEditor.resetFilter();
  }


}
