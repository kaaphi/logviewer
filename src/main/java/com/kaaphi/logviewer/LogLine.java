package com.kaaphi.logviewer;

import java.io.File;

public class LogLine {
	private String line;
	private File file;
	private int fileLineNumber;
	private int lineNumber;
	
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
	
	public String toString() {
		return line;
	}
}
