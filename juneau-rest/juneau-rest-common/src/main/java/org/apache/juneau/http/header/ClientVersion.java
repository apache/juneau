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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents a parsed <l>Client-Version</l> HTTP request header.
 *
 * <p>
 * Specifies a client-side version number.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Client-Version: 2.0.1
 * </p>
 *
 * <p>
 * Not part of the RFC2616 specification, but provided to allow for HTTP responses to be tailored to specified
 * known client versions.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Client-Version")
public class ClientVersion extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Client-Version";

	private static final Cache<String,ClientVersion> CACHE = Cache.of(String.class, ClientVersion.class).build();

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link Version#of(String)}
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ClientVersion of(String value) {
		return value == null ? null : CACHE.get(value, ()->new ClientVersion(value));
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ClientVersion of(Version value) {
		return value == null ? null : new ClientVersion(value);
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
	public static ClientVersion of(Supplier<Version> value) {
		return value == null ? null : new ClientVersion(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Version value;
	private final Supplier<Version> supplier;

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link Version#of(String)}
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ClientVersion(String value) {
		super(NAME, value);
		this.value = Version.of(value);
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ClientVersion(Version value) {
		super(NAME, stringify(value));
		this.value = value;
		this.supplier = null;
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
	public ClientVersion(Supplier<Version> value) {
		super(NAME, (String)null);
		this.value = null;
		this.supplier = value;
	}

	@Override /* Header */
	public String getValue() {
		if (supplier != null)
			return stringify(supplier.get());
		return super.getValue();
	}

	/**
	 * Returns the header value as a {@link Version} object.
	 *
	 * @return The header value as a {@link Version} object, or {@link Optional#empty()} if the value is <jk>null</jk>.
	 */
	public Optional<Version> asVersion() {
		return optional(value);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the response content is older than 1.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(ClientVersion.<jk>class</jk>).assertVersion().major().isGreaterThan(1);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentVersionAssertion<ClientVersion> assertVersion() {
		return new FluentVersionAssertion<>(asVersion().orElse(null), this);
	}
}
