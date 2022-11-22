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
package org.apache.juneau;

import static org.apache.juneau.common.internal.StringUtils.*;

import java.util.*;

/**
 * Represents the enablement settings of a feature.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public enum Enablement {

	/**
	 * Feature is always enabled.
	 */
	ALWAYS,

	/**
	 * Feature is enabled per HTTP request.
	 */
	CONDITIONAL,

	/**
	 * Feature is disabled.
	 */
	NEVER;

	/**
	 * Retrieves this enum using case-insensitive matching.
	 *
	 * @param s The enum name to resolve.
	 * @return The resolved value, or <jk>null</jk> if no match found.
	 */
	public static Enablement fromString(String s) {
		return MAP.get(emptyIfNull(s).toUpperCase());
	}

	private static final Map<String,Enablement> MAP = new HashMap<>();
	static {
		MAP.put("TRUE",ALWAYS);
		MAP.put("ALWAYS",ALWAYS);
		MAP.put("FALSE",NEVER);
		MAP.put("NEVER",NEVER);
		MAP.put("CONDITIONAL",CONDITIONAL);
	}

	/**
	 * Returns <jk>true</jk> if this enum is one of the specified values.
	 *
	 * @param values The values to check against.
	 * @return <jk>true</jk> if this enum is one of the specified values.
	 */
	public boolean isOneOf(Enablement...values) {
		for (Enablement v : values)
			 if (this == v)
				 return true;
		return false;
	}

	/**
	 * Tests for enablement.
	 *
	 * @param def The default value to use if this is {@link #CONDITIONAL}.
	 * @return <jk>true</jk> if this is {@link #ALWAYS} or {@link #CONDITIONAL} and <c>def</c> is <jk>true</jk>.
	 */
	public boolean isEnabled(boolean def) {
		if (this == ALWAYS)
			return true;
		if (this == NEVER)
			return false;
		return def;
	}
}

