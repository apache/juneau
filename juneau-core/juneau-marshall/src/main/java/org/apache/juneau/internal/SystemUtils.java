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

/**
 * System utilities.
 */
public class SystemUtils {

	/**
	 * Returns the first non-<jk>null</jk> system property.
	 * 
	 * @param def
	 * 	The default value if none are found.
	 * 	Can be <jk>null</jk>.
	 * @param keys
	 * 	The system properties to look for.
	 * @return
	 * 	The first non-<jk>null</jk> system property, or the default value if non were found.
	 */
	public static String getFirstString(String def, String...keys) {
		for (String key : keys) {
			String v = System.getProperty(key);
			if (v != null)
				return v;
		}
		return def;
	}

	/**
	 * Returns the first non-<jk>null</jk> boolean system property.
	 * 
	 * @param def
	 * 	The default value if none are found.
	 * 	Can be <jk>null</jk>.
	 * @param keys
	 * 	The system properties to look for.
	 * @return
	 * 	The first non-<jk>null</jk> system property, or the default value if non were found.
	 */
	public static Boolean getFirstBoolean(Boolean def, String...keys) {
		String s = getFirstString(null, keys);
		return s == null ? def : Boolean.parseBoolean(s);
	}

	/**
	 * Returns the first non-<jk>null</jk> integer system property.
	 * 
	 * @param def
	 * 	The default value if none are found.
	 * 	Can be <jk>null</jk>.
	 * @param keys
	 * 	The system properties to look for.
	 * @return
	 * 	The first non-<jk>null</jk> system property, or the default value if non were found.
	 */
	public static Integer getFirstInteger(Integer def, String...keys) {
		String s = getFirstString(null, keys);
		return s == null ? def : Integer.parseInt(s);
	}
}
