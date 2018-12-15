package com.kaaphi.logviewer.ui.util;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

public class MenuUtil {
	private JMenuBar bar;
	
	public MenuUtil() {
		bar = new JMenuBar();
	}
	
	public JMenuBar getMenuBar() {
		return bar;
	}
	
	public void installOn(JFrame frame) {
		frame.setJMenuBar(bar);
	}
	
	public void createMenu(String label, MenuEntry... actions) {
		JMenu menu = new JMenu();
		label = setLabelAndMnemonic(menu, label);

		for(MenuEntry action : actions) {
			menu.add(action.createMenuItem());
		}
		
		bar.add(menu);
	}
	
	private String setLabelAndMnemonic(AbstractButton button, String label) {
		String mnemonic = getMnemonic(label);
		
		label = stripAmpersand(label);
		button.setText(label);
		if(mnemonic != null) {
			button.setMnemonic(getKeyCode(mnemonic));
		}
		return label;
	}
	
	private static String getMnemonic(String label) {
		int loc = label.indexOf('&');
		if(loc < 0) {
			return null;
		} else {
			return label.substring(loc+1, loc+2);
		}
	}
	
	private static String stripAmpersand(String label) {
		return label.replaceAll("&", "");
	}
	
	private static int getKeyCode(String c) {
		String fieldName = "VK_" + c.toUpperCase();
		
		Field field;
		Integer code = -1;
		try {
			field = KeyEvent.class.getField(fieldName);

			code = (Integer) field.get(null);
		} catch (Exception e) {
			throw new Error(e);
		} 
		
		return code;
	}
	
	public static interface MenuEntry {
		JMenuItem createMenuItem();
	}
	
	public static abstract class MenuAction extends AbstractAction implements MenuEntry {
		private int mnemonic = -1;	
		private KeyStroke accel;
		
		public MenuAction(String label, Icon paramIcon, KeyStroke accel) {
			super(stripAmpersand(label), paramIcon);
			init(label, accel);			
		}

		public MenuAction(String label, KeyStroke accel) {
			super(stripAmpersand(label));
			init(label, accel);
		}
		
		public MenuAction(String label) {
			super(stripAmpersand(label));
			init(label, null);
		}
		
		public MenuAction(String label, String accel) {
			super(stripAmpersand(label));
			init(label, KeyStroke.getKeyStroke(getKeyCode(accel), KeyEvent.CTRL_DOWN_MASK));
		}
		
		private void init(String label, KeyStroke accel) {
			String mnemonic = getMnemonic(label);
			if(mnemonic != null) {
				this.mnemonic = getKeyCode(mnemonic);
			}
			this.accel = accel;
		}
		
		public JMenuItem createMenuItem() {
			JMenuItem item = new JMenuItem(this);
			if(mnemonic >= 0)
				item.setMnemonic(mnemonic);
			if(accel != null)
				item.setAccelerator(accel);

			return item;
		}
	}
}
