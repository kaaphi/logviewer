package com.kaaphi.logviewer.ui;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import com.kaaphi.logviewer.ui.LogDocument.LogLineElement;
import com.kaaphi.logviewer.ui.util.MenuUtil.MenuAction;

public class FullLineViewer {
  private final MouseListener mouseListener = new MyMouseListener();


  public void addToTextArea(JTextArea area) {
    area.addMouseListener(mouseListener);
  }

  private void showLine(int lineNumber, String line) {
    JFrame f = new JFrame("Full Line - " + lineNumber);

    JTextArea area = new JTextArea(line);
    area.setLineWrap(true);
    area.setEditable(false);

    f.getContentPane().add(new JScrollPane(area));
    f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    f.setSize(1024, 768);
    f.setVisible(true);
  }

  private void doPopUp(MouseEvent e) {
    JTextArea textArea = (JTextArea) e.getSource();

    int location = textArea.viewToModel(e.getPoint());
    //log.trace("Location: " + location);
    LogLineElement element = ((LogLineElement)((LogDocument)textArea.getDocument()).getParagraphElement(location));
    //log.trace("Element: " + element);
    if(element != null) {
      JPopupMenu menu = new JPopupMenu();
      menu.add(new MenuAction("Show Full Line") {						
        @Override
        public void actionPerformed(ActionEvent arg0) {
          showLine(element.getLine().getLineNumber(), element.getLine().getRawLine());
        }
      });
      menu.show(textArea, e.getX(), e.getY());
    }
  }

  private class MyMouseListener extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      if(e.isPopupTrigger()) {
        doPopUp(e);					
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if(e.isPopupTrigger()) {
        doPopUp(e);					
      }
    }


  }

}
