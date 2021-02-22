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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link FileFinder} objects.
 */
@FluentSetters
public class FileFinderBuilder {

	final Set<LocalDir> roots = new LinkedHashSet<>();
	long cachingLimit = -1;
	List<Pattern> include = AList.of(Pattern.compile(".*")), exclude = AList.create();
	private Class<? extends FileFinder> implClass;
	private BeanFactory beanFactory;

	/**
	 * Create a new {@link FileFinder} using this builder.
	 *
	 * @return A new {@link FileFinder}
	 */
	public FileFinder build() {
		try {
			Class<? extends FileFinder> ic = isConcrete(implClass) ? implClass : getDefaultImplClass();
			return BeanFactory.of(beanFactory).addBeans(FileFinderBuilder.class, this).createBean(ic);
		} catch (ExecutableException e) {
			throw new RuntimeException(e.getCause().getMessage(), e.getCause());
		}
	}

	/**
	 * Specifies the default implementation class if not specified via {@link #implClass(Class)}.
	 *
	 * @return The default implementation class if not specified via {@link #implClass(Class)}.
	 */
	protected Class<? extends FileFinder> getDefaultImplClass() {
		return BasicFileFinder.class;
	}

	/**
	 * Adds a class subpackage to the lookup paths.
	 *
	 * @param c The class whose package will be added to the lookup paths.  Must not be <jk>null</jk>.
	 * @param path The absolute or relative subpath.
	 * @param recursive If <jk>true</jk>, also recursively adds all the paths of the parent classes as well.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public FileFinderBuilder cp(Class<?> c, String path, boolean recursive) {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public FileFinderBuilder dir(String path) {
		assertArgNotNull("path", path);
		return path(Paths.get(".").resolve(path));
	}

	/**
	 * Adds a file system directory to the lookup paths.
	 *
	 * @param path The directory path.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public FileFinderBuilder path(Path path) {
		roots.add(new LocalDir(path));
		return this;
	}

	/**
	 * Enables in-memory caching of files for quicker retrieval.
	 *
	 * @param cachingLimit The maximum file size in bytes.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public FileFinderBuilder caching(long cachingLimit) {
		this.cachingLimit = cachingLimit;
		return this;
	}

	/**
	 * Specifies the regular expression file name patterns to use to include files being retrieved from the file source.
	 *
	 * @param patterns
	 * 	The regular expression include patterns.
	 * 	<br>The default is <js>".*"</js>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public FileFinderBuilder include(String...patterns) {
		this.include = Arrays.asList(patterns).stream().map(x->Pattern.compile(x)).collect(Collectors.toList());
		return this;
	}

	/**
	 * Specifies the regular expression file name pattern to use to exclude files from being retrieved from the file source.
	 *
	 * @param patterns
	 * 	The regular expression exclude patterns.
	 * 	<br>If none are specified, no files will be excluded.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public FileFinderBuilder exclude(String...patterns) {
		this.exclude = Arrays.asList(patterns).stream().map(x->Pattern.compile(x)).collect(Collectors.toList());
		return this;
	}

	/**
	 * Specifies the bean factory to use for instantiating the {@link FileFinder} object.
	 *
	 * <p>
	 * Can be used to instantiate {@link FileFinder} implementations with injected constructor argument beans.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public FileFinderBuilder beanFactory(BeanFactory value) {
		this.beanFactory = value;
		return this;
	}

	/**
	 * Specifies a subclass of {@link FileFinder} to create when the {@link #build()} method is called.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public FileFinderBuilder implClass(Class<? extends FileFinder> value) {
		this.implClass = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
