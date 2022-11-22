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
package org.apache.juneau.internal;

import java.util.*;

/**
 * Class-related utility methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class ResourceBundleUtils {

	private static final ResourceBundle EMPTY = new ResourceBundle() {
		@Override
		protected Object handleGetObject(String key) {
			return null;
		}
		@Override
		public Enumeration<String> getKeys() {
			return Collections.emptyEnumeration();
		}
	};

	/**
	 * Same as {@link ResourceBundle#getBundle(String, Locale, ClassLoader)} but never throws a {@link MissingResourceException}.
	 *
	 * @param baseName The base name of the resource bundle, a fully qualified class name.
	 * @param locale The locale for which a resource bundle is desired.
	 * @param loader The class loader from which to load the resource bundle.
	 * @return The matching resource bundle, or <jk>null</jk> if it could not be found.
	 */
	public static ResourceBundle findBundle(String baseName, Locale locale, ClassLoader loader) {
		try {
			return ResourceBundle.getBundle(baseName, locale, loader);
		} catch (MissingResourceException e) {}
		return null;
	}

	/**
	 * Returns an empty resource bundle.
	 *
	 * @return An empty resource bundle.
	 */
	public static ResourceBundle empty() {
		return EMPTY;
	}
}
