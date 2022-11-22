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
package org.apache.juneau.rest.httppart;

import static org.apache.juneau.common.internal.StringUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.part.*;

/**
 * Implementation of a named object.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HttpParts">HTTP Parts</a>
 * </ul>
 */
@BeanIgnore
public class BasicNamedAttribute implements NamedAttribute {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return A new {@link BasicPart} object.
	 */
	public static BasicNamedAttribute of(String name, Object value) {
		return new BasicNamedAttribute(name, value);
	}

	/**
	 * Creates a {@link BasicNamedAttribute} from a name/value pair string (e.g. <js>"Foo: bar"</js>)
	 *
	 * @param pair The pair string.
	 * @return A new {@link NameValuePair} object.
	 */
	public static BasicNamedAttribute ofPair(String pair) {
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
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value supplier.
	 * @return A new {@link BasicPart} object.
	 */
	public static BasicNamedAttribute of(String name, Supplier<?> value) {
		return new BasicNamedAttribute(name, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final String name;
	private final Object value;

	/**
	 * Constructor.
	 *
	 * @param name The parameter name.
	 * @param value The POJO to serialize to the parameter value.
	 */
	public BasicNamedAttribute(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyOf The object to copy.
	 */
	protected BasicNamedAttribute(BasicNamedAttribute copyOf) {
		this.name = copyOf.name;
		this.value = copyOf.value;
	}

	/**
	 * Provides an object for performing assertions against the name of this pair.
	 *
	 * @return An object for performing assertions against the name of this pair.
	 */
	public FluentStringAssertion<BasicNamedAttribute> assertName() {
		return new FluentStringAssertion<>(getName(), this);
	}

	/**
	 * Provides an object for performing assertions against the value of this pair.
	 *
	 * @return An object for performing assertions against the value of this pair.
	 */
	public FluentObjectAssertion<Object,BasicNamedAttribute> assertValue() {
		return new FluentObjectAssertion<>(getValue(), this);
	}

	@Override /* NameValuePair */
	public String getName() {
		return name;
	}

	@Override /* NameValuePair */
	public Object getValue() {
		return unwrap(value);
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
		return getValue() != null;
	}

	/**
	 * If a value is present, returns the value, otherwise throws {@link NoSuchElementException}.
	 *
	 * <p>
	 * This is a shortcut for calling <c>asString().get()</c>.
	 *
	 * @return The value if present.
	 */
	public Object get() {
		Object o = getValue();
		if (o == null)
			throw new NoSuchElementException();
		return o;
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
	public Object orElse(Object other) {
		Object o = getValue();
		return o != null ? o : other;
	}

	@Override /* Object */
	public String toString() {
		return urlEncode(getName()) + "=" + getValue();
	}

	private Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		return o;
	}
}
