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
import java.util.regex.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.BasicHttpException;
import org.apache.juneau.httppart.*;

/**
 * Represents a single header on an HTTP request.
 *
 * <p>
 * Typically accessed through the {@link RequestHeaders} class.
 *
 * <p>
 * 	Some important methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RequestHeader}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for retrieving simple string values:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestHeader#asString() asString()}
 * 			<li class='jm'>{@link RequestHeader#get() get()}
 * 			<li class='jm'>{@link RequestHeader#isPresent() isPresent()}
 * 			<li class='jm'>{@link RequestHeader#orElse(String) orElse(String)}
 * 		</ul>
 * 		<li>Methods for retrieving as other common types:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestHeader#asBoolean() asBoolean()}
 * 			<li class='jm'>{@link RequestHeader#asBooleanHeader() asBooleanHeader()}
 * 			<li class='jm'>{@link RequestHeader#asCsvArray() asCsvArray()}
 * 			<li class='jm'>{@link RequestHeader#asCsvArrayHeader() asCsvArrayHeader()}
 * 			<li class='jm'>{@link RequestHeader#asDate() asDate()}
 * 			<li class='jm'>{@link RequestHeader#asDateHeader() asDateHeader()}
 * 			<li class='jm'>{@link RequestHeader#asEntityTagArrayHeader() asEntityTagArrayHeader()}
 * 			<li class='jm'>{@link RequestHeader#asEntityTagHeader() asEntityTagHeader()}
 * 			<li class='jm'>{@link RequestHeader#asInteger() asInteger()}
 * 			<li class='jm'>{@link RequestHeader#asIntegerHeader() asIntegerHeader()}
 * 			<li class='jm'>{@link RequestHeader#asLong() asLong()}
 * 			<li class='jm'>{@link RequestHeader#asLongHeader() asLongHeader()}
 * 			<li class='jm'>{@link RequestHeader#asMatcher(Pattern) asMatcher(Pattern)}
 * 			<li class='jm'>{@link RequestHeader#asMatcher(String) asMatcher(String)}
 * 			<li class='jm'>{@link RequestHeader#asMatcher(String,int) asMatcher(String,int)}
 * 			<li class='jm'>{@link RequestHeader#asStringHeader() asStringHeader()}
 * 			<li class='jm'>{@link RequestHeader#asStringRangeArrayHeader() asStringRangeArrayHeader()}
 * 			<li class='jm'>{@link RequestHeader#asUriHeader() asUriHeader()}
 * 		</ul>
 * 		<li>Methods for retrieving as custom types:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestHeader#as(Class) as(Class)}
 * 			<li class='jm'>{@link RequestHeader#as(ClassMeta) as(ClassMeta)}
 * 			<li class='jm'>{@link RequestHeader#as(Type,Type...) as(Type,Type...)}
 * 			<li class='jm'>{@link RequestHeader#parser(HttpPartParserSession) parser(HttpPartParserSession)}
 * 			<li class='jm'>{@link RequestHeader#schema(HttpPartSchema) schema(HttpPartSchema)}
 * 		</ul>
 * 		<li>Methods for performing assertion checks:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestHeader#assertCsvArray() assertCsvArray()}
 * 			<li class='jm'>{@link RequestHeader#assertDate() assertDate()}
 * 			<li class='jm'>{@link RequestHeader#assertInteger() assertInteger()}
 * 			<li class='jm'>{@link RequestHeader#assertLong() assertLong()}
 * 			<li class='jm'>{@link RequestHeader#assertString() assertString()}
 * 		</ul>
 * 		<li>Other methods:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestHeader#getName() getName()}
 * 			<li class='jm'>{@link RequestHeader#getValue() getValue()}
* 		</ul>
 * </ul>
 */
public class RequestHeader extends RequestHttpPart implements Header {

	private final String value;

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param name The header name.
	 * @param value The header value.
	 */
	public RequestHeader(RestRequest request, String name, String value) {
		super(HEADER, request, name);
		this.value = value;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Retrievers
	//------------------------------------------------------------------------------------------------------------------

	@Override /* RequestHttpPart */
	public String getValue() {
		return value;
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

	//------------------------------------------------------------------------------------------------------------------
	// Assertions
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on this parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getHeader(<js>"foo"</js>)
	 * 		.assertString().contains(<js>"bar"</js>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original object allowing you to chain multiple requests like so:
	 * <p class='bcode w800'>
	 * 	String <jv>foo</jv> = <jv>request</jv>
	 * 		.getHeader(<js>"foo"</js>)
	 * 		.assertString().contains(<js>"bar"</js>)
	 * 		.asString().get();
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentStringAssertion<RequestHeader> assertString() {
		return new FluentStringAssertion<>(orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on an integer parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getHeader(<js>"age"</js>)
	 * 		.assertInteger().isGreaterThan(1);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentIntegerAssertion<RequestHeader> assertInteger() {
		return new FluentIntegerAssertion<>(asIntegerPart().asInteger().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on a long parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getHeader(<js>"length"</js>)
	 * 		.assertLong().isLessThan(100000);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentLongAssertion<RequestHeader> assertLong() {
		return new FluentLongAssertion<>(asLongPart().asLong().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on a date parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getHeader(<js>"time"</js>)
	 * 		.assertDate().isAfterNow();
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentZonedDateTimeAssertion<RequestHeader> assertDate() {
		return new FluentZonedDateTimeAssertion<>(asDatePart().asZonedDateTime().orElse(null), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on comma-separated string parameters.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jv>request</jv>
	 * 		.getHeader(<js>"allow"</js>)
	 * 		.assertCsvArray().contains(<js>"GET"</js>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentListAssertion<String,RequestHeader> assertCsvArray() {
		return new FluentListAssertion<>(asCsvArrayPart().asList().orElse(null), this);
	}

	/**
	 * Parses the value.
	 *
	 * @return An array of {@link HeaderElement} entries, may be empty, but is never <jk>null</jk>.
	 * @throws BasicHttpException In case of a parsing error.
	 */
	@Override /* Header */
	public HeaderElement[] getElements() throws BasicHttpException {
		return new HeaderElement[0];
	}

	@Override /* Object */
	public String toString() {
		return getName() + ": " + getValue();
	}

	// <FluentSetters>

	@Override /* GENERATED */
	public RequestHeader schema(HttpPartSchema value) {
		super.schema(value);
		return this;
	}

	@Override /* GENERATED */
	public RequestHeader parser(HttpPartParserSession value) {
		super.parser(value);
		return this;
	}

	// </FluentSetters>
}
