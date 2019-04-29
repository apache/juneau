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

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.utils.*;

/**
 * Convenience wrapper around a {@link StringResolver} that also constructs string-based objects.
 *
 */
public class ObjectResolver {

	private final StringResolver r;

	/**
	 * Constructor.
	 *
	 * @param r
	 */
	public ObjectResolver(StringResolver r) {
		this.r = r == null ? StringResolver.NOOP : r;
	}

	/**
	 * Resolves the specified string.
	 *
	 * @param in The string containing variables to resolve.
	 * @return The resolved string.
	 */
	public String string(String in) {
		return r.resolve(in);
	}

	/**
	 * Resolves the specified strings in the string array.
	 *
	 * @param in The string array containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	public String[] strings(String[] in) {
		String[] out = new String[in.length];
		for (int i = 0; i < in.length; i++)
			out[i] = r.resolve(in[i]);
		return out;
	}

	/**
	 * Resolves the specified string and converts it to a boolean.
	 *
	 * @param in The string containing variables to resolve.
	 * @return The resolved boolean.
	 */
	public boolean bool(String in) {
		return Boolean.parseBoolean(r.resolve(in));
	}

	/**
	 * Resolves the specified string and converts it to an int.
	 *
	 * @param in The string containing variables to resolve.
	 * @return The resolved int.
	 */
	public int integer(String in) {
		return Integer.parseInt(r.resolve(in));
	}

	/**
	 * Resolves the specified string and converts it to a Locale.
	 *
	 * @param in The string containing variables to resolve.
	 * @return The resolved Locale.
	 */
	public Locale locale(String in) {
		return Locale.forLanguageTag(r.resolve(in));
	}

	/**
	 * Resolves the specified string and converts it to a MediaType.
	 *
	 * @param in The string containing variables to resolve.
	 * @return The resolved MediaType.
	 */
	public MediaType mediaType(String in) {
		return MediaType.forString(r.resolve(in));
	}

	/**
	 * Resolves the specified string and converts it to a TimeZone.
	 *
	 * @param in The string containing variables to resolve.
	 * @return The resolved TimeZone.
	 */
	public TimeZone timeZone(String in) {
		return TimeZone.getTimeZone(r.resolve(in));
	}

	/**
	 * Resolves the specified string and converts it to a Visibility.
	 *
	 * @param in The string containing variables to resolve.
	 * @return The resolved Visibility.
	 */
	public Visibility visibility(String in) {
		return Visibility.valueOf(r.resolve(in));
	}
}
