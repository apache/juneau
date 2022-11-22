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
import static org.apache.juneau.common.internal.IOUtils.*;

import java.io.*;
import java.nio.file.*;

/**
 * Identifies a file located either on the classpath or file system.
 *
 * Used to encapsulate basic resolution and retrieval of files regardless of where they are located.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class LocalFile {

	private final Class<?> clazz;
	private final String clazzPath;
	private final Path path;
	private final String name;
	private byte[] cache;

	/**
	 * Constructor for classpath file.
	 *
	 * @param clazz The class used to retrieve resources.  Must not be <jk>null</jk>.
	 * @param clazzPath The path relative to the class.  Must be a non-null normalized relative path.
	 */
	public LocalFile(Class<?> clazz, String clazzPath) {
		this.clazz = assertArgNotNull("clazz", clazz);
		this.clazzPath = assertArgNotNull("clazzPath", clazzPath);
		this.path = null;
		int i = clazzPath.lastIndexOf('/');
		this.name = i == -1 ? clazzPath : clazzPath.substring(i+1);
	}

	/**
	 * Constructor for file system file.
	 *
	 * @param path Filesystem file location.  Must not be <jk>null</jk>.
	 */
	public LocalFile(Path path) {
		this.clazz = null;
		this.clazzPath = null;
		this.path = assertArgNotNull("path", path);
		this.name = path.getFileName().toString();
	}

	/**
	 * Returns the contents of this file.
	 *
	 * @return An input stream of the contents of this file.
	 * @throws IOException If file could not be read.
	 */
	public InputStream read() throws IOException {
		synchronized(this) {
			if (cache != null)
				return new ByteArrayInputStream(cache);
		}
		if (clazz != null)
			return clazz.getResourceAsStream(clazzPath);
		return Files.newInputStream(path);
	}

	/**
	 * Returns the size of this file.
	 *
	 * @return The size of this file in bytes, or <c>-1</c> if not known.
	 * @throws IOException If file size could not be determined.
	 */
	public long size() throws IOException {
		return (path == null ? -1 : Files.size(path));
	}

	/**
	 * Caches the contents of this file into an internal byte array for quick future retrieval.
	 *
	 * @return This object.
	 * @throws IOException If file could not be read.
	 */
	public LocalFile cache() throws IOException {
		synchronized(this) {
			this.cache = readBytes(read());
		}
		return this;
	}

	/**
	 * Returns the name of this file.
	 *
	 * @return The name of this file.
	 */
	public String getName() {
		return name;
	}
}
