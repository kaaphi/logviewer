package com.kaaphi.logviewer.util;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class DocumentAppender extends AppenderSkeleton {
	private final Document doc = new PlainDocument();
		
	public DocumentAppender() {
		super();
	}

	public DocumentAppender(boolean isActive) {
		super(isActive);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean requiresLayout() {
		return true;
	}
	
	public Document getDocument() {
		return doc;
	}

	@Override
	protected void append(LoggingEvent evt) {
		try {
			doc.insertString(doc.getLength(), getLayout().format(evt), null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

}
