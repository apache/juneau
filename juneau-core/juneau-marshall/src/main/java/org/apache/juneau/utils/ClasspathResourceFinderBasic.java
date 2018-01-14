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
 * Utility class for finding resources for a class.
 * 
 * <p>
 * Same as {@link ClasspathResourceFinderSimple}, but if the resource cannot be found in the classpath, then an attempt 
 * is made to look in the JVM working directory.
 * <br>Path traversals outside the working directory are not allowed for security reasons.
 */
public class ClasspathResourceFinderBasic extends ClasspathResourceFinderSimple {

	@Override /* ClasspathResourceFinder */
	public InputStream findResource(Class<?> baseClass, String name, Locale locale) throws IOException {
		InputStream is = findClasspathResource(baseClass, name, locale);
		if (is != null)
			return is;
		return findFileSystemResource(name, locale);
	}
	
	/**
	 * Workhorse method for retrieving a resource from the file system.
	 * 
	 * <p>
	 * This method can be overridden by subclasses to provide customized handling of resource retrieval from file systems.
	 * 
	 * @param name The resource name.
	 * @param locale 
	 * 	The resource locale.
	 * 	<br>Can be <jk>null</jk>.  
	 * @return The resource stream, or <jk>null</jk> if it couldn't be found.
	 * @throws IOException
	 */
	protected InputStream findFileSystemResource(String name, Locale locale) throws IOException {
		if (name.indexOf("..") == -1) {
			for (String n2 : getCandidateFileNames(name, locale)) {
				File f = new File(n2);
				if (f.exists() && f.canRead() && ! f.isAbsolute()) {
					return new FileInputStream(f);
				}
			}
		}
		return null;
	}
}
