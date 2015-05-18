package com.kaaphi.logviewer;

public interface FilterListener {
	public void filteringStarted();
	public void filteringEnded();
	public boolean filterUpdate(int linesProcessed, int totalLines);
}
