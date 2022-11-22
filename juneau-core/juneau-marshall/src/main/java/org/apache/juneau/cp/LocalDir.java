// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.cp;

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.net.*;
import java.nio.file.*;

import org.apache.juneau.internal.*;

/**
 * Identifies a directory located either on the classpath or file system.
 *
 * Used to encapsulate basic resolution and retrieval of files regardless of where they are located.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class LocalDir {

	private final Class<?> clazz;
	private final String clazzPath;
	private final Path path;
	private final int hashCode;

	/**
	 * Constructor for classpath directory.
	 *
	 * @param clazz The class used to retrieve resources.
	 * @param clazzPath
	 * 	The subpath.  Can be any of the following:
	 * 	<ul>
	 * 		<li><jk>null</jk> or an empty string - Package of the class.
	 * 		<li>Absolute path (starts with <js>'/'</js>) - Relative to root package.
	 * 		<li>Relative path (does not start with <js>'/'</js>) - Relative to class package.
	 * 	</ul>
	 */
	public LocalDir(Class<?> clazz, String clazzPath) {
		this.clazz = assertArgNotNull("clazz", clazz);
		this.clazzPath = "/".equals(clazzPath) ? "/" : nullIfEmpty(trimTrailingSlashes(clazzPath));
		this.path = null;
		this.hashCode = HashCode.of(clazz, clazzPath);
	}

	/**
	 * Constructor for file system directory.
	 *
	 * @param path Filesystem directory location.  Must not be <jk>null</jk>.
	 */
	public LocalDir(Path path) {
		this.clazz = null;
		this.clazzPath = null;
		this.path = assertArgNotNull("path", path);
		this.hashCode = path.hashCode();
	}

	/**
	 * Resolves the specified path.
	 *
	 * @param path
	 * 	The path to the file to resolve.
	 * 	<br>Must be a non-null relative path.
	 * 	<br>Does no cleanup of the path (e.g. checking for security holes or malformed values).
	 * @return The file if it exists, or <jk>null</jk> if it does not.
	 */
	public LocalFile resolve(String path) {
		if (clazz != null) {
			String p = clazzPath == null ? path : ("/".equals(clazzPath) ? "" : clazzPath) + '/' + path;
			if (isClasspathFile(clazz.getResource(p)))
				return new LocalFile(clazz, p);
		} else {
			Path p = this.path.resolve(path);
			if (Files.isReadable(p) && ! Files.isDirectory(p))
				return new LocalFile(p);
		}
		return null;
	}

	/**
	 * Validates that the specified classpath resource exists and is a file.
	 * Note that the behavior of Class.getResource(path) is different when pointing to directories on the classpath.
	 * When packaged as a jar, calling Class.getResource(path) on a directory returns null.
	 * When unpackaged, calling Class.getResource(path) on a directory returns a URL starting with "file:".
	 * We perform a test to make the behavior the same regardless of whether we're packaged or not.
	 */
	private boolean isClasspathFile(URL url) {
		try {
			if (url == null)
				return false;
			URI uri = url.toURI();
			if (uri.toString().startsWith("file:"))
				if (Files.isDirectory(Paths.get(uri)))
					return false;
		} catch (URISyntaxException e) {
			e.printStackTrace();  // Untestable.
			return false;
		}
		return true;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return o instanceof LocalDir && eq(this, (LocalDir)o, (x,y)->eq(x.clazz, y.clazz) && eq(x.clazzPath, y.clazzPath) && eq(x.path, y.path));
	}

	@Override /* Object */
	public int hashCode() {
		return hashCode;
	}

	@Override /* Object */
	public String toString() {
		if (clazz == null)
			return path.toString();
		return clazz.getName() + ":" + clazzPath;
	}
}
