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
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;

import java.io.*;
import java.util.*;
import java.util.ResourceBundle.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Basic implementation of a {@link FileFinder}.
 *
 * <p>
 * Specialized behavior can be implemented by overridding the {@link #find(String, Locale)} method.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jk>public class</jk> MyFileFinder <jk>extends</jk> BasicFileFinder {
 * 		<ja>@Override</ja>
 * 		<jk>protected</jk> Optional&lt;InputStream&gt; find(String <jv>name</jv>, Locale <jv>locale</jv>) <jk>throws</jk> IOException {
 * 			<jc>// Do special handling or just call super.find().</jc>
 * 			<jk>return super</jk>.find(<jv>name</jv>, <jv>locale</jv>);
 * 		}
 * 	}
 * </p>
 */
public class BasicFileFinder implements FileFinder {


	private static final ResourceBundle.Control RB_CONTROL = ResourceBundle.Control.getControl(Control.FORMAT_DEFAULT);

	private final Map<String,LocalFile> files = new ConcurrentHashMap<>();
	private final Map<Locale,Map<String,LocalFile>> localizedFiles = new ConcurrentHashMap<>();

	private final LocalDir[] roots;
	private final long cachingLimit;
	private final Pattern[] include, exclude;
	private final int hashCode;

	/**
	 * Builder-based constructor.
	 *
	 * @param builder The builder object.
	 */
	public BasicFileFinder(FileFinderBuilder builder) {
		this.roots = builder.roots.toArray(new LocalDir[builder.roots.size()]);
		this.cachingLimit = builder.cachingLimit;
		this.include = builder.include.toArray(new Pattern[builder.include.size()]);
		this.exclude = builder.exclude.toArray(new Pattern[builder.exclude.size()]);
		this.hashCode = HashCode.of(getClass(), roots, cachingLimit, getIncludePatterns(), getExcludePatterns());
	}

	/**
	 * Default constructor.
	 *
	 * <p>
	 * Can be used when providing a subclass that overrides the {@link #find(String, Locale)} method.
	 */
	protected BasicFileFinder() {
		this.roots = new LocalDir[0];
		this.cachingLimit = -1;
		this.include = new Pattern[0];
		this.exclude = new Pattern[0];
		this.hashCode = HashCode.of(getClass(), roots, cachingLimit, getIncludePatterns(), getExcludePatterns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// FileFinder methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FileFinder */
	public final Optional<InputStream> getStream(String name, Locale locale) throws IOException {
		return find(name, locale);
	}

	@Override /* FileFinder */
	public final Optional<InputStream> getStream(String name) throws IOException {
		return find(name, null);
	}

	@Override /* FileFinder */
	public final Optional<String> getString(String name) throws IOException {
		return Optional.ofNullable(read(find(name, null).orElse(null)));
	}

	@Override /* FileFinder */
	public Optional<String> getString(String name, Locale locale) throws IOException {
		return Optional.ofNullable(read(find(name, locale).orElse(null)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * The main implementation method for finding files.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own handling.
	 *
	 * @param name The resource name.
	 * 	See {@link Class#getResource(String)} for format.
	 * @param locale
	 * 	The locale of the resource to retrieve.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return The resolved resource contents, or <jk>null</jk> if the resource was not found.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected Optional<InputStream> find(String name, Locale locale) throws IOException {
		name = StringUtils.trimSlashesAndSpaces(name);

		if (isInvalidPath(name))
			return Optional.empty();

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

			if (lf != null && isIgnoredFile(lf.getName()))
				lf = null;

			if (lf != null) {
				fileCache.put(name, lf);

				if (cachingLimit >= 0) {
					long size = lf.size();
					if (size > 0 && size <= cachingLimit)
						lf.cache();
				}
			}
		}

		if (lf == null)
			return Optional.empty();

		return Optional.of(lf.read());
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

	/**
	 * Returns <jk>true</jk> if the file should be ignored based on file name.
	 *
	 * @param name The name to check.
	 * @return <jk>true</jk> if the file should be ignored.
	 */
	protected boolean isIgnoredFile(String name) {
		for (Pattern p : exclude)
			if (p.matcher(name).matches())
				return true;
		for (Pattern p : include)
			if (p.matcher(name).matches())
				return false;
		return true;
	}

	private List<String> getIncludePatterns() {
		return asList(include).stream().map(x->x.pattern()).collect(toList());
	}

	private List<String> getExcludePatterns() {
		return asList(include).stream().map(x->x.pattern()).collect(toList());
	}

	/**
	 * Returns the properties defined on this bean as a simple map for debugging purposes.
	 *
	 * <p>
	 * Use <c>SimpleJson.<jsf>DEFAULT</jsf>.println(<jv>thisBean</jv>)</c> to dump the contents of this bean to the console.
	 *
	 * @return A new map containing this bean's properties.
	 */
	public OMap toMap() {
		return OMap
			.create()
			.filtered()
			.a("class", getClass().getSimpleName())
			.a("roots", roots)
			.a("cachingLimit", cachingLimit)
			.a("include", getIncludePatterns())
			.a("exclude", getExcludePatterns())
			.a("hashCode", hashCode)
		;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return o instanceof BasicFileFinder && eq(this, (BasicFileFinder)o, (x,y)->eq(x.hashCode, y.hashCode) && eq(x.getClass(), y.getClass()) && eq(x.roots, y.roots) && eq(x.cachingLimit, y.cachingLimit) && eq(x.getIncludePatterns(), y.getIncludePatterns()) && eq(x.getExcludePatterns(), y.getExcludePatterns()));
	}

	@Override
	public String toString() {
		return toMap().toString();
	}
}
