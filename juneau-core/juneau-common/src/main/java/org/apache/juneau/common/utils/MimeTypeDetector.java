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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.Utils.*;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.common.collections.*;

/**
 * A lightweight MIME type detector that doesn't require Jakarta Activation.
 *
 * <p>
 * This class provides MIME type detection using multiple strategies:
 * <ol>
 *   <li>Java NIO's {@link Files#probeContentType(Path)} for content-based detection
 *   <li>Extension-based mapping for common file types
 *   <li>Configurable MIME type mappings
 * </ol>
 *
 * <p>
 * This class is thread-safe and can be used as a drop-in replacement for
 * {@link MimetypesFileTypeMap} without requiring Jakarta Activation.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Basic usage</jc>
 * 	MimeTypeDetector <jv>detector</jv> = MimeTypeDetector.builder().build();
 * 	String <jv>mimeType</jv> = <jv>detector</jv>.getContentType(<js>"document.pdf"</js>);
 * 	<jc>// mimeType = "application/pdf"</jc>
 *
 * 	<jc>// Custom configuration</jc>
 * 	MimeTypeDetector <jv>custom</jv> = MimeTypeDetector.builder()
 * 		.addExtensionType(<js>"custom"</js>, <js>"application/x-custom"</js>)
 * 		.addTypes(<js>"application/x-foo:foo,bar"</js>)
 * 		.setCacheSize(500)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jm'>{@link Files#probeContentType(Path)}
 * 	<li class='jm'>{@link MimetypesFileTypeMap}
 * </ul>
 */
public class MimeTypeDetector {
	/**
	 * Default MIME type detector instance.
	 */
	public static final MimeTypeDetector DEFAULT = builder().addDefaultMappings().build();

	/**
	 * Builder class for creating MimeTypeDetector instances.
	 */
	public static class Builder {
		private final Map<String,String> extMap = new ConcurrentHashMap<>();
		private final Map<String,String> fileMap = new ConcurrentHashMap<>();
		private boolean nioContentBasedDetection = true;
		private int cacheSize = 1000;
		private boolean cacheDisabled = false;
		private boolean cacheLogOnExit = false;
		private String defaultType = "application/octet-stream";

		/**
		 * Adds a file type mapping.
		 *
		 * @param name The file name or path pattern.
		 * @param type The MIME type.
		 * @return This builder.
		 * @throws IllegalArgumentException If name or type is null or blank.
		 */
		public Builder addFileType(String name, String type) {
			AssertionUtils.assertArgNotNullOrBlank("name", name);
			AssertionUtils.assertArgNotNullOrBlank("type", type);
			fileMap.put(name, type);
			return this;
		}

		/**
		 * Adds an extension type mapping.
		 *
		 * @param ext The file extension.
		 * @param type The MIME type.
		 * @return This builder.
		 * @throws IllegalArgumentException If ext or type is null or blank.
		 */
		public Builder addExtensionType(String ext, String type) {
			AssertionUtils.assertArgNotNullOrBlank("ext", ext);
			AssertionUtils.assertArgNotNullOrBlank("type", type);
			extMap.put(ext.toLowerCase(), type);
			return this;
		}

		/**
		 * Enables or disables NIO content-based detection.
		 *
		 * @param value Whether to enable NIO content-based detection.
		 * @return This builder.
		 */
		public Builder addNioContentBasedDetection(boolean value) {
			nioContentBasedDetection = value;
			return this;
		}

		/**
		 * Sets the cache size.
		 *
		 * @param value The maximum cache size.
		 * @return This builder.
		 */
		public Builder setCacheSize(int value) {
			cacheSize = value;
			return this;
		}

		/**
		 * Enables or disables the cache.
		 *
		 * @param value Whether to disable the cache.
		 * @return This builder.
		 */
		public Builder setCacheDisabled(boolean value) {
			cacheDisabled = value;
			return this;
		}

		/**
		 * Enables or disables cache logging on exit.
		 *
		 * @param value Whether to log cache statistics on exit.
		 * @return This builder.
		 */
		public Builder setCacheLogOnExit(boolean value) {
			cacheLogOnExit = value;
			return this;
		}

		/**
		 * Sets the default MIME type for unknown files.
		 *
		 * @param value The default MIME type.
		 * @return This builder.
		 */
		public Builder setDefaultType(String value) {
			defaultType = value;
			return this;
		}

		/**
		 * Adds MIME type mappings from mime.types file format.
		 *
		 * <p>
		 * Each line should follow the format:
		 * <pre>
		 * text/html        html htm
		 * image/png        png
		 * application/json json
		 * </pre>
		 *
		 * <p>
		 * This method supports both individual lines as varargs and entire
		 * mime.types file contents as a single string (which will be split on newlines).
		 *
		 * @param mimeTypesLines The MIME types lines or file contents.
		 * @return This builder.
		 */
		public Builder addTypes(String...mimeTypesLines) {
			for (String input : mimeTypesLines) {
				if (Utils.isNotEmpty(input)) {
					// Split on newlines to handle both individual lines and file contents
					var lines = input.split("\\r?\\n");
					for (String line : lines) {
						if (Utils.isNotEmpty(line) && ! line.trim().startsWith("#")) {
							var parts = line.trim().split("\\s+");
							if (parts.length >= 2) {
								var mimeType = parts[0];
								for (int i = 1; i < parts.length; i++) {
									addExtensionType(parts[i], mimeType);
								}
							}
						}
					}
				}
			}
			return this;
		}

		/**
		 * Adds the default MIME type mappings.
		 *
		 * @return This builder.
		 */
		public Builder addDefaultMappings() {
			return addTypes("application/epub+zip epub", "application/java-archive jar", "application/javascript js", "application/json json", "application/msword doc", "application/ogg ogx",
				"application/pdf pdf", "application/rtf rtf", "application/vnd.amazon.ebook azw", "application/vnd.apple.installer+xml mpkg", "application/vnd.mozilla.xul+xml xul",
				"application/vnd.ms-excel xls", "application/vnd.ms-powerpoint ppt", "application/vnd.oasis.opendocument.presentation odp", "application/vnd.oasis.opendocument.spreadsheet ods",
				"application/vnd.oasis.opendocument.text odt", "application/vnd.visio vsd", "application/x-7z-compressed 7z", "application/x-abiword abw", "application/x-bzip bz",
				"application/x-bzip2 bz2", "application/x-csh csh", "application/x-rar-compressed rar", "application/x-sh sh", "application/x-shockwave-flash swf", "application/x-tar tar",
				"application/xhtml+xml xhtml", "application/xml xml", "application/zip zip", "audio/aac aac", "audio/midi mid midi", "audio/ogg oga", "audio/webm weba", "audio/x-wav wav",
				"font/ttf ttf", "font/woff woff", "font/woff2 woff2", "image/gif gif", "image/jpeg jpeg jpg", "image/png png", "image/svg+xml svg", "image/tiff tif tiff", "image/webp webp",
				"image/x-icon ico", "text/calendar ics", "text/css css", "text/csv csv", "text/html htm html", "text/plain txt", "video/3gpp 3gp", "video/3gpp2 3g2", "video/mpeg mpeg",
				"video/ogg ogv", "video/webm webm", "video/x-msvideo avi");
		}

		/**
		 * Builds the MimeTypeDetector instance.
		 *
		 * @return A new MimeTypeDetector instance.
		 */
		public MimeTypeDetector build() {
			return new MimeTypeDetector(this);
		}
	}

	/**
	 * Creates a new builder for MimeTypeDetector.
	 *
	 * @return A new builder.
	 */
	public static Builder builder() {
		return new Builder();
	}

	private final Map<String,String> extMap;
	private final Map<String,String> fileMap;
	private final Cache<String,String> cache;
	private final boolean nioContentBasedDetection;
	private final String defaultType;

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	private MimeTypeDetector(Builder builder) {
		this.extMap = new ConcurrentHashMap<>(builder.extMap);
		this.fileMap = new ConcurrentHashMap<>(builder.fileMap);
		this.nioContentBasedDetection = builder.nioContentBasedDetection;
		this.defaultType = builder.defaultType;

		// Create cache for file-based lookups
		var cacheBuilder = Cache.of(String.class, String.class).maxSize(builder.cacheSize);

		if (builder.cacheDisabled) {
			cacheBuilder.disableCaching();
		}
		if (builder.cacheLogOnExit) {
			cacheBuilder.logOnExit();
		}

		this.cache = cacheBuilder.build();
	}

	/**
	 * Determines the MIME type of a file based on its name or path.
	 *
	 * <p>
	 * This method uses multiple strategies to determine the MIME type:
	 * <ol>
	 *   <li>Checks cache first for previously determined MIME types
	 *   <li>If the file exists, uses {@link Files#probeContentType(Path)} for content-based detection
	 *   <li>Falls back to extension-based mapping for common file types
	 *   <li>Returns the configured default type for unknown types
	 * </ol>
	 *
	 * <p>
	 * Results are cached to improve performance for repeated lookups of the same files.
	 *
	 * @param fileName The name or path of the file.
	 * @return The MIME type of the file, or the default type if unknown.
	 */
	public String getContentType(String fileName) {
		if (Utils.isEmpty(fileName)) {
			return defaultType;
		}

		// Check file map first (for specific file mappings)
		var fileMimeType = fileMap.get(fileName);
		if (nn(fileMimeType)) {
			return fileMimeType;
		}

		// Use cache with supplier for automatic cache management
		return cache.get(fileName, () -> determineMimeType(fileName));
	}

	/**
	 * Determines the MIME type without caching (internal method).
	 *
	 * @param fileName The name or path of the file.
	 * @return The MIME type of the file.
	 */
	private String determineMimeType(String fileName) {
		// Try Java NIO's content-based detection first
		if (nioContentBasedDetection) {
			try {
				var path = Paths.get(fileName);
				if (Files.exists(path)) {
					var contentType = Files.probeContentType(path);
					if (Utils.isNotEmpty(contentType)) {
						return contentType;
					}
				}
			} catch (Exception e) {
				// Fall back to extension-based detection
			}
		}

		// Fall back to extension-based detection
		var extension = FileUtils.getExtension(fileName);
		if (Utils.isNotEmpty(extension)) {
			var mimeType = extMap.get(extension.toLowerCase());
			if (nn(mimeType)) {
				return mimeType;
			}
		}

		// Default fallback
		return defaultType;
	}

	/**
	 * Clears the MIME type cache.
	 *
	 * <p>
	 * This method can be called to free memory or when you want to ensure
	 * fresh MIME type detection (e.g., after files have been modified).
	 */
	public void clearCache() {
		cache.clear();
	}

	/**
	 * Returns the current cache size.
	 *
	 * @return The number of cached MIME type entries.
	 */
	public int getCacheSize() { return cache.size(); }

	/**
	 * Returns the number of cache hits since the cache was created.
	 *
	 * @return The number of cache hits.
	 */
	public int getCacheHits() { return cache.getCacheHits(); }
}