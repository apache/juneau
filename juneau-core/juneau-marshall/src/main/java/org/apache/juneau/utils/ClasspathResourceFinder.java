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


import java.io.*;
import java.util.*;

/**
 * Interface for finding classpath resources.
 * 
 * <p>
 * Essentially a wrapper around {@link Class#getResourceAsStream(String)}, but with support for looking up resources
 * with localized names (e.g. <js>"myfile_ja_JP.txt"</js>).
 * 
 * <p>
 * The following predefined implementations are provided:
 * <ul>
 * 	<li>{@link ClasspathResourceFinderSimple} - Simple searching of classpath.
 * 	<li>{@link ClasspathResourceFinderBasic} - Same as above, but looks in local JVM working directory if resource
 * 		can't be found on classpath.
 * 	<li>{@link ClasspathResourceFinderRecursive} - Same as above, except if the resource can't be found on the
 * 		classpath relative to the base class, recursively searches up the parent class hierarchy.
 * </ul>
 */
public interface ClasspathResourceFinder {
	
	/**
	 * Represents "no" classpath resource finder.
	 */
	public static final class Null implements ClasspathResourceFinder {
		@Override
		public InputStream findResource(Class<?> baseClass, String name, Locale locale) throws IOException {
			throw new NoSuchMethodError();
		}
	}

	/**
	 * Returns the contents of the resource with the specified name.
	 * 
	 * @param baseClass 
	 * 	The class to use to retrieve the resource.
	 * @param name The resource name.  
	 * 	See {@link Class#getResource(String)} for format.
	 * @param locale 
	 * 	The locale of the resource to retrieve.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return The resolved resource contents, or <jk>null</jk> if the resource was not found.
	 * @throws IOException
	 */
	InputStream findResource(Class<?> baseClass, String name, Locale locale) throws IOException;
}
