package com.kaaphi.logviewer.ui.util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FindOpenWithoutClose {
	/*
	private Pattern openPattern;
	private int openGroup;
	private Pattern closePattern;
	private int closeGroup;
	*/
	private OpenCloseMatcher openMatcher;
	private OpenCloseMatcher closeMatcher;
	
	//private int closed;
	Set<String> unopened;
	Set<String> unclosed;
	Set<String> closed;
	
	
	public FindOpenWithoutClose(OpenCloseMatcher openMatcher,
			OpenCloseMatcher closeMatcher) {
		super();
		this.openMatcher = openMatcher;
		this.closeMatcher = closeMatcher;
	}

	private void processStream(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		
		String line;
		while((line = reader.readLine()) != null) {
			String key = openMatcher.matches(line);
			if(key != null) {
				closed.remove(key);
				if(unopened.remove(key)) {
					//closed.add(key);
				} else {
					unclosed.add(key);
				}
			} else {
				key = closeMatcher.matches(line);
				if(key != null) {
					if(unclosed.remove(key)) {
						closed.add(key);
					} else {
						unopened.add(key);
					}
					
				}
			}
		}
		
		//System.out.println(closed);
	}
	
	public void processFiles(File...files) throws IOException {
		unopened = new HashSet<String>();
		unclosed = new HashSet<String>();
		closed = new HashSet<String>();
		
		for(File f : files) {
			System.out.println(f);
			if(f.isFile()) {
				InputStream is = new FileInputStream(f);
				processStream(is);
				is.close();
			}
		}
	}
	
	private static class MultiMatcher implements OpenCloseMatcher {
		private OpenCloseMatcher[] matchers;
		
		public MultiMatcher(OpenCloseMatcher...matchers) {
			this.matchers = matchers;
		}
		
		public String matches(String line) {
			for(OpenCloseMatcher matcher : matchers) {
				String result = matcher.matches(line);
				if(result != null) {
					return result;
				}
			}
			
			return null;
		}
	}
	
	private static class RegExOpenCloseMatcher implements OpenCloseMatcher {
		private int[] groups;
		private Pattern pattern;
		
		public RegExOpenCloseMatcher(String pattern, int...groups) {
			this.pattern = Pattern.compile(pattern);
			this.groups = groups;
		}
		
		public String matches(String line) {
			Matcher m = pattern.matcher(line);
		
			if(m.find()) {
				StringBuilder key = new StringBuilder();
				for(int g : groups) {
					key.append(m.group(g));
					key.append(";");
				}
				return key.toString();
			}
			
			else {
				return null;
			}
		}
		
	}
	
	private static interface OpenCloseMatcher {
		public String matches(String line);
	}
	
	public static void main(String[] args) throws Exception {
		File dir = new File("C:/development/escalations/actelion/14234 - missing end events");
		/*
		FindOpenWithoutClose test = new FindOpenWithoutClose(
				"(dev=.*JTAPI: \\d+) \\[CiscoTransferStartEv\\]", 1,
				"(dev=.*JTAPI: \\d+) \\[CiscoTransferEndEv\\]", 1
		);
		*/
		
		
		
		FindOpenWithoutClose test = new FindOpenWithoutClose(
				new RegExOpenCloseMatcher("dev=(SEP.+?),.*BEGIN_CALL.*CallId=(\\d+);", 1, 2),
				//new RegExOpenCloseMatcher("dev=(SEP.+?),.*END_CALL.*CallId=(\\d+);", 1, 2)
				new MultiMatcher(
						new RegExOpenCloseMatcher("dev=(SEP.+?),.*END_CALL.*CallId=(\\d+);", 1, 2),
						new RegExOpenCloseMatcher("dev=(SEP.+?),.*MergedCall=(\\d+);", 1, 2)
				)
				//new RegExOpenCloseMatcher("dev=(SEP.+?),.*JTAPI: \\[.*CiscoTermOutOfServiceEv.*\\]", 1),
				//new RegExOpenCloseMatcher("CiscoTermCreatedEv (SEP.+?)>", 1)
				//new RegExOpenCloseMatcher("dev=(SEP.+?),.*JTAPI: \\[.*CiscoTermInServiceEv.*\\]", 1)
		);
		
		test.processFiles(
				//new File(dir, "ctiservice.dbg.1"),
				//new File(dir, "ctiservice.dbg")
				dir.listFiles()
		);
		/*
		test.processFiles(dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				//return name.startsWith("cti");
				return name.equals("ctiservice.dbg") || name.equals("ctiservice.dbg.1");
				//return "dbg".equals(name.subSequence(name.length()-5, name.length()-2));
			}
		}));
		*/
		
		System.out.println(test.unclosed.size());
		printDevices(test.unclosed);
		System.out.println(test.closed.size());
		//printDevices(test.closed);
		//System.out.println(test.unopened.size());
	}
	
	private static void printDevices(Set<String> devs) {
		for(String d : new TreeSet<String>(devs)) {
			System.out.println(d.substring(0, d.length()-1));
		}
		System.out.println();
	}
	
}
