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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Implementation of {@link NameValuePair} for serializing POJOs as URL-encoded form post entries.
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
@FluentSetters
@BeanIgnore
public class BasicPart implements NameValuePair, Headerable {
	private final String name;
	private final Object value;

	/**
	 * Convenience creator.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return A new {@link BasicPart} object.
	 */
	public static BasicPart of(String name, Object value) {
		return new BasicPart(name, value);
	}

	/**
	 * Creates a {@link NameValuePair} from a name/value pair string (e.g. <js>"Foo: bar"</js>)
	 *
	 * @param pair The pair string.
	 * @return A new {@link NameValuePair} object.
	 */
	public static BasicPart ofPair(String pair) {
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
	 * Part value is re-evaluated on each call to {@link NameValuePair#getValue()}.
	 *
	 * @param name The part name.
	 * @param value The part value supplier.
	 * @return A new {@link BasicPart} object.
	 */
	public static BasicPart of(String name, Supplier<?> value) {
		return new BasicPart(name, value);
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
		if (o instanceof NameValuePair) {
			NameValuePair p = (NameValuePair)o;
			return BasicPart.of(p.getName(), p.getValue());
		}
		if (o instanceof Headerable) {
			Header x = ((Headerable)o).asHeader();
			return BasicPart.of(x.getName(), x.getValue());
		}
		if (o instanceof Map.Entry) {
			Map.Entry e = (Map.Entry)o;
			return BasicPart.of(stringify(e.getKey()), e.getValue());
		}
		throw new BasicRuntimeException("Object of type {0} could not be converted to a Part.", o == null ? null : o.getClass().getName());
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
	 * @param name The part name.
	 * @param value The POJO to serialize to The part value.
	 */
	public BasicPart(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	protected BasicPart(BasicPart copyFrom) {
		assertArgNotNull("copyFrom", copyFrom);
		this.name = copyFrom.name;
		this.value = copyFrom.value;
	}

	/**
	 * Provides an object for performing assertions against the name of this pair.
	 *
	 * @return An object for performing assertions against the name of this pair.
	 */
	public FluentStringAssertion<BasicPart> assertName() {
		return new FluentStringAssertion<>(getName(), this);
	}

	/**
	 * Provides an object for performing assertions against the value of this pair.
	 *
	 * @return An object for performing assertions against the value of this pair.
	 */
	public FluentStringAssertion<BasicPart> assertValue() {
		return new FluentStringAssertion<>(getValue(), this);
	}

	@Override /* Headerable */
	public BasicHeader asHeader() {
		return BasicHeader.of(name, stringify(value));
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
	 * Returns the raw value of the part.
	 *
	 * @return The raw value of the part.
	 */
	public Object getRawValue() {
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
		return getName() + "=" + getValue();
	}

	private Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		return o;
	}

	// <FluentSetters>

	// </FluentSetters>
}
