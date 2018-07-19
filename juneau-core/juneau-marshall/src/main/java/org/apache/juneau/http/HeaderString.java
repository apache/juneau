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

import org.apache.juneau.internal.*;

/**
 * Category of headers that consist of a single string value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Accept-Ranges: bytes
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public class HeaderString {

	final String value;

	/**
	 * Constructor.
	 *
	 * @param value The raw header value.
	 */
	protected HeaderString(String value) {
		this.value = StringUtils.trim(value);
	}

	/**
	 * Returns <jk>true</jk> if the specified value is the same using {@link String#equalsIgnoreCase(String)}.
	 *
	 * @param compare The value to compare against.
	 * @return <jk>true</jk> if the specified value is the same.
	 */
	public boolean eqIC(String compare) {
		return value.equalsIgnoreCase(compare);
	}

	/**
	 * Returns <jk>true</jk> if the specified value is the same using {@link String#equals(Object)}.
	 *
	 * @param compare The value to compare against.
	 * @return <jk>true</jk> if the specified value is the same.
	 */
	public boolean eq(String compare) {
		return value.equals(compare);
	}

	/**
	 * Returns this header as a simple string value.
	 *
	 * <p>
	 * Functionally equivalent to calling {@link #toString()}.
	 *
	 * @return This header as a simple string.
	 */
	public String asString() {
		return value;
	}

	@Override /* Object */
	public String toString() {
		return value == null ? "" : value;
	}
}
