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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.nio.charset.*;

/**
 * A fluent builder for creating {@link Writer} instances for writing to files with configurable options.
 *
 * <p>
 * This builder provides a convenient way to create file writers with custom character encodings,
 * append mode, and buffering options. It's particularly useful when you need to write files with
 * specific encodings or control whether to append to existing files.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Fluent API - all methods return <c>this</c> for method chaining
 * 	<li>Character encoding support - specify custom charset for file writing
 * 	<li>Append mode - optionally append to existing files instead of overwriting
 * 	<li>Buffering support - optional buffering for improved performance
 * 	<li>Multiple file specification methods - accept File or String path
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Writing files with specific character encodings (UTF-8, ISO-8859-1, etc.)
 * 	<li>Appending to log files or data files
 * 	<li>Creating writers with consistent encoding across an application
 * 	<li>Writing files where buffering improves performance
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Basic usage</jc>
 * 	Writer <jv>writer</jv> = FileWriterBuilder.<jsm>create</jsm>()
 * 		.file(<js>"/path/to/file.txt"</js>)
 * 		.charset(<js>"UTF-8"</js>)
 * 		.build();
 *
 * 	<jc>// Append mode</jc>
 * 	Writer <jv>logWriter</jv> = FileWriterBuilder.<jsm>create</jsm>()
 * 		.file(<js>"app.log"</js>)
 * 		.append()
 * 		.build();
 *
 * 	<jc>// With buffering</jc>
 * 	Writer <jv>bufferedWriter</jv> = FileWriterBuilder.<jsm>create</jsm>()
 * 		.file(<js>"output.txt"</js>)
 * 		.buffered()
 * 		.charset(StandardCharsets.UTF_8)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>Character Encoding:</h5>
 * <p>
 * By default, the builder uses the system's default charset ({@link Charset#defaultCharset()}).
 * You can specify a custom charset using {@link #charset(Charset)} or {@link #charset(String)}.
 * This is important when writing files that need to be read with a specific encoding.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FileReaderBuilder} - Builder for file readers
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonIO">juneau-common-io</a>
 * </ul>
 */
public class FileWriterBuilder {

	/**
	 * Creates a new builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Writer <jv>writer</jv> = FileWriterBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"output.txt"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @return A new builder instance.
	 */
	public static FileWriterBuilder create() {
		return new FileWriterBuilder();
	}

	/**
	 * Creates a new builder initialized with the specified file.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	File <jv>file</jv> = <jk>new</jk> File(<js>"output.txt"</js>);
	 * 	Writer <jv>writer</jv> = FileWriterBuilder.<jsm>create</jsm>(<jv>file</jv>)
	 * 		.charset(<js>"UTF-8"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param file The file to write to.
	 * @return A new builder instance initialized with the specified file.
	 */
	public static FileWriterBuilder create(File file) {
		return new FileWriterBuilder().file(file);
	}

	/**
	 * Creates a new builder initialized with the specified file path.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Writer <jv>writer</jv> = FileWriterBuilder.<jsm>create</jsm>(<js>"/path/to/output.txt"</js>)
	 * 		.append()
	 * 		.build();
	 * </p>
	 *
	 * @param path The file path to write to.
	 * @return A new builder instance initialized with the specified path.
	 */
	public static FileWriterBuilder create(String path) {
		return new FileWriterBuilder().file(path);
	}

	private File file;

	private Charset cs = Charset.defaultCharset();

	private boolean append, buffered;

	/**
	 * Enables append mode, which appends to the file instead of overwriting it.
	 *
	 * <p>
	 * When append mode is enabled, data written to the file will be appended to the end of the
	 * existing file content rather than overwriting it. This is useful for log files or data
	 * files where you want to preserve existing content.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Append to log file</jc>
	 * 	Writer <jv>logWriter</jv> = FileWriterBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"app.log"</js>)
	 * 		.append()
	 * 		.build();
	 * 	<jv>logWriter</jv>.write(<js>"New log entry\n"</js>);  <jc>// Appends to existing content</jc>
	 * </p>
	 *
	 * @return This object for method chaining.
	 */
	public FileWriterBuilder append() {
		this.append = true;
		return this;
	}

	/**
	 * Enables buffering for improved write performance.
	 *
	 * <p>
	 * When buffering is enabled, the writer wraps the underlying output stream with a
	 * {@link BufferedOutputStream}, which can significantly improve performance for multiple
	 * small writes by batching them together.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Buffered writer for better performance</jc>
	 * 	Writer <jv>writer</jv> = FileWriterBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"output.txt"</js>)
	 * 		.buffered()
	 * 		.build();
	 * </p>
	 *
	 * @return This object for method chaining.
	 */
	public FileWriterBuilder buffered() {
		this.buffered = true;
		return this;
	}

	/**
	 * Creates a new {@link Writer} for writing to the configured file.
	 *
	 * <p>
	 * The writer is created with the specified character encoding, append mode, and buffering
	 * options. The file will be created if it doesn't exist (unless in append mode, where the
	 * file must exist or be creatable).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>try</jk> (Writer <jv>writer</jv> = FileWriterBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"output.txt"</js>)
	 * 		.charset(<js>"UTF-8"</js>)
	 * 		.buffered()
	 * 		.build()) {
	 * 		<jv>writer</jv>.write(<js>"Hello World"</js>);
	 * 	}
	 * </p>
	 *
	 * @return A new {@link Writer} for writing to the file.
	 * @throws FileNotFoundException If the file could not be created or opened for writing.
	 */
	public Writer build() throws FileNotFoundException {
		assertArgNotNull("file", file);
		var os = (OutputStream)new FileOutputStream(file, append);
		if (buffered)
			os = new BufferedOutputStream(os);
		return new OutputStreamWriter(os, cs != null ? cs : Charset.defaultCharset());
	}

	/**
	 * Sets the character encoding for writing to the file.
	 *
	 * <p>
	 * If not specified, the system's default charset ({@link Charset#defaultCharset()}) is used.
	 * Specifying the encoding is important when writing files that need to be read with a specific
	 * character encoding.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Writer <jv>writer</jv> = FileWriterBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"data.txt"</js>)
	 * 		.charset(StandardCharsets.UTF_8)
	 * 		.build();
	 * </p>
	 *
	 * @param cs The character encoding to use. The default is {@link Charset#defaultCharset()}.
	 * @return This object for method chaining.
	 */
	public FileWriterBuilder charset(Charset cs) {
		this.cs = cs;
		return this;
	}

	/**
	 * Sets the character encoding for writing to the file by charset name.
	 *
	 * <p>
	 * This is a convenience method that accepts a charset name string and converts it to a
	 * {@link Charset} using {@link Charset#forName(String)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Writer <jv>writer</jv> = FileWriterBuilder.<jsm>create</jsm>()
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
	public FileWriterBuilder charset(String cs) {
		this.cs = Charset.forName(assertArgNotNull("cs", cs));
		return this;
	}

	/**
	 * Sets the file to write to.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	File <jv>f</jv> = <jk>new</jk> File(<js>"output.txt"</js>);
	 * 	Writer <jv>writer</jv> = FileWriterBuilder.<jsm>create</jsm>()
	 * 		.file(<jv>f</jv>)
	 * 		.build();
	 * </p>
	 *
	 * @param value The file to write to.
	 * @return This object for method chaining.
	 */
	public FileWriterBuilder file(File value) {
		this.file = value;
		return this;
	}

	/**
	 * Sets the file path to write to.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Writer <jv>writer</jv> = FileWriterBuilder.<jsm>create</jsm>()
	 * 		.file(<js>"/path/to/output.txt"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param path The file path to write to.
	 * @return This object for method chaining.
	 */
	public FileWriterBuilder file(String path) {
		this.file = new File(path);
		return this;
	}
}