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

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.io.*;
import java.util.*;
import java.util.ResourceBundle.*;

/**
 * File utilities.
 */
public class FileUtils {

	private static final ResourceBundle.Control RB_CONTROL = ResourceBundle.Control.getControl(Control.FORMAT_DEFAULT);
	private static final List<Locale> ROOT_LOCALE = Arrays.asList(Locale.ROOT);

	/**
	 * Same as {@link File#mkdirs()} except throws a RuntimeExeption if directory could not be created.
	 *
	 * @param f The directory to create.  Must not be <jk>null</jk>.
	 * @param clean If <jk>true</jk>, deletes the contents of the directory if it already exists.
	 * @return The same file.
	 * @throws RuntimeException if directory could not be created.
	 */
	public static File mkdirs(File f, boolean clean) {
		assertFieldNotNull(f, "f");
		if (f.exists()) {
			if (clean) {
				if (! delete(f))
					throw new RuntimeException("Could not clean directory '"+f.getAbsolutePath()+"'");
			} else {
				return f;
			}
		}
		if (! f.mkdirs())
			throw new RuntimeException("Could not create directory '" + f.getAbsolutePath() + "'");
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
		assertFieldNotNull(path, "path");
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
	 * Throws a {@link RuntimeException} if the file could not be created.
	 *
	 * @param f The file to create.
	 */
	public static void create(File f) {
		if (f.exists())
			return;
		try {
			if (! f.createNewFile())
				throw new RuntimeException("Could not create file '"+f.getAbsolutePath()+"'");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Updates the modified timestamp on the specified file.
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
			throw new RuntimeException("Could not modify timestamp on file '"+f.getAbsolutePath()+"'");

		// Linux only gives 1s precision, so set the date 1s into the future.
		if (lm == f.lastModified()) {
			l += 1000;
			if (! f.setLastModified(l))
				throw new RuntimeException("Could not modify timestamp on file '"+f.getAbsolutePath()+"'");
		}
	}

	/**
	 * Create a temporary file with the specified name.
	 * <p>
	 * The name is broken into file name and suffix, and the parts
	 * are passed to {@link File#createTempFile(String, String)}.
	 * <p>
	 * {@link File#deleteOnExit()} is called on the resulting file before being returned by this method.
	 *
	 * @param name The file name
	 * @return A newly-created temporary file.
	 * @throws IOException
	 */
	public static File createTempFile(String name) throws IOException {
		String[] parts = name.split("\\.");
		File f = File.createTempFile(parts[0], "." + parts[1]);
		f.deleteOnExit();
		return f;
	}

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
	 * Returns the candidate file names for the specified file name in the specified locale.
	 * <p>
	 * For example, if looking for the <js>"MyResource.txt"</js> file in the Japanese locale, the iterator will return names in the following order:
	 * <ol>
	 * 	<li><js>"MyResource_ja_JP.txt"</js>
	 * 	<li><js>"MyResource_ja.txt"</js>
	 * 	<li><js>"MyResource.txt"</js>
	 * </ol>
	 * <p>
	 * If the locale is null, then it will only return <js>"MyResource.txt"</js>.
	 *
	 * @param fileName The name of the file to get candidate file names on.
	 * @param l The locale.
	 * @return An iterator of file names to look at.
	 */
	public static Iterable<String> getCandidateFileNames(final String fileName, final Locale l) {
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					final Iterator<Locale> locales = getCandidateLocales(l).iterator();
					String baseName, ext;

					@Override
					public boolean hasNext() {
						return locales.hasNext();
					}

					@Override
					public String next() {
						Locale l2 = locales.next();
						if (l2.toString().isEmpty())
							return fileName;
						if (baseName == null)
							baseName = getBaseName(fileName);
						if (ext == null)
							ext = getExtension(fileName);
						return baseName + "_" + l2.toString() + (ext.isEmpty() ? "" : ('.' + ext));
					}
					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}


	/**
	 * Returns the candidate locales for the specified locale.
	 * <p>
	 * For example, if <code>locale</code> is <js>"ja_JP"</js>, then this method will return:
	 * <ol>
	 * 	<li><js>"ja_JP"</js>
	 * 	<li><js>"ja"</js>
	 * 	<li><js>""</js>
	 * </ol>
	 *
	 * @param locale The locale to get the list of candidate locales for.
	 * @return The list of candidate locales.
	 */
	public static List<Locale> getCandidateLocales(Locale locale) {
		if (locale == null)
			return ROOT_LOCALE;
		return RB_CONTROL.getCandidateLocales("", locale);
	}
}
