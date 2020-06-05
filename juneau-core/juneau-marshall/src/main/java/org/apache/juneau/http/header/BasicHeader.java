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

import java.io.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Superclass of all headers defined in this package.
 */
@BeanIgnore
public class BasicHeader implements Header, Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

    private static final HeaderElement[] EMPTY_HEADER_ELEMENTS = new HeaderElement[] {};

	private final String name;
	private final Object value;

	/**
	 * Convenience creator.
	 *
	 * @param name The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Any non-String value will be converted to a String using {@link Object#toString()}.
	 * @return A new {@link BasicHeader} object.
	 */
	public static BasicHeader of(String name, Object value) {
		return new BasicHeader(name, value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The parameter name.
	 * @param value
	 * 	The parameter value supplier.
	 * 	<br>Any non-String value will be converted to a String using {@link Object#toString()}.
	 * @return A new {@link BasicHeader} object.
	 */
	public static BasicHeader of(String name, Supplier<?> value) {
		return new BasicHeader(name, value);
	}

	/**
	 * Constructor.
	 *
	 * @param name The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Any non-String value will be converted to a String using {@link Object#toString()}.
	 * 	<br>Can also be an <l>Object</l> {@link Supplier}.
	 */
	public BasicHeader(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	@Override /* Header */
	public String getName() {
		return name;
	}

	@Override /* Header */
	public String getValue() {
		return StringUtils.asString(getRawValue());
	}

	/**
	 * Returns the raw value of the header.
	 *
	 * @return The raw value of the header.
	 */
	protected Object getRawValue() {
		if (value instanceof Supplier) {
			return ((Supplier<?>)value).get();
		}
		return value;
	}

	@Override
	public HeaderElement[] getElements() throws ParseException {
		if (getValue() != null) {
			// result intentionally not cached, it's probably not used again
			return BasicHeaderValueParser.parseElements(getValue(), null);
		}
		return EMPTY_HEADER_ELEMENTS;
	}

	/**
	 * Returns <jk>true</jk> if the specified value is the same using {@link String#equalsIgnoreCase(String)}.
	 *
	 * @param compare The value to compare against.
	 * @return <jk>true</jk> if the specified value is the same.
	 */
	public boolean eqIC(String compare) {
		return getValue().equalsIgnoreCase(compare);
	}

	/**
	 * Returns <jk>true</jk> if the specified value is the same using {@link String#equals(Object)}.
	 *
	 * @param compare The value to compare against.
	 * @return <jk>true</jk> if the specified value is the same.
	 */
	public boolean eq(String compare) {
		return getValue().equals(compare);
	}

	/**
	 * Returns <jk>true</jk> if the specified object is a {@link Supplier}.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is a {@link Supplier}.
	 */
	protected boolean isSupplier(Object o) {
		return o instanceof Supplier;
	}

	@Override /* Object */
	public String toString() {
		return name + ": " + getValue();
	}
}