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
package org.apache.juneau.rest.client2;

import java.util.*;

/**
 * Identifies how to add elements to a list.
 */
public enum AddFlag {

	/**
	 * Any previous value(s) should be overwritten/replaced.
	 */
	APPEND,

	/**
	 * Any previous value(s) should be overwritten/replaced.
	 */
	PREPEND,

	/**
	 * Any previous value(s) should be overwritten/replaced.
	 */
	REPLACE,

	/**
	 * Don't add the value if it's <jk>null</jk> or an empty string.
	 */
	SKIP_IF_EMPTY;

	/**
	 * Default flags.
	 */
	public static final EnumSet<AddFlag> DEFAULT_FLAGS = EnumSet.of(APPEND);

	/**
	 * Default skip-if-empty flags.
	 */
	public static final EnumSet<AddFlag> SKIP_IF_EMPTY_FLAGS = EnumSet.of(APPEND, SKIP_IF_EMPTY);

	/**
	 * Returns {@link #DEFAULT_FLAGS} if the enum set is <jk>null</jk> or empty.
	 *
	 * @param s The set to check.
	 * @return Either the same set or {@link #DEFAULT_FLAGS}.  Never <jk>null</jk>.
	 */
	public static EnumSet<AddFlag> orDefault(EnumSet<AddFlag> s) {
		return s == null || s.isEmpty() ? DEFAULT_FLAGS : s;
	}
}
