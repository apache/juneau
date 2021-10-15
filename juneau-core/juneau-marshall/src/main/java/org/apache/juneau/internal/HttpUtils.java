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

import static org.apache.juneau.internal.StringUtils.*;
import java.lang.reflect.*;

/**
 * Utilities.
 */
public class HttpUtils {

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
				if (isOneOf(n2, "GET","PUT","POST","DELETE","OPTIONS","HEAD","CONNECT","TRACE","PATCH"))
					return n2;
			}
			for (String t : new String[]{"get","put","post","delete","options","head","connect","trace","patch"})
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
				if (isOneOf(n2, "GET","PUT","POST","DELETE","OPTIONS","HEAD","CONNECT","TRACE","PATCH"))
					return "/";
			}
			for (String t : new String[]{"get","put","post","delete","options","head","connect","trace","patch"}) {
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
