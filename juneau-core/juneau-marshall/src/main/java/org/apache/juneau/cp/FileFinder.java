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

import static org.apache.juneau.internal.FileUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.*;
import java.util.ResourceBundle.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.internal.*;

/**
 * Utility class for finding localized files on the classpath and file system.
 *
 * <p>
 * Typical example:
 *
 * <p class='bcode w800'>
 * 	<jc>// Constructor a file source that looks for files in the "files" working directory, then in the
 * 	// package "foo.bar", then in the package "foo.bar.files", then in the package "files".</jc>
 * 	FileFinder <jv>finder</jv> = FileFinder
 * 		.<jsm>create</jsm>()
 * 		.dir("files")
 * 		.cp(foo.bar.MyClass.<jk>class</jk>,<jk>null</jk>,<jk>true</jk>)
 * 		.cp(foo.bar.MyClass.<jk>class</jk>,<js>"files"</js>,<jk>true</jk>)
 * 		.cp(foo.bar.MyClass.<jk>class</jk>,<js>"/files"</js>,<jk>true</jk>)
 * 		.cache(1_000_000l)  <jc>// Cache files less than 1MB in size.</jc>
 * 		.ignore(Pattern.<jsm>compile</jsm>(<js>"(?i)(.*\\.(class|properties))|(package.html)"</js>)) <jc>// Ignore certain files.</jc>
 * 		.build();
 *
 * 	<jc>// Find a normal file.</jc>
 * 	InputStream <jv>is1</jv> = <jv>finder</jv>.getFile(<js>"text.txt"</js>);
 *
 * 	<jc>// Find a localized file called "text_ja_JP.txt".</jc>
 * 	InputStream <jv>is2</jv> = <jv>finder</jv>.getFile(<js>"text.txt"</js>, Locale.<jsf>JAPAN</jsf>);
 * </p>
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
 * <p>
 * This class (and the builder class) can be subclassed by extension and using the {@link FileFinderBuilder#build(Class)} method.
 */
public class FileFinder {

	private static final ResourceBundle.Control RB_CONTROL = ResourceBundle.Control.getControl(Control.FORMAT_DEFAULT);

	private final Map<String,LocalFile> files = new ConcurrentHashMap<>();
	private final Map<Locale,Map<String,LocalFile>> localizedFiles = new ConcurrentHashMap<>();

	private final LocalDir[] roots;
	private final long cachingLimit;
	private final Pattern ignorePattern;


	/**
	 * Instantiate a new builder.
	 *
	 * @return A new builder.
	 */
	public static FileFinderBuilder create() {
		return new FileFinderBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder object.
	 */
	public FileFinder(FileFinderBuilder builder) {
		this.roots = builder.getRoots();
		this.cachingLimit = builder.getCachingLimit();
		this.ignorePattern = builder.getIgnorePattern();
	}

	/**
	 * Returns the contents of the resource with the specified name.
	 *
	 * @param name The resource name.
	 * 	See {@link Class#getResource(String)} for format.
	 * @param locale
	 * 	The locale of the resource to retrieve.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return The resolved resource contents, or <jk>null</jk> if the resource was not found.
	 * @throws IOException Thrown by underlying stream.
	 */
	public InputStream getFile(String name, Locale locale) throws IOException {

		name = StringUtils.trimSlashesAndSpaces(name);

		if (isInvalidPath(name))
			return null;

		if (locale != null)
			localizedFiles.putIfAbsent(locale, new ConcurrentHashMap<>());

		Map<String,LocalFile> fileCache = locale == null ? files : localizedFiles.get(locale);

		LocalFile lf = fileCache.get(name);

		if (lf == null) {
			List<String> candidateFileNames = getCandidateFileNames(name, locale);
			paths: for (LocalDir root : roots) {
				for (String cfn : candidateFileNames) {
					lf = root.resolve(cfn);
					if (lf != null)
						break paths;
				}
			}

			if (lf != null  && ignorePattern != null && ignorePattern.matcher(lf.getName()).matches())
				lf = null;

			if (lf != null && (ignorePattern == null || ! ignorePattern.matcher(lf.getName()).matches())) {
				fileCache.put(name, lf);

				if (cachingLimit >= 0) {
					long size = lf.size();
					if (size > 0 && size <= cachingLimit)
						lf.cache();
				}
			}
		}

		if (lf == null)
			return null;

		return lf.read();
	}

	/**
	 * Returns the file with the specified name.
	 *
	 * @param name The file name.
	 * @return An input stream to the file if it exists, or <jk>null</jk> if it does not.
	 * @throws IOException If file could not be read.
	 */
	public InputStream getFile(String name) throws IOException {
		return getFile(name, null);
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
	 * @param locale
	 * 	The locale.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return An iterator of file names to look at.
	 */
	protected List<String> getCandidateFileNames(final String fileName, final Locale locale) {

		if (locale == null)
			return Collections.singletonList(fileName);

		List<String> list = new ArrayList<>();
		String baseName = getBaseName(fileName);
		String ext = getExtension(fileName);

		for (Locale l : getCandidateLocales(locale)) {
			String ls = l.toString();
			if (ls.isEmpty())
				list.add(fileName);
			else {
				list.add(baseName + "_" + ls + (ext.isEmpty() ? "" : ('.' + ext)));
				list.add(ls.replace('_', '/') + '/' + fileName);
			}
		}

		return list;
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
	protected List<Locale> getCandidateLocales(Locale locale) {
		return RB_CONTROL.getCandidateLocales("", locale);
	}

	/**
	 * Checks for path malformations such as use of <js>".."</js> which can be used to open up security holes.
	 *
	 * <p>
	 * Default implementation returns <jk>true</jk> if the path is any of the following:
	 * <ul>
	 * 	<li>Is blank or <jk>null</jk>.
	 * 	<li>Contains <js>".."</js> (to prevent traversing out of working directory).
	 * 	<li>Contains <js>"%"</js> (to prevent URI trickery).
	 * </ul>
	 *
	 * @param path The path to check.
	 * @return <jk>true</jk> if the path is invalid.
	 */
	protected boolean isInvalidPath(String path) {
		return isEmpty(path) || path.contains("..") || path.contains("%");
	}
}
