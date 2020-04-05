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
package org.apache.juneau.http;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Origin</l> HTTP request header.
 */
@Header("Origin")
public final class Origin extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Returns a parsed <c>Origin</c> header.
	 *
	 * @param value The <c>Origin</c> header string.
	 * @return The parsed <c>Origin</c> header, or <jk>null</jk> if the string was null.
	 */
	public static Origin of(String value) {
		if (value == null)
			return null;
		return new Origin(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value The value for this header.
	 */
	public Origin(String value) {
		super("Origin", value);
	}
}
