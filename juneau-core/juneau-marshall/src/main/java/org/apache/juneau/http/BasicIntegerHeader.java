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
 * Category of headers that consist of a single integer value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Age: 300
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header(type="integer",format="int32")
public class BasicIntegerHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	private final Integer value;

	/**
	 * Constructor.
	 *
	 * @param name HTTP header name.
	 * @param value The raw header value.
	 */
	protected BasicIntegerHeader(String name, Object value) {
		super(name, StringUtils.asString(value));
		this.value = toInt(value);
	}

	private static Integer toInt(Object value) {
		if (value instanceof Integer)
			return (Integer)value;
		String s = value.toString();
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			try {
				Long.parseLong(s);
				return Integer.MAX_VALUE;
			} catch (NumberFormatException e2) {}
		}
		return 0;
	}

	/**
	 * Returns this header as a simple string value.
	 *
	 * <p>
	 * Functionally equivalent to calling {@link #toString()}.
	 *
	 * @return This header as a simple string.
	 */
	public int asInt() {
		return value;
	}
}
