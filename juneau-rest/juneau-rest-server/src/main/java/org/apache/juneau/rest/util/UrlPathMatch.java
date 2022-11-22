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

import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Represents a URL path pattern match.
 *
 * For example, given the pattern <js>"/foo/{bar}/*"</js> and the path <js>"/foo/123/baz/qux"</js>, this match gives
 * you a map containing <js>"{bar:123}"</js> and a remainder string containing <js>"baz/qux"</js>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class UrlPathMatch {

	private final int matchedParts;
	private final String path;
	private final Map<String,String> vars;

	/**
	 * Constructor.
	 *
	 * @param path The path being matched against.  Can be <jk>null</jk>.
	 * @param matchedParts The number of parts that were matched against the path.
	 * @param keys The variable keys.  Can be <jk>null</jk>.
	 * @param values The variable values.  Can be <jk>null</jk>.
	 */
	protected UrlPathMatch(String path, int matchedParts, String[] keys, String[] values) {
		this.path = path;
		this.matchedParts = matchedParts;
		this.vars = keys == null ? Collections.emptyMap() : new SimpleMap<>(keys, values);
	}

	/**
	 * Returns a map of the path variables and values.
	 *
	 * @return
	 * 	An unmodifiable map of variable keys/values.
	 * 	<br>Returns an empty map if no variables were found in the path.
	 */
	public Map<String,String> getVars() {
		return vars;
	}

	/**
	 * Returns <jk>true</jk> if this match contains one or more variables.
	 *
	 * @return <jk>true</jk> if this match contains one or more variables.
	 */
	public boolean hasVars() {
		return ! vars.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if any of the variable values are blank.
	 *
	 * @return <jk>true</jk> if any of the variable values are blank.
	 */
	public boolean hasEmptyVars() {
		for (String v : vars.values())
			if (isEmpty(v))
				return true;
		return false;
	}

	/**
	 * Returns the remainder of the path after the pattern match has been made.
	 *
	 * <p>
	 * Same as {#link {@link #getSuffix()} but trims the leading slash if there is one.
	 *
	 * @return The remainder of the path after the pattern match has been made.
	 */
	public String getRemainder() {
		String suffix = getSuffix();
		if (isNotEmpty(suffix) && suffix.charAt(0) == '/')
			suffix = suffix.substring(1);
		return suffix;
	}

	/**
	 * Returns the remainder of the URL after the pattern was matched.
	 *
	 * @return
	 * The remainder of the URL after the pattern was matched.
	 * <br>Can be <jk>null</jk> if nothing remains to be matched.
	 * <br>Otherwise, always starts with <js>'/'</js>.
	 */
	public String getSuffix() {
		String s = path;
		for (int j = 0; j < matchedParts; j++) {
			int k = s.indexOf('/', 1);
			if (k == -1)
				return null;
			s = s.substring(k);
		}
		return s;
	}

	/**
	 * Returns the part of the URL that the pattern matched against.
	 *
	 * @return
	 * The part of the URL that the pattern matched against.
	 * <br>Can be <jk>null</jk> if nothing matched.
	 * <br>Otherwise, always starts with <js>'/'</js>.
	 */
	public String getPrefix() {
		int c = 0;
		for (int j = 0; j < matchedParts; j++) {
			c = path.indexOf('/', c+1);
			if (c == -1)
				c = path.length();
		}
		return nullIfEmpty(path.substring(0, c));
	}

	@Override /* Object */
	public String toString() {
		return filteredMap()
			.append("v", getVars())
			.append("r", getRemainder())
			.asString();
	}
}

