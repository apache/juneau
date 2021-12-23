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

import static org.apache.juneau.httppart.HttpPartType.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;

/**
 * Represents a single HTTP part on an HTTP request.
 *
 * Parent of the following classes:
 * <ul class='javatreec'>
 * 	<li class='jc'>{@link RequestHeader}
 * 	<li class='jc'>{@link RequestQueryParam}
 * 	<li class='jc'>{@link RequestFormParam}
 * 	<li class='jc'>{@link RequestPathParam}
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jrs.HttpParts}
 * 	<li class='extlink'>{@source}
 * </ul>
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
	 * @return This object.
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
	 * If not specified, uses the part parser defined on the client by calling {@link org.apache.juneau.rest.RestContext.Builder#partParser()}.
	 *
	 * @param value
	 * 	The new part parser to use for this part.
	 * 	<br>If <jk>null</jk>, {@link SimplePartParser#DEFAULT} will be used.
	 * @return This object.
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
	 * <p>
	 * See {@doc jm.ComplexDataTypes Complex Data Types} for information on defining complex generic types of {@link Map Maps} and {@link Collection Collections}.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @param args The type parameters.
	 * @return The converted type, or {@link Optional#empty()} if the part is not present.
	 * @throws BasicHttpException If value could not be parsed.
	 */
	public <T> Optional<T> as(Type type, Type...args) throws BasicHttpException {
		return as(request.getBeanSession().getClassMeta(type, args));
	}

	/**
	 * Converts this part to the specified POJO type using the request {@link HttpPartParser} and optional schema.
	 *
	 * <p>
	 * If the specified type is an HTTP part type (extends from {@link org.apache.http.Header}/{@link NameValuePair}), then looks for
	 * one of the following constructors:
	 * <ul class='javatree'>
	 * 	<li class='jm><c><jk>public</jv> T(String <jv>value</jv>);</c>
	 * 	<li class='jm><c><jk>public</jv> T(String <jv>name</jv>, String <jv>value</jv>);</c>
	 * </ul>
	 *
	 * <p>
	 * If it doesn't find one of those constructors, then it parses it into the specified type using the part parser.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or {@link Optional#empty()} if the part is not present.
	 * @throws BasicHttpException If value could not be parsed.
	 */
	public <T> Optional<T> as(Class<T> type) throws BasicHttpException {
		return as(request.getBeanSession().getClassMeta(type));
	}

	/**
	 * Converts this part to the specified POJO type using the request {@link HttpPartParser} and optional schema.
	 *
	 * <p>
	 * If the specified type is an HTTP part type (extends from {@link org.apache.http.Header}/{@link NameValuePair}), then looks for
	 * one of the following constructors:
	 * <ul class='javatree'>
	 * 	<li class='jm><c><jk>public</jv> T(String <jv>value</jv>);</c>
	 * 	<li class='jm><c><jk>public</jv> T(String <jv>name</jv>, String <jv>value</jv>);</c>
	 * </ul>
	 *
	 * <p>
	 * If it doesn't find one of those constructors, then it parses it into the specified type using the part parser.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or {@link Optional#empty()} if the part is not present.
	 * @throws BasicHttpException If value could not be parsed.
	 */
	public <T> Optional<T> as(ClassMeta<T> type) throws BasicHttpException {
		try {
			if (HttpParts.isHttpPart(partType, type)) {
				ConstructorInfo cc = HttpParts.getConstructor(type).orElse(null);
				if (cc != null) {
					if (! isPresent())
						return Optional.empty();
					if (cc.hasParamTypes(String.class))
						return Optional.of(cc.invoke(get()));
					if (cc.hasParamTypes(String.class, String.class))
						return Optional.of(cc.invoke(getName(), get()));
				}
			}
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
	 * @throws BasicHttpException If a connection error occurred.
	 */
	public Matcher asMatcher(Pattern pattern) throws BasicHttpException {
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
	 * @throws BasicHttpException If a connection error occurred.
	 */
	public Matcher asMatcher(String regex) throws BasicHttpException {
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
	 * @throws BasicHttpException If a connection error occurred.
	 */
	public Matcher asMatcher(String regex, int flags) throws BasicHttpException {
		return asMatcher(Pattern.compile(regex, flags));
	}

	/**
	 * Returns the value of this parameter as an integer.
	 *
	 * @return The value of this parameter as an integer, or {@link Optional#empty()} if the parameter was not present.
	 */
	public Optional<Integer> asInteger() {
		return asIntegerPart().asInteger();
	}

	/**
	 * Returns the value of this parameter as a boolean.
	 *
	 * @return The value of this parameter as a boolean, or {@link Optional#empty()} if the parameter was not present.
	 */
	public Optional<Boolean> asBoolean() {
		return asBooleanPart().asBoolean();
	}

	/**
	 * Returns the value of this parameter as a long.
	 *
	 * @return The value of this parameter as a long, or {@link Optional#empty()} if the parameter was not present.
	 */
	public Optional<Long> asLong() {
		return asLongPart().asLong();
	}

	/**
	 * Returns the value of this parameter as a date.
	 *
	 * @return The value of this parameter as a date, or {@link Optional#empty()} if the parameter was not present.
	 */
	public Optional<ZonedDateTime> asDate() {
		return asDatePart().asZonedDateTime();
	}

	/**
	 * Returns the value of this parameter as a list from a comma-delimited string.
	 *
	 * @return The value of this parameter as a list from a comma-delimited string, or {@link Optional#empty()} if the parameter was not present.
	 */
	public Optional<List<String>> asCsvArray() {
		return asCsvArrayPart().asList();
	}

	/**
	 * Returns the value of this parameter as a {@link BasicCsvArrayPart}.
	 *
	 * @return The value of this parameter as a {@link BasicCsvArrayPart}, never <jk>null</jk>.
	 */
	public BasicCsvArrayPart asCsvArrayPart() {
		return new BasicCsvArrayPart(getName(), getValue());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicDatePart}.
	 *
	 * @return The value of this parameter as a {@link BasicDatePart}, never <jk>null</jk>.
	 */
	public BasicDatePart asDatePart() {
		return new BasicDatePart(getName(), getValue());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicIntegerPart}.
	 *
	 * @return The value of this parameter as a {@link BasicIntegerPart}, never <jk>null</jk>.
	 */
	public BasicIntegerPart asIntegerPart() {
		return new BasicIntegerPart(getName(), getValue());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicBooleanPart}.
	 *
	 * @return The value of this parameter as a {@link BasicBooleanPart}, never <jk>null</jk>.
	 */
	public BasicBooleanPart asBooleanPart() {
		return new BasicBooleanPart(getName(), getValue());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicLongPart}.
	 *
	 * @return The value of this parameter as a {@link BasicLongPart}, never <jk>null</jk>.
	 */
	public BasicLongPart asLongPart() {
		return new BasicLongPart(getName(), getValue());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicStringPart}.
	 *
	 * @return The value of this parameter as a {@link BasicStringPart}, never <jk>null</jk>.
	 */
	public BasicStringPart asStringPart() {
		return new BasicStringPart(getName(), getValue());
	}

	/**
	 * Returns the value of this parameter as a {@link BasicUriPart}.
	 *
	 * @return The value of this parameter as a {@link BasicUriPart}, never <jk>null</jk>.
	 */
	public BasicUriPart asUriPart() {
		return new BasicUriPart(getName(), getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the request that created this part.
	 *
	 * @return The request that created this part.
	 */
	protected RestRequest getRequest() {
		return request;
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
		return getName() + "=" + getValue();
	}

	// <FluentSetters>

	// </FluentSetters>
}
