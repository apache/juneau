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
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.exception.HttpException;
import org.apache.juneau.http.pair.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.*;

/**
 * Represents a single query parameter on an HTTP request.
 */
public class RequestQueryParam implements NameValuePair {

	private final String name, value;
	private final RestRequest request;
	private HttpPartParserSession parser;
	private HttpPartSchema schema;

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public RequestQueryParam(RestRequest request, String name, String value) {
		this.request = request;
		this.name = name;
		this.value = value;
		parser(null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Setters
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies the part schema for this parameter.
	 *
	 * <p>
	 * Used by schema-based part parsers such as {@link OpenApiParser}.
	 *
	 * @param value
	 * 	The part schema.
	 * @return This object (for method chaining).
	 */
	public RequestQueryParam schema(HttpPartSchema value) {
		this.schema = value;
		return this;
	}

	/**
	 * Specifies the part parser to use for this parameter.
	 *
	 * <p>
	 * If not specified, uses the part parser defined on the client by calling {@link RestContextBuilder#partParser(Class)}.
	 *
	 * @param value
	 * 	The new part parser to use for this parameter.
	 * 	<br>If <jk>null</jk>, {@link SimplePartParser#DEFAULT} will be used.
	 * @return This object (for method chaining).
	 */
	public RequestQueryParam parser(HttpPartParserSession value) {
		this.parser = value == null ? SimplePartParser.DEFAULT_SESSION : value;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Retrievers
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this parameter exists on the request.
	 *
	 * @return <jk>true</jk> if this parameter exists on the request.
	 */
	public boolean exists() {
		return value != null;
	}

	/**
	 * Returns the value of this parameter as a string.
	 *
	 * @return The value of this parameter as a string, or {@link Optional#empty()} if the parameter was not present.
	 */
	public Optional<String> asString() {
		return asNamedString().asString();
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
				return cc.invoke(asString());
			cc = ci.getConstructor(Visibility.PUBLIC, String.class, String.class);
			if (cc != null)
				return cc.invoke(getName(), asString());
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

	/**
	 * Converts this parameter to the specified POJO type using the request {@link HttpPartParser} and optional schema.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return The converted type, or {@link Optional#empty()} if the parameter is not present.
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> Optional<T> as(Type type, Type...args) throws HttpException {
		return as(request.getBeanSession().getClassMeta(type, args));
	}

	/**
	 * Converts this parameter to the specified POJO type using the request {@link HttpPartParser} and optional schema.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or {@link Optional#empty()} if the parameter is not present.
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> Optional<T> as(Class<T> type) throws HttpException {
		return as(request.getBeanSession().getClassMeta(type));
	}

	/**
	 * Converts this parameter to the specified POJO type using the request {@link HttpPartParser} and optional schema.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or {@link Optional#empty()} if the parameter is not present.
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> Optional<T> as(ClassMeta<T> type) throws HttpException {
		try {
			return Optional.ofNullable(parser.parse(HEADER, schema, asString().orElse(null), type));
		} catch (ParseException e) {
			throw new BadRequest(e, "Could not parse query parameter ''{0}''.", getName());
		}
	}

	/**
	 * Matches the specified pattern against this parameter value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse parameter using a regular expression.</jc>
	 * 	Matcher <jv>matcher</jv> = <jv>request</jv>
	 * 		.getQueryParam(<js>"foo"</js>)
	 * 		.asMatcher(Pattern.<jsm>compile</jsm>(<js>"foo/(.*)"</js>));
	 *
	 * 	<jk>if</jk> (<jv>matcher</jv>.matches()) {
	 * 		String <jv>bar</jv> = <jv>matcher</jv>.group(1);
	 * 	}
	 * </p>
	 *
	 * @param pattern The regular expression pattern to match.
	 * @return The matcher.
	 * @throws HttpException If a connection error occurred.
	 */
	public Matcher asMatcher(Pattern pattern) throws HttpException {
		return pattern.matcher(asString().orElse(""));
	}

	/**
	 * Matches the specified pattern against this parameter value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse parameter using a regular expression.</jc>
	 * 	Matcher <jv>matcher</jv> = <jv>request</jv>
	 * 		.getQueryParam(<js>"foo"</js>)
	 * 		.asMatcher(<js>"foo/(.*)"</js>);
	 *
	 * 	<jk>if</jk> (<jv>matcher</jv>.matches()) {
	 * 		String <jv>bar</jv> = <jv>matcher</jv>.group(1);
	 * 	}
	 * </p>
	 *
	 * @param regex The regular expression pattern to match.
	 * @return The matcher.
	 * @throws HttpException If a connection error occurred.
	 */
	public Matcher asMatcher(String regex) throws HttpException {
		return asMatcher(regex, 0);
	}

	/**
	 * Matches the specified pattern against this parameter value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse parameter using a regular expression.</jc>
	 * 	Matcher <jv>matcher</jv> = <jv>request</jv>
	 * 		.getQueryParam(<js>"foo"</js>)
	 * 		.asMatcher(<js>"foo/(.*)"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jk>if</jk> (<jv>matcher</jv>.matches()) {
	 * 		String <jv>bar</jv> = <jv>matcher</jv>.group(1);
	 * 	}
	 * </p>
	 *
	 * @param regex The regular expression pattern to match.
	 * @param flags Pattern match flags.  See {@link Pattern#compile(String, int)}.
	 * @return The matcher.
	 * @throws HttpException If a connection error occurred.
	 */
	public Matcher asMatcher(String regex, int flags) throws HttpException {
		return asMatcher(Pattern.compile(regex, flags));
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
	 * 		.getQueryParam(<js>"foo"</js>)
	 * 		.assertString().contains(<js>"bar"</js>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original object allowing you to chain multiple requests like so:
	 * <p class='bcode w800'>
	 * 	String <jv>foo</jv> = <jv>request</jv>
	 * 		.getQueryParam(<js>"foo"</js>)
	 * 		.assertString().contains(<js>"bar"</js>)
	 * 		.asString().get();
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentStringAssertion<RequestQueryParam> assertString() {
		return new FluentStringAssertion<>(asString().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on an integer parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getQueryParam(<js>"age"</js>)
	 * 		.assertInteger().isGreaterThan(1);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentIntegerAssertion<RequestQueryParam> assertInteger() {
		return new FluentIntegerAssertion<>(asNamedInteger().asInteger().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on a long parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getQueryParam(<js>"length"</js>)
	 * 		.assertLong().isLessThan(100000);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentLongAssertion<RequestQueryParam> assertLong() {
		return new FluentLongAssertion<>(asNamedLong().asLong().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on a date parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getQueryParam(<js>"time"</js>)
	 * 		.assertDate().isAfterNow();
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentZonedDateTimeAssertion<RequestQueryParam> assertDate() {
		return new FluentZonedDateTimeAssertion<>(asNamedDate().asZonedDateTime().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on comma-separated string parameters.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getQueryParam(<js>"allow"</js>)
	 * 		.assertCsvArray().contains(<js>"GET"</js>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentListAssertion<RequestQueryParam> assertCsvArray() {
		return new FluentListAssertion<>(asNamedCsvArray().asList().orElse(null), this);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Header passthrough methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Gets the name of this pair.
	 *
	 * @return The name of this pair, never <jk>null</jk>.
	 */
	@Override /* Header */
	public String getName() {
		return name;
	}

	/**
	 * Gets the value of this pair.
	 *
	 * <ul class='notes'>
	 * 	<li>{@link #asString()} is an equivalent method and the preferred method for fluent-style coding.
	 * </ul>
	 *
	 * @return The value of this pair, may be <jk>null</jk>.
	 */
	@Override /* Header */
	public String getValue() {
		return value;
	}

	@Override /* Object */
	public String toString() {
		return getName() + "=" + getValue();
	}
}
