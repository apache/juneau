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
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.utils.*;

/**
 * Represents a single header on an HTTP response.
 */
public class RequestHeader implements Header {

	static final Header NULL_HEADER = new Header() {

		@Override /* Header */
		public String getName() {
			return null;
		}

		@Override /* Header */
		public String getValue() {
			return null;
		}

		@Override /* Header */
		public HeaderElement[] getElements() throws org.apache.http.ParseException {
			return new HeaderElement[0];
		}
	};

	private final Header header;
	private final RestRequest request;
	private final RestResponse response;
	private HttpPartParserSession parser;
	private HttpPartSchema schema;

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param response The response object.
	 * @param header The wrapped header.  Can be <jk>null</jk>.
	 */
	public RequestHeader(RestRequest request, RestResponse response, Header header) {
		this.request = request;
		this.response = response;
		this.header = header == null ? NULL_HEADER : header;
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
	 * Returns <jk>true</jk> if this header exists on the response.
	 *
	 * @return <jk>true</jk> if this header exists on the response.
	 */
	public boolean exists() {
		return header != NULL_HEADER;
	}

	/**
	 * Returns the value of this header as a string.
	 *
	 * @return The value of this header as a string, or <jk>null</jk> if header was not present.
	 */
	public String asString() {
		return getValue();
	}

	/**
	 * Same as {@link #asString()} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the header value in.
	 * @return The response object (for method chaining).
	 */
	public RestResponse asString(Mutable<String> m) {
		m.set(asString());
		return response;
	}

	/**
	 * Returns the value of this header as an {@link Optional}.
	 *
	 * @return The value of this header as an {@link Optional}, or an empty optional if header was not present.
	 */
	public Optional<String> asOptionalString() {
		return Optional.ofNullable(getValue());
	}

	/**
	 * Returns the value of this header as a string with a default value.
	 *
	 * @param def The default value.
	 * @return The value of this header as a string, or the default value if header was not present.
	 */
	public String asStringOrElse(String def) {
		return getValue() == null ? def : getValue();
	}

	/**
	 * Same as {@link #asStringOrElse(String)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the header value in.
	 * @param def The default value.
	 * @return The response object (for method chaining).
	 */
	public RestResponse asStringOrElse(Mutable<String> m, String def) {
		m.set(asStringOrElse(def));
		return response;
	}

	/**
	 * Converts this header to the specified type.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return The converted type, or <jk>null</jk> if header is not present.
	 * @throws ParseException If value could not be parsed.
	 */
	public <T> T as(Type type, Type...args) throws ParseException {
		return as(request.getClassMeta(type, args));
	}

	/**
	 * Same as {@link #as(Type,Type...)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return The response object (for method chaining).
	 * @throws ParseException If value could not be parsed.
	 */
	public <T> RestResponse as(Mutable<T> m, Type type, Type...args) throws ParseException {
		m.set(as(type, args));
		return response;
	}

	/**
	 * Converts this header to the specified type.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or <jk>null</jk> if header is not present.
	 * @throws ParseException If value could not be parsed.
	 */
	public <T> T as(Class<T> type) throws ParseException {
		return as(request.getClassMeta(type));
	}

	/**
	 * Same as {@link #as(Class)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The response object (for method chaining).
	 * @throws ParseException If value could not be parsed.
	 */
	public <T> RestResponse as(Mutable<T> m, Class<T> type) throws ParseException {
		m.set(as(type));
		return response;
	}

	/**
	 * Converts this header to the specified type.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or <jk>null</jk> if header is not present.
	 * @throws ParseException If value could not be parsed.
	 */
	public <T> T as(ClassMeta<T> type) throws ParseException {
		return parser.parse(HEADER, schema, asString(), type);
	}

	/**
	 * Same as {@link #as(ClassMeta)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The response object (for method chaining).
	 * @throws ParseException If value could not be parsed.
	 */
	public <T> RestResponse as(Mutable<T> m, ClassMeta<T> type) throws ParseException {
		m.set(as(type));
		return response;
	}

	/**
	 * Same as {@link #as(Type,Type...)} but returns the value as an {@link Optional}.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return The parsed value as an {@link Optional}, or an empty optional if header was not present.
	 * @throws ParseException If value could not be parsed.
	 */
	public <T> Optional<T> asOptional(Type type, Type...args) throws ParseException {
		return Optional.ofNullable(as(type, args));
	}

	/**
	 * Same as {@link #asOptional(Type,Type...)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return The response object (for method chaining).
	 * @throws ParseException If value could not be parsed.
	 */
	public <T> RestResponse asOptional(Mutable<Optional<T>> m, Type type, Type...args) throws ParseException {
		m.set(asOptional(type, args));
		return response;
	}

	/**
	 * Same as {@link #as(Class)} but returns the value as an {@link Optional}.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The parsed value as an {@link Optional}, or an empty optional if header was not present.
	 * @throws ParseException If value could not be parsed.
	 */
	public <T> Optional<T> asOptional(Class<T> type) throws ParseException {
		return Optional.ofNullable(as(type));
	}

	/**
	 * Same as {@link #asOptional(Class)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The response object (for method chaining).
	 * @throws ParseException If value could not be parsed.
	 */
	public <T> RestResponse asOptional(Mutable<Optional<T>> m, Class<T> type) throws ParseException {
		m.set(asOptional(type));
		return response;
	}

	/**
	 * Same as {@link #as(ClassMeta)} but returns the value as an {@link Optional}.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The parsed value as an {@link Optional}, or an empty optional if header was not present.
	 * @throws ParseException If value could not be parsed.
	 */
	public <T> Optional<T> asOptional(ClassMeta<T> type) throws ParseException {
		return Optional.ofNullable(as(type));
	}

	/**
	 * Same as {@link #asOptional(ClassMeta)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The response object (for method chaining).
	 * @throws ParseException If value could not be parsed.
	 */
	public <T> RestResponse asOptional(Mutable<Optional<T>> m, ClassMeta<T> type) throws ParseException {
		m.set(asOptional(type));
		return response;
	}

	/**
	 * Matches the specified pattern against this header value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse header using a regular expression.</jc>
	 * 	Matcher m = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).asMatcher(Pattern.<jsm>compile</jsm>(<js>"application/(.*)"</js>));
	 *
	 * 	<jk>if</jk> (m.matches())
	 * 		String mediaType = m.group(1);
	 * </p>
	 *
	 * @param pattern The regular expression pattern to match.
	 * @return The matcher.
	 */
	public Matcher asMatcher(Pattern pattern) {
		return pattern.matcher(asString());
	}

	/**
	 * Same as {@link #asMatcher(Pattern)} but sets the value in a mutable for fluent calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse header using a regular expression.</jc>
	 * 	Mutable&lt;Matcher&gt; m = Mutable.create();
	 * 	Matcher m = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).asMatcher(m, Pattern.<jsm>compile</jsm>(<js>"application/(.*)"</js>));
	 *
	 * 	<jk>if</jk> (m.get().matches())
	 * 		String mediaType = m.get().group(1);
	 * </p>
	 *
	 * @param m The mutable to set the value in.
	 * @param pattern The regular expression pattern to match.
	 * @return The response object (for method chaining).
	 */
	public RestResponse asMatcher(Mutable<Matcher> m, Pattern pattern) {
		m.set(pattern.matcher(asString()));
		return response;
	}

	/**
	 * Matches the specified pattern against this header value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse header using a regular expression.</jc>
	 * 	Matcher m = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).asMatcher(<js>"application/(.*)"</js>);
	 *
	 * 	<jk>if</jk> (m.matches())
	 * 		String mediaType = m.group(1);
	 * </p>
	 *
	 * @param regex The regular expression pattern to match.
	 * @return The matcher.
	 */
	public Matcher asMatcher(String regex) {
		return asMatcher(regex, 0);
	}

	/**
	 * Same as {@link #asMatcher(String)} but sets the value in a mutable for fluent calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse header using a regular expression.</jc>
	 * 	Mutable&lt;Matcher&gt; m = Mutable.create();
	 * 	Matcher m = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).asMatcher(m, <js>"application/(.*)"</js>);
	 *
	 * 	<jk>if</jk> (m.get().matches())
	 * 		String mediaType = m.get().group(1);
	 * </p>
	 *
	 * @param m The mutable to set the value in.
	 * @param regex The regular expression pattern to match.
	 * @return The response object (for method chaining).
	 */
	public RestResponse asMatcher(Mutable<Matcher> m, String regex) {
		asMatcher(regex, 0);
		return response;
	}

	/**
	 * Matches the specified pattern against this header value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse header using a regular expression.</jc>
	 * 	Matcher m = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).asMatcher(<js>"application/(.*)"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jk>if</jk> (m.matches())
	 * 		String mediaType = m.group(1);
	 * </p>
	 *
	 * @param regex The regular expression pattern to match.
	 * @param flags Pattern match flags.  See {@link Pattern#compile(String, int)}.
	 * @return The matcher.
	 */
	public Matcher asMatcher(String regex, int flags) {
		return asMatcher(Pattern.compile(regex, flags));
	}

	/**
	 * Same as {@link #asMatcher(String,int)} but sets the value in a mutable for fluent calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse header using a regular expression.</jc>
	 * 	Mutable&lt;Matcher&gt; m = Mutable.create();
	 * 	Matcher m = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).asMatcher(m, <js>"application/(.*)"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jk>if</jk> (m.get().matches())
	 * 		String mediaType = m.get().group(1);
	 * </p>
	 *
	 * @param m The mutable to set the value in.
	 * @param regex The regular expression pattern to match.
	 * @param flags Pattern match flags.  See {@link Pattern#compile(String, int)}.
	 * @return The response object (for method chaining).
	 */
	public RestResponse asMatcher(Mutable<Matcher> m, String regex, int flags) {
		asMatcher(Pattern.compile(regex, flags));
		return response;
	}

	/**
	 * Returns the response that created this object.
	 *
	 * @return The response that created this object.
	 */
	public RestResponse toResponse() {
		return response;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the header equals the specified value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type header is provided.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertExists();
	 * </p>
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public RestResponse assertExists() throws AssertionError {
		if (! exists())
			throw new BasicAssertionError("Response did not have the expected header {0}.", getName());
		return response;
	}

	/**
	 * Asserts that the header equals the specified value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue(<js>"application/json"</js>);
	 * </p>
	 *
	 * @param value The value to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public RestResponse assertValue(String value) throws AssertionError {
		if (! StringUtils.isEquals(value, asString()))
			throw new BasicAssertionError("Response did not have the expected value for header {0}.\n\tExpected=[{1}]\n\tActual=[{2}]", getName(), value, asString());
		return response;
	}

	/**
	 * Asserts that the header passes the specified predicate test.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue(x -&gt; x.equals(<js>"application/json"</js>));
	 * </p>
	 *
	 * @param test The predicate to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public RestResponse assertValue(Predicate<String> test) throws AssertionError {
		String text = asString();
		if (! test.test(text))
			throw new BasicAssertionError("Response did not have the expected value for header {0}.\n\tActual=[{1}]", getName(), text);
		return response;
	}

	/**
	 * Asserts that the header contains all the specified substrings.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValueContains(<js>"json"</js>);
	 * </p>
	 *
	 * @param values The substrings to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public RestResponse assertContains(String...values) throws AssertionError {
		String text = asString();
		for (String substring : values)
			if (! StringUtils.contains(text, substring))
				throw new BasicAssertionError("Response did not have the expected substring in header {0}.\n\tExpected=[{1}]\n\tHeader=[{2}]", getName(), substring, text);
		return response;
	}

	/**
	 * Asserts that the header matches the specified regular expression.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValueMatches(<js>".*json.*"</js>);
	 * </p>
	 *
	 * @param regex The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public RestResponse assertMatches(String regex) throws AssertionError {
		return assertMatches(regex, 0);
	}

	/**
	 * Asserts that the header matches the specified regular expression.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValueMatches(<js>".*json.*"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 * </p>
	 *
	 * @param regex The pattern to test for.
	 * @param flags Pattern match flags.  See {@link Pattern#compile(String, int)}.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public RestResponse assertMatches(String regex, int flags) throws AssertionError {
		String text = asString();
		if (! Pattern.compile(regex, flags).matcher(text).matches())
			throw new BasicAssertionError("Response did not match expected pattern in header {0}.\n\tpattern=[{1}]\n\tHeader=[{2}]", getName(), regex, text);
		return response;
	}

	/**
	 * Asserts that the header matches the specified pattern.
	 *
	 * <p>
	 * The pattern can contain <js>"*"</js> to represent zero or more arbitrary characters.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	Pattern p = Pattern.<jsm>compile</jsm>(<js>".*application\\/json.*"</js>);
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValueMatches(p);
	 * </p>
	 *
	 * @param pattern The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public RestResponse assertMatches(Pattern pattern) throws AssertionError {
		String text = asString();
		if (! pattern.matcher(text).matches())
			throw new BasicAssertionError("Response did not match expected pattern in header {0}.\n\tpattern=[{1}]\n\tHeader=[{2}]", getName(), pattern.pattern(), text);
		return response;
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
		return header.getName();
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
		return header.getValue();
	}

	/**
	 * Parses the value.
	 *
	 * @return An array of {@link HeaderElement} entries, may be empty, but is never <jk>null</jk>.
	 * @throws org.apache.http.ParseException In case of a parsing error.
	 */
	@Override /* Header */
	public HeaderElement[] getElements() throws org.apache.http.ParseException {
		return header.getElements();
	}

	@Override /* Object */
	public String toString() {
		return getName() + ": " + getValue();
	}
}
