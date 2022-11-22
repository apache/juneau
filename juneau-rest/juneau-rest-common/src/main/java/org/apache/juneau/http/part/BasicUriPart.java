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
package org.apache.juneau.http.part;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.net.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;

/**
 * A {@link NameValuePair} that consists of a single URL value.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
public class BasicUriPart extends BasicPart {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return A new {@link BasicUriPart} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicUriPart of(String name, URI value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicUriPart(name, value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Part value is re-evaluated on each call to {@link NameValuePair#getValue()}.
	 *
	 * @param name The part name.
	 * @param value The part value supplier.
	 * @return A new {@link BasicUriPart} object, or <jk>null</jk> if the name or supplier is <jk>null</jk>.
	 */
	public static BasicUriPart of(String name, Supplier<URI> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicUriPart(name, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final URI value;
	private final Supplier<URI> supplier;

	/**
	 * Constructor.
	 *
	 * @param name The part name.  Must not be <jk>null</jk>.
	 * @param value The part value.  Can be <jk>null</jk>.
	 */
	public BasicUriPart(String name, URI value) {
		super(name, value);
		this.value = value;
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param name The part name.  Must not be <jk>null</jk>.
	 * @param value The part value supplier.  Can be <jk>null</jk> or supply <jk>null</jk>.
	 */
	public BasicUriPart(String name, Supplier<URI> value) {
		super(name, value);
		this.value = null;
		this.supplier = value;
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * <jk>null</jk> and empty values are treated as <jk>null</jk>.
	 * Otherwise parses using {@link URI#create(String)}.
	 *
	 * @param name The part name.  Must not be <jk>null</jk>.
	 * @param value The part value.  Can be <jk>null</jk>.
	 */
	public BasicUriPart(String name, String value) {
		super(name, value);
		this.value = isEmpty(value) ? null : URI.create(value);
		this.supplier = null;
	}

	/**
	 * Returns The part value as a {@link URI} wrapped in an {@link Optional}.
	 *
	 * @return The part value as a {@link URI} wrapped in an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<URI> asUri() {
		return optional(value());
	}

	/**
	 * Returns The part value as a {@link URI}.
	 *
	 * @return The part value as a {@link URI}, or <jk>null</jk> if the value <jk>null</jk>.
	 */
	public URI toUri() {
		return value();
	}

	/**
	 * Return the value if present, otherwise return <c>other</c>.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asString().orElse(<jv>other</jv>)</c>.
	 *
	 * @param other The value to be returned if there is no value present, can be <jk>null</jk>.
	 * @return The value, if present, otherwise <c>other</c>.
	 */
	public URI orElse(URI other) {
		URI x = value();
		return x != null ? x : other;
	}

	private URI value() {
		if (supplier != null)
			return supplier.get();
		return value;
	}
}
