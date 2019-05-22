package com.streamsets.utils.file;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestFileUtils  {

  @Test
  public void testGetJarUrlsInDirectory() throws MalformedURLException {
    final String[] expectedJarNames = new String[] {"empty1.jar", "empty2.jar", "empty3.jar"};
    final String sampleDir = getClass().getResource("sampleJarDirectory").getFile();

    final Set<URL> expectedJars = new HashSet<>();
    for (final String jarName : expectedJarNames) {
      expectedJars.add(new URL(String.format("file:%s/%s", sampleDir, jarName)));
    }

    final URL[] jars = FileUtils.getJarUrlsInDirectory(sampleDir);
    final Set<URL> actualJars = new HashSet<>(Arrays.asList(jars));

    assertThat(actualJars, equalTo(expectedJars));
  }
}
