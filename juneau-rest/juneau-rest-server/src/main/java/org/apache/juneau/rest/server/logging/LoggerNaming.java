/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.logging;

import java.util.*;

/**
 * Logger-name normalization for resource classes.
 *
 * <p>
 * Walks up known proxy class names and returns the first non-proxy superclass so default logger naming is
 * stable across common runtime-generated proxies while preserving ordinary nested-class names.
 *
 * @since 10.0.0
 */
public final class LoggerNaming {

	private static final List<String> KNOWN_PROXY_INFIXES = List.of(
		"$$SpringCGLIB$$",
		"$$EnhancerBySpringCGLIB$$",
		"$$EnhancerByCGLIB$$",
		"$$FastClassBySpringCGLIB$$",
		"$ByteBuddy$",
		"$Proxy",
		"$HibernateProxy$",
		"$MockitoMock$"
	);

	private LoggerNaming() {}

	/**
	 * Returns the user-class binary name for a class used in default logger derivation.
	 *
	 * <p>
	 * If the supplied class name contains a known proxy infix, this walks superclasses until it finds a
	 * class name without a known proxy infix. Ordinary nested classes are returned unchanged.
	 *
	 * @param c The class to normalize. Must not be <jk>null</jk>.
	 * @return The normalized binary class name.
	 */
	public static String userClassName(Class<?> c) {
		var original = c;
		while (c != null && containsKnownProxyInfix(c.getName()))
			c = c.getSuperclass();
		return (c != null ? c : original).getName();
	}

	private static boolean containsKnownProxyInfix(String className) {
		for (var infix : KNOWN_PROXY_INFIXES)
			if (className.contains(infix))
				return true;
		return false;
	}
}
