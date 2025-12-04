/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.utils;

import static org.apache.juneau.TestUtils.assertThrowsWithMessage;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link FileUtils}.
 */
class FileUtils_Test extends TestBase {

	@TempDir
	Path tempDir;

	//====================================================================================================
	// Constructor (line 32)
	//====================================================================================================
	@Test
	void a00_constructor() {
		// Test line 32: class instantiation
		// FileUtils has an implicit public no-arg constructor
		var instance = new FileUtils();
		assertNotNull(instance);
	}

	//====================================================================================================
	// create(File)
	//====================================================================================================
	@Test
	void a001_create() {
		var f = new File(tempDir.toFile(), "test.txt");
		assertFalse(f.exists());
		FileUtils.create(f);
		assertTrue(f.exists());
		assertTrue(f.isFile());
	}

	@Test
	void a002_create_existingFile() throws IOException {
		var f = new File(tempDir.toFile(), "test.txt");
		f.createNewFile();
		assertTrue(f.exists());
		var lastModified = f.lastModified();
		FileUtils.create(f);
		assertTrue(f.exists());
		// Should not modify existing file
		assertEquals(lastModified, f.lastModified());
	}

	@Test
	void a003_create_parentDirectoryDoesNotExist() {
		var f = new File(tempDir.toFile(), "nonexistent/test.txt");
		var e = assertThrows(RuntimeException.class, () -> {
			FileUtils.create(f);
		});
		// When parent directory doesn't exist, createNewFile() throws IOException
		// which gets wrapped in RuntimeException by toRex()
		assertTrue(e.getCause() instanceof IOException);
		assertNotNull(e.getCause().getMessage());
	}

	//====================================================================================================
	// createTempFile(String)
	//====================================================================================================
	@Test
	void a004_createTempFile() throws IOException {
		var f = FileUtils.createTempFile("test.txt");
		assertNotNull(f);
		assertTrue(f.exists());
		assertTrue(f.getName().startsWith("test"));
		assertTrue(f.getName().endsWith(".txt"));
		f.delete();
	}

	@Test
	void a005_createTempFile_multipleDots() throws IOException {
		var f = FileUtils.createTempFile("test.backup.txt");
		assertNotNull(f);
		assertTrue(f.exists());
		assertTrue(f.getName().startsWith("test"));
		// createTempFile splits by '.' and uses first two parts: prefix="test", suffix=".backup"
		// So the file name will contain ".backup", not ".txt"
		assertTrue(f.getName().contains(".backup"));
		f.delete();
	}

	@Test
	void a006_createTempFile_noExtension() {
		// When there's no dot, split("\\.") returns array with one element
		// This will cause ArrayIndexOutOfBoundsException when accessing parts[1]
		// Note: This tests the current behavior, which may be a bug in the implementation
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
			FileUtils.createTempFile("test");
		});
	}

	//====================================================================================================
	// createTempFile(String, String)
	//====================================================================================================
	@Test
	void a007_createTempFile_withContents() throws IOException {
		var contents = "test content\nline 2";
		var f = FileUtils.createTempFile("test.txt", contents);
		assertNotNull(f);
		assertTrue(f.exists());

		try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
			var sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				if (sb.length() > 0)
					sb.append('\n');
				sb.append(line);
			}
			assertEquals(contents, sb.toString());
		}
		f.delete();
	}

	@Test
	void a008_createTempFile_withEmptyContents() throws IOException {
		var f = FileUtils.createTempFile("test.txt", "");
		assertNotNull(f);
		assertTrue(f.exists());
		assertEquals(0, f.length());
		f.delete();
	}

	@Test
	void a009_createTempFile_withNullContents() throws IOException {
		var f = FileUtils.createTempFile("test.txt", null);
		assertNotNull(f);
		assertTrue(f.exists());
		// null contents should result in empty file
		assertEquals(0, f.length());
		f.delete();
	}

	//====================================================================================================
	// deleteFile(File)
	//====================================================================================================
	@Test
	void a010_deleteFile() throws IOException {
		var f = new File(tempDir.toFile(), "test.txt");
		f.createNewFile();
		assertTrue(f.exists());
		assertTrue(FileUtils.deleteFile(f));
		assertFalse(f.exists());
	}

	@Test
	void a011_deleteFile_null() {
		assertTrue(FileUtils.deleteFile(null));
	}

	@Test
	void a012_deleteFile_nonexistent() {
		var f = new File(tempDir.toFile(), "nonexistent.txt");
		assertFalse(f.exists());
		assertFalse(FileUtils.deleteFile(f));
	}

	@Test
	void a013_deleteFile_directory() throws IOException {
		var dir = new File(tempDir.toFile(), "testdir");
		dir.mkdirs();
		var f1 = new File(dir, "file1.txt");
		var f2 = new File(dir, "file2.txt");
		f1.createNewFile();
		f2.createNewFile();
		assertTrue(dir.exists());
		assertTrue(f1.exists());
		assertTrue(f2.exists());
		assertTrue(FileUtils.deleteFile(dir));
		assertFalse(dir.exists());
		assertFalse(f1.exists());
		assertFalse(f2.exists());
	}

	@Test
	void a014_deleteFile_nestedDirectories() throws IOException {
		var dir1 = new File(tempDir.toFile(), "dir1");
		var dir2 = new File(dir1, "dir2");
		var f = new File(dir2, "file.txt");
		dir2.mkdirs();
		f.createNewFile();
		assertTrue(dir1.exists());
		assertTrue(dir2.exists());
		assertTrue(f.exists());
		assertTrue(FileUtils.deleteFile(dir1));
		assertFalse(dir1.exists());
		assertFalse(dir2.exists());
		assertFalse(f.exists());
	}

	@Test
	void a015_deleteFile_emptyDirectory() {
		var dir = new File(tempDir.toFile(), "emptydir");
		dir.mkdirs();
		assertTrue(dir.exists());
		assertTrue(FileUtils.deleteFile(dir));
		assertFalse(dir.exists());
	}

	//====================================================================================================
	// fileExists(File, String)
	//====================================================================================================
	@Test
	void a016_fileExists() throws IOException {
		var dir = tempDir.toFile();
		var f = new File(dir, "test.txt");
		f.createNewFile();
		assertTrue(FileUtils.fileExists(dir, "test.txt"));
	}

	@Test
	void a017_fileExists_nonexistentFile() {
		var dir = tempDir.toFile();
		assertFalse(FileUtils.fileExists(dir, "nonexistent.txt"));
	}

	@Test
	void a018_fileExists_nullDir() {
		assertFalse(FileUtils.fileExists(null, "test.txt"));
	}

	@Test
	void a019_fileExists_nullFileName() {
		var dir = tempDir.toFile();
		assertFalse(FileUtils.fileExists(dir, null));
	}

	@Test
	void a020_fileExists_bothNull() {
		assertFalse(FileUtils.fileExists(null, null));
	}

	@Test
	void a021_fileExists_subdirectory() throws IOException {
		var dir = tempDir.toFile();
		var subdir = new File(dir, "subdir");
		subdir.mkdirs();
		var f = new File(subdir, "test.txt");
		f.createNewFile();
		assertTrue(FileUtils.fileExists(subdir, "test.txt"));
	}

	//====================================================================================================
	// getBaseName(String)
	//====================================================================================================
	@Test
	void a022_getBaseName() {
		assertEquals("test", FileUtils.getBaseName("test.txt"));
		assertEquals("file.backup", FileUtils.getBaseName("file.backup.txt"));
	}

	@Test
	void a023_getBaseName_noExtension() {
		assertEquals("test", FileUtils.getBaseName("test"));
		assertEquals("file", FileUtils.getBaseName("file"));
	}

	@Test
	void a024_getBaseName_null() {
		assertNull(FileUtils.getBaseName(null));
	}

	@Test
	void a025_getBaseName_onlyExtension() {
		assertEquals("", FileUtils.getBaseName(".txt"));
	}

	@Test
	void a026_getBaseName_multipleDots() {
		assertEquals("test.backup", FileUtils.getBaseName("test.backup.txt"));
	}

	//====================================================================================================
	// getFileExtension(String)
	//====================================================================================================
	@Test
	void a027_getFileExtension() {
		assertEquals("txt", FileUtils.getFileExtension("test.txt"));
		assertEquals("java", FileUtils.getFileExtension("FileUtils.java"));
	}

	@Test
	void a028_getFileExtension_noExtension() {
		assertEquals("", FileUtils.getFileExtension("test"));
		assertEquals("", FileUtils.getFileExtension("file"));
	}

	@Test
	void a029_getFileExtension_null() {
		assertNull(FileUtils.getFileExtension(null));
	}

	@Test
	void a030_getFileExtension_onlyExtension() {
		assertEquals("txt", FileUtils.getFileExtension(".txt"));
	}

	@Test
	void a031_getFileExtension_multipleDots() {
		assertEquals("txt", FileUtils.getFileExtension("test.backup.txt"));
	}

	@Test
	void a032_getFileExtension_emptyString() {
		assertEquals("", FileUtils.getFileExtension(""));
	}

	//====================================================================================================
	// getFileName(String)
	//====================================================================================================
	@Test
	void a033_getFileName() {
		assertEquals("test.txt", FileUtils.getFileName("test.txt"));
	}

	@Test
	void a034_getFileName_withPath() {
		assertEquals("test.txt", FileUtils.getFileName("/path/to/test.txt"));
		assertEquals("file.java", FileUtils.getFileName("dir/subdir/file.java"));
	}

	@Test
	void a035_getFileName_withTrailingSlash() {
		assertEquals("test.txt", FileUtils.getFileName("/path/to/test.txt/"));
		assertEquals("dir", FileUtils.getFileName("/path/to/dir/"));
	}

	@Test
	void a036_getFileName_null() {
		assertNull(FileUtils.getFileName(null));
	}

	@Test
	void a037_getFileName_emptyString() {
		assertNull(FileUtils.getFileName(""));
	}

	@Test
	void a038_getFileName_onlySlashes() {
		assertNull(FileUtils.getFileName("/"));
		assertNull(FileUtils.getFileName("//"));
	}

	@Test
	void a039_getFileName_windowsPath() {
		assertEquals("test.txt", FileUtils.getFileName("C:\\path\\to\\test.txt"));
	}

	//====================================================================================================
	// hasExtension(String, String)
	//====================================================================================================
	@Test
	void a040_hasExtension() {
		assertTrue(FileUtils.hasExtension("test.txt", "txt"));
		assertTrue(FileUtils.hasExtension("file.java", "java"));
	}

	@Test
	void a041_hasExtension_notMatching() {
		assertFalse(FileUtils.hasExtension("test.txt", "java"));
		assertFalse(FileUtils.hasExtension("file.java", "txt"));
	}

	@Test
	void a042_hasExtension_noExtension() {
		assertFalse(FileUtils.hasExtension("test", "txt"));
	}

	@Test
	void a043_hasExtension_nullName() {
		assertFalse(FileUtils.hasExtension(null, "txt"));
	}

	@Test
	void a044_hasExtension_nullExt() {
		assertFalse(FileUtils.hasExtension("test.txt", null));
	}

	@Test
	void a045_hasExtension_bothNull() {
		assertFalse(FileUtils.hasExtension(null, null));
	}

	@Test
	void a046_hasExtension_caseSensitive() {
		assertFalse(FileUtils.hasExtension("test.TXT", "txt"));
		assertTrue(FileUtils.hasExtension("test.TXT", "TXT"));
	}

	@Test
	void a047_hasExtension_multipleDots() {
		assertTrue(FileUtils.hasExtension("test.backup.txt", "txt"));
		assertFalse(FileUtils.hasExtension("test.backup.txt", "backup"));
	}

	//====================================================================================================
	// mkdirs(File, boolean)
	//====================================================================================================
	@Test
	void a048_mkdirs() {
		var dir = new File(tempDir.toFile(), "newdir");
		assertFalse(dir.exists());
		var result = FileUtils.mkdirs(dir, false);
		assertTrue(dir.exists());
		assertTrue(dir.isDirectory());
		assertSame(dir, result);
	}

	@Test
	void a049_mkdirs_nestedDirectories() {
		var dir = new File(tempDir.toFile(), "dir1/dir2/dir3");
		assertFalse(dir.exists());
		var result = FileUtils.mkdirs(dir, false);
		assertTrue(dir.exists());
		assertTrue(dir.isDirectory());
		assertSame(dir, result);
	}

	@Test
	void a050_mkdirs_existingDirectory_cleanFalse() {
		var dir = new File(tempDir.toFile(), "existingdir");
		dir.mkdirs();
		assertTrue(dir.exists());
		var result = FileUtils.mkdirs(dir, false);
		assertTrue(dir.exists());
		assertSame(dir, result);
	}

	@Test
	void a051_mkdirs_existingDirectory_cleanTrue() throws IOException {
		var dir = new File(tempDir.toFile(), "cleandir");
		dir.mkdirs();
		var f = new File(dir, "test.txt");
		f.createNewFile();
		assertTrue(dir.exists());
		assertTrue(f.exists());
		var result = FileUtils.mkdirs(dir, true);
		assertTrue(dir.exists());
		assertFalse(f.exists());
		assertSame(dir, result);
	}

	@Test
	void a052_mkdirs_null() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileUtils.mkdirs((File)null, false);
		});
	}

	//====================================================================================================
	// mkdirs(String, boolean)
	//====================================================================================================
	@Test
	void a053_mkdirs_string() {
		var dir = new File(tempDir.toFile(), "newdir");
		var path = dir.getAbsolutePath();
		var result = FileUtils.mkdirs(path, false);
		assertTrue(dir.exists());
		assertTrue(dir.isDirectory());
		assertEquals(dir.getAbsolutePath(), result.getAbsolutePath());
	}

	@Test
	void a054_mkdirs_string_nestedDirectories() {
		var dir = new File(tempDir.toFile(), "dir1/dir2/dir3");
		var path = dir.getAbsolutePath();
		var result = FileUtils.mkdirs(path, false);
		assertTrue(dir.exists());
		assertTrue(dir.isDirectory());
		assertEquals(dir.getAbsolutePath(), result.getAbsolutePath());
	}

	@Test
	void a055_mkdirs_string_existingDirectory_cleanTrue() throws IOException {
		var dir = new File(tempDir.toFile(), "cleandir");
		dir.mkdirs();
		var f = new File(dir, "test.txt");
		f.createNewFile();
		var path = dir.getAbsolutePath();
		var result = FileUtils.mkdirs(path, true);
		assertTrue(dir.exists());
		assertFalse(f.exists());
		assertEquals(dir.getAbsolutePath(), result.getAbsolutePath());
	}

	@Test
	void a056_mkdirs_string_null() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileUtils.mkdirs((String)null, false);
		});
	}

	//====================================================================================================
	// modifyTimestamp(File)
	//====================================================================================================
	@Test
	void a057_modifyTimestamp() throws IOException, InterruptedException {
		var f = new File(tempDir.toFile(), "test.txt");
		f.createNewFile();
		var originalTime = f.lastModified();

		// Wait a bit to ensure time difference
		Thread.sleep(10);

		FileUtils.modifyTimestamp(f);
		var newTime = f.lastModified();
		assertTrue(newTime > originalTime || newTime == originalTime + 1000,
			"Expected newTime > originalTime or newTime == originalTime + 1000, but newTime=" + newTime + ", originalTime=" + originalTime);
	}

	@Test
	void a058_modifyTimestamp_nonexistentFile() {
		var f = new File(tempDir.toFile(), "nonexistent.txt");
		assertFalse(f.exists());
		assertThrowsWithMessage(RuntimeException.class, "Could not modify timestamp", () -> {
			FileUtils.modifyTimestamp(f);
		});
	}

	@Test
	void a059_modifyTimestamp_directory() throws InterruptedException {
		var dir = new File(tempDir.toFile(), "testdir");
		dir.mkdirs();
		var originalTime = dir.lastModified();

		// Wait a bit to ensure time difference
		Thread.sleep(10);

		FileUtils.modifyTimestamp(dir);
		var newTime = dir.lastModified();
		assertTrue(newTime > originalTime || newTime == originalTime + 1000);
	}
}

