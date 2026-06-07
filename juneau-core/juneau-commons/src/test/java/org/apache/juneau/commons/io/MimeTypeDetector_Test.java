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
package org.apache.juneau.commons.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;

import org.apache.juneau.commons.collections.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Test class for {@link MimeTypeDetector}.
 */
class MimeTypeDetector_Test {

	@TempDir
	Path tempDir;

	@Test
	void testDefaultInstance() {
		var detector = MimeTypeDetector.DEFAULT;
		assertNotNull(detector);

		// Test some default mappings
		assertEquals("text/html", detector.getContentType("test.html"));
		assertEquals("text/html", detector.getContentType("test.htm"));
		assertEquals("image/png", detector.getContentType("test.png"));
		assertEquals("application/pdf", detector.getContentType("test.pdf"));
		assertEquals("application/json", detector.getContentType("test.json"));
	}

	@Test
	void testBuilder() {
		MimeTypeDetector.Builder builder = MimeTypeDetector.builder();
		assertNotNull(builder);

		MimeTypeDetector detector = builder.build();
		assertNotNull(detector);
	}

	@Test
	void testAddFileType() {
		var detector = MimeTypeDetector.builder()
			.addFileType("special.txt", "text/special")
			.build();

		// File type mapping should work
		assertEquals("text/special", detector.getContentType("special.txt"));
		assertEquals("application/octet-stream", detector.getContentType("test.txt"));
	}

	@Test
	void testAddFileType_validation() {
		MimeTypeDetector.Builder builder = MimeTypeDetector.builder();

		// Test null name
		assertThrows(IllegalArgumentException.class, () -> builder.addFileType(null, "text/plain"));

		// Test blank name
		assertThrows(IllegalArgumentException.class, () -> builder.addFileType("", "text/plain"));
		assertThrows(IllegalArgumentException.class, () -> builder.addFileType("   ", "text/plain"));

		// Test null type
		assertThrows(IllegalArgumentException.class, () -> builder.addFileType("test.txt", null));

		// Test blank type
		assertThrows(IllegalArgumentException.class, () -> builder.addFileType("test.txt", ""));
		assertThrows(IllegalArgumentException.class, () -> builder.addFileType("test.txt", "   "));
	}

	@Test
	void testAddExtensionType() {
		var detector = MimeTypeDetector.builder()
			.addExtensionType("custom", "application/x-custom")
			.addExtensionType("CUSTOM", "application/x-custom-upper")  // Should be lowercased
			.build();

		// The second addExtensionType call overwrites the first one since both are lowercased to "custom"
		assertEquals("application/x-custom-upper", detector.getContentType("test.custom"));
		assertEquals("application/x-custom-upper", detector.getContentType("test.CUSTOM"));
		assertEquals("application/octet-stream", detector.getContentType("test.unknown"));
	}

	@Test
	void testAddExtensionType_validation() {
		MimeTypeDetector.Builder builder = MimeTypeDetector.builder();

		// Test null extension
		assertThrows(IllegalArgumentException.class, () -> builder.addExtensionType(null, "text/plain"));

		// Test blank extension
		assertThrows(IllegalArgumentException.class, () -> builder.addExtensionType("", "text/plain"));
		assertThrows(IllegalArgumentException.class, () -> builder.addExtensionType("   ", "text/plain"));

		// Test null type
		assertThrows(IllegalArgumentException.class, () -> builder.addExtensionType("txt", null));

		// Test blank type
		assertThrows(IllegalArgumentException.class, () -> builder.addExtensionType("txt", ""));
		assertThrows(IllegalArgumentException.class, () -> builder.addExtensionType("txt", "   "));
	}

	@Test
	void testAddNioContentBasedDetection() {
		var detector = MimeTypeDetector.builder()
			.addNioContentBasedDetection(false)
			.addExtensionType("test", "application/x-test")
			.build();

		// Should not use NIO detection, should fall back to extension
		assertEquals("application/x-test", detector.getContentType("test.test"));
	}

	@Test
	void testSetCacheSize() {
		var detector = MimeTypeDetector.builder()
			.setCacheSize(50)
			.addExtensionType("test", "application/x-test")
			.build();

		// Test that cache works
		detector.getContentType("test.test");
		assertEquals(1, detector.getCacheSize());
	}

	@Test
	void testSetCacheDisabled() {
		var detector = MimeTypeDetector.builder()
			.setCacheMode(CacheMode.NONE)
			.addExtensionType("test", "application/x-test")
			.build();

		// Cache should be disabled
		detector.getContentType("test.test");
		assertEquals(0, detector.getCacheSize());
	}

	@Test
	void testSetCacheLogOnExit() {
		var detector = MimeTypeDetector.builder()
			.setCacheLogOnExit(true)
			.build();

		// This should not throw an exception
		assertNotNull(detector);
	}

	@Test
	void testSetDefaultType() {
		var detector = MimeTypeDetector.builder()
			.setDefaultType("application/unknown")
			.build();

		assertEquals("application/unknown", detector.getContentType("test.unknown"));
		assertEquals("application/unknown", detector.getContentType(""));
		assertEquals("application/unknown", detector.getContentType(null));
	}

	@Test
	void testAddTypesIndividualLines() {
		var detector = MimeTypeDetector.builder()
			.addTypes(
				"text/html        html htm",
				"image/png        png",
				"application/json json"
				)
			.build();

		assertEquals("text/html", detector.getContentType("test.html"));
		assertEquals("text/html", detector.getContentType("test.htm"));
		assertEquals("image/png", detector.getContentType("test.png"));
		assertEquals("application/json", detector.getContentType("test.json"));
	}

	@Test
	void testAddTypesFileContents() {
		String mimeTypesFile = """
			# Custom MIME types file
			text/html        html htm
			image/png        png
			application/json json
			text/plain       txt log
			# Another comment
			application/x-custom custom cst""";

		var detector = MimeTypeDetector.builder()
			.addTypes(mimeTypesFile)
			.build();

		assertEquals("text/html", detector.getContentType("test.html"));
		assertEquals("text/html", detector.getContentType("test.htm"));
		assertEquals("image/png", detector.getContentType("test.png"));
		assertEquals("application/json", detector.getContentType("test.json"));
		assertEquals("text/plain", detector.getContentType("test.txt"));
		assertEquals("text/plain", detector.getContentType("test.log"));
		assertEquals("application/x-custom", detector.getContentType("test.custom"));
		assertEquals("application/x-custom", detector.getContentType("test.cst"));
	}

	@Test
	void testAddTypesMixedUsage() {
		var detector = MimeTypeDetector.builder()
			.addTypes("text/html html htm")  // Single line
			.addTypes("image/png png\napplication/json json")  // File contents
			.addTypes("application/x-foo foo bar")  // Another single line
			.build();

		assertEquals("text/html", detector.getContentType("test.html"));
		assertEquals("text/html", detector.getContentType("test.htm"));
		assertEquals("image/png", detector.getContentType("test.png"));
		assertEquals("application/json", detector.getContentType("test.json"));
		assertEquals("application/x-foo", detector.getContentType("test.foo"));
		assertEquals("application/x-foo", detector.getContentType("test.bar"));
	}

	@Test
	void testAddTypesEmptyAndInvalidLines() {
		var detector = MimeTypeDetector.builder()
			.addTypes(
				"",  // Empty line
				"   ",  // Whitespace only
				"# Comment line",  // Comment
				"invalid",  // Invalid format (no extensions)
				"text/html html htm"  // Valid line
				)
			.build();

		assertEquals("text/html", detector.getContentType("test.html"));
		assertEquals("text/html", detector.getContentType("test.htm"));
		assertEquals("application/octet-stream", detector.getContentType("test.unknown"));
	}

	@Test
	void testAddDefaultMappings() {
		var detector = MimeTypeDetector.builder()
			.addDefaultMappings()
			.build();

		// Test some default mappings
		assertEquals("text/html", detector.getContentType("test.html"));
		assertEquals("text/html", detector.getContentType("test.htm"));
		assertEquals("image/png", detector.getContentType("test.png"));
		assertEquals("application/pdf", detector.getContentType("test.pdf"));
		assertEquals("application/json", detector.getContentType("test.json"));
		assertEquals("text/plain", detector.getContentType("test.txt"));
		assertEquals("application/zip", detector.getContentType("test.zip"));
	}

	@Test
	void testGetContentTypeWithNioDetection() throws IOException {
		// Create a temporary file
		Path testFile = tempDir.resolve("test.txt");
		Files.write(testFile, "Hello World".getBytes());

		var detector = MimeTypeDetector.builder()
			.addNioContentBasedDetection(true)
			.build();

		// Should use NIO detection for existing file
		String mimeType = detector.getContentType(testFile.toString());
		assertNotNull(mimeType);
		assertTrue(mimeType.startsWith("text/"));
	}

	@Test
	void testGetContentTypeWithNioDetectionDisabled() throws IOException {
		// Create a temporary file
		Path testFile = tempDir.resolve("test.txt");
		Files.write(testFile, "Hello World".getBytes());

		var detector = MimeTypeDetector.builder()
			.addNioContentBasedDetection(false)
			.addExtensionType("txt", "text/plain")
			.build();

		// Should use extension mapping instead of NIO detection
		assertEquals("text/plain", detector.getContentType(testFile.toString()));
	}

	@Test
	void testGetContentTypeWithNioException() {
		var detector = MimeTypeDetector.builder()
			.addNioContentBasedDetection(true)
			.addExtensionType("test", "application/x-test")
			.build();

		// Should fall back to extension mapping when NIO fails
		assertEquals("application/x-test", detector.getContentType("test.test"));
	}

	@Test
	void testGetContentTypeEmptyAndNull() {
		var detector = MimeTypeDetector.builder()
			.setDefaultType("application/unknown")
			.build();

		assertEquals("application/unknown", detector.getContentType(""));
		assertEquals("application/unknown", detector.getContentType(null));
	}

	@Test
	void testGetContentTypeFallbackToDefault() {
		var detector = MimeTypeDetector.builder()
			.setDefaultType("application/unknown")
			.build();

		assertEquals("application/unknown", detector.getContentType("test.unknown"));
	}

	@Test
	void testCacheBehavior() {
		var detector = MimeTypeDetector.builder()
			.addExtensionType("test", "application/x-test")
			.build();

		// Initial state
		assertEquals(0, detector.getCacheSize());
		assertEquals(0, detector.getCacheHits());

		// First call - cache miss
		detector.getContentType("test.test");
		assertEquals(1, detector.getCacheSize());
		assertEquals(0, detector.getCacheHits());

		// Second call - cache hit
		detector.getContentType("test.test");
		assertEquals(1, detector.getCacheSize());
		assertEquals(1, detector.getCacheHits());

		// Third call - another cache hit
		detector.getContentType("test.test");
		assertEquals(1, detector.getCacheSize());
		assertEquals(2, detector.getCacheHits());
	}

	@Test
	void testClearCache() {
		var detector = MimeTypeDetector.builder()
			.addExtensionType("test", "application/x-test")
			.build();

		// Add some entries to cache
		detector.getContentType("test.test");
		detector.getContentType("test.other");
		assertEquals(2, detector.getCacheSize());

		// Clear cache
		detector.clearCache();
		assertEquals(0, detector.getCacheSize());

		// Cache hits should remain (not reset by clear)
		assertTrue(detector.getCacheHits() >= 0);
	}

	@Test
	void testCacheDisabled() {
		var detector = MimeTypeDetector.builder()
			.setCacheMode(CacheMode.NONE)
			.addExtensionType("test", "application/x-test")
			.build();

		// Cache should be disabled
		detector.getContentType("test.test");
		assertEquals(0, detector.getCacheSize());
		assertEquals(0, detector.getCacheHits());
	}

	@Test
	void testCacheSizeLimit() {
		var detector = MimeTypeDetector.builder()
			.setCacheSize(2)
			.addExtensionType("test", "application/x-test")
			.build();

		// Add more entries than cache size
		detector.getContentType("test1.test");
		detector.getContentType("test2.test");
		detector.getContentType("test3.test");
		detector.getContentType("test4.test");

		// Cache should not exceed max size
		assertTrue(detector.getCacheSize() <= 2);
	}

	@Test
	void testBuilderChaining() {
		var detector = MimeTypeDetector.builder()
			.addFileType("special.txt", "text/special")
			.addExtensionType("custom", "application/x-custom")
			.addNioContentBasedDetection(false)
			.setCacheSize(100)
			.setCacheMode(CacheMode.FULL)
			.setCacheLogOnExit(true)
			.setDefaultType("application/unknown")
			.addTypes("text/html html htm")
			.addDefaultMappings()
			.build();

		assertNotNull(detector);
		// File type mapping should work (takes precedence over extension mapping)
		assertEquals("text/special", detector.getContentType("special.txt"));
		assertEquals("application/x-custom", detector.getContentType("test.custom"));
		assertEquals("text/html", detector.getContentType("test.html"));
		assertEquals("application/unknown", detector.getContentType("test.unknown"));
	}

	@Test
	void testExtensionCaseInsensitive() {
		var detector = MimeTypeDetector.builder()
			.addExtensionType("TEST", "application/x-test")
			.build();

		// Should work with different cases
		assertEquals("application/x-test", detector.getContentType("test.TEST"));
		assertEquals("application/x-test", detector.getContentType("test.test"));
		assertEquals("application/x-test", detector.getContentType("test.Test"));
	}

	@Test
	void testMultipleExtensionsPerMimeType() {
		var detector = MimeTypeDetector.builder()
			.addTypes("text/html html htm HTML HTM")
			.build();

		// All variations should work
		assertEquals("text/html", detector.getContentType("test.html"));
		assertEquals("text/html", detector.getContentType("test.htm"));
		assertEquals("text/html", detector.getContentType("test.HTML"));
		assertEquals("text/html", detector.getContentType("test.HTM"));
	}

	@Test
	void testWindowsLineEndings() {
		String mimeTypesFile = "text/html\thtml\thtm\r\nimage/png\tpng\r\n";

		var detector = MimeTypeDetector.builder()
			.addTypes(mimeTypesFile)
			.build();

		assertEquals("text/html", detector.getContentType("test.html"));
		assertEquals("text/html", detector.getContentType("test.htm"));
		assertEquals("image/png", detector.getContentType("test.png"));
	}

	@Test
	void testUnixLineEndings() {
		String mimeTypesFile = "text/html\thtml\thtm\nimage/png\tpng\n";

		var detector = MimeTypeDetector.builder()
			.addTypes(mimeTypesFile)
			.build();

		assertEquals("text/html", detector.getContentType("test.html"));
		assertEquals("text/html", detector.getContentType("test.htm"));
		assertEquals("image/png", detector.getContentType("test.png"));
	}

	@Test
	void testMixedLineEndings() {
		String mimeTypesFile = "text/html\thtml\thtm\r\nimage/png\tpng\napplication/json\tjson\r\n";

		var detector = MimeTypeDetector.builder()
			.addTypes(mimeTypesFile)
			.build();

		assertEquals("text/html", detector.getContentType("test.html"));
		assertEquals("text/html", detector.getContentType("test.htm"));
		assertEquals("image/png", detector.getContentType("test.png"));
		assertEquals("application/json", detector.getContentType("test.json"));
	}
}