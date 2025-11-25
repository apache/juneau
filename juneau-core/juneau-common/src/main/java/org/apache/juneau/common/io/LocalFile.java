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
import static org.apache.juneau.common.utils.IOUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.io.*;
import java.nio.file.*;

/**
 * Represents a file that can be located either on the classpath or in the file system.
 *
 * <p>
 * This class provides a unified interface for working with files regardless of their location,
 * allowing code to transparently access files from either the classpath (as resources) or the
 * file system. This is particularly useful in applications that need to work with files in both
 * development (file system) and production (packaged JAR) environments.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Unified file access - works with both classpath resources and file system files
 * 	<li>Optional caching - can cache file contents in memory for fast repeated access
 * 	<li>Transparent resolution - automatically resolves files based on construction parameters
 * 	<li>Thread-safe reading - synchronized access for cached content
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Reading configuration files that may be on classpath or file system
 * 	<li>Accessing template files in both development and production environments
 * 	<li>Loading resources that need to work in both JAR and unpackaged scenarios
 * 	<li>Applications that need to support both embedded and external file access
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Classpath file</jc>
 * 	LocalFile <jv>classpathFile</jv> = <jk>new</jk> LocalFile(MyClass.<jk>class</jk>, <js>"config.properties"</js>);
 * 	InputStream <jv>is</jv> = <jv>classpathFile</jv>.read();
 *
 * 	<jc>// File system file</jc>
 * 	LocalFile <jv>fsFile</jv> = <jk>new</jk> LocalFile(Paths.get(<js>"/path/to/file.txt"</js>));
 * 	InputStream <jv>is2</jv> = <jv>fsFile</jv>.read();
 *
 * 	<jc>// With caching for repeated access</jc>
 * 	LocalFile <jv>cachedFile</jv> = <jk>new</jk> LocalFile(MyClass.<jk>class</jk>, <js>"template.html"</js>);
 * 	<jv>cachedFile</jv>.cache();  <jc>// Cache contents in memory</jc>
 * 	InputStream <jv>is3</jv> = <jv>cachedFile</jv>.read();  <jc>// Fast - uses cache</jc>
 * </p>
 *
 * <h5 class='section'>Caching:</h5>
 * <p>
 * The {@link #cache()} method loads the entire file contents into memory, which can improve
 * performance for files that are accessed multiple times. Once cached, subsequent calls to
 * {@link #read()} return a {@link ByteArrayInputStream} backed by the cached data. Caching is
 * thread-safe and synchronized.
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is thread-safe for reading operations. The caching mechanism uses synchronization
 * to ensure thread-safe access to cached content. Multiple threads can safely call {@link #read()}
 * concurrently.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LocalDir} - Directory counterpart for resolving files within directories
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonIO">juneau-common-io</a>
 * </ul>
 */
@SuppressWarnings("resource")
public class LocalFile {

	private final Class<?> clazz;
	private final String clazzPath;
	private final Path path;
	private final String name;
	private byte[] cache;

	/**
	 * Constructor for classpath file.
	 *
	 * <p>
	 * Creates a LocalFile that references a file on the classpath, relative to the specified class.
	 * The path is resolved using {@link Class#getResourceAsStream(String)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// File in same package as MyClass</jc>
	 * 	LocalFile <jv>file1</jv> = <jk>new</jk> LocalFile(MyClass.<jk>class</jk>, <js>"config.properties"</js>);
	 *
	 * 	<jc>// File in subdirectory</jc>
	 * 	LocalFile <jv>file2</jv> = <jk>new</jk> LocalFile(MyClass.<jk>class</jk>, <js>"templates/index.html"</js>);
	 *
	 * 	<jc>// Absolute path from classpath root</jc>
	 * 	LocalFile <jv>file3</jv> = <jk>new</jk> LocalFile(MyClass.<jk>class</jk>, <js>"/com/example/config.xml"</js>);
	 * </p>
	 *
	 * @param clazz The class used to retrieve resources. Must not be <jk>null</jk>.
	 * @param clazzPath The path relative to the class. Must be a non-null normalized relative path.
	 *                  Use absolute paths (starting with <js>'/'</js>) to reference from classpath root.
	 */
	public LocalFile(Class<?> clazz, String clazzPath) {
		this.clazz = assertArgNotNull("clazz", clazz);
		this.clazzPath = assertArgNotNull("clazzPath", clazzPath);
		this.path = null;
		var i = clazzPath.lastIndexOf('/');
		this.name = i == -1 ? clazzPath : clazzPath.substring(i + 1);
	}

	/**
	 * Constructor for file system file.
	 *
	 * <p>
	 * Creates a LocalFile that references a file on the file system using a {@link Path}.
	 * The path must point to an actual file (not a directory) and must have a filename component.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Absolute path</jc>
	 * 	LocalFile <jv>file1</jv> = <jk>new</jk> LocalFile(Paths.get(<js>"/etc/config.properties"</js>));
	 *
	 * 	<jc>// Relative path</jc>
	 * 	LocalFile <jv>file2</jv> = <jk>new</jk> LocalFile(Paths.get(<js>"data/input.txt"</js>));
	 *
	 * 	<jc>// From File object</jc>
	 * 	File <jv>f</jv> = <jk>new</jk> File(<js>"output.log"</js>);
	 * 	LocalFile <jv>file3</jv> = <jk>new</jk> LocalFile(<jv>f</jv>.toPath());
	 * </p>
	 *
	 * @param path Filesystem file location. Must not be <jk>null</jk>.
	 *             Must not be a root path (must have a filename).
	 * @throws IllegalArgumentException if the path is a root path (has no filename).
	 */
	public LocalFile(Path path) {
		this.clazz = null;
		this.clazzPath = null;
		this.path = assertArgNotNull("path", path);
		var fileName = path.getFileName();
		assertArg(fileName != null, "Argument 'path' must not be a root path (must have a filename).");
		this.name = opt(fileName).map(Object::toString).orElse(null);
	}

	/**
	 * Caches the contents of this file into an internal byte array for quick future retrieval.
	 *
	 * <p>
	 * This method reads the entire file into memory and stores it in a byte array. Subsequent
	 * calls to {@link #read()} will return a {@link ByteArrayInputStream} backed by this cached
	 * data, avoiding repeated file I/O operations. This is useful for files that are accessed
	 * multiple times.
	 *
	 * <p>
	 * The caching operation is thread-safe and synchronized. If multiple threads call this method
	 * concurrently, only one will perform the actual read operation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LocalFile <jv>file</jv> = <jk>new</jk> LocalFile(MyClass.<jk>class</jk>, <js>"template.html"</js>);
	 * 	<jv>file</jv>.cache();  <jc>// Load into memory</jc>
	 *
	 * 	<jc>// Multiple reads are fast - no I/O</jc>
	 * 	InputStream <jv>is1</jv> = <jv>file</jv>.read();
	 * 	InputStream <jv>is2</jv> = <jv>file</jv>.read();
	 * </p>
	 *
	 * <h5 class='section'>Memory Considerations:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>The entire file is loaded into memory, so use with caution for large files
	 * 	<li>Once cached, the file contents remain in memory for the lifetime of the LocalFile object
	 * 	<li>Cache is shared across all threads accessing this LocalFile instance
	 * </ul>
	 *
	 * @return This object for method chaining.
	 * @throws IOException If the file could not be read or does not exist.
	 */
	public LocalFile cache() throws IOException {
		synchronized (this) {
			this.cache = readBytes(read());
		}
		return this;
	}

	/**
	 * Returns the name of this file (filename without directory path).
	 *
	 * <p>
	 * For classpath files, this is the last component of the classpath path.
	 * For file system files, this is the filename component of the path.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LocalFile <jv>file1</jv> = <jk>new</jk> LocalFile(MyClass.<jk>class</jk>, <js>"templates/index.html"</js>);
	 * 	String <jv>name1</jv> = <jv>file1</jv>.getName();  <jc>// Returns "index.html"</jc>
	 *
	 * 	LocalFile <jv>file2</jv> = <jk>new</jk> LocalFile(Paths.get(<js>"/var/log/app.log"</js>));
	 * 	String <jv>name2</jv> = <jv>file2</jv>.getName();  <jc>// Returns "app.log"</jc>
	 * </p>
	 *
	 * @return The name of this file (filename component only).
	 */
	public String getName() { return name; }

	/**
	 * Returns an input stream for reading the contents of this file.
	 *
	 * <p>
	 * If the file has been cached via {@link #cache()}, this method returns a
	 * {@link ByteArrayInputStream} backed by the cached data. Otherwise, it returns
	 * a new input stream that reads directly from the file (classpath resource or file system).
	 *
	 * <p>
	 * Each call to this method returns a new input stream. The caller is responsible
	 * for closing the returned stream.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LocalFile <jv>file</jv> = <jk>new</jk> LocalFile(MyClass.<jk>class</jk>, <js>"data.txt"</js>);
	 *
	 * 	<jc>// Read file contents</jc>
	 * 	<jk>try</jk> (InputStream <jv>is</jv> = <jv>file</jv>.read()) {
	 * 		<jc>// Process stream</jc>
	 * 	}
	 * </p>
	 *
	 * @return An input stream for reading the contents of this file.
	 * @throws IOException If the file could not be read, does not exist, or is not accessible.
	 */
	public InputStream read() throws IOException {
		synchronized (this) {
			if (nn(cache))
				return new ByteArrayInputStream(cache);
		}
		if (nn(clazz)) {
			var is = clazz.getResourceAsStream(clazzPath);
			if (is == null)
				throw new IOException("Classpath resource not found: " + clazzPath + " (relative to " + clazz.getName() + ")");
			return is;
		}
		return Files.newInputStream(path);
	}

	/**
	 * Returns the size of this file in bytes.
	 *
	 * <p>
	 * For file system files, this method returns the actual file size using {@link Files#size(Path)}.
	 * For classpath files, the size cannot be determined and this method returns <c>-1</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LocalFile <jv>fsFile</jv> = <jk>new</jk> LocalFile(Paths.get(<js>"/var/log/app.log"</js>));
	 * 	<jk>long</jk> <jv>size</jv> = <jv>fsFile</jv>.size();  <jc>// Returns actual file size</jc>
	 *
	 * 	LocalFile <jv>cpFile</jv> = <jk>new</jk> LocalFile(MyClass.<jk>class</jk>, <js>"resource.txt"</js>);
	 * 	<jk>long</jk> <jv>size2</jv> = <jv>cpFile</jv>.size();  <jc>// Returns -1 (unknown)</jc>
	 * </p>
	 *
	 * @return The size of this file in bytes, or <c>-1</c> if the size cannot be determined
	 *         (e.g., for classpath resources).
	 * @throws IOException If the file size could not be determined (for file system files).
	 */
	public long size() throws IOException {
		return (path == null ? -1 : Files.size(path));
	}
}