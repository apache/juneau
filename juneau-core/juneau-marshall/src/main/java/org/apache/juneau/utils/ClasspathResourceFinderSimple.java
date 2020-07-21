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
package org.apache.juneau.utils;

import static org.apache.juneau.internal.FileUtils.*;

import java.io.*;
import java.util.*;
import java.util.ResourceBundle.*;

import org.apache.juneau.cp.*;

/**
 * Utility class for finding resources for a class.
 *
 * <p>
 * Same as {@link Class#getResourceAsStream(String)} except looks for resources with localized file names.
 *
 * <p>
 * If the <c>locale</c> is specified, then we look for resources whose name matches that locale.
 * For example, if looking for the resource <js>"MyResource.txt"</js> for the Japanese locale, we will look for
 * files in the following order:
 * <ol>
 * 	<li><js>"MyResource_ja_JP.txt"</js>
 * 	<li><js>"MyResource_ja.txt"</js>
 * 	<li><js>"MyResource.txt"</js>
 * </ol>
 *
 * @deprecated Use {@link SimpleResourceFinder}.
 */
@Deprecated
public class ClasspathResourceFinderSimple implements ClasspathResourceFinder {

	/**
	 * Reusable instance.
	 */
	public static final ClasspathResourceFinderSimple INSTANCE = new ClasspathResourceFinderSimple();

	private static final ResourceBundle.Control RB_CONTROL = ResourceBundle.Control.getControl(Control.FORMAT_DEFAULT);
	private static final List<Locale> ROOT_LOCALE = Arrays.asList(Locale.ROOT);


	@Override /* ClasspathResourceFinder */
	public InputStream findResource(Class<?> baseClass, String name, Locale locale) throws IOException {
		return findClasspathResource(baseClass, name, locale);
	}

	/**
	 * Workhorse method for retrieving a resource from the classpath.
	 *
	 * <p>
	 * This method can be overridden by subclasses to provide customized handling of resource retrieval from the classpath.
	 *
	 * @param baseClass The base class providing the classloader.
	 * @param name The resource name.
	 * @param locale
	 * 	The resource locale.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return The resource stream, or <jk>null</jk> if it couldn't be found.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected InputStream findClasspathResource(Class<?> baseClass, String name, Locale locale) throws IOException {

		if (locale == null)
			return getResourceAsStream(baseClass, name);

		for (String n : getCandidateFileNames(name, locale)) {
			InputStream is = getResourceAsStream(baseClass, n);
			if (is != null)
				return is;
		}
		return null;
	}

	private InputStream getResourceAsStream(Class<?> baseClass, String name) {
		return baseClass.getResourceAsStream(name);
	}

	/**
	 * Returns the candidate file names for the specified file name in the specified locale.
	 *
	 * <p>
	 * For example, if looking for the <js>"MyResource.txt"</js> file in the Japanese locale, the iterator will return
	 * names in the following order:
	 * <ol>
	 * 	<li><js>"MyResource_ja_JP.txt"</js>
	 * 	<li><js>"MyResource_ja.txt"</js>
	 * 	<li><js>"MyResource.txt"</js>
	 * </ol>
	 *
	 * <p>
	 * If the locale is <jk>null</jk>, then it will only return <js>"MyResource.txt"</js>.
	 *
	 * @param fileName The name of the file to get candidate file names on.
	 * @param l
	 * 	The locale.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return An iterator of file names to look at.
	 */
	protected static Iterable<String> getCandidateFileNames(final String fileName, final Locale l) {
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
	 *
	 * <p>
	 * For example, if <c>locale</c> is <js>"ja_JP"</js>, then this method will return:
	 * <ol>
	 * 	<li><js>"ja_JP"</js>
	 * 	<li><js>"ja"</js>
	 * 	<li><js>""</js>
	 * </ol>
	 *
	 * @param locale The locale to get the list of candidate locales for.
	 * @return The list of candidate locales.
	 */
	static final List<Locale> getCandidateLocales(Locale locale) {
		if (locale == null)
			return ROOT_LOCALE;
		return RB_CONTROL.getCandidateLocales("", locale);
	}
}
