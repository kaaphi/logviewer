package com.kaaphi.logviewer.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;
import org.apache.log4j.Logger;
import com.kaaphi.logviewer.ui.LogDocument.LogLineElement;

public class LogLineNumbers extends JPanel implements DocumentListener, ComponentListener, MouseListener, MouseMotionListener {
  private static final Logger log = Logger.getLogger(LogFileViewer.class);

  private JTextComponent component;
  private HashMap<String, FontMetrics> fonts;


  private int lastDigits;
  private int lastCount;

  private JTextField tip;
  private Popup popup;

  public LogLineNumbers(JTextComponent component, JScrollPane pane) {
    this.component = component;
    component.addComponentListener(this);
    component.getDocument().addDocumentListener(this);
    setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), 
        BorderFactory.createEmptyBorder(0, 2, 0, 2)));

    setPreferredWidth();

    tip = new JTextField();
    tip.setEditable(false);

    addMouseListener(this);
    addMouseMotionListener(this);
  }

  /**
   *  Draw the line numbers
   */
  @Override
  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    try {
      log.trace(String.format("this: %s; component: %s", this.getSize(), component.getSize()));
      FontMetrics fontMetrics = component.getFontMetrics( component.getFont() );
      Insets insets = getInsets();
      int availableWidth = getSize().width - insets.left - insets.right;

      Rectangle clip = g.getClipBounds();
      log.trace(String.format("Clip=%s", clip));
      int rowStartOffset = component.viewToModel( new Point(0, clip.y) );
      int endOffset = component.viewToModel( new Point(0, clip.y + clip.height) );

      int startIdx = component.getDocument().getDefaultRootElement().getElementIndex(rowStartOffset);
      int endIdx = component.getDocument().getDefaultRootElement().getElementIndex(endOffset);

      log.trace(String.format("rowStartOffset=%d; endOffset=%d; startIdx=%d; endIdx=%d", 
          rowStartOffset, endOffset, startIdx, endIdx)); 

      if(startIdx < 0 || endIdx < 0) {
        return;
      }

      for(int i = startIdx; i <= endIdx; i++) {
        LogLineElement e = (LogLineElement) component.getDocument().getDefaultRootElement().getElement(i);
        int lineNumber = e.getLine().getLineNumber();
        String lineNumberString = Integer.toString(lineNumber);
        log.trace(String.format("i=%d; lineNumber=%s", i, lineNumberString)); 

        if(((LogDocument)component.getDocument()).isBookmarked(lineNumber)) {
          g.setColor(Color.RED.darker());
          g.setFont(getBookmarkFont());
        } else {
          g.setColor( getForeground() );
          g.setFont(getFont());
        }

        int stringWidth = getFontMetrics(g.getFont()).stringWidth( lineNumberString );
        int x = getOffsetX(availableWidth, stringWidth) + insets.left;
        int y = getOffsetY(e.getStartOffset(), fontMetrics);

        g.drawString(lineNumberString, x, y);

      }
    }

    catch(Exception e) {
      log.warn("Log Line Error!", e);
    }
  }

  /*
   *	Get the line number to be drawn. The empty string will be returned
   *  when a line of text has wrapped.
   */
  protected String getTextLineNumber(int rowStartOffset)
  {
    Element root = component.getDocument().getDefaultRootElement();
    int index = root.getElementIndex( rowStartOffset );
    Element line = root.getElement( index );

    if (line.getStartOffset() == rowStartOffset)
      return String.valueOf(index + 1);
    else
      return "";
  }

  /*
   *  Determine the X offset to properly align the line number when drawn
   */
  private int getOffsetX(int availableWidth, int stringWidth)
  {
    return ((availableWidth - stringWidth));
  }

  /*
   *  Determine the Y offset for the current row
   */
  private int getOffsetY(int rowStartOffset, FontMetrics fontMetrics)
      throws BadLocationException
  {
    //  Get the bounding rectangle of the row

    Rectangle r = component.modelToView( rowStartOffset );
    int lineHeight = fontMetrics.getHeight();
    int y = r.y + r.height;
    int descent = 0;

    //  The text needs to be positioned above the bottom of the bounding
    //  rectangle based on the descent of the font(s) contained on the row.

    if (r.height == lineHeight)  // default font is being used
    {
      descent = fontMetrics.getDescent();
    }
    else  // We need to check all the attributes for font changes
    {
      if (fonts == null)
        fonts = new HashMap<String, FontMetrics>();

      Element root = component.getDocument().getDefaultRootElement();
      int index = root.getElementIndex( rowStartOffset );
      Element line = root.getElement( index );

      for (int i = 0; i < line.getElementCount(); i++)
      {
        Element child = line.getElement(i);
        AttributeSet as = child.getAttributes();
        String fontFamily = (String)as.getAttribute(StyleConstants.FontFamily);
        Integer fontSize = (Integer)as.getAttribute(StyleConstants.FontSize);
        String key = fontFamily + fontSize;

        FontMetrics fm = fonts.get( key );

        if (fm == null)
        {
          Font font = new Font(fontFamily, Font.PLAIN, fontSize);
          fm = component.getFontMetrics( font );
          fonts.put(key, fm);
        }

        descent = Math.max(descent, fm.getDescent());
      }
    }

    return y - descent;
  }

  /**
   *  Calculate the width needed to display the maximum line number
   */
  public void setPreferredWidth()
  {
    Element root = component.getDocument().getDefaultRootElement();
    int digits;
    if(root.getElementCount() > 0) {
      LogLineElement e = (LogLineElement) root.getElement(root.getElementCount()-1);
      int lines = e.getLine().getLineNumber();
      digits = String.valueOf(lines).length();
    } else {
      digits = 2;
    }

    //  Update sizes when number of digits in the line number changes

    int preferredWidth;
    Dimension d = getSize();
    if (lastDigits != digits)
    {
      lastDigits = digits;
      FontMetrics fontMetrics = getFontMetrics( getBookmarkFont() );
      int width = fontMetrics.charWidth( '0' ) * digits;
      Insets insets = getInsets();
      preferredWidth = insets.left + insets.right + width;

    } else {
      preferredWidth = d.width;
    }

    d.setSize(preferredWidth, component.getSize().getHeight());
    setPreferredSize( d );
    setSize( d );
  }

  private Font getBookmarkFont() {
    return getFont().deriveFont(Font.BOLD);
  }

  @Override
  public void changedUpdate(DocumentEvent e)
  {
    documentChanged();
  }

  @Override
  public void insertUpdate(DocumentEvent e)
  {
    documentChanged();
  }

  @Override
  public void removeUpdate(DocumentEvent e)
  {
    documentChanged();
  }

  /*
   *  A document change may affect the number of displayed lines of text.
   *  Therefore the lines numbers will also change.
   */
  private void documentChanged()
  {
    //  View of the component has not been updated at the time
    //  the DocumentEvent is fired

    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {

        int count = component.getDocument().getDefaultRootElement().getElementCount();


        if (count != lastCount)
        {
          log.trace(String.format("Repaint %d=>%d.", lastCount, count));
          setPreferredWidth();
          repaint();
          lastCount = count;
        }
      }
    });
  }

  @Override
  public void componentResized(ComponentEvent e) {
    // TODO Auto-generated method stub
    setPreferredWidth();
  }

  @Override
  public void componentMoved(ComponentEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void componentShown(ComponentEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void componentHidden(ComponentEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mousePressed(MouseEvent evt) {
    if(SwingUtilities.isLeftMouseButton(evt)) {
      Point p = evt.getPoint();
      p.x = 0;
      int offset = component.viewToModel(p);
      int idx = component.getDocument().getDefaultRootElement().getElementIndex(offset);
      if(evt.isShiftDown()) {
        int start = Math.min(component.getSelectionStart(), offset);
        int end = Math.max(component.getSelectionEnd(), component.getDocument().getDefaultRootElement().getElement(idx).getEndOffset());

        component.select(start, end);
      } else {
        component.select(offset, component.getDocument().getDefaultRootElement().getElement(idx).getEndOffset());
      }
    } else if(SwingUtilities.isRightMouseButton(evt)) {
      if(popup != null) {
        popup.hide();
        popup = null;
      }

      Point p = evt.getPoint();
      p.x = 0;
      int offset = component.viewToModel(p);
      int idx = component.getDocument().getDefaultRootElement().getElementIndex(offset);
      LogLineElement e = (LogLineElement)component.getDocument().getDefaultRootElement().getElement(idx);
      String text = String.format("%s:%d", e.getLine().getFile().getName(), e.getLine().getFileLineNumber());


      tip.setText(text);
      System.out.println(text);
      p = evt.getLocationOnScreen();
      popup = PopupFactory.getSharedInstance().getPopup(this, tip, p.x+3, p.y-20);
      popup.show();
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if(popup != null) {
      popup.hide();
      popup = null;
    }
  }


  @Override
  public void mouseEntered(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseExited(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseDragged(MouseEvent evt) {
    if(SwingUtilities.isLeftMouseButton(evt)) {
      Point p = evt.getPoint();
      p.x = 0;
      int offset = component.viewToModel(p);
      int idx = component.getDocument().getDefaultRootElement().getElementIndex(offset);

      int start = Math.min(component.getSelectionStart(), offset);
      int end = Math.max(component.getSelectionEnd(), component.getDocument().getDefaultRootElement().getElement(idx).getEndOffset());

      component.select(start, end);
    }
  }

  @Override
  public void mouseMoved(MouseEvent e) {

  }
}
