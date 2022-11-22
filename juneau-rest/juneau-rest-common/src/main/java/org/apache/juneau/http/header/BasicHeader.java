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

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;

/**
 * Superclass of all headers defined in this package.
 *
 * Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Default support for various streams and readers.
 * 	<li>
 * 		Content from {@link Supplier Suppliers}.
 * 	<li>
 * 		Caching.
 * 	<li>
 * 		Fluent setters.
 * 	<li>
 * 		Fluent assertions.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@FluentSetters
@BeanIgnore
public class BasicHeader implements Header, Cloneable, Serializable {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final HeaderElement[] EMPTY_HEADER_ELEMENTS = new HeaderElement[] {};

	/**
	 * Static creator.
	 *
	 * @param name The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicHeader of(String name, Object value) {
		return value == null ? null : new BasicHeader(name, value);
	}

	/**
	 * Static creator.
	 *
	 * @param o The name value pair that makes up the header name and value.
	 * 	The parameter value.
	 * 	<br>Any non-String value will be converted to a String using {@link Object#toString()}.
	 * @return A new header bean.
	 */
	public static BasicHeader of(NameValuePair o) {
		return new BasicHeader(o.getName(), o.getValue());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final String name;
	private final String stringValue;

	private final Object value;
	private final Supplier<Object> supplier;

	private HeaderElement[] elements;

	/**
	 * Constructor.
	 *
	 * @param name The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Any non-String value will be converted to a String using {@link Object#toString()}.
	 * 	<br>Can also be an <l>Object</l> {@link Supplier}.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicHeader(String name, Object value) {
		assertArg(StringUtils.isNotEmpty(name), "Name cannot be empty on header.");
		this.name = name;
		this.value = value instanceof Supplier ? null : value;
		this.stringValue = stringify(value);
		this.supplier = cast(Supplier.class, value);
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
	public BasicHeader(String name, Supplier<Object> value) {
		assertArg(StringUtils.isNotEmpty(name), "Name cannot be empty on header.");
		this.name = name;
		this.value = null;
		this.stringValue = null;
		this.supplier = value;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	protected BasicHeader(BasicHeader copyFrom) {
		this.name = copyFrom.name;
		this.value = copyFrom.value;
		this.stringValue = copyFrom.stringValue;
		this.supplier = copyFrom.supplier;
	}

	@Override /* Header */
	public String getName() {
		return name;
	}

	@Override /* Header */
	public String getValue() {
		if (supplier != null)
			return stringify(supplier.get());
		return stringValue;
	}

	@Override
	public HeaderElement[] getElements() throws ParseException {
		if (elements == null) {
			String s = getValue();
			HeaderElement[] x = s == null ? EMPTY_HEADER_ELEMENTS : BasicHeaderValueParser.parseElements(s, null);
			if (supplier == null)
				elements = x;
			return x;
		}
		return elements;
	}

	/**
	 * Returns <jk>true</jk> if the specified value is the same using {@link String#equalsIgnoreCase(String)}.
	 *
	 * @param compare The value to compare against.
	 * @return <jk>true</jk> if the specified value is the same.
	 */
	public boolean equalsIgnoreCase(String compare) {
		return eqic(getValue(), compare);
	}

	/**
	 * Provides an object for performing assertions against the name of this header.
	 *
	 * @return An object for performing assertions against the name of this header.
	 */
	public FluentStringAssertion<BasicHeader> assertName() {
		return new FluentStringAssertion<>(getName(), this);
	}

	/**
	 * Provides an object for performing assertions against the value of this header.
	 *
	 * @return An object for performing assertions against the value of this header.
	 */
	public FluentStringAssertion<BasicHeader> assertStringValue() {
		return new FluentStringAssertion<>(getValue(), this);
	}

	/**
	 * Returns the value of this header as a string.
	 *
	 * @return The value of this header as a string, or {@link Optional#empty()} if the value is <jk>null</jk>
	 */
	public Optional<String> asString() {
		return optional(getValue());
	}

	/**
	 * Returns <jk>true</jk> if the value exists.
	 *
	 * <p>
	 * This is a shortcut for calling <c>asString().isPresent()</c>.
	 *
	 * @return <jk>true</jk> if the value exists.
	 */
	public boolean isPresent() {
		return asString().isPresent();
	}

	/**
	 * Returns <jk>true</jk> if the value exists and is not empty.
	 *
	 * <p>
	 * This is a shortcut for calling <c>!asString().orElse(<js>""</js>).isEmpty()</c>.
	 *
	 * @return <jk>true</jk> if the value exists and is not empty.
	 */
	public boolean isNotEmpty() {
		return ! asString().orElse("").isEmpty();
	}

	/**
	 * If a value is present, returns the value, otherwise throws {@link NoSuchElementException}.
	 *
	 * <p>
	 * This is a shortcut for calling <c>asString().get()</c>.
	 *
	 * @return The value if present.
	 */
	public String get() {
		return asString().get();
	}

	/**
	 * If a value is present, returns the value, otherwise returns other.
	 *
	 * <p>
	 * This is a shortcut for calling <c>asString().orElse(<jv>other</jv>)</c>.
	 *
	 * @param other The other value.
	 * @return The value if present or the other value if not.
	 */
	public String orElse(String other) {
		return asString().orElse(other);
	}

	@Override /* Object */
	public boolean equals(Object o) {
		// Functionality provided for HttpRequest.removeHeader().
		// Not a perfect equality operator if using SVL vars.
		if (! (o instanceof Header))
			return false;
		return eq(this, (Header)o, (x,y)->eq(x.name,y.getName()) && eq(x.getValue(),y.getValue()));
	}

	@Override /* Object */
	public int hashCode() {
		// Implemented since we override equals(Object).
		return super.hashCode();
	}

	@Override /* Object */
	public String toString() {
		return getName() + ": " + getValue();
	}

	// <FluentSetters>

	// </FluentSetters>
}
