package com.kaaphi.logviewer;

public class Bookmark {
	private String label;
	private final LogLine line;
	public Bookmark(String label, LogLine line) {
		super();
		this.label = label;
		this.line = line;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public LogLine getLine() {
		return line;
	}
	public String toString() {
		return String.format("%d : %s", line.getLineNumber(), label);
	}
}
