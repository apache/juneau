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

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

/**
 * Represents a parsed URL path-info string.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class UrlPath {

	final String[] parts;
	final String path;

	/**
	 * Creates a new parsed {@link UrlPath} object from the specified string.
	 *
	 * @param path The path to create.  Must be <jk>null</jk> or or start with '/' per HttpServletRequest.getPathInfo().
	 * @return A new {@link UrlPath} object.
	 */
	public static UrlPath of(String path) {
		if (path != null && ! path.startsWith("/"))
			throw new RuntimeException("Invalid path specified.  Must be null or start with '/' per HttpServletRequest.getPathInfo().");
		return new UrlPath(path);
	}

	/**
	 * Constructor.
	 *
	 * @param path The path.
	 */
	UrlPath(String path) {
		this.path = path;
		parts = path == null ? new String[0] : split(path.substring(1), '/');
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
	 * Returns the filename portion of the path if there is one.
	 *
	 * <p>
	 * For example, given the path <js>"/foo/bar.txt"</js>, this returns <js>"bar.txt"</js>.
	 *
	 * @return The filename portion of the path, or <jk>null</jk> if the path doesn't match a file name.
	 */
	public Optional<String> getFileName() {
		if (parts.length == 0)
			return empty();
		String p = parts[parts.length-1];
		if (p.indexOf('.') == -1)
			return empty();
		return optional(p);
	}

	/**
	 * Returns the raw path passed into this object.
	 *
	 * @return The raw path passed into this object.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns <jk>true</jk> if this path ends with a slash.
	 *
	 * @return <jk>true</jk> if this path ends with a slash.
	 */
	public boolean isTrailingSlash() {
		return path != null && path.endsWith("/");
	}

	@Override /* Object */
	public String toString() {
		return filteredMap()
			.append("raw", path)
			.append("parts", parts)
			.asReadableString();
	}
}
