package com.kaaphi.logviewer.ui.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.EventListenerList;

public class ActionSupport {
  private Object source;
  private EventListenerList listeners;

  public ActionSupport(Object source) {
    this.source = source;
    this.listeners = new EventListenerList();
  }

  public void addActionListener(ActionListener listener) {
    listeners.add(ActionListener.class, listener);
  }

  public void removeActionListener(ActionListener listener) {
    listeners.remove(ActionListener.class, listener);
  }

  public void fireActionPerformed() {
    ActionEvent event = new ActionEvent(source, ActionEvent.ACTION_PERFORMED, null);
    for(ActionListener l : listeners.getListeners(ActionListener.class)) {
      l.actionPerformed(event);
    }
  }
}
