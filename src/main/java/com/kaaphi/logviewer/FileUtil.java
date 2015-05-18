package com.kaaphi.logviewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FileUtil {
	public static List<LogLine> readLines(InputStream in, File file, Charset charset) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
		
		List<LogLine> lines = new ArrayList<LogLine>();
		readLines(reader, file, 1, lines);
		
		return lines;
	}
	
	public static List<LogLine> readLines(String file, Charset charset) throws IOException {
		return readLines(new FileInputStream(file), new File(file), charset);
	}
	
	public static List<LogLine> readLines(File file, Charset charset) throws IOException {
		return readLines(new FileInputStream(file), file, charset);
	}
	
	private static int readLines(BufferedReader reader, File file, int lineNumber, List<LogLine> lines) throws IOException {
		String line;
		int fileLine = 1;
		while((line = reader.readLine()) != null) {
			lines.add(new LogLine(line+"\n", file, lineNumber++, fileLine++));
		}
		
		reader.close();
		
		return lineNumber;
	}
	
	public static List<LogLine> readLines(List<File> files, Charset charset) throws IOException {
		List<LogLine> lines = new ArrayList<LogLine>();
		int lineNumber = 1;
		for(File file : files) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
			lineNumber = readLines(reader, file, lineNumber, lines);
		}
		return lines;
	}
	
	public static Comparator<File> getLastModifiedComparator() {
		return new Comparator<File>() {
			public int compare(File o1, File o2) {
				long val = o1.lastModified() - o2.lastModified();
				if(val < 0) {
					return -1;
				} else if (val > 0) {
					return 1;
				} else {
					return 0;
				}
			}
		};
	}
	
	public static Comparator<File> getFilePartComparator() {
		return new Comparator<File>() {
			public int compare(File o1, File o2) {
				String[] n1 = o1.getName().split("\\.");
				String[] n2 = o2.getName().split("\\.");
				
				for(int i = 0; i < n1.length; i++) {
					if(i >= n2.length) {
						return -1;
					}
					int c;
					
					try {
						c = Integer.parseInt(n1[i]) - Integer.parseInt(n2[i]);
					} catch (NumberFormatException e) {
						c = n1[i].compareToIgnoreCase(n2[i]);
					}
					if(c != 0) {
						return -c;
					}
				}
				
				return n2.length > n1.length ? 1 : 0;
			}
		};
	}
}
