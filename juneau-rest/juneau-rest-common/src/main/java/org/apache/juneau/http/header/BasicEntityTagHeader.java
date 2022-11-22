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

/**
 * Category of headers that consist of a single entity validator value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	ETag: "xyzzy"
 * 	ETag: W/"xyzzy"
 * 	ETag: ""
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
public class BasicEntityTagHeader extends BasicHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an entity tag value (e.g. <js>"\"xyzzy\""</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicEntityTagHeader of(String name, String value) {
		return value == null ? null : new BasicEntityTagHeader(name, value);
	}

	/**
	 * Static creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicEntityTagHeader of(String name, EntityTag value) {
		return value == null ? null : new BasicEntityTagHeader(name, value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicEntityTagHeader of(String name, Supplier<EntityTag> value) {
		return value == null ? null : new BasicEntityTagHeader(name, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final EntityTag value;
	private final Supplier<EntityTag> supplier;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an entity tag value (e.g. <js>"\"xyzzy\""</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicEntityTagHeader(String name, String value) {
		super(name, value);
		this.value = EntityTag.of(value);
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicEntityTagHeader(String name, EntityTag value) {
		super(name, value);
		this.value = value;
		this.supplier = null;
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicEntityTagHeader(String name, Supplier<EntityTag> value) {
		super(name, null);
		this.value = null;
		this.supplier = value;
	}

	@Override /* Header */
	public String getValue() {
		return stringify(value());
	}

	/**
	 * Returns the header value as an {@link EntityTag} wrapped in an {@link Optional}.
	 *
	 * @return The header value as an {@link EntityTag} wrapped in an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<EntityTag> asEntityTag() {
		return optional(value());
	}

	/**
	 * Returns the header value as an {@link EntityTag}.
	 *
	 * @return The header value as an {@link EntityTag}.  Can be <jk>null</jk>.
	 */
	public EntityTag toEntityTag() {
		return value();
	}

	/**
	 * Return the value if present, otherwise return <c>other</c>.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asEntityTag().orElse(<jv>other</jv>)</c>.
	 *
	 * @param other The value to be returned if there is no value present, can be <jk>null</jk>.
	 * @return The value, if present, otherwise <c>other</c>.
	 */
	public EntityTag orElse(EntityTag other) {
		EntityTag x = value();
		return x != null ? x : other;
	}

	private EntityTag value() {
		if (supplier != null)
			return supplier.get();
		return value;
	}
}
