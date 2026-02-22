/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.annotation;

/**
 * Static strings used for Swagger parameter format types.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification/v2/#data-types">Swagger 2.0 Data Types</a>
 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification/#data-types">OpenAPI 3.0 Data Types</a>
 * </ul>
 */
public class FormatType {

	/**
	 * Prevents instantiation.
	 */
	private FormatType() {}

	/** Signed 32-bit integer format. */
	public static final String INT32 = "int32";

	/** Signed 64-bit integer format (long). */
	public static final String INT64 = "int64";

	/** Single precision floating point number. */
	public static final String FLOAT = "float";

	/** Double precision floating point number. */
	public static final String DOUBLE = "double";

	/** Base64-encoded characters. */
	public static final String BYTE = "byte";

	/** Binary data (octet stream). */
	public static final String BINARY = "binary";

	/** Full-date notation as defined by RFC 3339, section 5.6 (e.g., "2017-07-21"). */
	public static final String DATE = "date";

	/** Date-time notation as defined by RFC 3339, section 5.6 (e.g., "2017-07-21T17:32:28Z"). */
	public static final String DATE_TIME = "date-time";

	/** Password input - hints UIs to mask the input. */
	public static final String PASSWORD = "password";

	/** UON (URL-Encoding Object Notation) - Juneau-specific format. */
	public static final String UON = "uon";
}