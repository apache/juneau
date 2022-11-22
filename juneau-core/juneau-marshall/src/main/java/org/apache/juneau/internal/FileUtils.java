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
package org.apache.juneau.internal;

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.io.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;

/**
 * File utilities.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class FileUtils {

	/**
	 * Same as {@link File#mkdirs()} except throws a RuntimeExeption if directory could not be created.
	 *
	 * @param f The directory to create.  Must not be <jk>null</jk>.
	 * @param clean If <jk>true</jk>, deletes the contents of the directory if it already exists.
	 * @return The same file.
	 * @throws RuntimeException if directory could not be created.
	 */
	public static File mkdirs(File f, boolean clean) {
		assertArgNotNull("f", f);
		if (f.exists()) {
			if (clean) {
				if (! delete(f))
					throw new BasicRuntimeException("Could not clean directory ''{0}''", f.getAbsolutePath());
			} else {
				return f;
			}
		}
		if (! f.mkdirs())
			throw new BasicRuntimeException("Could not create directory ''{0}''", f.getAbsolutePath());
		return f;
	}

	/**
	 * Same as {@link #mkdirs(String, boolean)} but uses String path.
	 *
	 * @param path The path of the directory to create.  Must not be <jk>null</jk>
	 * @param clean If <jk>true</jk>, deletes the contents of the directory if it already exists.
	 * @return The directory.
	 */
	public static File mkdirs(String path, boolean clean) {
		assertArgNotNull("path", path);
		return mkdirs(new File(path), clean);
	}

	/**
	 * Recursively deletes a file or directory.
	 *
	 * @param f The file or directory to delete.
	 * @return <jk>true</jk> if file or directory was successfully deleted.
	 */
	public static boolean delete(File f) {
		if (f == null)
			return true;
		if (f.isDirectory()) {
			File[] cf = f.listFiles();
			if (cf != null)
				for (File c : cf)
					delete(c);
		}
		return f.delete();
	}

	/**
	 * Creates a file if it doesn't already exist using {@link File#createNewFile()}.
	 *
	 * <p>
	 * Throws a {@link RuntimeException} if the file could not be created.
	 *
	 * @param f The file to create.
	 */
	public static void create(File f) {
		if (f.exists())
			return;
		try {
			if (! f.createNewFile())
				throw new BasicRuntimeException("Could not create file ''{0}''", f.getAbsolutePath());
		} catch (IOException e) {
			throw asRuntimeException(e);
		}
	}

	/**
	 * Updates the modified timestamp on the specified file.
	 *
	 * <p>
	 * Method ensures that the timestamp changes even if it's been modified within the past millisecond.
	 *
	 * @param f The file to modify the modified timestamp on.
	 */
	public static void modifyTimestamp(File f) {
		long lm = f.lastModified();
		long l = System.currentTimeMillis();
		if (lm == l)
			l++;
		if (! f.setLastModified(l))
			throw new BasicRuntimeException("Could not modify timestamp on file ''{0}''", f.getAbsolutePath());

		// Linux only gives 1s precision, so set the date 1s into the future.
		if (lm == f.lastModified()) {
			l += 1000;
			if (! f.setLastModified(l))
				throw new BasicRuntimeException("Could not modify timestamp on file ''{0}''", f.getAbsolutePath());
		}
	}

	/**
	 * Create a temporary file with the specified name.
	 *
	 * <p>
	 * The name is broken into file name and suffix, and the parts are passed to
	 * {@link File#createTempFile(String, String)}.
	 *
	 * <p>
	 * {@link File#deleteOnExit()} is called on the resulting file before being returned by this method.
	 *
	 * @param name The file name
	 * @return A newly-created temporary file.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static File createTempFile(String name) throws IOException {
		String[] parts = name.split("\\.");
		File f = File.createTempFile(parts[0], "." + parts[1]);
		f.deleteOnExit();
		return f;
	}

	/**
	 * Create a temporary file with the specified name and specified contents.
	 *
	 * <p>
	 * The name is broken into file name and suffix, and the parts are passed to
	 * {@link File#createTempFile(String, String)}.
	 *
	 * <p>
	 * {@link File#deleteOnExit()} is called on the resulting file before being returned by this method.
	 *
	 * @param name The file name
	 * @param contents The file contents.
	 * @return A newly-created temporary file.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static File createTempFile(String name, String contents) throws IOException {
		File f = createTempFile(name);
		try (Reader r = new StringReader(contents); Writer w = new FileWriter(f)) {
			IOUtils.pipe(r, w);
			w.flush();
		}
		return f;
	};

	/**
	 * Strips the extension from a file name.
	 *
	 * @param name The file name.
	 * @return The file name without the extension, or <jk>null</jk> if name was <jk>null</jk>.
	 */
	public static String getBaseName(String name) {
		if (name == null)
			return null;
		int i = name.lastIndexOf('.');
		if (i == -1)
			return name;
		return name.substring(0, i);
	}

	/**
	 * Returns the extension from a file name.
	 *
	 * @param name The file name.
	 * @return The the extension, or <jk>null</jk> if name was <jk>null</jk>.
	 */
	public static String getExtension(String name) {
		if (name == null)
			return null;
		int i = name.lastIndexOf('.');
		if (i == -1)
			return "";
		return name.substring(i+1);
	}

	/**
	 * Returns <jk>true</jk> if the specified file exists in the specified directory.
	 *
	 * @param dir The directory.
	 * @param fileName The file name.
	 * @return <jk>true</jk> if the specified file exists in the specified directory.
	 */
	public static boolean exists(File dir, String fileName) {
		if (dir == null || fileName == null)
			return false;
		return Files.exists(dir.toPath().resolve(fileName));
	}

	/**
	 * Returns <jk>true</jk> if the specified file name contains the specified extension.
	 *
	 * @param name The file name.
	 * @param ext The extension.
	 * @return <jk>true</jk> if the specified file name contains the specified extension.
	 */
	public static boolean hasExtension(String name, String ext) {
		if (name == null || ext == null)
			return false;
		return ext.equals(getExtension(name));
	}

	/**
	 * Given an arbitrary path, returns the file name portion of that path.
	 *
	 * @param path The path to check.
	 * @return The file name.
	 */
	public static String getFileName(String path) {
		if (isEmpty(path))
			return null;
		path = trimTrailingSlashes(path);
		int i = path.lastIndexOf('/');
		return i == -1 ? path : path.substring(i+1);
	}
}
