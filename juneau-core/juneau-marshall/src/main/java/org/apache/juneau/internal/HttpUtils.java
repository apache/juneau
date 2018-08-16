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
	 * @param detectMethod Whether we should auto-detect the HTTP method name from the Java method name.
	 * @return The REST path or <jk>null</jk> if not detected.
	 */
	public static String detectHttpPath(Method m, boolean detectMethod) {
		String n = m.getName();
		if (detectMethod) {
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
		}
		return '/' + n;
	}

	/**
	 * Given a Java method, returns the arguments signature.
	 *
	 * @param m The Java method.
	 * @param full Whether fully-qualified names should be used for arguments.
	 * @return The arguments signature for the specified method.
	 */
	public static String getMethodArgsSignature(Method m, boolean full) {
		StringBuilder sb = new StringBuilder();
		Class<?>[] pt = m.getParameterTypes();
		if (pt.length == 0)
			return "";
		sb.append('(');
		for (int i = 0; i < pt.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append(full ? ClassUtils.getReadableClassName(pt[i]) : ClassUtils.getSimpleName(pt[i]));
		}
		sb.append(')');
		return sb.toString();
	}
}
