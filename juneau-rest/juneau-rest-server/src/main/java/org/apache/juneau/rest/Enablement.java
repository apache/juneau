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
package org.apache.juneau.rest;

import org.apache.juneau.internal.*;

/**
 * Represents the enablement settings of a feature.
 */
public enum Enablement {

	/**
	 * Feature is always enabled.
	 */
	TRUE,

	/**
	 * Feature is enabled per HTTP request.
	 */
	PER_REQUEST,

	/**
	 * Feature is disabled.
	 */
	FALSE;

	/**
	 * Retrieves this enum using case-insensitive matching.
	 *
	 * @param s The enum name to resolve.
	 * @return The resolved value.
	 */
	public static Enablement fromString(String s) {
		if (! StringUtils.isEmpty(s)) {
			try {
				return valueOf(s.replace('-', '_').toUpperCase());
			} catch (IllegalArgumentException  e) {}
		}
		return null;
	}
}

