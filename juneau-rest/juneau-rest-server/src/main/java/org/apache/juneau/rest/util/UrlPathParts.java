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
package org.apache.juneau.rest.util;

import static org.apache.juneau.internal.StringUtils.*;

/**
 * Represents a parsed URL path.
 */
public class UrlPathParts {

	final String[] parts;
	final String raw;

	/**
	 * Constructor.
	 *
	 * @param path The path.
	 */
	public UrlPathParts(String path) {
		path = emptyIfNull(path);
		raw = path;
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		parts = split(path, '/');
		for (int i = 0; i < parts.length; i++)
			parts[i] = urlDecode(parts[i]);
	}

	/**
	 * Returns the path parts.
	 *
	 * @return The path parts.
	 */
	public String[] getParts() {
		return parts;
	}

	/**
	 * Returns a path remainder given the specified number of prefix parts.
	 *
	 * @param i The number of prefix parts to discard.
	 * @return The remainder.
	 */
	public String getRemainder(int i) {
		String s = raw;
		if (s.length() > 0 && s.charAt(0) == '/')
			s = s.substring(1);
		for (int j = 0; j < s.length(); j++) {
			if (i == 0)
				return s.substring(j);
			if (i > 0 && s.charAt(j) == '/')
				i--;
		}
		return isTrailingSlash() ? "" : null;
	}

	/**
	 * Returns <jk>true</jk> if this path ends with a slash.
	 *
	 * @return <jk>true</jk> if this path ends with a slash.
	 */
	public boolean isTrailingSlash() {
		return raw.endsWith("/");
	}
}
