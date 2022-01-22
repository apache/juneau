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
package org.apache.juneau.rest.client;

import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;
import static java.util.Optional.*;

import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.client.assertion.*;
import org.apache.juneau.utils.*;

/**
 * Represents a single header on an HTTP response.
 *
 * <p>
 * An extension of an HttpClient {@link Header} that provides various support for converting the header to POJOs and
 * other convenience methods.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-client}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class ResponseHeader implements Header {

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

	private final String name, value;
	private final HeaderElement[] elements;
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
	public ResponseHeader(RestRequest request, RestResponse response, Header header) {
		this.request = request;
		this.response = response;
		this.name = header == null ? "" : header.getName();
		this.value = header == null ? null : header.getValue();
		this.elements = header == null ? new HeaderElement[0] : header.getElements();
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
	 * @return This object.
	 */
	public ResponseHeader schema(HttpPartSchema value) {
		this.schema = value;
		return this;
	}

	/**
	 * Specifies the part parser to use for this header.
	 *
	 * <p>
	 * If not specified, uses the part parser defined on the client by calling {@link RestClient.Builder#partParser(Class)}.
	 *
	 * @param value
	 * 	The new part parser to use for this header.
	 * 	<br>If <jk>null</jk>, {@link SimplePartParser#DEFAULT} will be used.
	 * @return This object.
	 */
	public ResponseHeader parser(HttpPartParserSession value) {
		this.parser = value == null ? SimplePartParser.DEFAULT_SESSION : value;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Retrievers
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this header exists on the response.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asString().isPresent()</c>.
	 *
	 * @return <jk>true</jk> if this header exists on the response.
	 */
	public boolean isPresent() {
		return value != null;
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
			ConstructorInfo cc = ci.getPublicConstructor(x -> x.hasParamTypes(String.class));
			if (cc != null)
				return cc.invoke(getValue());
			cc = ci.getPublicConstructor(x -> x.hasParamTypes(String.class, String.class));
			if (cc != null)
				return cc.invoke(getName(), getValue());
		} catch (Throwable e) {
			if (e instanceof ExecutableException)
				e = ((ExecutableException)e).getCause();
			throw runtimeException(e);
		}
		throw runtimeException("Could not determine a method to construct type {0}", className(c));
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
	 * @return This object.
	 */
	public RestResponse asString(Mutable<String> m) {
		m.set(orElse(null));
		return response;
	}

	/**
	 * Converts this header to the specified type.
	 *
	 * <p>
	 * See {@doc jm.ComplexDataTypes Complex Data Types} for information on defining complex generic types of {@link Map Maps} and {@link Collection Collections}.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return The converted type, or <jk>null</jk> if header is not present.
	 * @throws RestCallException If value could not be parsed.
	 */
	public <T> Optional<T> asType(Type type, Type...args) throws RestCallException {
		return asType(request.getClassMeta(type, args));
	}

	/**
	 * Same as {@link #asType(Type,Type...)} but sets the value in a mutable for fluent calls.
	 *
	 * <p>
	 * See {@doc jm.ComplexDataTypes Complex Data Types} for information on defining complex generic types of {@link Map Maps} and {@link Collection Collections}.
	 *
	 * @param m The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return This object.
	 * @throws RestCallException If value could not be parsed.
	 */
	@SuppressWarnings("unchecked")
	public <T> RestResponse asType(Mutable<T> m, Type type, Type...args) throws RestCallException {
		m.set((T)asType(type, args).orElse(null));
		return response;
	}

	/**
	 * Converts this header to the specified type.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or <jk>null</jk> if header is not present.
	 * @throws RestCallException If value could not be parsed.
	 */
	public <T> Optional<T> asType(Class<T> type) throws RestCallException {
		return asType(request.getClassMeta(type));
	}

	/**
	 * Same as {@link #asType(Class)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return This object.
	 * @throws RestCallException If value could not be parsed.
	 */
	public <T> RestResponse asType(Mutable<T> m, Class<T> type) throws RestCallException {
		m.set(asType(type).orElse(null));
		return response;
	}

	/**
	 * Converts this header to the specified type.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or <jk>null</jk> if header is not present.
	 * @throws RestCallException If value could not be parsed.
	 */
	public <T> Optional<T> asType(ClassMeta<T> type) throws RestCallException {
		try {
			return ofNullable(parser.parse(HEADER, schema, getValue(), type));
		} catch (ParseException e) {
			throw new RestCallException(response, e, "Could not parse response header {0}.", getName());
		}
	}

	/**
	 * Same as {@link #asType(ClassMeta)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return This object.
	 * @throws RestCallException If value could not be parsed.
	 */
	public <T> RestResponse asType(Mutable<T> m, ClassMeta<T> type) throws RestCallException {
		m.set(asType(type).orElse(null));
		return response;
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
	 * 		.getResponseHeader(<js>"Content-Type"</js>).asMatcher(Pattern.<jsm>compile</jsm>(<js>"application/(.*)"</js>));
	 *
	 * 	<jk>if</jk> (<jv>matcher</jv>.matches()) {
	 * 		String <jv>mediaType</jv> = <jv>matcher</jv>.group(1);
	 * 	}
	 * </p>
	 *
	 * @param pattern The regular expression pattern to match.
	 * @return The matcher.
	 * @throws RestCallException If a connection error occurred.
	 */
	public Matcher asMatcher(Pattern pattern) throws RestCallException {
		return pattern.matcher(orElse(""));
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
	 * @throws RestCallException If a connection error occurred.
	 */
	public Matcher asMatcher(String regex) throws RestCallException {
		return asMatcher(regex, 0);
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
	 * @throws RestCallException If a connection error occurred.
	 */
	public Matcher asMatcher(String regex, int flags) throws RestCallException {
		return asMatcher(Pattern.compile(regex, flags));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on this response header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type header is provided.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().exists();
	 *
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).equals(<js>"application/json"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using test predicate.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().passes(<jv>x</jv> -&gt; <jv>x</jv>.equals(<js>"application/json"</js>));
	 *
	 * 	<jc>// Validates the content type is JSON by just checking for substring.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().contains(<js>"json"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().matches(<js>".*json.*"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using case-insensitive regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().matches(<js>".*json.*"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original response object allowing you to chain multiple requests like so:
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the header and converts it to a bean.</jc>
	 * 	MediaType <jv>mediaType</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().exists()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().matches(<js>".*json.*"</js>)
	 * 		.getHeader(<js>"Content-Type"</js>).as(MediaType.<jk>class</jk>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentResponseHeaderAssertion<ResponseHeader> assertValue() {
		return new FluentResponseHeaderAssertion<>(this, this);
	}

	/**
	 * Returns the response that created this object.
	 *
	 * @return The response that created this object.
	 */
	public RestResponse response() {
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

	/**
	 * Parses the value.
	 *
	 * @return An array of {@link HeaderElement} entries, may be empty, but is never <jk>null</jk>.
	 * @throws org.apache.http.ParseException In case of a parsing error.
	 */
	@Override /* Header */
	public HeaderElement[] getElements() throws org.apache.http.ParseException {
		return elements;
	}

	@Override /* Object */
	public String toString() {
		return getName() + ": " + getValue();
	}
}
