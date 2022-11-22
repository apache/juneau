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

import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.client.assertion.*;

/**
 * Represents a single header on an HTTP response.
 *
 * <p>
 * An extension of an HttpClient {@link Header} that provides various support for converting the header to POJOs and
 * other convenience methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public class ResponseHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

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

	private final HeaderElement[] elements;
	private final RestRequest request;
	private final RestResponse response;
	private HttpPartParserSession parser;
	private HttpPartSchema schema;

	/**
	 * Constructor.
	 * @param name The header name.
	 * @param request The request object.
	 * @param response The response object.
	 * @param header The wrapped header.  Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public ResponseHeader(String name, RestRequest request, RestResponse response, Header header) {
		super(name, header == null ? null : header.getValue());
		this.request = request;
		this.response = response;
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
	public Optional<String[]> asCsvArray() {
		return asCsvHeader().asArray();
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
			throw asRuntimeException(e);
		}
		throw new BasicRuntimeException("Could not determine a method to construct type {0}", className(c));
	}

	/**
	 * Returns the value of this header as a CSV array header.
	 *
	 * @return The value of this header as a CSV array header, never <jk>null</jk>.
	 */
	public BasicCsvHeader asCsvHeader() {
		return new BasicCsvHeader(getName(), getValue());
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
	public BasicEntityTagsHeader asEntityTagsHeader() {
		return new BasicEntityTagsHeader(getName(), getValue());
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
	public BasicStringRangesHeader asStringRangesHeader() {
		return new BasicStringRangesHeader(getName(), getValue());
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
	 * @param value The mutable to set the header value in.
	 * @return This object.
	 */
	public RestResponse asString(Value<String> value) {
		value.set(orElse(null));
		return response;
	}

	/**
	 * Converts this header to the specified type.
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../../index.html#jm.ComplexDataTypes">Complex Data Types</a> for information on defining complex generic types of {@link Map Maps} and {@link Collection Collections}.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return The converted type, or <jk>null</jk> if header is not present.
	 */
	public <T> Optional<T> as(Type type, Type...args) {
		return as(request.getClassMeta(type, args));
	}

	/**
	 * Same as {@link #as(Type,Type...)} but sets the value in a mutable for fluent calls.
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../../index.html#jm.ComplexDataTypes">Complex Data Types</a> for information on defining complex generic types of {@link Map Maps} and {@link Collection Collections}.
	 *
	 * @param value The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public <T> RestResponse as(Value<T> value, Type type, Type...args) {
		value.set((T)as(type, args).orElse(null));
		return response;
	}

	/**
	 * Converts this header to the specified type.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or <jk>null</jk> if header is not present.
	 */
	public <T> Optional<T> as(Class<T> type) {
		return as(request.getClassMeta(type));
	}

	/**
	 * Same as {@link #as(Class)} but sets the value in a mutable for fluent calls.
	 *
	 * @param value The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return This object.
	 */
	public <T> RestResponse as(Value<T> value, Class<T> type) {
		value.set(as(type).orElse(null));
		return response;
	}

	/**
	 * Converts this header to the specified type.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or <jk>null</jk> if header is not present.
	 */
	public <T> Optional<T> as(ClassMeta<T> type) {
		try {
			return optional(parser.parse(HEADER, schema, getValue(), type));
		} catch (ParseException e) {
			throw new BasicRuntimeException(e, "Could not parse response header {0}.", getName());
		}
	}

	/**
	 * Same as {@link #as(ClassMeta)} but sets the value in a mutable for fluent calls.
	 *
	 * @param value The mutable to set the parsed header value in.
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return This object.
	 */
	public <T> RestResponse as(Value<T> value, ClassMeta<T> type) {
		value.set(as(type).orElse(null));
		return response;
	}

	/**
	 * Matches the specified pattern against this header value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
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
	 */
	public Matcher asMatcher(Pattern pattern) {
		return pattern.matcher(orElse(""));
	}

	/**
	 * Matches the specified pattern against this header value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
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
	 */
	public Matcher asMatcher(String regex) {
		return asMatcher(regex, 0);
	}

	/**
	 * Matches the specified pattern against this header value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
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
	 */
	public Matcher asMatcher(String regex, int flags) {
		return asMatcher(Pattern.compile(regex, flags));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on this response header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
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
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().equals(<js>"application/json"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using test predicate.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().is(<jv>x</jv> -&gt; <jv>x</jv>.equals(<js>"application/json"</js>));
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
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().isPattern(<js>".*json.*"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using case-insensitive regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().isPattern(<js>".*json.*"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original response object allowing you to chain multiple requests like so:
	 * <p class='bjava'>
	 * 	<jc>// Validates the header and converts it to a bean.</jc>
	 * 	MediaType <jv>mediaType</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().isNotEmpty()
	 * 		.getHeader(<js>"Content-Type"</js>).assertValue().isPattern(<js>".*json.*"</js>)
	 * 		.getHeader(<js>"Content-Type"</js>).as(MediaType.<jk>class</jk>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentResponseHeaderAssertion<ResponseHeader> assertValue() {
		return new FluentResponseHeaderAssertion<>(this, this);
	}

	/**
	 * Shortcut for calling <c>assertValue().asString()</c>.
	 *
	 * @return A new fluent assertion.
	 */
	public FluentStringAssertion<ResponseHeader> assertString() {
		return new FluentResponseHeaderAssertion<>(this, this).asString();
	}

	/**
	 * Shortcut for calling <c>assertValue().asInteger()</c>.
	 *
	 * @return A new fluent assertion.
	 */
	public FluentIntegerAssertion<ResponseHeader> assertInteger() {
		return new FluentResponseHeaderAssertion<>(this, this).asInteger();
	}

	/**
	 * Shortcut for calling <c>assertValue().asLong()</c>.
	 *
	 * @return A new fluent assertion.
	 */
	public FluentLongAssertion<ResponseHeader> assertLong() {
		return new FluentResponseHeaderAssertion<>(this, this).asLong();
	}

	/**
	 * Shortcut for calling <c>assertValue().asZonedDateTime()</c>.
	 *
	 * @return A new fluent assertion.
	 */
	public FluentZonedDateTimeAssertion<ResponseHeader> assertZonedDateTime() {
		return new FluentResponseHeaderAssertion<>(this, this).asZonedDateTime();
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
	 * Parses the value.
	 *
	 * @return An array of {@link HeaderElement} entries, may be empty, but is never <jk>null</jk>.
	 * @throws org.apache.http.ParseException In case of a parsing error.
	 */
	@Override /* Header */
	public HeaderElement[] getElements() throws org.apache.http.ParseException {
		return elements;
	}
}
