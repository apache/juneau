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

import java.lang.reflect.*;

/**
 * HTTP utilities.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class HttpUtils {

	private static final String[]
		LC_METHODS = new String[]{"get","put","post","delete","options","head","connect","trace","patch"},
		UC_METHODS = new String[]{"GET","PUT","POST","DELETE","OPTIONS","HEAD","CONNECT","TRACE","PATCH"};

	/**
	 * Given a method name, infers the REST method name.
	 *
	 * @param m The Java method.
	 * @param detectMethod Whether we should auto-detect the HTTP method name from the Java method name.
	 * @param def The default HTTP method if not detected.
	 * @return The REST method name, or the default value if not found.
	 */
	public static String detectHttpMethod(Method m, boolean detectMethod, String def) {
		String n = m.getName();
		if (detectMethod) {
			if (n.startsWith("do") && n.length() > 2) {
				String n2 = n.substring(2).toUpperCase();
				for (String t : UC_METHODS)
					if (n2.equals(t))
						return n2;
			}
			for (String t : LC_METHODS)
				if (n.startsWith(t) && (n.length() == t.length() || Character.isUpperCase(n.charAt(t.length()))))
					return t.toUpperCase();
		}
		return def;
	}

	/**
	 * Given a Java method, infers the REST path.
	 *
	 * @param m The Java method.
	 * @param method The HTTP method name if it's known.
	 * @return The REST path or <jk>null</jk> if not detected.
	 */
	public static String detectHttpPath(Method m, String method) {
		String n = m.getName();
		if (method == null) {
			if (n.startsWith("do") && n.length() > 2) {
				String n2 = n.substring(2).toUpperCase();
				for (String t : UC_METHODS)
					if (n2.equals(t))
						return "/";
			}
			for (String t : LC_METHODS) {
				if (n.startsWith(t) && (n.length() == t.length() || Character.isUpperCase(n.charAt(t.length())))) {
					return '/' + java.beans.Introspector.decapitalize(n.substring(t.length()));
				}
			}
		} else {
			if (n.equalsIgnoreCase(method) || n.equals("do") || n.equals("_"))
				return "/";
			if (n.startsWith(method) && (n.length() == method.length() || Character.isUpperCase(n.charAt(method.length())))) {
				return '/' + java.beans.Introspector.decapitalize(n.substring(method.length()));
			}
		}
		return '/' + n;
	}
}
