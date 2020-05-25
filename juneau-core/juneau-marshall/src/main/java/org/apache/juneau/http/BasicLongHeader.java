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
import org.apache.juneau.internal.*;

/**
 * Category of headers that consist of a single long value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Content-Length: 300
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header(type="integer",format="int64")
public class BasicLongHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	private final Long value;

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name.
	 * @param value The raw header value.
	 */
	public BasicLongHeader(String name, Object value) {
		super(name, StringUtils.asString(value));
		this.value = toLong(value);
	}

	private static Long toLong(Object value) {
		if (value instanceof Long)
			return (Long)value;
		String s = value.toString();
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e2) {}
		return 0l;
	}

	/**
	 * Returns this header as a simple string value.
	 *
	 * <p>
	 * Functionally equivalent to calling {@link #toString()}.
	 *
	 * @return This header as a simple string.
	 */
	public long asLong() {
		return value;
	}
}
