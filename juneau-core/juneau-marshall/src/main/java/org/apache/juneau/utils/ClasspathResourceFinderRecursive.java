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
 * Same as {@link ClasspathResourceFinderBasic} but searches for resources up the parent class hierarchy chain.
 */
public class ClasspathResourceFinderRecursive extends ClasspathResourceFinderBasic {

	/**
	 * Reusable instance.
	 */
	public static final ClasspathResourceFinderRecursive INSTANCE = new ClasspathResourceFinderRecursive();

	@Override /* ResourceFinder2 */
	public InputStream findResource(Class<?> baseClass, String name, Locale locale) throws IOException {
		while (baseClass != null) {
			InputStream is = findClasspathResource(baseClass, name, locale);
			if (is != null)
				return is;
			baseClass = baseClass.getSuperclass();
		}
		return findFileSystemResource(name, locale);
	}
}
