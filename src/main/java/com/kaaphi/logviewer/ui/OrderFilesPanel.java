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
		final JList fileList = new JList(model);
		fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
			public void actionPerformed(ActionEvent e) {
				int i = fileList.getSelectedIndex();
				model.moveUp(i);
				fileList.setSelectedIndex(i-1);
			}
		});
		
		down.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int i = fileList.getSelectedIndex();
				model.moveDown(i);
				fileList.setSelectedIndex(i+1);
			}
		});
		
		fileList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if(!e.getValueIsAdjusting()) {
					int i = fileList.getSelectedIndex();
					if(i < 0) {
						up.setEnabled(false);
						down.setEnabled(false);
					} else {
						up.setEnabled(i > 0);
						down.setEnabled(i < (model.getSize()-1));
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
		
	    public void mousePressed(MouseEvent e) {
	        maybeShowPopup(e);
	    }

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
	
	private static class OrderableListModel<T> extends AbstractListModel {
		private List<T> items;
		
		public OrderableListModel(List<T> items) {
			this.items = new ArrayList<T>(items);
		}
		
		@Override
		public Object getElementAt(int i) {
			return items.get(i);
		}

		@Override
		public int getSize() {
			return items.size();
		}
		
		public void moveUp(int i) {
			T o1 = items.get(i);
			T o2 = items.get(i-1);
			items.set(i, o2);
			items.set(i-1, o1);
			fireContentsChanged(this, i-1, i);
		}
		
		public void moveDown(int i) {
			T o1 = items.get(i);
			T o2 = items.get(i+1);
			items.set(i, o2);
			items.set(i+1, o1);
			fireContentsChanged(this, i, i+1);
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
