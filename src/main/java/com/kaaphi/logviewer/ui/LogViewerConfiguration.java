package com.kaaphi.logviewer.ui;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import com.kaaphi.logviewer.ui.util.Configuration;
import com.kaaphi.logviewer.ui.util.ConfigurationObject;

public class LogViewerConfiguration extends Configuration {
	private static final LogViewerConfiguration instance = new LogViewerConfiguration();
    
    public final ConfigurationObject<Font> font = new FontObject(new Font(Font.MONOSPACED, Font.PLAIN, 14));
	public final ConfigurationObject<Color> searchHighlight = new ColorObject(Color.YELLOW);
	
	
	public static LogViewerConfiguration getInstance() {
	    return instance;
	}
	
	public void loadConfig() throws IOException {
		Properties storedConfig = new Properties();
		File configFile = new File("logviewer.cfg");
		if(configFile.exists()) {
			InputStream in = new FileInputStream(configFile);
			try {
				storedConfig.load(in);
			} finally {
				in.close();
			}
			load(storedConfig);
		}
	}
	
	public void storeConfig() throws IOException {
		Properties toStore = store();
		File configFile = new File("logviewer.cfg");
		OutputStream out = new FileOutputStream(configFile);
		try {
			toStore.store(out, "Log Viewer Configuration");
		} finally {
			out.close();
		}
	}
	
	
	public static void main(String[] args) {
		LogViewerConfiguration config = new LogViewerConfiguration();
		//Properties props
		System.out.println(config.store());
		
		//new LogViewerConfiguration().test();
	}
	
	private static class FontObject extends ConfigurationObject<Font> {
		public FontObject(Font defaultFont) {
			super(defaultFont);
		}
		
		public Font from(String value) {
			String[] vals = value.split(";");
			return new Font(vals[0], Integer.parseInt(vals[1]), Integer.parseInt(vals[2]));
		}

		public String to(Font value) {
			return String.format("%s;%d;%d", value.getFamily(), value.getStyle(), value.getSize());
		}
	}
	
	private static class ColorObject extends ConfigurationObject<Color> {
        public ColorObject(Color defaultValue) {
            super(defaultValue);
        }

        public Color from(String value) {
            return new Color(Integer.parseInt(value));
        }

        @Override
        public String to(Color value) {
        	return Integer.toString(value.getRGB());
        }
	}
}
