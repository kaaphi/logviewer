package com.kaaphi.logviewer.ui.filter;

import java.awt.event.ActionListener;
import javax.swing.JPanel;
import com.kaaphi.logviewer.ui.util.ActionSupport;

public abstract class AbstractFilterEditor extends JPanel implements FilterEditor {
  private ActionSupport support;

  public AbstractFilterEditor() {
    support = new ActionSupport(this);
  }

  public final void addActionListener(ActionListener l) {
    support.addActionListener(l);
  }

  public final void removeActionListener(ActionListener l) {
    support.removeActionListener(l);
  }

  protected final void fireActionPerformed() {
    support.fireActionPerformed();
  }
}
