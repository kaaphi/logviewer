package com.kaaphi.logviewer.ui.filter;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import org.apache.log4j.Logger;
import com.kaaphi.logviewer.LogFile;
import com.kaaphi.logviewer.LogFile.Filter;

public class RegexFilterEditor extends AbstractFilterEditor {
  private static final Logger log = Logger.getLogger(RegexFilterEditor.class);

  private JComboBox regex;
  private FilterHistoryModel model;

  public RegexFilterEditor() {
    setLayout(new BorderLayout());
    model = new FilterHistoryModel(10);
    regex = new JComboBox(model);
    regex.setEditable(true);
    /*
		regex.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addToSearchHistory(getText());
				fireActionPerformed();
			}
		});
     */

    regex.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ENTER) {
          fireActionPerformed();
        }
      }
    });


    add(regex, BorderLayout.CENTER);
  }

  private void addToSearchHistory(String text) {
    if(text.length() > 0)
      model.add(text);
  }

  private String getText() {
    return toStringEmptyIfNull(regex.getSelectedItem());
  }

  @Override
  public void setFilterFont(Font font) {
    regex.setFont(font);
  }

  @Override
  public Filter getFilter() {
    try {
      if(getText().length() == 0) {
        return null;
      } else {
        Filter filter;
        if(getText().startsWith("~")) {
          filter = LogFile.createRegexFilter("(?iu)"+getText().substring(1));
        } else {
          filter = LogFile.createContainsFilter(getText());
        }
        addToSearchHistory(getText());
        return filter;
      }
    } catch (Throwable e) {
      log.warn("Problem creating filter from filter string.", e);
      return null;
    }
  }

  private static String toStringEmptyIfNull(Object obj) {
    return obj == null ? "" : obj.toString();
  }

  @Override
  public boolean isFilterValid() {
    return getFilter() != null;
  }

  @Override
  public void resetFilter() {
    regex.setSelectedItem("");
  }


  private static class FilterHistoryModel extends AbstractListModel implements ComboBoxModel {
    private int maxItems;
    private Object selectedObject;
    private List<String> items = new LinkedList<String>();

    public FilterHistoryModel(int maxItems) {
      this.maxItems = maxItems;
    }

    @Override
    public void setSelectedItem(Object paramObject) {
      if ((((this.selectedObject == null) || (this.selectedObject.equals(paramObject)))) && (((this.selectedObject != null) || (paramObject == null))))
        return;
      this.selectedObject = paramObject;
      fireContentsChanged(this, -1, -1);
    }

    @Override
    public Object getSelectedItem() {
      return this.selectedObject;
    }

    @Override
    public Object getElementAt(int paramInt) {
      return items.get(paramInt);
    }

    @Override
    public int getSize() {
      return items.size();
    }

    public void add(String item) {
      if(!items.contains(item)) {
        int idx = items.size();
        items.add(item);
        fireIntervalAdded(this, idx, idx);
      }

      if(items.size() > maxItems) {
        items.remove(0);
        fireIntervalRemoved(this, 0, 0);
      }
    }
  }

}
