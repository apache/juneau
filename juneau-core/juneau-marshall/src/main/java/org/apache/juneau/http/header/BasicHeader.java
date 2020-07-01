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

import static org.apache.juneau.internal.StringUtils.*;
import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.BasicHeader;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Superclass of all headers defined in this package.
 */
@BeanIgnore
public class BasicHeader implements Header, Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

    private static final HeaderElement[] EMPTY_HEADER_ELEMENTS = new HeaderElement[] {};

	private final String name;
	private final Object value;
	private HeaderElement[] elements;

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
	 * Convenience creator.
	 *
	 * @param o The name value pair that makes up the header name and value.
	 * 	The parameter value.
	 * 	<br>Any non-String value will be converted to a String using {@link Object#toString()}.
	 * @return A new {@link BasicHeader} object.
	 */
	public static Header of(NameValuePair o) {
		return new BasicHeader(o.getName(), o.getValue());
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
	 * Utility method for converting an arbitrary object to a {@link Header}.
	 *
	 * @param o
	 * 	The object to cast or convert to a {@link Header}.
	 * @return Either the same object cast as a {@link Header} or converted to a {@link Header}.
	 */
	@SuppressWarnings("rawtypes")
	public static Header cast(Object o) {
		if (o instanceof Header)
			return (Header)o;
		if (o instanceof Headerable)
			return ((Headerable)o).asHeader();
		if (o instanceof NameValuePair)
			return BasicHeader.of((NameValuePair)o);
		if (o instanceof NameValuePairable)
			return BasicHeader.of(((NameValuePairable)o).asNameValuePair());
		if (o instanceof Map.Entry) {
			Map.Entry e = (Map.Entry)o;
			return BasicHeader.of(stringify(e.getKey()), e.getValue());
		}
		throw new BasicRuntimeException("Object of type {0} could not be converted to a Header.", o == null ? null : o.getClass().getName());
	}

	/**
	 * Returns <jk>true</jk> if the {@link #cast(Object)} method can be used on the specified object.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the {@link #cast(Object)} method can be used on the specified object.
	 */
	public static boolean canCast(Object o) {
		ClassInfo ci = ClassInfo.of(o);
		return ci != null && ci.isChildOfAny(Header.class, Headerable.class, NameValuePair.class, NameValuePairable.class, Map.Entry.class);
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
		return stringify(getRawValue());
	}

	/**
	 * Returns the raw value of the header.
	 *
	 * @return The raw value of the header.
	 */
	protected Object getRawValue() {
		return unwrap(value);
	}

	@Override
	public HeaderElement[] getElements() throws ParseException {
		if (elements == null) {
			String s = getValue();
			HeaderElement[] x = s == null ? EMPTY_HEADER_ELEMENTS : BasicHeaderValueParser.parseElements(s, null);
			if (value instanceof Supplier)
				return x;
			elements = x;
		}
		return elements;
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

	/**
	 * If the specified object is a {@link Supplier}, returns the supplied value, otherwise the same value.
	 *
	 * @param o The object to unwrap.
	 * @return The unwrapped object.
	 */
	protected Object unwrap(Object o) {
		if (o instanceof Supplier)
			return ((Supplier<?>)o).get();
		return o;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof Header) && ObjectUtils.eq(this, (Header)o, (x,y)->isEquals(x.name, y.getName()) && isEquals(x.getValue(), y.getValue()));
	}

	@Override /* Object */
	public int hashCode() {
		return HashCode.create().add(name).add(value).get();
	}

	@Override /* Object */
	public String toString() {
		return name + ": " + getValue();
	}
}
