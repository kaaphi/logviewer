package com.kaaphi.logviewer.ui;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.kaaphi.logviewer.LogFile;
import com.kaaphi.logviewer.LogLine;

@RunWith(Parameterized.class)
public class LogDocumentTest {
	static String[] lines;
	static String[] filters = {"a", "b", "c", "d", "e"};
	static {
		Random r = new Random(1234);
		lines = new String[4];
		for(int i = 0; i < lines.length; i++) {
			char[] line = new char[r.nextInt(5)+5];
			for(int j = 0;  j < line.length-1; j++) {
				line[j] = (char)(r.nextInt(90-65)+65);
			}
			line[line.length - 1] = '\n';
			lines[i] = new String(line);
			if(i % 2 == 0) {
				lines[i] = filters[0] + lines[i];
			}
			if((i+1) % 2 == 0) {
				lines[i] = filters[1] + lines[i];
			}
			if((i+1) % 3 == 0) {
				lines[i] = filters[2] + lines[i];
			}
			if((i+1) % 4 == 0) {
				lines[i] = filters[3] + lines[i];
			}
		}
	}
	
	//= new String[] {"abcdef\n", "ghijklmno\n", "pqrs\n"};
	static String[] testDocs = new String[filters.length+1];
	static List<LogLine>  testLines;
	
	static { 
		File file = new File("/1");
		testLines = new ArrayList<>(lines.length);
		StringBuilder[] sb = new StringBuilder[testDocs.length];
		for(int i = 0; i < sb.length; i++) sb[i] = new StringBuilder();
		
		for(int i = 0; i < lines.length; i++) {
			testLines.add(new LogLine(lines[i], file, i+1, i+1));
			sb[0].append(lines[i]);
			for(int j = 0; j < filters.length; j++) {
				if(lines[i].contains(filters[j])) {
					sb[j+1].append(lines[i]);
				}
			}
		}
		
		for(int i = 0; i < testDocs.length; i++) {
			testDocs[i] = sb[i].toString();
			System.out.println(testDocs[i]);
		}
	}
	
	private LogFile log;
	private LogDocument doc;
	
	private int docIdx;
	private int start;
	private int len;
	
	public LogDocumentTest(int docIdx, int start, int len) {
		this.docIdx = docIdx;
		this.start = start;
		this.len = len;
	}
	
	@Before
	public void before() {
		log = new LogFile(new ArrayList<>(testLines));
		doc = new LogDocument();
		doc.setLogFile(log);
		
		if(docIdx > 0) {
			doc.applyFilter(LogFile.createContainsFilter(filters[docIdx-1]));
		}
	}
	
	
	@Parameterized.Parameters(name="{0},{1},{2}")
		    public static Iterable<?> data() {
		LinkedList<Object> data = new LinkedList<Object>();
		for(int j = 0; j < testDocs.length; j++) {
			for(int i = 0; i <= testDocs[j].length(); i++) {
				int maxLen = testDocs[j].length() - i;
				for(int len = 0; len <= maxLen; len++) {
					data.add(new Object[] {j, i, len});
				}
			}
		}
		
		return data;
	    }

	
	@Test
	public void test() throws Exception {
		String txt = doc.getText(start, len);
		System.out.format("%d,%d,%d: %s%n", docIdx, start, len, txt);
		assertEquals(testDocs[docIdx].substring(start, start+len), txt);
	}
	
}
