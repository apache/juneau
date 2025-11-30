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
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.net.*;
import java.nio.file.*;

import org.apache.juneau.common.utils.*;

/**
 * Represents a directory that can be located either on the classpath or in the file system.
 *
 * <p>
 * This class provides a unified interface for working with directories regardless of their location,
 * allowing code to transparently access files within directories from either the classpath (as resources)
 * or the file system. This is particularly useful in applications that need to work with directory-based
 * resources in both development (file system) and production (packaged JAR) environments.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Unified directory access - works with both classpath resources and file system directories
 * 	<li>File resolution - can resolve files within the directory using relative paths
 * 	<li>Transparent resolution - automatically resolves files based on construction parameters
 * 	<li>Immutable - directory location cannot be changed after construction
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Accessing template directories that may be on classpath or file system
 * 	<li>Loading configuration files from directories in both development and production
 * 	<li>Resolving resources within package directories
 * 	<li>Applications that need to support both embedded and external directory access
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Classpath directory</jc>
 * 	LocalDir <jv>classpathDir</jv> = <jk>new</jk> LocalDir(MyClass.<jk>class</jk>, <js>"templates"</js>);
 * 	LocalFile <jv>file</jv> = <jv>classpathDir</jv>.resolve(<js>"index.html"</js>);
 *
 * 	<jc>// File system directory</jc>
 * 	LocalDir <jv>fsDir</jv> = <jk>new</jk> LocalDir(Paths.get(<js>"/var/config"</js>));
 * 	LocalFile <jv>file2</jv> = <jv>fsDir</jv>.resolve(<js>"app.properties"</js>);
 *
 * 	<jc>// Package directory (null or empty path)</jc>
 * 	LocalDir <jv>packageDir</jv> = <jk>new</jk> LocalDir(MyClass.<jk>class</jk>, <jk>null</jk>);
 * 	LocalFile <jv>file3</jv> = <jv>packageDir</jv>.resolve(<js>"resource.txt"</js>);
 * </p>
 *
 * <h5 class='section'>Path Resolution:</h5>
 * <p>
 * The {@link #resolve(String)} method resolves files within the directory using relative paths.
 * For classpath directories, the path resolution follows Java resource path conventions:
 * <ul class='spaced-list'>
 * 	<li><jk>null</jk> or empty string - resolves relative to the class's package
 * 	<li>Absolute path (starts with <js>'/'</js>) - resolves relative to classpath root
 * 	<li>Relative path - resolves relative to the specified classpath path
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is immutable and therefore thread-safe. Multiple threads can safely access a LocalDir
 * instance concurrently without synchronization.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LocalFile} - File counterpart for individual file access
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonIO">juneau-common-io</a>
 * </ul>
 */
public class LocalDir {

	/**
	 * Validates that the specified classpath resource exists and is a file.
	 * Note that the behavior of Class.getResource(path) is different when pointing to directories on the classpath.
	 * When packaged as a jar, calling Class.getResource(path) on a directory returns null.
	 * When unpackaged, calling Class.getResource(path) on a directory returns a URL starting with "file:".
	 * We perform a test to make the behavior the same regardless of whether we're packaged or not.
	 */
	private static boolean isClasspathFile(URL url) {
		return safeSupplier(() -> {
			if (url == null)
				return false;
			var uri = url.toURI();
			if (uri.toString().startsWith("file:"))
				if (Files.isDirectory(Paths.get(uri)))
					return false;
			return true;
		});
	}
	private final Class<?> clazz;
	private final String clazzPath;
	private final Path path;

	private final int hashCode;

	/**
	 * Constructor for classpath directory.
	 *
	 * <p>
	 * Creates a LocalDir that references a directory on the classpath, relative to the specified class.
	 * The path resolution follows Java resource path conventions.
	 *
	 * <h5 class='section'>Path Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Package directory (null or empty)</jc>
	 * 	LocalDir <jv>dir1</jv> = <jk>new</jk> LocalDir(MyClass.<jk>class</jk>, <jk>null</jk>);
	 * 	<jc>// Resolves files in same package as MyClass</jc>
	 *
	 * 	<jc>// Absolute path from classpath root</jc>
	 * 	LocalDir <jv>dir2</jv> = <jk>new</jk> LocalDir(MyClass.<jk>class</jk>, <js>"/com/example/templates"</js>);
	 * 	<jc>// Resolves files from classpath root</jc>
	 *
	 * 	<jc>// Relative path from class package</jc>
	 * 	LocalDir <jv>dir3</jv> = <jk>new</jk> LocalDir(MyClass.<jk>class</jk>, <js>"templates"</js>);
	 * 	<jc>// Resolves files relative to MyClass package</jc>
	 * </p>
	 *
	 * @param clazz The class used to retrieve resources. Must not be <jk>null</jk>.
	 * @param clazzPath The subpath. Can be any of the following:
	 *                  <ul>
	 *                  	<li><jk>null</jk> or an empty string - Package of the class
	 *                  	<li>Absolute path (starts with <js>'/'</js>) - Relative to root package
	 *                  	<li>Relative path (does not start with <js>'/'</js>) - Relative to class package
	 *                  </ul>
	 */
	public LocalDir(Class<?> clazz, String clazzPath) {
		this.clazz = assertArgNotNull("clazz", clazz);
		this.clazzPath = "/".equals(clazzPath) ? "/" : StringUtils.nullIfEmpty(trimTrailingSlashes(clazzPath));
		this.path = null;
		this.hashCode = HashCode.of(clazz, clazzPath);
	}

	/**
	 * Constructor for file system directory.
	 *
	 * <p>
	 * Creates a LocalDir that references a directory on the file system using a {@link Path}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Absolute path</jc>
	 * 	LocalDir <jv>dir1</jv> = <jk>new</jk> LocalDir(Paths.get(<js>"/var/config"</js>));
	 *
	 * 	<jc>// Relative path</jc>
	 * 	LocalDir <jv>dir2</jv> = <jk>new</jk> LocalDir(Paths.get(<js>"data/templates"</js>));
	 *
	 * 	<jc>// From File object</jc>
	 * 	File <jv>f</jv> = <jk>new</jk> File(<js>"output"</js>);
	 * 	LocalDir <jv>dir3</jv> = <jk>new</jk> LocalDir(<jv>f</jv>.toPath());
	 * </p>
	 *
	 * @param path Filesystem directory location. Must not be <jk>null</jk>.
	 */
	public LocalDir(Path path) {
		this.clazz = null;
		this.clazzPath = null;
		this.path = assertArgNotNull("path", path);
		this.hashCode = path.hashCode();
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return o instanceof LocalDir o2 && eq(this, o2, (x, y) -> eq(x.clazz, y.clazz) && eq(x.clazzPath, y.clazzPath) && eq(x.path, y.path));
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return hashCode;
	}

	/**
	 * Resolves a file within this directory using the specified relative path.
	 *
	 * <p>
	 * This method attempts to locate a file within the directory. If the file exists and is readable,
	 * a {@link LocalFile} instance is returned. If the file does not exist or is not accessible,
	 * <jk>null</jk> is returned.
	 *
	 * <p>
	 * For classpath directories, the path is resolved according to the directory's path type:
	 * <ul class='spaced-list'>
	 * 	<li>Package directory (null clazzPath) - path is kept relative
	 * 	<li>Root directory ("/") - path is made absolute if not already
	 * 	<li>Absolute clazzPath - resolved path is made absolute
	 * 	<li>Relative clazzPath - resolved path remains relative
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LocalDir <jv>dir</jv> = <jk>new</jk> LocalDir(MyClass.<jk>class</jk>, <js>"templates"</js>);
	 *
	 * 	<jc>// Resolve file in directory</jc>
	 * 	LocalFile <jv>file</jv> = <jv>dir</jv>.resolve(<js>"index.html"</js>);
	 * 	<jk>if</jk> (<jv>file</jv> != <jk>null</jk>) {
	 * 		InputStream <jv>is</jv> = <jv>file</jv>.read();
	 * 	}
	 *
	 * 	<jc>// Resolve file in subdirectory</jc>
	 * 	LocalFile <jv>file2</jv> = <jv>dir</jv>.resolve(<js>"pages/about.html"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Security Note:</h5>
	 * <p>
	 * This method does not perform path validation or security checks (e.g., checking for path
	 * traversal attacks or malformed values). The caller is responsible for ensuring the path
	 * is safe and valid.
	 *
	 * @param path The relative path to the file to resolve within this directory.
	 *             Must be a non-null relative path.
	 * @return A {@link LocalFile} instance if the file exists and is readable, or <jk>null</jk> if it does not.
	 */
	public LocalFile resolve(String path) {
		assertArgNotNull("path", path);
		if (nn(clazz)) {
			String p;
			if (clazzPath == null) {
				// Relative to class package - keep path relative
				p = path;
			} else if ("/".equals(clazzPath)) {
				// Root - make path absolute
				p = path.startsWith("/") ? path : "/" + path;
			} else if (clazzPath.startsWith("/")) {
				// Absolute clazzPath - make resolved path absolute
				p = clazzPath + '/' + path;
			} else {
				// Relative clazzPath - keep resolved path relative
				p = clazzPath + '/' + path;
			}
			if (isClasspathFile(clazz.getResource(p)))
				return new LocalFile(clazz, p);
		} else {
			var p = this.path.resolve(path);
			if (Files.isReadable(p) && ! Files.isDirectory(p))
				return new LocalFile(p);
		}
		return null;
	}

	@Override /* Overridden from Object */
	public String toString() {
		if (clazz == null)
			return path.toString();
		return cn(clazz) + ":" + clazzPath;
	}
}