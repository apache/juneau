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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshall.*;

/**
 * Represents a URL path pattern match.
 *
 * For example, given the pattern <js>"/foo/{bar}/*"</js> and the path <js>"/foo/123/baz/qux"</js>, this match gives
 * you a map containing <js>"{bar:123}"</js> and a remainder string containing <js>"baz/qux"</js>.
 */
public class UrlPathPatternMatch {

	private final String remainder;
	private final Map<String,String> vars;

	/**
	 * Constructor.
	 *
	 * @param keys The variable keys.  Can be <jk>null</jk>.
	 * @param values The variable values.  Can be <jk>null</jk>.
	 * @param remainder
	 */
	protected UrlPathPatternMatch(String[] keys, String[] values, String remainder) {
		this.remainder = remainder;
		this.vars = keys == null ? Collections.emptyMap() : new SimpleMap<>(keys, values);
	}

	/**
	 * Returns a map of the path variables and values.
	 *
	 * @return An unmodifiable map of variable keys/values.
	 */
	public Map<String,String> getVars() {
		return vars;
	}

	/**
	 * Returns the remainder of the path after the pattern match has been made.
	 *
	 * @return The remainder of the path after the pattern match has been made.
	 */
	public String getRemainder() {
		return remainder;
	}

	/**
	 * Converts this object to a map.
	 *
	 * @return This object converted to a map.
	 */
	public ObjectMap toMap() {
		return new DefaultFilteringObjectMap().append("v", getVars()).append("r", getRemainder());
	}

	@Override /* Object */
	public String toString() {
		return SimpleJson.DEFAULT.toString(toMap());
	}
}

