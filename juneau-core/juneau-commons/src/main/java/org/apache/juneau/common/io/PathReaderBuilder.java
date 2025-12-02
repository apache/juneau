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
package org.apache.juneau.common.io;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;

/**
 * A fluent builder for creating {@link Reader} instances from {@link Path} objects with configurable character encoding.
 *
 * <p>
 * This builder provides a convenient way to create readers from NIO {@link Path} objects with custom
 * character encodings and optional handling for missing files. It's similar to {@link FileReaderBuilder}
 * but works with the modern NIO Path API instead of the legacy File API.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Fluent API - all methods return <c>this</c> for method chaining
 * 	<li>NIO Path support - works with modern {@link Path} API
 * 	<li>Character encoding support - specify custom charset for file reading
 * 	<li>Missing file handling - optional support for returning empty reader when file doesn't exist
 * 	<li>Multiple path specification methods - accept Path or String path
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Reading files with specific character encodings using NIO Path API
 * 	<li>Handling optional configuration files that may not exist
 * 	<li>Creating readers with consistent encoding across an application
 * 	<li>Working with NIO-based file operations
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Basic usage</jc>
 * 	Reader <jv>reader</jv> = PathReaderBuilder.<jsm>create</jsm>()
 * 		.path(Paths.get(<js>"/path/to/file.txt"</js>))
 * 		.charset(<js>"UTF-8"</js>)
 * 		.build();
 *
 * 	<jc>// With missing file handling</jc>
 * 	Reader <jv>reader2</jv> = PathReaderBuilder.<jsm>create</jsm>()
 * 		.path(<js>"optional-config.properties"</js>)
 * 		.allowNoFile()
 * 		.build();  <jc>// Returns empty StringReader if file doesn't exist</jc>
 *
 * 	<jc>// Using Path object</jc>
 * 	Path <jv>path</jv> = Paths.get(<js>"data.txt"</js>);
 * 	Reader <jv>reader3</jv> = PathReaderBuilder.<jsm>create</jsm>(<jv>path</jv>)
 * 		.charset(StandardCharsets.UTF_8)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>Character Encoding:</h5>
 * <p>
 * By default, the builder uses the system's default charset ({@link Charset#defaultCharset()}).
 * You can specify a custom charset using {@link #charset(Charset)} or {@link #charset(String)}.
 * This is important when reading files that were written with a specific encoding.
 *
 * <h5 class='section'>Comparison with FileReaderBuilder:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>FileReaderBuilder:</b> Works with legacy {@link File} API
 * 	<li><b>PathReaderBuilder:</b> Works with modern NIO {@link Path} API
 * 	<li><b>FileReaderBuilder:</b> Uses {@link FileInputStream}
 * 	<li><b>PathReaderBuilder:</b> Uses {@link Files#newBufferedReader(Path, Charset)}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FileReaderBuilder} - Builder for File-based readers
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonIO">juneau-common-io</a>
 * </ul>
 *
 * @since 9.1.0
 */
public class PathReaderBuilder {

	/**
	 * Creates a new builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Reader <jv>reader</jv> = PathReaderBuilder.<jsm>create</jsm>()
	 * 		.path(Paths.get(<js>"data.txt"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @return A new builder instance.
	 */
	public static PathReaderBuilder create() {
		return new PathReaderBuilder();
	}

	/**
	 * Creates a new builder initialized with the specified path.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Path <jv>path</jv> = Paths.get(<js>"config.properties"</js>);
	 * 	Reader <jv>reader</jv> = PathReaderBuilder.<jsm>create</jsm>(<jv>path</jv>)
	 * 		.charset(<js>"UTF-8"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param path The path to read from.
	 * @return A new builder instance initialized with the specified path.
	 */
	public static PathReaderBuilder create(Path path) {
		return new PathReaderBuilder().path(path);
	}

	private Path path;

	private Charset charset = Charset.defaultCharset();

	private boolean allowNoFile;

	/**
	 * Enables handling of missing files by returning an empty reader instead of throwing an exception.
	 *
	 * <p>
	 * When this option is enabled, if the path is <jk>null</jk> or does not exist, the {@link #build()}
	 * method will return a {@link StringReader} with empty content instead of throwing an
	 * {@link IOException}. This is useful for optional configuration files.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Without allowNoFile - throws exception if file doesn't exist</jc>
	 * 	Reader <jv>reader1</jv> = PathReaderBuilder.<jsm>create</jsm>()
	 * 		.path(Paths.get(<js>"required.txt"</js>))
	 * 		.build();  <jc>// Throws NoSuchFileException if file missing</jc>
	 *
	 * 	<jc>// With allowNoFile - returns empty reader if file doesn't exist</jc>
	 * 	Reader <jv>reader2</jv> = PathReaderBuilder.<jsm>create</jsm>()
	 * 		.path(Paths.get(<js>"optional.txt"</js>))
	 * 		.allowNoFile()
	 * 		.build();  <jc>// Returns empty StringReader if file missing</jc>
	 * </p>
	 *
	 * @return This object for method chaining.
	 */
	public PathReaderBuilder allowNoFile() {
		this.allowNoFile = true;
		return this;
	}

	/**
	 * Creates a new {@link Reader} for reading from the configured path.
	 *
	 * <p>
	 * If {@link #allowNoFile()} was called and the path is <jk>null</jk> or does not exist,
	 * this method returns an empty {@link StringReader}. Otherwise, it creates a buffered reader
	 * using {@link Files#newBufferedReader(Path, Charset)} with the specified character encoding.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>try</jk> (Reader <jv>reader</jv> = PathReaderBuilder.<jsm>create</jsm>()
	 * 		.path(Paths.get(<js>"data.txt"</js>))
	 * 		.charset(<js>"UTF-8"</js>)
	 * 		.build()) {
	 * 		<jc>// Read from file</jc>
	 * 	}
	 * </p>
	 *
	 * @return A new {@link Reader} for reading from the path.
	 * @throws IllegalStateException If no path is configured and {@link #allowNoFile()} was not called.
	 * @throws NoSuchFileException If the path does not exist and {@link #allowNoFile()} was not called.
	 * @throws IOException If an I/O error occurs opening the path.
	 */
	public Reader build() throws IOException {
		if (! allowNoFile && path == null) {
			throw new IllegalStateException("No path");
		}
		if (! allowNoFile && ! Files.exists(path)) {
			throw new NoSuchFileException(path.toString());
		}
		return allowNoFile ? new StringReader("") : Files.newBufferedReader(path, opt(charset).orElse(Charset.defaultCharset()));
	}

	/**
	 * Sets the character encoding for reading the path.
	 *
	 * <p>
	 * If not specified, the system's default charset ({@link Charset#defaultCharset()}) is used.
	 * Specifying the encoding is important when reading files that were written with a specific
	 * character encoding. Passing <jk>null</jk> resets to the default charset.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Reader <jv>reader</jv> = PathReaderBuilder.<jsm>create</jsm>()
	 * 		.path(Paths.get(<js>"data.txt"</js>))
	 * 		.charset(StandardCharsets.UTF_8)
	 * 		.build();
	 * </p>
	 *
	 * @param charset The character encoding to use. The default is {@link Charset#defaultCharset()}.
	 *                <jk>null</jk> resets to the default.
	 * @return This object for method chaining.
	 */
	public PathReaderBuilder charset(Charset charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * Sets the character encoding for reading the path by charset name.
	 *
	 * <p>
	 * This is a convenience method that accepts a charset name string and converts it to a
	 * {@link Charset} using {@link Charset#forName(String)}. Passing <jk>null</jk> resets to
	 * the default charset.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Reader <jv>reader</jv> = PathReaderBuilder.<jsm>create</jsm>()
	 * 		.path(Paths.get(<js>"data.txt"</js>))
	 * 		.charset(<js>"UTF-8"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param charset The character encoding name (e.g., <js>"UTF-8"</js>, <js>"ISO-8859-1"</js>).
	 *                The default is {@link Charset#defaultCharset()}.
	 *                <jk>null</jk> resets to the default.
	 * @return This object for method chaining.
	 */
	public PathReaderBuilder charset(String charset) {
		this.charset = nn(charset) ? Charset.forName(charset) : null;
		return this;
	}

	/**
	 * Sets the path to read from.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Path <jv>p</jv> = Paths.get(<js>"config.properties"</js>);
	 * 	Reader <jv>reader</jv> = PathReaderBuilder.<jsm>create</jsm>()
	 * 		.path(<jv>p</jv>)
	 * 		.build();
	 * </p>
	 *
	 * @param path The path to read from.
	 * @return This object for method chaining.
	 */
	public PathReaderBuilder path(Path path) {
		this.path = path;
		return this;
	}

	/**
	 * Sets the path to read from by string path.
	 *
	 * <p>
	 * This is a convenience method that converts a string path to a {@link Path} using
	 * {@link Paths#get(String, String...)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Reader <jv>reader</jv> = PathReaderBuilder.<jsm>create</jsm>()
	 * 		.path(<js>"/path/to/file.txt"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param path The file path to read from. Must not be <jk>null</jk>.
	 * @return This object for method chaining.
	 */
	public PathReaderBuilder path(String path) {
		this.path = Paths.get(assertArgNotNull("path", path));
		return this;
	}
}