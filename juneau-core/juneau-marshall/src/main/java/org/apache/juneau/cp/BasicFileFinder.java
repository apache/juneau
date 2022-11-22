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

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.FileUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.io.*;
import java.util.*;
import java.util.ResourceBundle.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;

/**
 * Basic implementation of a {@link FileFinder}.
 *
 * <p>
 * Specialized behavior can be implemented by overridding the {@link #find(String, Locale)} method.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>public class</jk> MyFileFinder <jk>extends</jk> BasicFileFinder {
 * 		<ja>@Override</ja>
 * 		<jk>protected</jk> Optional&lt;InputStream&gt; find(String <jv>name</jv>, Locale <jv>locale</jv>) <jk>throws</jk> IOException {
 * 			<jc>// Do special handling or just call super.find().</jc>
 * 			<jk>return super</jk>.find(<jv>name</jv>, <jv>locale</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class BasicFileFinder implements FileFinder {


	private static final ResourceBundle.Control RB_CONTROL = ResourceBundle.Control.getControl(Control.FORMAT_DEFAULT);

	private final Map<String,LocalFile> files = new ConcurrentHashMap<>();
	private final Map<Locale,Map<String,LocalFile>> localizedFiles = new ConcurrentHashMap<>();

	private final LocalDir[] roots;
	private final long cachingLimit;
	private final Pattern[] include, exclude;
	private final String[] includePatterns, excludePatterns;
	private final int hashCode;

	/**
	 * Builder-based constructor.
	 *
	 * @param builder The builder object.
	 */
	public BasicFileFinder(FileFinder.Builder builder) {
		this.roots = builder.roots.toArray(new LocalDir[builder.roots.size()]);
		this.cachingLimit = builder.cachingLimit;
		this.include = builder.include;
		this.exclude = builder.exclude;
		this.includePatterns = alist(include).stream().map(x->x.pattern()).toArray(String[]::new);
		this.excludePatterns = alist(exclude).stream().map(x->x.pattern()).toArray(String[]::new);
		this.hashCode = HashCode.of(getClass(), roots, cachingLimit, includePatterns, excludePatterns);
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
		this.includePatterns = new String[0];
		this.excludePatterns = new String[0];
		this.hashCode = HashCode.of(getClass(), roots, cachingLimit, includePatterns, excludePatterns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// FileFinder methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FileFinder */
	public final Optional<InputStream> getStream(String name, Locale locale) throws IOException {
		return find(name, locale);
	}

	@Override /* FileFinder */
	public Optional<String> getString(String name, Locale locale) throws IOException {
		return optional(read(find(name, locale).orElse(null)));
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
			return empty();

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

		return optional(lf == null ? null : lf.read());
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

		getCandidateLocales(locale).forEach(x -> {
			String ls = x.toString();
			if (ls.isEmpty())
				list.add(fileName);
			else {
				list.add(baseName + "_" + ls + (ext.isEmpty() ? "" : ('.' + ext)));
				list.add(ls.replace('_', '/') + '/' + fileName);
			}
		});

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

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return o instanceof BasicFileFinder && eq(this, (BasicFileFinder)o, (x,y)->eq(x.hashCode, y.hashCode) && eq(x.getClass(), y.getClass()) && eq(x.roots, y.roots) && eq(x.cachingLimit, y.cachingLimit) && eq(x.includePatterns, y.includePatterns) && eq(x.excludePatterns, y.excludePatterns));
	}

	@Override /* Object */
	public String toString() {
		return filteredMap()
			.append("class", getClass().getSimpleName())
			.append("roots", roots)
			.append("cachingLimit", cachingLimit)
			.append("include", includePatterns)
			.append("exclude", excludePatterns)
			.append("hashCode", hashCode)
			.asReadableString();
	}
}
