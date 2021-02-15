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

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.reflect.*;

/**
 * Subclass of {@link NameValuePair} for serializing POJOs as URL-encoded form post entries.
 *
 * Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Values from {@link Supplier Suppliers}.
 * 	<li>
 * 		Caching.
 * 	<li>
 * 		Fluent setters.
 * 	<li>
 * 		Fluent assertions.
 * </ul>
 */
@BeanIgnore
public class BasicNameValuePair implements NameValuePair, Headerable {
	private final String name;
	private final Object value;

	/**
	 * Convenience creator.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return A new {@link BasicNameValuePair} object.
	 */
	public static BasicNameValuePair of(String name, Object value) {
		return new BasicNameValuePair(name, value);
	}

	/**
	 * Creates a {@link NameValuePair} from a name/value pair string (e.g. <js>"Foo: bar"</js>)
	 *
	 * @param pair The pair string.
	 * @return A new {@link NameValuePair} object.
	 */
	public static BasicNameValuePair ofPair(String pair) {
		if (pair == null)
			return null;
		int i = pair.indexOf(':');
		if (i == -1)
			i = pair.indexOf('=');
		if (i == -1)
			return of(pair, "");
		return of(pair.substring(0,i).trim(), pair.substring(i+1).trim());
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value supplier.
	 * @return A new {@link BasicNameValuePair} object.
	 */
	public static BasicNameValuePair of(String name, Supplier<?> value) {
		return new BasicNameValuePair(name, value);
	}

	/**
	 * Utility method for converting an arbitrary object to a {@link NameValuePair}.
	 *
	 * @param o
	 * 	The object to cast or convert to a {@link NameValuePair}.
	 * @return Either the same object cast as a {@link NameValuePair} or converted to a {@link NameValuePair}.
	 */
	@SuppressWarnings("rawtypes")
	public static NameValuePair cast(Object o) {
		if (o instanceof NameValuePair)
			return (NameValuePair)o;
		if (o instanceof NameValuePairable)
			return ((NameValuePairable)o).asNameValuePair();
		if (o instanceof Headerable)
			return ((Headerable)o).asHeader();
		if (o instanceof Map.Entry) {
			Map.Entry e = (Map.Entry)o;
			return BasicNameValuePair.of(stringify(e.getKey()), e.getValue());
		}
		throw new BasicRuntimeException("Object of type {0} could not be converted to a NameValuePair.", o == null ? null : o.getClass().getName());
	}

	/**
	 * Returns <jk>true</jk> if the {@link #cast(Object)} method can be used on the specified object.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the {@link #cast(Object)} method can be used on the specified object.
	 */
	public static boolean canCast(Object o) {
		ClassInfo ci = ClassInfo.of(o);
		return ci != null && ci.isChildOfAny(Headerable.class, NameValuePair.class, NameValuePairable.class, Map.Entry.class);
	}

	/**
	 * Constructor.
	 *
	 * @param name The parameter name.
	 * @param value The POJO to serialize to the parameter value.
	 */
	public BasicNameValuePair(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Provides an object for performing assertions against the name of this pair.
	 *
	 * @return An object for performing assertions against the name of this pair.
	 */
	public FluentStringAssertion<BasicNameValuePair> assertName() {
		return new FluentStringAssertion<>(getName(), this);
	}

	/**
	 * Provides an object for performing assertions against the value of this pair.
	 *
	 * @return An object for performing assertions against the value of this pair.
	 */
	public FluentStringAssertion<BasicNameValuePair> assertValue() {
		return new FluentStringAssertion<>(getValue(), this);
	}

	@Override /* Headerable */
	public BasicHeader asHeader() {
		return BasicHeader.of(name, value);
	}

	@Override /* NameValuePair */
	public String getName() {
		return name;
	}

	@Override /* NameValuePair */
	public String getValue() {
		return stringify(unwrap(value));
	}

	/**
	 * Returns the raw value of the parameter.
	 *
	 * @return The raw value of the parameter.
	 */
	protected Object getRawValue() {
		return unwrap(value);
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
		return urlEncode(getName()) + "=" + urlEncode(getValue());
	}

	private Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		return o;
	}

	// <FluentSetters>

	// </FluentSetters>
}
