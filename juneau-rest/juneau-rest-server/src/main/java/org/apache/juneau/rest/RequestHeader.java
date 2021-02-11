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
import java.util.Date;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.exception.HttpException;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;

/**
 * Represents a single header on an HTTP request.
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
	private HttpPartParserSession parser;
	private HttpPartSchema schema;

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param header The wrapped header.  Can be <jk>null</jk>.
	 */
	public RequestHeader(RestRequest request, Header header) {
		this.request = request;
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
	public Optional<Date> asDate() {
		return asDateHeader().asDate();
	}

	/**
	 * Returns the value of this header as a date.
	 *
	 * @return The value of this header as a date, or {@link Optional#empty()} if the header was not present.
	 */
	public Optional<ZonedDateTime> asZonedDateTime() {
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
	 * Returns the value of this header as a CSV array header.
	 *
	 * @return The value of this header as a CSV array header, never <jk>null</jk>.
	 */
	public BasicCsvArrayHeader asCsvArrayHeader() {
		return new BasicCsvArrayHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a date header.
	 *
	 * @return The value of this header as a date header, never <jk>null</jk>.
	 */
	public BasicDateHeader asDateHeader() {
		return new BasicDateHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an entity validator array header.
	 *
	 * @return The value of this header as an entity validator array header, never <jk>null</jk>.
	 */
	public BasicEntityTagArrayHeader asEntityTagArrayHeader() {
		return new BasicEntityTagArrayHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an entity validator header.
	 *
	 * @return The value of this header as an entity validator array, never <jk>null</jk>.
	 */
	public BasicEntityTagHeader asEntityTagHeader() {
		return new BasicEntityTagHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an integer header.
	 *
	 * @return The value of this header as an integer header, never <jk>null</jk>.
	 */
	public BasicIntegerHeader asIntegerHeader() {
		return new BasicIntegerHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an boolean header.
	 *
	 * @return The value of this header as an boolean header, never <jk>null</jk>.
	 */
	public BasicBooleanHeader asBooleanHeader() {
		return new BasicBooleanHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a long header.
	 *
	 * @return The value of this header as a long header, never <jk>null</jk>.
	 */
	public BasicLongHeader asLongHeader() {
		return new BasicLongHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a range array header.
	 *
	 * @return The value of this header as a range array header, never <jk>null</jk>.
	 */
	public BasicStringRangeArrayHeader asStringRangeArrayHeader() {
		return new BasicStringRangeArrayHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a string header.
	 *
	 * @return The value of this header as a string header, never <jk>null</jk>.
	 */
	public BasicStringHeader asStringHeader() {
		return new BasicStringHeader(getName(), getValue());
	}

	/**
	 * Returns the value of this header as a URI header.
	 *
	 * @return The value of this header as a URI header, never <jk>null</jk>.
	 */
	public BasicUriHeader asUriHeader() {
		return new BasicUriHeader(getName(), getValue());
	}

	/**
	 * Same as {@link #asString()} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the header value in.
	 * @return The response object (for method chaining).
	 */
	public RestRequest asString(Mutable<String> m) {
		m.set(asString().orElse(null));
		return request;
	}

	/**
	 * Converts this header to the specified type.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return The converted type, or {@link Optional#empty()} if the header is not present.
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> Optional<T> as(Type type, Type...args) throws HttpException {
		return as(request.getBeanSession().getClassMeta(type, args));
	}

	/**
	 * Same as {@link #as(Type,Type...)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return The response object (for method chaining).
	 * @throws HttpException If value could not be parsed.
	 */
	@SuppressWarnings("unchecked")
	public <T> RestRequest as(Mutable<T> m, Type type, Type...args) throws HttpException {
		m.set((T)as(type, args).orElse(null));
		return request;
	}

	/**
	 * Converts this header to the specified type.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or {@link Optional#empty()} if the header is not present.
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> Optional<T> as(Class<T> type) throws HttpException {
		return as(request.getBeanSession().getClassMeta(type));
	}

	/**
	 * Same as {@link #as(Class)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The response object (for method chaining).
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> RestRequest as(Mutable<T> m, Class<T> type) throws HttpException {
		m.set(as(type).orElse(null));
		return request;
	}

	/**
	 * Converts this header to the specified type.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or {@link Optional#empty()} if the header is not present.
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> Optional<T> as(ClassMeta<T> type) throws HttpException {
		try {
			return Optional.ofNullable(parser.parse(HEADER, schema, asString().orElse(null), type));
		} catch (ParseException e) {
			throw new BadRequest("Could not parse header ''{0}''.", getName());
		}
	}

	/**
	 * Same as {@link #as(ClassMeta)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The response object (for method chaining).
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> RestRequest as(Mutable<T> m, ClassMeta<T> type) throws HttpException {
		m.set(as(type).orElse(null));
		return request;
	}

	/**
	 * Matches the specified pattern against this header value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse header using a regular expression.</jc>
	 * 	Matcher <jv>matcher</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).asMatcher(Pattern.<jsm>compile</jsm>(<js>"application/(.*)"</js>));
	 *
	 * 	<jk>if</jk> (<jv>matcher</jv>.matches()) {
	 * 		String <jv>mediaType</jv> = <jv>matcher</jv>.group(1);
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
	 * Same as {@link #asMatcher(Pattern)} but sets the value in a mutable for fluent calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse header using a regular expression.</jc>
	 * 	Mutable&lt;Matcher&gt; <jv>mutable</jv> = Mutable.<jsm>create</jsm>();
	 *
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).asMatcher(<jv>mutable</jv>, Pattern.<jsm>compile</jsm>(<js>"application/(.*)"</js>));
	 *
	 * 	<jk>if</jk> (<jv>mutable</jv>.get().matches()) {
	 * 		String <jv>mediaType</jv> = <jv>mutable</jv>.get().group(1);
	 * 	}
	 * </p>
	 *
	 * @param m The mutable to set the value in.
	 * @param pattern The regular expression pattern to match.
	 * @return The response object (for method chaining).
	 * @throws HttpException If a connection error occurred.
	 */
	public RestRequest asMatcher(Mutable<Matcher> m, Pattern pattern) throws HttpException {
		m.set(pattern.matcher(asString().orElse("")));
		return request;
	}

	/**
	 * Matches the specified pattern against this header value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse header using a regular expression.</jc>
	 * 	Matcher <jv>matcher</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).asMatcher(<js>"application/(.*)"</js>);
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
	 * Same as {@link #asMatcher(String)} but sets the value in a mutable for fluent calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse header using a regular expression.</jc>
	 * 	Mutable&lt;Matcher&gt; <jv>mutable</jv> = Mutable.<jsm>create</jsm>();
	 *
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).asMatcher(<jv>mutable</jv>, <js>"application/(.*)"</js>);
	 *
	 * 	<jk>if</jk> (<jv>mutable</jv>.get().matches()) {
	 * 		String <jv>mediaType</jv> = <jv>mutable</jv>.get().group(1);
	 * 	}
	 * </p>
	 *
	 * @param m The mutable to set the value in.
	 * @param regex The regular expression pattern to match.
	 * @return The response object (for method chaining).
	 * @throws HttpException If a connection error occurred.
	 */
	public RestRequest asMatcher(Mutable<Matcher> m, String regex) throws HttpException {
		m.set(asMatcher(regex, 0));
		return request;
	}

	/**
	 * Matches the specified pattern against this header value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse header using a regular expression.</jc>
	 * 	Matcher <jv>matcher</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).asMatcher(<js>"application/(.*)"</js>, <jsf>CASE_INSENSITIVE</jsf>);
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

	/**
	 * Same as {@link #asMatcher(String,int)} but sets the value in a mutable for fluent calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse header using a regular expression.</jc>
	 * 	Mutable&lt;Matcher&gt; <jv>mutable</jv> = Mutable.<jsm>create</jsm>();
	 *
	 * 	client
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).asMatcher(<jv>mutable</jv>, <js>"application/(.*)"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jk>if</jk> (<jv>mutable</jv>.get().matches()) {
	 * 		String <jv>mediaType</jv> = <jv>mutable</jv>.get().group(1);
	 * 	}
	 * </p>
	 *
	 * @param m The mutable to set the value in.
	 * @param regex The regular expression pattern to match.
	 * @param flags Pattern match flags.  See {@link Pattern#compile(String, int)}.
	 * @return The response object (for method chaining).
	 * @throws HttpException If a connection error occurred.
	 */
	public RestRequest asMatcher(Mutable<Matcher> m, String regex, int flags) throws HttpException {
		m.set(asMatcher(Pattern.compile(regex, flags)));
		return request;
	}

	/**
	 * Returns the response that created this object.
	 *
	 * @return The response that created this object.
	 */
	public RestRequest toRequest() {
		return request;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on this response header.
	 *
	 * <p>
	 * This method is called directly from the {@link RestRequest#assertStringHeader(String)} method to instantiate a fluent assertions object.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type header is provided.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).exists();
	 *
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).equals(<js>"application/json"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using test predicate.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).passes(<jv>x</jv> -&gt; <jv>x</jv>.equals(<js>"application/json"</js>));
	 *
	 * 	<jc>// Validates the content type is JSON by just checking for substring.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).contains(<js>"json"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).matches(<js>".*json.*"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using case-insensitive regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).matches(<js>".*json.*"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original response object allowing you to chain multiple requests like so:
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the header and converts it to a bean.</jc>
	 * 	MediaType <jv>mediaType</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).exists()
	 * 		.assertHeader(<js>"Content-Type"</js>).matches(<js>".*json.*"</js>)
	 * 		.getHeader(<js>"Content-Type"</js>).as(MediaType.<jk>class</jk>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentStringAssertion<RequestHeader> assertString() {
		return new FluentStringAssertion<>(asString().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on an integer response header.
	 *
	 * <p>
	 * This method is called directly from the {@link RestRequest#assertIntegerHeader(String)} method to instantiate a fluent assertions object.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the response content age is greater than 1.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertIntegerHeader(<js>"Age"</js>).isGreaterThan(1);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentIntegerAssertion<RequestHeader> assertInteger() {
		return new FluentIntegerAssertion<>(asIntegerHeader().asInteger().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on a long response header.
	 *
	 * <p>
	 * This method is called directly from the {@link RestRequest#assertLongHeader(String)} method to instantiate a fluent assertions object.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the response body is not too long.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertLongHeader(<js>"Length"</js>).isLessThan(100000);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentLongAssertion<RequestHeader> assertLong() {
		return new FluentLongAssertion<>(asLongHeader().asLong().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on a date response header.
	 *
	 * <p>
	 * This method is called directly from the {@link RestRequest#assertDateHeader(String)} method to instantiate a fluent assertions object.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the response content is not expired.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Expires"</js>).assertDate().isAfterNow();
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentZonedDateTimeAssertion<RequestHeader> assertDate() {
		return new FluentZonedDateTimeAssertion<>(asDateHeader().asZonedDateTime().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on comma-separated string headers.
	 *
	 * <p>
	 * This method is called directly from the {@link RestRequest#assertCsvArrayHeader(String)} method to instantiate a fluent assertions object.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the response content is not expired.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Allow"</js>).assertCsvArray().contains(<js>"GET"</js>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentListAssertion<RequestHeader> assertCsvArray() {
		return new FluentListAssertion<>(asCsvArrayHeader().asList().orElse(null), this);
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
	 * @throws HttpException In case of a parsing error.
	 */
	@Override /* Header */
	public HeaderElement[] getElements() throws HttpException {
		return header.getElements();
	}

	@Override /* Object */
	public String toString() {
		return getName() + ": " + getValue();
	}
}
