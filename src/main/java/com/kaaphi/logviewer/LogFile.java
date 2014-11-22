package com.kaaphi.logviewer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


public class LogFile implements Iterable<String> {
	private static final Logger log = Logger.getLogger(LogFile.class);
	
	private List<LogLine> logLines;
	private int[] view;
	private int[] model;
	private int viewSize;
	private FilterListener listener;
	
	
	public LogFile(List<LogLine> logLines) {
		this.logLines = logLines;
		this.view = new int[logLines.size()];
		this.model = new int[logLines.size()];
		for(int i = 0; i < logLines.size(); i++) {
			view[i] = i;
			model[i] = i;
		}
		viewSize = view.length;
	}
	
	public void setListener(FilterListener listener) {
		this.listener = listener;
	}
	
	public String getRow(int i) {
		int index = view[i];
		return logLines.get(index).getLine();
	}
	
	public LogLine getLine(int i) {
		return logLines.get(view[i]);
	}
	
	public int getFileRow(int filteredRow) {
		return view[filteredRow];
	}
	
	public int getFilteredRow(int fileRow) {
	    return model[fileRow];
	}
	
	public int size() {
		return viewSize;
	}
	
	public int unfilteredSize() {
	    return logLines.size();
	}
	
	public Iterator<String> iterator() {
		return new Iterator<String>() {
			int index = 0;
			int max = size();
			
			public boolean hasNext() {
				return index < max;
			}
			public String next() {
				return getRow(index++);
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	public void applyFilter(Filter filter) {
		try {
			if(listener != null) listener.filteringStarted();
			int i = 0;
			int j = 0;
			int size = logLines.size();
			for(LogLine line : logLines) {
				if(filter == null || filter.filter(line.getLine())) {
					int viewIdx = j++;
					view[viewIdx] = i;
					model[i] = viewIdx; 
				} else {
					model[i] = -j;
				}
				i++;
				if((i % 1000 == 0) && (listener != null) && (!listener.filterUpdate(i, size))) {
					resetFilter();
					return;
				}
			}
			viewSize = j;
		} finally {
			if(listener != null) listener.filteringEnded();
		}
	}
	
	private void resetFilter() {
		for(int i = 0; i < logLines.size(); i++) {
			view[i] = i;
			model[i] = i;
		}
		viewSize = logLines.size();
	}
	
	public static interface Filter {
		public boolean filter(String line);
	}
	
	public static Filter createRegexFilter(String regex) {
		final Pattern pattern = Pattern.compile(regex);
		
		return new Filter() {
			public boolean filter(String line) {
				return pattern.matcher(line).find();
			}
		};
	}
	
	private static Pattern unescapePattern = Pattern.compile("\\\\(.)");
	public static Filter createContainsFilter(String str) {
		/*
		 * This only half-works. We will split on pipes that aren't preceded by
		 * a backslash, but we don't check to see if that backslash is itself
		 * escaped. Except for file path searches, this will be rare, and in
		 * almost all file path searches, it shouldn't matter. If it does
		 * matter, the user can fall back on a regex search.
		 */
		final String[] lcaseStrs = str.split("(?<!\\\\)\\|");
		for(int i = 0; i < lcaseStrs.length; i++) {
			Matcher m = unescapePattern.matcher(lcaseStrs[i]);
			lcaseStrs[i] = m.replaceAll("$1").toLowerCase();
		}
		log.debug("Filters: " + Arrays.toString(lcaseStrs));
		if(lcaseStrs.length > 1) {
			return new Filter() {
				public boolean filter(String line) {
					String lcaseLine = line.toLowerCase();
					for(String str : lcaseStrs) {
						if(lcaseLine.contains(str)) {
							return true;
						}
					}
					return false;
				}
			};
		} else {
			return new Filter() {
				public boolean filter(String line) {
					return line.toLowerCase().contains(lcaseStrs[0]);
				}
			};
		}
	}

	public static void main(String[] args) throws Exception {
		/*
		LogFile logFile = new LogFile(
				FileUtil.readLines("testFile.dat", Charset.defaultCharset()),
				createRegexColumnizer("(.*) (.*) (.*) (.*)", 1,2,3,4)
		);
		
		
		logFile.applyFilter(createRegexFilter(".*1.*"));
		
		for(String[] line : logFile) {
			System.out.println(Arrays.toString(line));
		}
		*/
	}
}
 