package com.kaaphi.logviewer.ui;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;

public class ConcatFileNamesTest {
  @Test
  public void testTooLongParentDirectoryValidPrefix() {
    File parentDir = new File("C:/dev/myLogFiles/thisDir/isWayTooLong/theAwesomeOnes");

    List<File> files = IntStream.range(0, 3).mapToObj(i -> String.format("LogFileName%02d.dbg", i)).map(n -> new File(parentDir, n))
        .collect(Collectors.toList());

    Assert.assertEquals("myLogFiles\\thisDir\\isWayTooLong\\theAwesomeOnes : LogFileName0 (0.dbg, 1.dbg, 2.dbg)", LogFileViewer.concatFileNames(files));
  }

  @Test
  public void testTooLongParentDirectoryNoPrefix() {
    File parentDir = new File("C:/dev/myLogFiles/thisDir/isWayTooLong/theAwesomeOnes");

    List<File> files = IntStream.range(0, 3).mapToObj(i -> String.format("%dLogFileName.dbg", i)).map(n -> new File(parentDir, n))
        .collect(Collectors.toList());

    Assert.assertEquals("myLogFiles\\thisDir\\isWayTooLong\\theAwesomeOnes : 0LogFileName.dbg, 1LogFileName.dbg, 2LogFileName.dbg", LogFileViewer.concatFileNames(files));
  }

  @Test
  public void testSafeParentDirectoryValidPrefix() {
    File parentDir = new File("C:/dev/myLogFiles/theAwesomeOnes");

    List<File> files = IntStream.range(0, 3).mapToObj(i -> String.format("LogFileName%02d.dbg", i)).map(n -> new File(parentDir, n))
        .collect(Collectors.toList());

    Assert.assertEquals("C:\\dev\\myLogFiles\\theAwesomeOnes : LogFileName0 (0.dbg, 1.dbg, 2.dbg)", LogFileViewer.concatFileNames(files));
  }

  @Test
  public void testSafeParentDirectoryNoPrefix() {
    File parentDir = new File("C:/dev/myLogFiles/theAwesomeOnes");

    List<File> files = IntStream.range(0, 3).mapToObj(i -> String.format("%dLogFileName.dbg", i)).map(n -> new File(parentDir, n))
        .collect(Collectors.toList());

    Assert.assertEquals("C:\\dev\\myLogFiles\\theAwesomeOnes : 0LogFileName.dbg, 1LogFileName.dbg, 2LogFileName.dbg", LogFileViewer.concatFileNames(files));
  }

  @Test
  public void testNoCommonParentDir() {
    File parentDir1 = new File("C:/dev/myLogFiles/theAwesomeOnes");
    File parentDir2 = new File("C:/dev/myLogFiles/theBadOnes");

    List<File> files = Arrays.asList(new File(parentDir1, "a.dbg"), new File(parentDir2, "b.dbg"));

    Assert.assertEquals("a.dbg, b.dbg", LogFileViewer.concatFileNames(files));
  }
}
