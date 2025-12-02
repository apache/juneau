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

import java.io.*;
import java.nio.charset.*;

/**
 * A fluent builder for creating {@link Reader} instances from files with configurable character encoding.
 *
 * <p>
 * This builder provides a convenient way to create file readers with custom character encodings
 * and optional handling for missing files. It's particularly useful when you need to read files
 * with specific encodings or handle cases where files may not exist.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Fluent API - all methods return <c>this</c> for method chaining
 * 	<li>Character encoding support - specify custom charset for file reading
 * 	<li>Missing file handling - optional support for returning empty reader when file doesn't exist
 * 	<li>Multiple file specification methods - accept File, String path, or Path
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Reading files with specific character encodings (UTF-8, ISO-8859-1, etc.)
 * 	<li>Handling optional configuration files that may not exist
 * 	<li>Creating readers with consistent encoding across an application
 * 	<li>Reading files where encoding must be explicitly specified
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Basic usage</jc>
 * 	Reader <jv>reader</jv> = FileReaderBuilder.<jsm>create</jsm>()
 * 		.file(<js>"/path/to/file.txt"</js>)
 * 		.charset(<js>"UTF-8"</js>)
 * 		.build();
 *
 * 	<jc>// With missing file handling</jc>
 * 	Reader <jv>reader2</jv> = FileReaderBuilder.<jsm>create</jsm>()
 * 		.file(<js>"optional-config.properties"</js>)
 * 		.allowNoFile()
 * 		.build();  <jc>// Returns empty StringReader if file doesn't exist</jc>
 *
 * 	<jc>// Using File object</jc>
 * 	File <jv>f</jv> = <jk>new</jk> File(<js>"data.txt"</js>);
 * 	Reader <jv>reader3</jv> = FileReaderBuilder.<jsm>create</jsm>(<jv>f</jv>)
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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FileWriterBuilder} - Builder for file writers
 * 	<li class='jc'>{@link PathReaderBuilder} - Builder for Path-based readers
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonIO">juneau-common-io</a>
 * </ul>
 */
public class FileReaderBuilder {

	/**
	 * Creates a new builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Reader <jv>reader</jv> = FileReaderBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"data.txt"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @return A new builder instance.
	 */
	public static FileReaderBuilder create() {
		return new FileReaderBuilder();
	}

	/**
	 * Creates a new builder initialized with the specified file.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	File <jv>file</jv> = <jk>new</jk> File(<js>"config.properties"</js>);
	 * 	Reader <jv>reader</jv> = FileReaderBuilder.<jsm>create</jsm>(<jv>file</jv>)
	 * 		.charset(<js>"UTF-8"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param file The file to read from.
	 * @return A new builder instance initialized with the specified file.
	 */
	public static FileReaderBuilder create(File file) {
		return new FileReaderBuilder().file(file);
	}

	private File file;

	private Charset cs = Charset.defaultCharset();

	private boolean allowNoFile;

	/**
	 * Enables handling of missing files by returning an empty reader instead of throwing an exception.
	 *
	 * <p>
	 * When this option is enabled, if the file is <jk>null</jk> or does not exist, the {@link #build()}
	 * method will return a {@link StringReader} with empty content instead of throwing a
	 * {@link FileNotFoundException}. This is useful for optional configuration files.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Without allowNoFile - throws exception if file doesn't exist</jc>
	 * 	Reader <jv>reader1</jv> = FileReaderBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"required.txt"</js>)
	 * 		.build();  <jc>// Throws FileNotFoundException if file missing</jc>
	 *
	 * 	<jc>// With allowNoFile - returns empty reader if file doesn't exist</jc>
	 * 	Reader <jv>reader2</jv> = FileReaderBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"optional.txt"</js>)
	 * 		.allowNoFile()
	 * 		.build();  <jc>// Returns empty StringReader if file missing</jc>
	 * </p>
	 *
	 * @return This object for method chaining.
	 */
	public FileReaderBuilder allowNoFile() {
		this.allowNoFile = true;
		return this;
	}

	/**
	 * Creates a new {@link Reader} for reading from the configured file.
	 *
	 * <p>
	 * If {@link #allowNoFile()} was called and the file is <jk>null</jk> or does not exist,
	 * this method returns an empty {@link StringReader}. Otherwise, it creates an
	 * {@link InputStreamReader} with the specified character encoding.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>try</jk> (Reader <jv>reader</jv> = FileReaderBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"data.txt"</js>)
	 * 		.charset(<js>"UTF-8"</js>)
	 * 		.build()) {
	 * 		<jc>// Read from file</jc>
	 * 	}
	 * </p>
	 *
	 * @return A new {@link Reader} for reading from the file.
	 * @throws FileNotFoundException If the file could not be found and {@link #allowNoFile()} was not called.
	 */
	public Reader build() throws FileNotFoundException {
		if (allowNoFile && (file == null || ! file.exists()))
			return new StringReader("");
		assertArgNotNull("file", file);
		return new InputStreamReader(new FileInputStream(file), cs != null ? cs : Charset.defaultCharset());
	}

	/**
	 * Sets the character encoding for reading the file.
	 *
	 * <p>
	 * If not specified, the system's default charset ({@link Charset#defaultCharset()}) is used.
	 * Specifying the encoding is important when reading files that were written with a specific
	 * character encoding.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Reader <jv>reader</jv> = FileReaderBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"data.txt"</js>)
	 * 		.charset(StandardCharsets.UTF_8)
	 * 		.build();
	 * </p>
	 *
	 * @param cs The character encoding to use. The default is {@link Charset#defaultCharset()}.
	 * @return This object for method chaining.
	 */
	public FileReaderBuilder charset(Charset cs) {
		this.cs = cs;
		return this;
	}

	/**
	 * Sets the character encoding for reading the file by charset name.
	 *
	 * <p>
	 * This is a convenience method that accepts a charset name string and converts it to a
	 * {@link Charset} using {@link Charset#forName(String)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Reader <jv>reader</jv> = FileReaderBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"data.txt"</js>)
	 * 		.charset(<js>"UTF-8"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param cs The character encoding name (e.g., <js>"UTF-8"</js>, <js>"ISO-8859-1"</js>).
	 *           The default is {@link Charset#defaultCharset()}.
	 *           Must not be <jk>null</jk>.
	 * @return This object for method chaining.
	 */
	public FileReaderBuilder charset(String cs) {
		this.cs = Charset.forName(assertArgNotNull("cs", cs));
		return this;
	}

	/**
	 * Sets the file to read from.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	File <jv>f</jv> = <jk>new</jk> File(<js>"config.properties"</js>);
	 * 	Reader <jv>reader</jv> = FileReaderBuilder.<jsm>create</jsm>()
	 * 		.file(<jv>f</jv>)
	 * 		.build();
	 * </p>
	 *
	 * @param value The file to read from.
	 * @return This object for method chaining.
	 */
	public FileReaderBuilder file(File value) {
		this.file = value;
		return this;
	}

	/**
	 * Sets the file path to read from.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Reader <jv>reader</jv> = FileReaderBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"/path/to/file.txt"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param path The file path to read from.
	 * @return This object for method chaining.
	 */
	public FileReaderBuilder file(String path) {
		this.file = new File(path);
		return this;
	}
}