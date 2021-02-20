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
import org.apache.juneau.http.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.exception.HttpException;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.assertions.*;

/**
 * Represents a single header on an HTTP request.
 */
public class RequestHeader implements Header {

	private final String name, value;
	private final RestRequest request;
	private HttpPartParserSession parser;
	private HttpPartSchema schema;

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param name The header name.
	 * @param value The header value.
	 */
	public RequestHeader(RestRequest request, String name, String value) {
		this.request = request;
		this.name = name;
		this.value = value;
		parser(null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Setters
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies the part schema for this header.
	 *
	 * <p>
	 * Used by schema-based part parsers such as {@link OpenApiParser}.
	 *
	 * @param value
	 * 	The part schema.
	 * @return This object (for method chaining).
	 */
	public RequestHeader schema(HttpPartSchema value) {
		this.schema = value;
		return this;
	}

	/**
	 * Specifies the part parser to use for this header.
	 *
	 * <p>
	 * If not specified, uses the part parser defined on the client by calling {@link RestContextBuilder#partParser(Class)}.
	 *
	 * @param value
	 * 	The new part parser to use for this header.
	 * 	<br>If <jk>null</jk>, {@link SimplePartParser#DEFAULT} will be used.
	 * @return This object (for method chaining).
	 */
	public RequestHeader parser(HttpPartParserSession value) {
		this.parser = value == null ? SimplePartParser.DEFAULT_SESSION : value;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Retrievers
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this header exists on the request.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asString().isPresent()</c>.
	 *
	 * @return <jk>true</jk> if this header exists on the request.
	 */
	public boolean isPresent() {
		return asString().isPresent();
	}

	/**
	 * If a value is present, returns the value, otherwise throws {@link NoSuchElementException}.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asString().get()</c>.
	 *
	 * @return The value if present.
	 */
	public String get() {
		return asString().get();
	}

	/**
	 * Return the value if present, otherwise return other.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asString().orElse(<jv>other</jv>)</c>.
	 *
	 * @param other The value to be returned if there is no value present, may be <jk>null</jk>.
	 * @return The value, if present, otherwise other.
	 */
	public String orElse(String other) {
		return asString().orElse(other);
	}

	/**
	 * Returns the value of this header as a string.
	 *
	 * @return The value of this header as a string, or {@link Optional#empty()} if the header was not present.
	 */
	public Optional<String> asString() {
		return asStringHeader().asString();
	}

	/**
	 * Returns the value of this header as an integer.
	 *
	 * @return The value of this header as an integer, or {@link Optional#empty()} if the header was not present.
	 */
	public Optional<Integer> asInteger() {
		return asIntegerHeader().asInteger();
	}

	/**
	 * Returns the value of this header as a boolean.
	 *
	 * @return The value of this header as a boolean, or {@link Optional#empty()} if the header was not present.
	 */
	public Optional<Boolean> asBoolean() {
		return asBooleanHeader().asBoolean();
	}

	/**
	 * Returns the value of this header as a long.
	 *
	 * @return The value of this header as a long, or {@link Optional#empty()} if the header was not present.
	 */
	public Optional<Long> asLong() {
		return asLongHeader().asLong();
	}

	/**
	 * Returns the value of this header as a date.
	 *
	 * @return The value of this header as a date, or {@link Optional#empty()} if the header was not present.
	 */
	public Optional<ZonedDateTime> asDate() {
		return asDateHeader().asZonedDateTime();
	}

	/**
	 * Returns the value of this header as a list from a comma-delimited string.
	 *
	 * @return The value of this header as a list from a comma-delimited string, or {@link Optional#empty()} if the header was not present.
	 */
	public Optional<List<String>> asCsvArray() {
		return asCsvArrayHeader().asList();
	}

	/**
	 * Returns the value of this header as a {@link BasicHeader}.
	 *
	 * @param c The subclass of {@link BasicHeader} to instantiate.
	 * @param <T> The subclass of {@link BasicHeader} to instantiate.
	 * @return The value of this header as a string, never <jk>null</jk>.
	 */
	public <T extends BasicHeader> T asHeader(Class<T> c) {
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
	 * Returns the value of this header as a {@link BasicCsvArrayHeader}.
	 *
	 * @return The value of this header as a  {@link BasicCsvArrayHeader}, never <jk>null</jk>.
	 */
	public BasicCsvArrayHeader asCsvArrayHeader() {
		return new BasicCsvArrayHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicDateHeader}.
	 *
	 * @return The value of this header as a {@link BasicDateHeader}, never <jk>null</jk>.
	 */
	public BasicDateHeader asDateHeader() {
		return new BasicDateHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicEntityTagArrayHeader}.
	 *
	 * @return The value of this header as a {@link BasicEntityTagArrayHeader}, never <jk>null</jk>.
	 */
	public BasicEntityTagArrayHeader asEntityTagArrayHeader() {
		return new BasicEntityTagArrayHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicEntityTagHeader}.
	 *
	 * @return The value of this header as a {@link BasicEntityTagHeader}, never <jk>null</jk>.
	 */
	public BasicEntityTagHeader asEntityTagHeader() {
		return new BasicEntityTagHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicIntegerHeader}.
	 *
	 * @return The value of this header as a {@link BasicIntegerHeader}, never <jk>null</jk>.
	 */
	public BasicIntegerHeader asIntegerHeader() {
		return new BasicIntegerHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicBooleanHeader}.
	 *
	 * @return The value of this header as a {@link BasicBooleanHeader}, never <jk>null</jk>.
	 */
	public BasicBooleanHeader asBooleanHeader() {
		return new BasicBooleanHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicLongHeader}.
	 *
	 * @return The value of this header as a {@link BasicLongHeader}, never <jk>null</jk>.
	 */
	public BasicLongHeader asLongHeader() {
		return new BasicLongHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicStringRangeArrayHeader}.
	 *
	 * @return The value of this header as a {@link BasicStringRangeArrayHeader}, never <jk>null</jk>.
	 */
	public BasicStringRangeArrayHeader asStringRangeArrayHeader() {
		return new BasicStringRangeArrayHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicStringHeader}.
	 *
	 * @return The value of this header as a {@link BasicStringHeader}, never <jk>null</jk>.
	 */
	public BasicStringHeader asStringHeader() {
		return new BasicStringHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a {@link BasicUriHeader}.
	 *
	 * @return The value of this header as a {@link BasicUriHeader}, never <jk>null</jk>.
	 */
	public BasicUriHeader asUriHeader() {
		return new BasicUriHeader(getName(), getValue());
	}

	/**
	 * Converts this header to the specified POJO type using the request {@link HttpPartParser} and optional schema.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return The converted type, or {@link Optional#empty()} if the header is not present.
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> Optional<T> asType(Type type, Type...args) throws HttpException {
		return asType(request.getBeanSession().getClassMeta(type, args));
	}

	/**
	 * Converts this header to the specified POJO type using the request {@link HttpPartParser} and optional schema.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or {@link Optional#empty()} if the header is not present.
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> Optional<T> asType(Class<T> type) throws HttpException {
		return asType(request.getBeanSession().getClassMeta(type));
	}

	/**
	 * Converts this header to the specified POJO type using the request {@link HttpPartParser} and optional schema.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or {@link Optional#empty()} if the header is not present.
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> Optional<T> asType(ClassMeta<T> type) throws HttpException {
		try {
			return Optional.ofNullable(parser.parse(HEADER, schema, orElse(null), type));
		} catch (ParseException e) {
			throw new BadRequest(e, "Could not parse header ''{0}''.", getName());
		}
	}

	/**
	 * Matches the specified pattern against this header value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Matcher <jv>matcher</jv> = <jv>request</jv>
	 * 		.getRequestHeader(<js>"Content-Type"</js>)
	 * 		.asMatcher(Pattern.<jsm>compile</jsm>(<js>"application/(.*)"</js>));
	 *
	 * <jk>if</jk> (<jv>matcher</jv>.matches()) {
	 * 		String <jv>mediaType</jv> = <jv>matcher</jv>.group(1);
	 * 	}
	 * </p>
	 *
	 * @param pattern The regular expression pattern to match.
	 * @return The matcher.
	 * @throws HttpException If a connection error occurred.
	 */
	public Matcher asMatcher(Pattern pattern) throws HttpException {
		return pattern.matcher(orElse(""));
	}

	/**
	 * Matches the specified pattern against this header value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Matcher <jv>matcher</jv> = <jv>request</jv>
	 * 		.getRequestHeader(<js>"Content-Type"</js>)
	 * 		.asMatcher(<js>"application/(.*)"</js>);
	 *
	 * 	<jk>if</jk> (<jv>matcher</jv>.matches()) {
	 * 		String <jv>mediaType</jv> = <jv>matcher</jv>.group(1);
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
	 * Matches the specified pattern against this header value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Matcher <jv>matcher</jv> = <jv>request</jv>
	 * 		.getRequestHeader(<js>"Content-Type"</js>)
	 * 		.asMatcher(<js>"application/(.*)"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jk>if</jk> (<jv>matcher</jv>.matches()) {
	 * 		String <jv>mediaType</jv> = <jv>matcher</jv>.group(1);
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
	 * Provides the ability to perform fluent-style assertions on this request header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getHeader(<js>"Content-Type"</js>)
	 * 		.assertValue().is(<js>"application/json"</js>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentRequestHeaderAssertion<RequestHeader> assertValue() {
		return new FluentRequestHeaderAssertion<>(this, this);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Header passthrough methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Gets the name of this header.
	 *
	 * @return The name of this header, never <jk>null</jk>.
	 */
	@Override /* Header */
	public String getName() {
		return name;
	}

	/**
	 * Gets the value of this header.
	 *
	 * @return The value of this header, may be <jk>null</jk>.
	 */
	@Override /* Header */
	public String getValue() {
		return value;
	}

	/**
	 * Parses the value.
	 *
	 * @return An array of {@link HeaderElement} entries, may be empty, but is never <jk>null</jk>.
	 * @throws HttpException In case of a parsing error.
	 */
	@Override /* Header */
	public HeaderElement[] getElements() throws HttpException {
		return new HeaderElement[0];
	}

	@Override /* Object */
	public String toString() {
		return getName() + ": " + getValue();
	}
}
