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
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.ParseException;

/**
 * Represents a single HTTP part on an HTTP request.
 */
@FluentSetters
public abstract class RequestHttpPart {

	private final HttpPartType partType;
	private final String name;
	private final RestRequest request;
	private HttpPartParserSession parser;
	private HttpPartSchema schema;

	/**
	 * Constructor.
	 *
	 * @param partType The HTTP part type.
	 * @param request The request object.
	 * @param name The header name.
	 */
	public RequestHttpPart(HttpPartType partType, RestRequest request, String name) {
		this.partType = partType;
		this.request = request;
		this.name = name;
		parser(null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Setters
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies the part schema for this part.
	 *
	 * <p>
	 * Used by schema-based part parsers such as {@link OpenApiParser}.
	 *
	 * @param value
	 * 	The part schema.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RequestHttpPart schema(HttpPartSchema value) {
		this.schema = value;
		return this;
	}

	/**
	 * Specifies the part parser to use for this part.
	 *
	 * <p>
	 * If not specified, uses the part parser defined on the client by calling {@link RestContextBuilder#partParser(Class)}.
	 *
	 * @param value
	 * 	The new part parser to use for this part.
	 * 	<br>If <jk>null</jk>, {@link SimplePartParser#DEFAULT} will be used.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RequestHttpPart parser(HttpPartParserSession value) {
		this.parser = value == null ? SimplePartParser.DEFAULT_SESSION : value;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Retrievers
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this part exists on the request.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asString().isPresent()</c>.
	 *
	 * @return <jk>true</jk> if this part exists on the request.
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
	 * Returns the value of this part as a string.
	 *
	 * @return The value of this part as a string, or {@link Optional#empty()} if the part was not present.
	 */
	public Optional<String> asString() {
		return Optional.ofNullable(getValue());
	}

	/**
	 * Converts this part to the specified POJO type using the request {@link HttpPartParser} and optional schema.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return The converted type, or {@link Optional#empty()} if the part is not present.
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> Optional<T> asType(Type type, Type...args) throws HttpException {
		return asType(request.getBeanSession().getClassMeta(type, args));
	}

	/**
	 * Converts this part to the specified POJO type using the request {@link HttpPartParser} and optional schema.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or {@link Optional#empty()} if the part is not present.
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> Optional<T> asType(Class<T> type) throws HttpException {
		return asType(request.getBeanSession().getClassMeta(type));
	}

	/**
	 * Converts this part to the specified POJO type using the request {@link HttpPartParser} and optional schema.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or {@link Optional#empty()} if the part is not present.
	 * @throws HttpException If value could not be parsed.
	 */
	public <T> Optional<T> asType(ClassMeta<T> type) throws HttpException {
		try {
			return Optional.ofNullable(parser.parse(HEADER, schema, orElse(null), type));
		} catch (ParseException e) {
			throw new BadRequest(e, "Could not parse {0} parameter ''{1}''.", partType.toString().toLowerCase(), getName());
		}
	}

	/**
	 * Matches the specified pattern against this part value.
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
	 * Matches the specified pattern against this part value.
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
	 * Matches the specified pattern against this part value.
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

	/**
	 * Gets the name of this part.
	 *
	 * @return The name of this part, never <jk>null</jk>.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the value of this part.
	 *
	 * @return The value of this part, may be <jk>null</jk>.
	 */
	public abstract String getValue();

	@Override /* Object */
	public String toString() {
		return getName() + ": " + getValue();
	}

	// <FluentSetters>

	// </FluentSetters>
}
