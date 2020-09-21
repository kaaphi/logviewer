package com.kaaphi.logviewer.ui;

import com.kaaphi.logviewer.Bookmark;
import com.kaaphi.logviewer.FileUtil;
import com.kaaphi.logviewer.LogFile;
import com.kaaphi.logviewer.LogLine;
import com.kaaphi.logviewer.ui.LogDocument.LogLineElement;
import com.kaaphi.logviewer.ui.filter.FiltersPanel;
import com.kaaphi.logviewer.ui.search.TypeAheadSearchPanel;
import com.kaaphi.logviewer.ui.search.TypeAheadSearchPanel.SearchPanelListener;
import com.kaaphi.logviewer.ui.search.TypeAheadSearchSession;
import com.kaaphi.logviewer.ui.util.MenuUtil;
import com.kaaphi.logviewer.ui.util.MenuUtil.MenuAction;
import com.kaaphi.logviewer.ui.util.MenuUtil.MenuEntry;
import com.kaaphi.logviewer.util.DocumentAppender;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import org.apache.log4j.Logger;
import say.swing.JFontChooser;

public class LogFileViewer extends JPanel {
  private static final Logger log = Logger.getLogger(LogFileViewer.class);

  private static final Color[] STYLE_TOKEN_COLORS = {
      new Color(174,194,208), //pale blue
      new Color(199,215,207), //pale green
      new Color(235,228,194), //pale yellow
      new Color(213,202,208), //pale purple
      new Color(223,205,187) //pale orange
  };

  private LogDocument doc;
  private JTextArea textArea;
  private JScrollPane scroller;
  private JFrame frame;
  private LogLineNumbers rowHeader;
  private TypeAheadSearchPanel searchPanel;
  private TypeAheadSearchSession searchSession;
  private JPanel footer;
  private FooterDetails footerDetails;
  private LogViewerConfiguration config;
  private List<File> loadedFiles;
  private FiltersPanel filters;
  private BookmarkDialog bookmarkDialog;
  private SequenceHighlighter sequenceHighlighter;
  private int mark;

  public LogFileViewer() throws Exception  {
    super(new BorderLayout());

    config = LogViewerConfiguration.getInstance();
    config.loadConfig();

    doc = new LogDocument();
    footerDetails = new FooterDetails(doc);
    textArea = new JTextArea(doc);

    textArea.setEditable(false);
    textArea.setCaret(new DefaultCaret() {
      @Override
      public void focusGained(FocusEvent e) {
        setVisible(true);
        setSelectionVisible(true);
      }

      @Override
      public void focusLost(FocusEvent e) {
        setVisible(false);
      }

    });

    textArea.addCaretListener(footerDetails);

    new SelectionInterpreter(textArea);

    new FullLineViewer().addToTextArea(textArea);

    scroller = new JScrollPane(textArea);
    scroller.setRowHeaderView(rowHeader = new LogLineNumbers(textArea, scroller));
    doc.addDocumentListener(rowHeader);

    JPanel corner = new JPanel();
    corner.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    scroller.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, corner);

    filters = new FiltersPanel();

    filters.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doFilter(true);
      }
    });

    footer = new JPanel(new BorderLayout());
    footer.add(footerDetails, BorderLayout.SOUTH);

    searchSession = new TypeAheadSearchSession(textArea, doc);


    add(filters, BorderLayout.NORTH);
    add(scroller, BorderLayout.CENTER);
    add(footer, BorderLayout.SOUTH);


    frame = new JFrame("Log Viewer");
    frame.setIconImage(loadIcon());

    buildMenuBar(frame);
    frame.getContentPane().add(this);

    TransferHandler th = createTransferHandler();
    frame.setTransferHandler(th);
    //textArea.setTransferHandler(th);

    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    bookmarkDialog = new BookmarkDialog(this, this.frame);

    sequenceHighlighter = new SequenceHighlighter(scroller.getViewport(), textArea);
    scroller.getViewport().addChangeListener(sequenceHighlighter);

    applyConfig();
  }

  private static Image loadIcon() {
    InputStream in = null;
    try {
      in = ClassLoader.getSystemResourceAsStream("icon.png");
      int len = in.available();
      byte[] bytes = new byte[len];
      in.read(bytes, 0, len);
      in.close();
      return new ImageIcon(bytes).getImage();
    } catch (Throwable th) {
      log.error("Failed to load icon.", th);
      return null;
    } finally {
      try {
        if(in != null) in.close();
      } catch (IOException e) {
        log.error("Failed to close icon input stream.", e);
      }
    }
  }

  public void displayFrame() {
    frame.pack();
    Dimension d = frame.getPreferredSize();
    d.width = 800;
    d.height = 600;
    frame.setPreferredSize(d);
    frame.setSize(d);
    frame.setVisible(true);
  }

  private void setLogFile(LogFile file) {
    file.setListener(footerDetails);
    doc.setLogFile(file);
    doFilter(false);
    try {
      //setTextAreaWidth();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    rowHeader.setPreferredWidth();
  }

  private void doFilter(final boolean scrollToRow) {
    int currentIdx = textArea.getDocument().getDefaultRootElement().getElementIndex(textArea.getCaret().getDot());

    final int currentYPos;
    if(scrollToRow) {
      int yPos = 0;
      try {
        Rectangle viewRect = scroller.getViewport().getViewRect();
        Rectangle currentRect = textArea.modelToView(textArea.getCaret().getDot());

        yPos = currentRect.y - viewRect.y;
      } catch (BadLocationException e) {
        log.error("Bad location!", e);
      }
      currentYPos = yPos;
    } else {
      currentYPos = 0;
    }

    final LogLine currentLine = currentIdx < 0 ? null : ((LogLineElement)textArea.getDocument().getDefaultRootElement().getElement(currentIdx)).getLine();
    new Thread("FilterThread") {
      @Override
      public void run() {
        textArea.getCaret().setDot(0);
        if(scrollToRow) {
          doc.applyFilter(filters.getFilter());
          textArea.invalidate();

          int offset = currentLine == null ? 0 : currentLine.getStartIndex();
          if(offset < 0) {
            offset = -offset;
          }

          scrollToOffset(offset, currentYPos);
          //textArea.getCaret().setDot(offset);
        } else {
          doc.applyFilter(filters.getFilter());
        }

      }
    }.start();
  }

  private static void ensureDispatchThread(Runnable r) {
    if(SwingUtilities.isEventDispatchThread()) {
      r.run();
    } else {
      SwingUtilities.invokeLater(r);
    }
  }

  private void setLoading(final boolean isLoading) {
    log.debug("Start set loading " + isLoading);
    ensureDispatchThread(() -> {
      log.debug("Run set loading " + isLoading);
      if(isLoading) {
        frame.setTitle("LogViewer - loading...");
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      } else {
        frame.setTitle("LogViewer - " + concatFileNames(loadedFiles));
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
    });
  }

  private void setLogFiles(File ... files) throws IOException {
    setLogFiles(Arrays.asList(files));
  }

  private void setLogFiles(List<File> files) throws IOException {
    try {
      if(files.size() > 1) {
        files = OrderFilesPanel.showOrderFilesDialog(frame, files);
        //for some reason have to set again here, otherwise mouse cursor
        //switches back
        if(files != null) {
          setLoading(true);
          long start = System.currentTimeMillis();
          LogFile logFile = new LogFile(FileUtil.readLines(files, Charset.defaultCharset()));
          setLogFile(logFile);
          loadedFiles = files;
          long total = System.currentTimeMillis() - start;
          log.debug("Total seconds loading = " + (total/1000.0));
          setLoading(false);
        }
      } else if (files.size() == 1) {
        File file = files.get(0);
        setLoading(true);
        long start = System.currentTimeMillis();
        LogFile logFile = new LogFile(FileUtil.readLines(file, Charset.defaultCharset()));
        setLogFile(logFile);
        loadedFiles = files;
        long total = System.currentTimeMillis() - start;
        log.debug("Total seconds loading = " + (total/1000.0));
        setLoading(false);
      }
    } finally {
      setLoading(false);
    }
  }

  static String concatFileNames(List<File> files) {
    Iterator<File> it = files.iterator();
    File first = it.next();

    if(!it.hasNext()) {
      return first.toString();
    }

    /*
    Determine whether the files are all in the same directory and determine the string index up to
    which all the file names are the same.
     */
    File parentDir = first.getParentFile();
    String firstFileName = first.getName();
    int commonPrefixIdx = first.getName().length();
    while(it.hasNext() && (parentDir != null || commonPrefixIdx > 0)) {
      File file = it.next();
      if(parentDir != null && !parentDir.equals(file.getParentFile())) {
        parentDir = null;
      }

      String fileName = file.getName();
      commonPrefixIdx = Math.min(commonPrefixIdx, fileName.length());
      for(int i = 0; i <= commonPrefixIdx; i++) {
        if(firstFileName.charAt(i) != fileName.charAt(i)) {
          commonPrefixIdx = i;
          break;
        }
      }
    }

    /*
    Build the string to display for the file names
     */
    String fileString;
    if(parentDir != null && commonPrefixIdx > 3) {
      final int idx = commonPrefixIdx;
      fileString = String.format("%s (%s)", firstFileName.substring(0, commonPrefixIdx), files.stream()
          .map(File::getName)
          .map(s -> s.substring(idx))
          .collect(Collectors.joining(", "))
      );
    } else {
      fileString = files.stream().map(File::getName).collect(Collectors.joining(", "));
    }


    if(parentDir != null) {
      //build the string to display for the parent directory
      String parentDirString = parentDir.toString();
      if(parentDirString.length() > 50) {
        Path path = parentDir.toPath();
        for(int i = 0; i < path.getNameCount(); i++) {
          parentDirString =  path.subpath(i, path.getNameCount()).toString();
          if(parentDirString.length() < 50) {
            break;
          }
        }
      }

      return String.format("%s : %s", parentDirString, fileString);
    } else {
      return fileString;
    }
  }


  private TransferHandler createTransferHandler() {
    try {
      final DataFlavor uriListFlavor = new DataFlavor("text/uri-list; class=java.lang.String; charset=Unicode");


      return new TransferHandler() {


        @Override
        public boolean canImport(TransferSupport support) {
          log.debug(String.format("canImport: %d", support.getSourceDropActions()));
          for(DataFlavor flavor : support.getDataFlavors()) {
            log.trace(flavor);
          }
          try {
            return 
                support.isDataFlavorSupported(uriListFlavor) 
                || support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
          } catch (Throwable th) {
            log.error(th);
            return false;
          } 
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean importData(TransferSupport support) {
          log.debug("importData");
          if (!canImport(support)) {
            return false;
          }

          Transferable t = support.getTransferable();


          try {
            List<File> files = null;
            if(support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
              files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
            } else if(support.isDataFlavorSupported(uriListFlavor)) {
              String uriList = (String)t.getTransferData(uriListFlavor);
              files = new LinkedList<File>();
              for(String uri : uriList.split("\r\n")) {
                files.add(new File(new URI(uri)));
              }
            }

            setLogFiles(files);
          } catch (UnsupportedFlavorException e) {
            return false;
          } catch (Throwable e) {
            return false;
          }

          return true;
        }
      };
    } catch (ClassNotFoundException e1) {
      throw new Error(e1);
    }
  }

  private void buildMenuBar(final JFrame frame) {
    final JFileChooser chooser = new JFileChooser(new File("."));
    chooser.setMultiSelectionEnabled(true);

    MenuUtil menu = new MenuUtil();

    menu.addMenu("&File", 
        new MenuAction("&Open...", "O") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        if(chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
          List<File> files = Arrays.asList(chooser.getSelectedFiles());
          try {

            setLogFiles(files);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    },
        new MenuAction("&Reload...", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)) {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        if(loadedFiles != null) {
          try {
            setLogFiles(loadedFiles);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    },
        new MenuAction("&Delete Files", KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK)) {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        if(loadedFiles != null) {
          int option = JOptionPane.showConfirmDialog(LogFileViewer.this, "Do you want to delete the open log files?", "Delete files?", JOptionPane.YES_NO_OPTION);

          if(option == JOptionPane.YES_OPTION) {
            boolean couldNotDeleteAll = false;
            for(File f : loadedFiles) {
              if(f.exists() && !f.delete()) {
                couldNotDeleteAll = true;
              }
            }

            if(couldNotDeleteAll) {
              JOptionPane.showMessageDialog(LogFileViewer.this, "Could not delete all files!", "Could Not Delete", JOptionPane.WARNING_MESSAGE);
            }
          }
        }
      }
    }
        );


    Stream.Builder<MenuEntry> styleEntries = Stream.builder();
    IntStream.range(0, STYLE_TOKEN_COLORS.length)
    .mapToObj(i -> new MenuAction("Toggle Style Token " + (i+1), Integer.toString(i+1)) {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        sequenceHighlighter.toggleSequence(textArea.getSelectedText(), STYLE_TOKEN_COLORS[i]);
      }
    }).forEach(styleEntries::add);
    IntStream.range(0, STYLE_TOKEN_COLORS.length)
    .mapToObj(i -> new MenuAction("Clear Style Token " + (i+1)) {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        sequenceHighlighter.clearSequences(STYLE_TOKEN_COLORS[i]);
      }
    }).forEach(styleEntries::add);
    styleEntries.add(new MenuAction("Clear All Style Tokens") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        sequenceHighlighter.clearAllSequences();
      }
    });		

    menu.addMenu("&View",
        new MenuAction("&Font...") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        JFontChooser chooser = new JFontChooser();
        chooser.setSelectedFont(textArea.getFont());
        int result = chooser.showDialog(LogFileViewer.this);
        if(result == JFontChooser.OK_OPTION) {
          config.font.set(chooser.getSelectedFont());
          applyConfig();
          try {
            config.storeConfig();
          } catch (Exception e) {
            log.error("Failed to save config.", e);
          }
        }
      }
    },

        new MenuAction("Search &Color...") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        Color newColor = JColorChooser.showDialog(LogFileViewer.this, "Choose Search Highlight Color", config.searchHighlight.get());
        if(newColor != null) {
          config.searchHighlight.set(newColor);
          applyConfig();
          try {
            config.storeConfig();
          } catch (Exception e) {
            log.error("Failed to save config.", e);
          }
        }				
      }
    },

        MenuUtil.createMenu("Style &Token", styleEntries.build().toArray(MenuEntry[]::new)),
        
        new MenuAction("Set &Mark", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK)) {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        mark = textArea.getCaretPosition();
      }
    },
        
        new MenuAction("Select &Region", KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        int startOffset = Math.min(mark, textArea.getCaretPosition());
        int endOffset = Math.max(mark, textArea.getCaretPosition());
        
        startOffset = doc.getDefaultRootElement().getElement(doc.getLogFile().getLineIndex(startOffset)).getStartOffset();
        endOffset = doc.getDefaultRootElement().getElement(doc.getLogFile().getLineIndex(endOffset)).getEndOffset();
        
        textArea.select(startOffset, endOffset);
      }
    }
        );

    menu.addMenu("&Search", 
        new MenuAction("&Search", "S") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        if(verifySearchPanel()) {
          if(searchPanel.isReverse()) {
            searchPanel.setDirection(false);
          }
          searchPanel.search();
        }
      }
    },
        new MenuAction("&Reverse Search", "R") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        if(verifySearchPanel()) {
          if(!searchPanel.isReverse()) {
            searchPanel.setDirection(true);
          }
          searchPanel.search();
        }
      }
    },

        new MenuAction("Go to &Line", "L") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        scrollToLine();
      }
    },

        new MenuAction("Show Book&marks", "M") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        bookmarkDialog.setVisible(true);
      }
    },

        new MenuAction("&Bookmark Line", "B") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        try {
          int viewLine = textArea.getLineOfOffset(textArea.getCaretPosition());
          LogLine logLine = doc.getLogFile().getLine(viewLine);
          String bookmarkLabel = JOptionPane.showInputDialog(LogFileViewer.this, "Enter Bookmark Name:");
          if(bookmarkLabel != null) {
            addBookmark(new Bookmark(bookmarkLabel, logLine));
          }
        } catch (Throwable th) {
          log.error("Failed to add bookmark!", th);
        }
      }
    }
  

        );

    menu.addMenu("&Help", 
        new MenuAction("&Error Log") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        Document doc = ((DocumentAppender)Logger.getRootLogger().getAppender("doc")).getDocument();


        JFrame f = new JFrame("Log");

        JTextArea area = new JTextArea(doc);
        area.setEditable(false);
        area.setCaretPosition(doc.getLength());
        DefaultCaret caret = (DefaultCaret)area.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        f.getContentPane().add(new JScrollPane(area));
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setSize(1024, 768);
        f.setVisible(true);

      }
    });

    menu.installOn(frame);
  }

  private void applyConfig() {
    textArea.setFont(config.font.get());
    filters.setFilterFont(config.font.get());
    searchSession.setSearchHighlightColor(config.searchHighlight.get());
  }

  public void addBookmark(Bookmark bookmark) {
    bookmarkDialog.addBookmark(bookmark);
    doc.addBookmark(bookmark);
    rowHeader.repaint();
  }

  public void removeBookmark(Bookmark bookmark) {
    bookmarkDialog.deleteBookmark(bookmark);;
    doc.removeBookmark(bookmark);
    rowHeader.repaint();
  }


  private void scrollToLine() {
    String rowString = JOptionPane.showInputDialog(this, "Enter Line:");
    if(rowString != null) {
      scrollToLine(Integer.parseInt(rowString));
    }
  }

  public void scrollToLine(int lineNumber) {
    int row = doc.getLogFile().getFilteredRow(lineNumber);

    if(row < 0) {
      row = -row;
    }
    row--;

    LogLine line = doc.getLogFile().getLine(row);
    log.debug("line: " + line + " startOffset: " + line.getStartIndex());
    int offset = line.getStartIndex();
    scrollToOffset(offset, 0);
  }

  private void scrollToOffset(int offset, int yPos) {
    try {
      Rectangle rect = textArea.modelToView(offset);

      JViewport port = (JViewport) textArea.getParent();
      log.debug("Viewable rect: "+rect+", view size: "+scroller.getViewport().getExtentSize());

      rect.y -= yPos;
      rect.height = port.getExtentSize().height;
      log.debug("Modified rect: "+rect);

      textArea.scrollRectToVisible(rect);
      textArea.getCaret().setDot(offset);
      textArea.requestFocus();
    } catch (BadLocationException e) {
      log.error("Bad location!", e);
    }
  }

  private boolean verifySearchPanel() {
    if(searchPanel == null) {
      searchPanel = new TypeAheadSearchPanel(searchSession);

      searchSession.setStartingOffset(textArea.getCaretPosition());

      searchPanel.addSearchPanelListener(new SearchPanelListener() {
        @Override
        public void searchCanceled() {
          footer.remove(searchPanel);
          searchPanel = null;
          searchSession.reset(null);
          footer.revalidate();
        }
      });

      footer.add(searchPanel, BorderLayout.NORTH);
      footer.validate();
      validate();
      searchPanel.requestFocus();
      return false;
    } else {
      return true;
    }
  }


  private static class SelectionInterpreter implements MouseListener, FocusListener, KeyListener {
    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    private SimpleDateFormat local = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    {
      utc.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
    }

    private Popup popup;
    private JTextArea owner;
    private JTextField tip;
    private String currentSelection;

    public SelectionInterpreter(JTextArea field) {
      owner = field;
      tip = new JTextField();
      tip.setEditable(false);

      field.addMouseListener(this);
      field.addFocusListener(this);
      field.addKeyListener(this);
      tip.addFocusListener(this);
    }

    private void showPopup(String text) {
      if(popup != null) {
        popup.hide();
        popup = null;
      }
      if(text != null) {
        try {
          Rectangle r = owner.modelToView(owner.getSelectionStart());
          Point p = new Point(r.x+10, r.y-10);
          SwingUtilities.convertPointToScreen(p, owner);
          tip.setText(text);
          popup = PopupFactory.getSharedInstance().getPopup(owner, tip, p.x, p.y);
          popup.show();
        } catch (BadLocationException e) {
          log.error("Bad location!", e);
        }
      }
    }

    private void hidePopup() {
      if(popup != null) {
        popup.hide();
        popup = null;
      }
    }

    private String getTimestampString() {
      currentSelection = owner.getSelectedText();
      if(NUMBER.matcher(currentSelection).matches()) {
        long stamp = Long.parseLong(currentSelection);
        if(stamp < 1000000000000l) {
          stamp *= 1000l;
        }
        return String.format("%s (%s)", utc.format(new Date(stamp)), local.format(new Date(stamp)));
      }
      return null;
    }

    @Override
    public void focusGained(FocusEvent e) { }

    @Override
    public void focusLost(FocusEvent e) {
      if((e.getComponent() == owner && e.getOppositeComponent() != tip) || (e.getComponent() == tip && e.getOppositeComponent() != owner)) {
        hidePopup();
      }
    }

    @Override
    public void keyTyped(KeyEvent e) {
      //no op
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_T && (e.getModifiersEx() | InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK) {
        String timestamp = getTimestampString();
        if(timestamp != null) {
          showPopup(timestamp);
        }
      }
      else if(e.getKeyCode() == KeyEvent.VK_C) {
        owner.copy();
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      //no op
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      //no op
    }

    @Override
    public void mousePressed(MouseEvent e) {
      hidePopup();			
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      //no op
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      //no op
    }

    @Override
    public void mouseExited(MouseEvent e) {
      //no op
    }

  }


  public static void main(final String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler((thread, th) -> {
      log.error(String.format("Uncaught Exception on Thread %s: %s", thread, th), th);
    });

    UIManager.setLookAndFeel(
        UIManager.getSystemLookAndFeelClassName());

    class Holder {
      LogFileViewer viewer;
    }
    final Holder viewHolder = new Holder();
    SwingUtilities.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        try {
          LogFileViewer viewer = new LogFileViewer();
          viewHolder.viewer = viewer;

          viewer.displayFrame();

        } catch (Throwable th) {
          th.printStackTrace();
        }

      }
    });

    SwingUtilities.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        try {
          if(args.length > 0) {
            viewHolder.viewer.setLogFiles(new File(args[0]));
          }
        } catch (Throwable th) {
          th.printStackTrace();
        }
      }
    });
  }
}
