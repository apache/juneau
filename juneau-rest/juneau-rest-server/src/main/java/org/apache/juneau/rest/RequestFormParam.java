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
package org.apache.juneau.rest;

import static org.apache.juneau.httppart.HttpPartType.*;

import java.io.*;
import java.time.*;
import java.util.*;

import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.pair.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Represents a single form-data parameter on an HTTP request.
 */
public class RequestFormParam extends RequestHttpPart implements NameValuePair {

	private final Part part;
	private String value;

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param part The HTTP part.
	 */
	public RequestFormParam(RestRequest request, Part part) {
		super(FORMDATA, request, part.getName());
		this.part = part;
	}

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public RequestFormParam(RestRequest request, String name, String value) {
		super(FORMDATA, request, name);
		this.value = value;
		this.part = null;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Retrievers
	//------------------------------------------------------------------------------------------------------------------

	@Override /* RequestHttpPart */
	public String getValue() {
		if (value == null && part != null)
			try {
				value = IOUtils.read(part.getInputStream());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		return value;
	}

	/**
	 * Returns the value of this parameter as an integer.
	 *
	 * @return The value of this parameter as an integer, or {@link Optional#empty()} if the parameter was not present.
	 */
	public Optional<Integer> asInteger() {
		return asNamedInteger().asInteger();
	}

	/**
	 * Returns the value of this parameter as a boolean.
	 *
	 * @return The value of this parameter as a boolean, or {@link Optional#empty()} if the parameter was not present.
	 */
	public Optional<Boolean> asBoolean() {
		return asNamedBoolean().asBoolean();
	}

	/**
	 * Returns the value of this parameter as a long.
	 *
	 * @return The value of this parameter as a long, or {@link Optional#empty()} if the parameter was not present.
	 */
	public Optional<Long> asLong() {
		return asNamedLong().asLong();
	}

	/**
	 * Returns the value of this parameter as a date.
	 *
	 * @return The value of this parameter as a date, or {@link Optional#empty()} if the parameter was not present.
	 */
	public Optional<ZonedDateTime> asDate() {
		return asNamedDate().asZonedDateTime();
	}

	/**
	 * Returns the value of this parameter as a list from a comma-delimited string.
	 *
	 * @return The value of this parameter as a list from a comma-delimited string, or {@link Optional#empty()} if the parameter was not present.
	 */
	public Optional<List<String>> asCsvArray() {
		return asNamedCsvArray().asList();
	}

	/**
	 * Returns the value of this parameter as a {@link BasicNameValuePair}.
	 *
	 * @param c The subclass of {@link BasicNameValuePair} to instantiate.
	 * @param <T> The subclass of {@link BasicNameValuePair} to instantiate.
	 * @return The value of this parameter as a string, never <jk>null</jk>.
	 */
	public <T extends BasicNameValuePair> T asNameValuePair(Class<T> c) {
		try {
			ClassInfo ci = ClassInfo.of(c);
			ConstructorInfo cc = ci.getConstructor(Visibility.PUBLIC, String.class);
			if (cc != null)
				return cc.invoke(orElse(null));
			cc = ci.getConstructor(Visibility.PUBLIC, String.class, String.class);
			if (cc != null)
				return cc.invoke(getName(), orElse(null));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		throw new BasicRuntimeException("Could not determine a method to construct type {0}", c.getClass().getName());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicNamedCsvArray}.
	 *
	 * @return The value of this parameter as a {@link BasicNamedCsvArray}, never <jk>null</jk>.
	 */
	public BasicNamedCsvArray asNamedCsvArray() {
		return new BasicNamedCsvArray(getName(), getValue());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicNamedDate}.
	 *
	 * @return The value of this parameter as a {@link BasicNamedDate}, never <jk>null</jk>.
	 */
	public BasicNamedDate asNamedDate() {
		return new BasicNamedDate(getName(), getValue());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicNamedInteger}.
	 *
	 * @return The value of this parameter as a {@link BasicNamedInteger}, never <jk>null</jk>.
	 */
	public BasicNamedInteger asNamedInteger() {
		return new BasicNamedInteger(getName(), getValue());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicNamedBoolean}.
	 *
	 * @return The value of this parameter as a {@link BasicNamedBoolean}, never <jk>null</jk>.
	 */
	public BasicNamedBoolean asNamedBoolean() {
		return new BasicNamedBoolean(getName(), getValue());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicNamedLong}.
	 *
	 * @return The value of this parameter as a {@link BasicNamedLong}, never <jk>null</jk>.
	 */
	public BasicNamedLong asNamedLong() {
		return new BasicNamedLong(getName(), getValue());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicNamedString}.
	 *
	 * @return The value of this parameter as a {@link BasicNamedString}, never <jk>null</jk>.
	 */
	public BasicNamedString asNamedString() {
		return new BasicNamedString(getName(), getValue());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicNamedUri}.
	 *
	 * @return The value of this parameter as a {@link BasicNamedUri}, never <jk>null</jk>.
	 */
	public BasicNamedUri asNamedUri() {
		return new BasicNamedUri(getName(), getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on this parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getFormParam(<js>"foo"</js>)
	 * 		.assertString().contains(<js>"bar"</js>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original object allowing you to chain multiple requests like so:
	 * <p class='bcode w800'>
	 * 	String <jv>foo</jv> = <jv>request</jv>
	 * 		.getFormParam(<js>"foo"</js>)
	 * 		.assertString().contains(<js>"bar"</js>)
	 * 		.asString().get();
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentStringAssertion<RequestFormParam> assertString() {
		return new FluentStringAssertion<>(orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on an integer parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getFormParam(<js>"age"</js>)
	 * 		.assertInteger().isGreaterThan(1);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentIntegerAssertion<RequestFormParam> assertInteger() {
		return new FluentIntegerAssertion<>(asNamedInteger().asInteger().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on a long parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getFormParam(<js>"length"</js>)
	 * 		.assertLong().isLessThan(100000);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentLongAssertion<RequestFormParam> assertLong() {
		return new FluentLongAssertion<>(asNamedLong().asLong().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on a date parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getFormParam(<js>"time"</js>)
	 * 		.assertDate().isAfterNow();
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentZonedDateTimeAssertion<RequestFormParam> assertDate() {
		return new FluentZonedDateTimeAssertion<>(asNamedDate().asZonedDateTime().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on comma-separated string parameters.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getFormParam(<js>"allow"</js>)
	 * 		.assertCsvArray().contains(<js>"GET"</js>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentListAssertion<RequestFormParam> assertCsvArray() {
		return new FluentListAssertion<>(asNamedCsvArray().asList().orElse(null), this);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Header passthrough methods.
	//------------------------------------------------------------------------------------------------------------------

	@Override /* Object */
	public String toString() {
		return getName() + "=" + getValue();
	}

	// <FluentSetters>

	@Override /* GENERATED */
	public RequestFormParam schema(HttpPartSchema value) {
		super.schema(value);
		return this;
	}

	@Override /* GENERATED */
	public RequestFormParam parser(HttpPartParserSession value) {
		super.parser(value);
		return this;
	}

	// </FluentSetters>
}
