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
import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Utility class for finding regular or localized files on the classpath and file system.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Constructor a file source that looks for files in the "files" working directory, then in the
 * 	// package "foo.bar", then in the package "foo.bar.files", then in the package "files".</jc>
 * 	FileFinder <jv>finder</jv> = FileFinder
 * 		.<jsm>create</jsm>()
 * 		.dir(<js>"files"</js>)
 * 		.cp(foo.bar.MyClass.<jk>class</jk>,<jk>null</jk>,<jk>true</jk>)
 * 		.cp(foo.bar.MyClass.<jk>class</jk>,<js>"files"</js>,<jk>true</jk>)
 * 		.cp(foo.bar.MyClass.<jk>class</jk>,<js>"/files"</js>,<jk>true</jk>)
 * 		.cache(1_000_000l)  <jc>// Cache files less than 1MB in size.</jc>
 * 		.ignore(Pattern.<jsm>compile</jsm>(<js>"(?i)(.*\\.(class|properties))|(package.html)"</js>)) <jc>// Ignore certain files.</jc>
 * 		.build();
 *
 * 	<jc>// Find a normal file.</jc>
 * 	InputStream <jv>is1</jv> = <jv>finder</jv>.getStream(<js>"text.txt"</js>);
 *
 * 	<jc>// Find a localized file called "text_ja_JP.txt".</jc>
 * 	InputStream <jv>is2</jv> = <jv>finder</jv>.getStream(<js>"text.txt"</js>, Locale.<jsf>JAPAN</jsf>);
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
 * The default implementation of this interface is {@link BasicFileFinder}.
 * The {@link Builder#type(Class)} method is provided for instantiating other instances.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>public class</jk> MyFileFinder <jk>extends</jk> BasicFileFinder {
 * 		<jk>public</jk> MyFileFinder(FileFinder.Builder <jv>builder</jv>) {
 * 			<jk>super</jk>(<jv>builder</jv>);
 * 		}
 * 	}
 *
 * 	<jc>// Instantiate subclass.</jc>
 * 	FileFinder <jv>myFinder</jv> = FileFinder.<jsm>create</jsm>().type(MyFileFinder.<jk>class</jk>).build();
 * </p>
 *
 * <p>
 * Subclasses must provide a public constructor that takes in any of the following arguments:
 * <ul>
 * 	<li>{@link Builder} - The builder object.
 * 	<li>Any beans present in the bean store passed into the constructor.
 * 	<li>Any {@link Optional} beans optionally present in bean store passed into the constructor.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public interface FileFinder {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Represents no file finder */
	public abstract class Void implements FileFinder {}

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	/**
	 * Static creator.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder(BeanStore.INSTANCE);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanBuilder<FileFinder> {

		final Set<LocalDir> roots;
		long cachingLimit;
		Pattern[] include, exclude;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(BasicFileFinder.class, beanStore);
			roots = set();
			cachingLimit = -1;
			include = new Pattern[]{Pattern.compile(".*")};
			exclude = new Pattern[0];
		}

		@Override /* BeanBuilder */
		protected FileFinder buildDefault() {
			return new BasicFileFinder(this);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

		/**
		 * Adds a class subpackage to the lookup paths.
		 *
		 * @param c The class whose package will be added to the lookup paths.  Must not be <jk>null</jk>.
		 * @param path The absolute or relative subpath.
		 * @param recursive If <jk>true</jk>, also recursively adds all the paths of the parent classes as well.
		 * @return This object.
		 */
		@FluentSetter
		public Builder cp(Class<?> c, String path, boolean recursive) {
			assertArgNotNull("c", c);
			while (c != null) {
				roots.add(new LocalDir(c, path));
				c = recursive ? c.getSuperclass() : null;
			}
			return this;
		}

		/**
		 * Adds a file system directory to the lookup paths.
		 *
		 * @param path The path relative to the working directory.  Must not be <jk>null</jk>
		 * @return This object.
		 */
		@FluentSetter
		public Builder dir(String path) {
			assertArgNotNull("path", path);
			return path(Paths.get(".").resolve(path));
		}

		/**
		 * Adds a file system directory to the lookup paths.
		 *
		 * @param path The directory path.
		 * @return This object.
		 */
		@FluentSetter
		public Builder path(Path path) {
			roots.add(new LocalDir(path));
			return this;
		}

		/**
		 * Enables in-memory caching of files for quicker retrieval.
		 *
		 * @param cachingLimit The maximum file size in bytes.
		 * @return This object.
		 */
		@FluentSetter
		public Builder caching(long cachingLimit) {
			this.cachingLimit = cachingLimit;
			return this;
		}

		/**
		 * Specifies the regular expression file name patterns to use to include files being retrieved from the file source.
		 *
		 * @param patterns
		 * 	The regular expression include patterns.
		 * 	<br>The default is <js>".*"</js>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder include(String...patterns) {
			this.include = alist(patterns).stream().map(x->Pattern.compile(x)).toArray(Pattern[]::new);
			return this;
		}

		/**
		 * Specifies the regular expression file name pattern to use to exclude files from being retrieved from the file source.
		 *
		 * @param patterns
		 * 	The regular expression exclude patterns.
		 * 	<br>If none are specified, no files will be excluded.
		 * @return This object.
		 */
		@FluentSetter
		public Builder exclude(String...patterns) {
			this.exclude = alist(patterns).stream().map(x->Pattern.compile(x)).toArray(Pattern[]::new);
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

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
	public Optional<InputStream> getStream(String name, Locale locale) throws IOException;

	/**
	 * Returns the file with the specified name as a string.
	 *
	 * @param name The file name.
	 * @param locale
	 * 	The locale of the resource to retrieve.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return The contents of the file as a string.  Assumes UTF-8 encoding.
	 * @throws IOException If file could not be read.
	 */
	public Optional<String> getString(String name, Locale locale) throws IOException;
}
