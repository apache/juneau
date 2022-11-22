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
package org.apache.juneau.http.header;

import java.util.function.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Age</l> HTTP response header.
 *
 * <p>
 * The age the object has been in a proxy cache in seconds.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Age: 12
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Age response-header field conveys the sender's estimate of the amount of time since the response (or its
 * revalidation) was generated at the origin server.
 * A cached response is "fresh" if its age does not exceed its freshness lifetime.
 * Age values are calculated as specified in section 13.2.3.
 *
 * <p class='bcode'>
 * 	Age = "Age" ":" age-value
 * 	age-value = delta-seconds
 * </p>
 *
 * <p>
 * Age values are non-negative decimal integers, representing time in seconds.
 *
 * <p>
 * If a cache receives a value larger than the largest positive integer it can represent, or if any of its age
 * calculations overflows, it MUST transmit an Age header with a value of 2147483648 (2^31).
 *
 * <p>
 * An HTTP/1.1 server that includes a cache MUST include an Age header field in every response generated from its own
 * cache.
 *
 * <p>
 * Caches SHOULD use an arithmetic type of at least 31 bits of range.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Age")
public class Age extends BasicIntegerHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Age";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable using {@link Integer#parseInt(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Age of(String value) {
		return value == null ? null : new Age(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Age of(Integer value) {
		return value == null ? null : new Age(value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Age of(Supplier<Integer> value) {
		return value == null ? null : new Age(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable using {@link Integer#parseInt(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Age(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Age(Integer value) {
		super(NAME, value);
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Age(Supplier<Integer> value) {
		super(NAME, value);
	}
}
