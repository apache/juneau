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

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;

/**
 * Represents a parsed URL path-info string.
 */
public class UrlPathInfo {

	final String[] parts;
	final String path;

	/**
	 * Constructor.
	 *
	 * @param path The path.
	 */
	public UrlPathInfo(String path) {
		if (path != null && ! path.startsWith("/"))
			throw new RuntimeException("Invalid path specified.  Must be null or start with '/' per HttpServletRequest.getPathInfo().");
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
		return path.endsWith("/");
	}

	/**
	 * Converts this object to a map.
	 *
	 * @return This object converted to a map.
	 */
	public ObjectMap toMap() {
		return new DefaultFilteringObjectMap().append("raw", path).append("parts", parts);
	}

	@Override /* Object */
	public String toString() {
		return SimpleJson.DEFAULT.toString(toMap());
	}
}
