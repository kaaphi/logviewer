package com.kaaphi.logviewer;

import java.io.File;

public class LogLine {
	public static final int MAX_VIEW_LENGTH = Integer.getInteger("maxLineLength", 4000);
	
	private final String lineView;
	private final String rawLine;
	private final File file;
	private final int fileLineNumber;
	private final int lineNumber;
	private int startIndex;
	
	public LogLine(String line, File file, int lineNumber, int fileLineNumber) {
		this.rawLine = line;
		this.lineView = line.length() > MAX_VIEW_LENGTH ? line.substring(0, MAX_VIEW_LENGTH) + "[!TRUNCATED!]" : line;
		this.file = file;
		this.fileLineNumber = fileLineNumber;
		this.lineNumber = lineNumber;
	}
	
	public String getLine() {
		return lineView;
	}
	
	public String getRawLine() {
		return rawLine;
	}
	public File getFile() {
		return file;
	}
	public int getFileLineNumber() {
		return fileLineNumber;
	}
	
	public int getLineNumber() {
		return lineNumber;
	}
	/*
	public String toString() {
		return line;
	}
	*/
	public int setStartIndex(int index) {
		this.startIndex = index;
		return index + lineView.length();
	}
	
	public int getStartIndex() {
		return startIndex;
	}
	
	public int getEndIndex() {
		return startIndex + lineView.length();
	}
	
	public boolean containsIndex(int idx) {
		return idx >= startIndex && idx < startIndex + lineView.length();
	}
	
	public String toString() {
		return String.format("%s:%d (%d)", file.getName(), fileLineNumber, lineNumber);
	}
}
