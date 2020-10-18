package com.kaaphi.logviewer.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import com.kaaphi.logviewer.FileUtil;

public class OrderFilesPanel extends JPanel {
  private OrderableListModel<File> model;

  public OrderFilesPanel(List<File> files) {
    super(new BorderLayout());

    model = new OrderableListModel<File>(files);
    model.sort(FileUtil.getLastModifiedComparator());
    final JList<File> fileList = new JList<>(model);
    fileList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    final JButton up = new JButton("\u2191");
    final JButton down = new JButton("\u2193");

    final JPopupMenu menu = new JPopupMenu();

    menu.add(new AbstractAction("Order by Modified") {
      @Override
      public void actionPerformed(ActionEvent e) {
        model.sort(FileUtil.getLastModifiedComparator());
      }
    });

    menu.add(new AbstractAction("Order by Name") {
      @Override
      public void actionPerformed(ActionEvent e) {
        model.sort(FileUtil.getFilePartComparator());
      }
    });

    menu.add(new AbstractAction("Reverse") {
      @Override
      public void actionPerformed(ActionEvent e) {
        model.reverse();
      }
    });

    fileList.addMouseListener(new PopupListener(menu));

    up.setEnabled(false);
    down.setEnabled(false);

    up.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int min = fileList.getMinSelectionIndex();
        int max = fileList.getMaxSelectionIndex();
        if(model.moveUp(min, max)) {
          fileList.getSelectionModel().setSelectionInterval(min-1,max-1);
        }
      }
    });

    down.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int min = fileList.getMinSelectionIndex();
        int max = fileList.getMaxSelectionIndex();
        if(model.moveDown(min, max)) {
          fileList.getSelectionModel().setSelectionInterval(min+1,max+1);
        }
      }
    });

    fileList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if(!e.getValueIsAdjusting()) {
          int min = fileList.getMinSelectionIndex();
          int max = fileList.getMaxSelectionIndex();

          if(min < 0) {
            up.setEnabled(false);
            down.setEnabled(false);
          } else {
            up.setEnabled(min > 0);
            down.setEnabled(max < (model.getSize()-1));
          }
        }
      }
    });

    JPanel buttons = new JPanel(new GridLayout(2, 1));
    buttons.add(up);
    buttons.add(down);

    add(new JScrollPane(fileList), BorderLayout.CENTER);
    add(buttons, BorderLayout.EAST);
  }

  private class PopupListener extends MouseAdapter {
    private JPopupMenu menu;

    public PopupListener(JPopupMenu menu) {
      this.menu = menu;
    }

    @Override
    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        menu.show(e.getComponent(),
            e.getX(), e.getY());
      }
    }
  }

  public static List<File> showOrderFilesDialog(Frame owner, List<File> files) {
    OrderFilesPanel panel = new OrderFilesPanel(files);

    int option = JOptionPane.showOptionDialog(
        owner, new Object[] {panel}, "Order Files",  
        JOptionPane.OK_CANCEL_OPTION, 
        JOptionPane.PLAIN_MESSAGE, 
        null, 
        null, null);

    if(option == JOptionPane.OK_OPTION) {
      return panel.model.getItems();
    } else {
      return null;
    }
  }

  public static void main(String[] args) {
    System.out.println(showOrderFilesDialog(null, Arrays.asList(new File(".").listFiles())));
  }

  private static class OrderableListModel<T> extends AbstractListModel<T> {
    private List<T> items;

    public OrderableListModel(List<T> items) {
      this.items = new ArrayList<T>(items);
    }

    @Override
    public T getElementAt(int i) {
      return items.get(i);
    }

    @Override
    public int getSize() {
      return items.size();
    }

    public boolean moveUp(int min, int max) {
      if(min <  1) {
        return false;
      }

      T o = items.remove(min - 1);
      items.add(max, o);

      fireContentsChanged(this, min-1, max);
      return true;
    }

    public boolean  moveDown(int min, int max) {
      if(max > items.size()-2) {
        return false;
      }

      T o = items.remove(max+1);
      items.add(min, o);

      fireContentsChanged(this, min, max+1);
      return true;
    }

    public void sort(Comparator<T> c) {
      Collections.sort(items, c);
      fireContentsChanged(this, 0, items.size()-1);
    }

    public void reverse() {
      Collections.reverse(items);
      fireContentsChanged(this, 0, items.size()-1);
    }

    public List<T> getItems() {
      return items;
    }
  }
}
