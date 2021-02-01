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

import java.io.*;
import java.util.*;

/**
 * Utility class for finding regular or localized files on the classpath and file system.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
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
 * The {@link FileFinderBuilder#implClass(Class)} method is provided for instantiating other instances.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jk>public class</jk> MyFileFinder <jk>extends</jk> BasicFileFinder {
 * 		<jk>public</jk> MyFileFinder(FileFinderBuilder <jv>builder</jv>) {
 * 			<jk>super</jk>(builder);
 * 		}
 * 	}
 *
 * 	<jc>// Instantiate subclass.</jc>
 * 	FileFinder <jv>myFinder</jv> = FileFinder.<jsm>create</jsm>().implClass(MyFileFinder.<jk>class</jk>).build();
 * </p>
 *
 * <p>
 * Subclasses must provide a public constructor that takes in any of the following arguments:
 * <ul>
 * 	<li>{@link FileFinderBuilder} - The builder object.
 * 	<li>Any beans present in the registered {@link FileFinderBuilder#beanFactory(BeanFactory) bean factory}.
 * 	<li>Any {@link Optional} beans optionally present in the registered {@link FileFinderBuilder#beanFactory(BeanFactory) bean factory}.
 * </ul>
 */
public interface FileFinder {

	/** Represents no file finder */
	public abstract class Null implements FileFinder {}

	/**
	 * Instantiate a new builder.
	 *
	 * @return A new builder.
	 */
	public static FileFinderBuilder create() {
		return new FileFinderBuilder();
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
	public Optional<InputStream> getStream(String name, Locale locale) throws IOException;

	/**
	 * Returns the file with the specified name.
	 *
	 * @param name The file name.
	 * @return An input stream to the file if it exists, or <jk>null</jk> if it does not.
	 * @throws IOException If file could not be read.
	 */
	public Optional<InputStream> getStream(String name) throws IOException;

	/**
	 * Returns the file with the specified name as a string.
	 *
	 * @param name The file name.
	 * @return The contents of the file as a string.  Assumes UTF-8 encoding.
	 * @throws IOException If file could not be read.
	 */
	public Optional<String> getString(String name) throws IOException;

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
