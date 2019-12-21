package com.kaaphi.logviewer.ui;

import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import org.apache.log4j.Logger;
import com.kaaphi.logviewer.Bookmark;

public class BookmarkDialog extends JDialog {
  private static final Logger log = Logger.getLogger(BookmarkDialog.class);

  private JList<Bookmark> bookmarks;
  private DefaultListModel<Bookmark> bookmarksModel;
  private LogFileViewer viewer;

  public BookmarkDialog(LogFileViewer viewer, Frame parent) {
    super(parent, "Bookmarks");
    this.viewer = viewer;
    bookmarksModel = new DefaultListModel<>();
    bookmarks = new JList<>(bookmarksModel);

    bookmarks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    bookmarks.addListSelectionListener(e -> {
      if(!e.getValueIsAdjusting()) {
        goToSelectedBookmark();
      }
    });
    bookmarks.addMouseListener(new MouseAdapter() {		
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() > 1) {
          goToSelectedBookmark();
        }
      }
    });
    bookmarks.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        Bookmark b = bookmarks.getSelectedValue();
        if(b != null && e.getKeyCode() == KeyEvent.VK_DELETE) {
          viewer.removeBookmark(b);
        }
      }
    });

    getContentPane().add(new JScrollPane(bookmarks));
    pack();
  }

  private void goToSelectedBookmark() {
    Bookmark b = bookmarks.getSelectedValue();
    if(b != null) {
      viewer.scrollToLine(b.getLine().getLineNumber());
    }
  }

  public void addBookmark(Bookmark bookmark) {
    bookmarksModel.addElement(bookmark);
  }

  public void deleteBookmark(Bookmark bookmark) {
    bookmarksModel.removeElement(bookmark);
  }
}
