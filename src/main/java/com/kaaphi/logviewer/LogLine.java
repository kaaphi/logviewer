package com.kaaphi.logviewer;

import java.io.File;

public class LogLine {
	private final String line;
	private final File file;
	private final int fileLineNumber;
	private final int lineNumber;
	private int startIndex;
	
	public LogLine(String line, File file, int lineNumber, int fileLineNumber) {
		this.line = line;
		this.file = file;
		this.fileLineNumber = fileLineNumber;
		this.lineNumber = lineNumber;
	}
	
	public String getLine() {
		return line;
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
		return index + line.length();
	}
	
	public int getStartIndex() {
		return startIndex;
	}
	
	public int getEndIndex() {
		return startIndex + line.length();
	}
	
	public boolean containsIndex(int idx) {
		return idx >= startIndex && idx < startIndex + line.length();
	}
	
	public String toString() {
		return String.format("%s:%d (%d)", file.getName(), fileLineNumber, lineNumber);
	}
}
