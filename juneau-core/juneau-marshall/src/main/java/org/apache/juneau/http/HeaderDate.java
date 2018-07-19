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
 * Category of headers that consist of a single HTTP-date.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public class HeaderDate {

	private final java.util.Date date;
	private final String raw;

	/**
	 * Constructor.
	 *
	 * @param raw The raw header value.
	 */
	protected HeaderDate(String raw) {
		this.raw = raw;
		this.date = DateUtils.parseDate(raw);
	}

	/**
	 * Returns this header value as a {@link java.util.Date}.
	 *
	 * @return This header value as a {@link java.util.Date}, or <jk>null</jk> if the header could not be parsed.
	 */
	public java.util.Date asDate() {
		return date;
	}

	@Override /* Object */
	public String toString() {
		return raw;
	}
}
