package com.kaaphi.logviewer;

import java.io.PrintStream;
import java.util.Random;

public class GenerateTestFile {
	static final String[] words = new String[] {
		"aardvark",
		"fist",
		"something",
		"nothing",
		"keeper",
		"cat",
		"castle",
		"category",
		"killer",
		"really",
		"real",
		"mist",
		"list",
		"lost",
		"catcher",
		"fastener",
		"fast",
		"gorge",
		"gasoline",
		"filler",
		"forty",
		"faster",
		"fisher",
		"car"
	};
	
	public static void main(String[] args) throws Exception {
		Random rand = new Random(1234);

		PrintStream out = new PrintStream("testData/words.txt");
		//PrintStream out = System.out;
		
		int rows = 400;
		int cols = 4;

		for(int r = 0; r < rows; r++) {
			out.print(words[rand.nextInt(words.length)]);
			for(int c = 1; c < cols; c++) {
				out.print(" ");
				out.print(words[rand.nextInt(words.length)]);
			}
			out.println();
		}
	}
}
