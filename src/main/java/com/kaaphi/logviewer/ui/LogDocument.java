package com.kaaphi.logviewer.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.undo.UndoableEdit;
import org.apache.log4j.Logger;
import com.kaaphi.logviewer.Bookmark;
import com.kaaphi.logviewer.LogFile;
import com.kaaphi.logviewer.LogLine;

public class LogDocument extends AbstractDocument {
	private static final Logger logger = Logger.getLogger(LogFileViewer.class);
	
	private static final LogFile EMPTY_LOG = new LogFile(Collections.<LogLine>emptyList());

	
	
	private LogFile log;
	private Map<Integer, Bookmark> bookmarks = new HashMap<>();
	
	
	
	private Element rootElement = new Element() {
		@Override
		public Document getDocument() {
			return LogDocument.this;
		}

		@Override
		public Element getParentElement() {
			return null;
		}

		@Override
		public String getName() {
			return "root";
		}

		@Override
		public AttributeSet getAttributes() {
			return new SimpleAttributeSet();
		}

		@Override
		public int getStartOffset() {
			return 0;
		}

		@Override
		public int getEndOffset() {
			return log.size() == 0 ? 0 : log.getLine(log.size()-1).getEndIndex();
		}

		@Override
		public int getElementIndex(int offset) {
			return log.getLineIndex(offset);
		}

		@Override
		public int getElementCount() {
			return log.size();
		}

		@Override
		public Element getElement(int index) {
			return new LogLineElement(log.getLine(index));
		}

		@Override
		public boolean isLeaf() {
			return getElementCount() > 0;
		}		
	};
	
	public LogDocument() {
		super(new LogContent());
		this.log = EMPTY_LOG;
	}
	
	public void addBookmark(Bookmark bookmark) {
	  this.bookmarks.put(bookmark.getLine().getLineNumber(), bookmark);
	}
	
	public void removeBookmark(Bookmark bookmark) {
	  this.bookmarks.remove(bookmark.getLine().getLineNumber());
	}
	
	public boolean isBookmarked(int lineNumber) {
	  return bookmarks.containsKey(lineNumber);
	}
	
	public void setLogFile(LogFile log) {
		try { 
			fireRemoveUpdate();
			this.log = log;
			((LogContent)getContent()).setLogFile(log);
			fireInsertUpdate();
		} catch (Throwable th) {
			th.printStackTrace();
		}
	}
	
	private void fireRemoveUpdate() {
		if(this.log.characterLength() > 0) {
			fireRemoveUpdate(getCurrentEvent(EventType.REMOVE));
		}
	}
	
	private void fireInsertUpdate() {
		if(this.log.characterLength() > 0) {
			fireInsertUpdate(getCurrentEvent(EventType.INSERT));
		}
	}
	
	public LogFile getLogFile() {
		return log;
	}
	
	public void applyFilter(LogFile.Filter filter) {
		fireRemoveUpdate();
		writeLock();
		log.applyFilter(filter);
		writeUnlock();
		fireInsertUpdate();
	}
	
	private DocumentEvent getCurrentEvent(DocumentEvent.EventType type) {
		DefaultDocumentEvent e = new DefaultDocumentEvent(0, log.characterLength(), type);
		
		Element[] elements = new Element[log.size()];
		int i = 0;
		for(LogLine l : log.lines()) {
			elements[i++] = new LogLineElement(l);
		}
		
		Element[] removed;
		Element[] added;
		if(type == DocumentEvent.EventType.REMOVE) {
			removed = elements;
			added = new Element[0];
		} else {
			removed = new Element[0];
			added = elements;
		}
		e.addEdit(new ElementEdit(rootElement, 0, removed, added));
	
		return e;
	}
	

	@Override
	public Element getDefaultRootElement() {
		return rootElement;
	}

	@Override
	public Element getParagraphElement(int pos) {
		int currentIndex = 0;
		for(LogLine line : log.lines()) {
 			int nextIndex = currentIndex + line.getLine().length();
 			if(pos >= currentIndex && pos < nextIndex) {
				return new LogLineElement(line);
			}
			currentIndex = nextIndex;
		}
		
		return null;
	}
	
	private static class LogContent implements Content {
		private LogFile log;
		
		public LogContent() {
			log = EMPTY_LOG;
		}
		
		public void setLogFile(LogFile log) {
			this.log = log;
		}
		
		@Override
		public Position createPosition(final int offset) throws BadLocationException {
			return new Position() {
				@Override
				public int getOffset() {
					return offset;
				}
			};
		}

		@Override
		public int length() {
			return log.characterLength();
		}

		@Override
		public UndoableEdit insertString(int where, String str)
				throws BadLocationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public UndoableEdit remove(int where, int nitems)
				throws BadLocationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getString(int startIndex, int len) throws BadLocationException {
			int endIndex = startIndex + len;

			if(startIndex < 0 || startIndex > length() || endIndex < 0 || endIndex > length()) {
				throw new BadLocationException("Bad Location!", startIndex);
			} else if (len < 0) {
				return "";
			}

			List<int[]> debugDetails = new LinkedList<>();
			try {
				int idx = log.getLineIndex(startIndex);
				StringBuilder sb = new StringBuilder();
				
				while(sb.length() < len) {
					LogLine line = log.getLine(idx);
					int currentIndex = line.getStartIndex();
					int begin = startIndex - currentIndex;
					debugDetails.add(new int[] {idx, currentIndex, begin});
					if(line.getEndIndex() > endIndex) {
						sb.append(line.getLine().substring(begin, endIndex - currentIndex));
					} else {
						sb.append(line.getLine().substring(begin));
						startIndex = line.getEndIndex();
					}

					idx++;
				}

				return sb.toString();
			} catch (Throwable th) {
				StringBuilder sb = new StringBuilder();
				for(int[] debug : debugDetails) {
					String run = String.format("Run: idx=%d; current=%d; begin=%d", debug[0], debug[1], debug[2]);
					logger.debug(run);
					sb.append("\n").append(run);
				}
				
				String error = String.format("Trouble with start=%d,end=%d,len=%d (log details: %d(%d))", startIndex, endIndex, len, log.size(), log.characterLength());
				logger.error(error, th);

				throw new BadLocationException(error + sb, startIndex);
			}
		}

		@Override
		public void getChars(int where, int len, Segment txt)
				throws BadLocationException {
			txt.array = getString(where, len).toCharArray();
			txt.count = len;
			txt.offset = 0;
		}		
	}


	public class LogLineElement implements Element {
		private LogLine line;
		
		public LogLineElement(LogLine line) {
			this.line = line;
		}
		public LogLine getLine() {
			return line;
		}
		
		@Override
		public Document getDocument() {
			return LogDocument.this;
		}

		@Override
		public Element getParentElement() {
			return rootElement;
		}

		@Override
		public String getName() {
			return "logline";
		}

		@Override
		public AttributeSet getAttributes() {
			return new SimpleAttributeSet();
		}

		@Override
		public int getStartOffset() {
			return line.getStartIndex();
		}

		@Override
		public int getEndOffset() {
			return line.getEndIndex();
		}

		@Override
		public int getElementIndex(int offset) {
			return -1;
		}

		@Override
		public int getElementCount() {
			return 0;
		}

		@Override
		public Element getElement(int index) {
			return null;
		}

		@Override
		public boolean isLeaf() {
			return true;
		}
		
	}
}
