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
package org.apache.juneau.rest.logger;

import org.apache.juneau.common.internal.*;

/**
 * Represents the amount of detail to include in a log entry for HTTP requests and responses.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.LoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 */
public enum CallLoggingDetail {

	/**
	 * Lowest detail - Log only the request and response status lines.
	 */
	STATUS_LINE,

	/**
	 * Medium detail - Log status lines and also headers.
	 */
	HEADER,

	/**
	 * Highest detail - Log status lines, headers, and bodies if available.
	 */
	ENTITY;

	boolean isOneOf(CallLoggingDetail...values) {
		for (CallLoggingDetail v : values)
			if (v == this)
				return true;
		return false;
	}

	/**
	 * Retrieves this enum using case-insensitive matching.
	 *
	 * @param s The enum name to resolve.
	 * @return The resolved value.
	 */
	public static CallLoggingDetail fromString(String s) {
		if (! StringUtils.isEmpty(s)) {
			try {
				return valueOf(s.toUpperCase());
			} catch (IllegalArgumentException  e) {}
		}
		return null;
	}
}
