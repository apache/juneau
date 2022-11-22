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
import java.util.regex.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.BasicHttpException;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.*;

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
 * 			<li class='jm'>{@link RequestHeader#asCsvHeader() asCsvArrayHeader()}
 * 			<li class='jm'>{@link RequestHeader#asDate() asDate()}
 * 			<li class='jm'>{@link RequestHeader#asDateHeader() asDateHeader()}
 * 			<li class='jm'>{@link RequestHeader#asEntityTagsHeader() asEntityTagArrayHeader()}
 * 			<li class='jm'>{@link RequestHeader#asEntityTagHeader() asEntityTagHeader()}
 * 			<li class='jm'>{@link RequestHeader#asInteger() asInteger()}
 * 			<li class='jm'>{@link RequestHeader#asIntegerHeader() asIntegerHeader()}
 * 			<li class='jm'>{@link RequestHeader#asLong() asLong()}
 * 			<li class='jm'>{@link RequestHeader#asLongHeader() asLongHeader()}
 * 			<li class='jm'>{@link RequestHeader#asMatcher(Pattern) asMatcher(Pattern)}
 * 			<li class='jm'>{@link RequestHeader#asMatcher(String) asMatcher(String)}
 * 			<li class='jm'>{@link RequestHeader#asMatcher(String,int) asMatcher(String,int)}
 * 			<li class='jm'>{@link RequestHeader#asStringHeader() asStringHeader()}
 * 			<li class='jm'>{@link RequestHeader#asStringRangesHeader() asStringRangeArrayHeader()}
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
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HttpParts">HTTP Parts</a>
 * </ul>
 */
public class RequestHeader extends RequestHttpPart implements Header {

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param name The header name.
	 * @param value The header value.
	 */
	public RequestHeader(RestRequest request, String name, String value) {
		super(HEADER, request, name, value);
	}

	/**
	 * Returns the value of this header as a {@link BasicCsvHeader}.
	 *
	 * @return The value of this header as a  {@link BasicCsvHeader}, never <jk>null</jk>.
	 */
	public BasicCsvHeader asCsvHeader() {
		return new BasicCsvHeader(getName(), getValue());
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
	 * Returns the value of this header as a {@link BasicEntityTagsHeader}.
	 *
	 * @return The value of this header as a {@link BasicEntityTagsHeader}, never <jk>null</jk>.
	 */
	public BasicEntityTagsHeader asEntityTagsHeader() {
		return new BasicEntityTagsHeader(getName(), getValue());
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
	 * Returns the value of this header as a {@link BasicStringRangesHeader}.
	 *
	 * @return The value of this header as a {@link BasicStringRangesHeader}, never <jk>null</jk>.
	 */
	public BasicStringRangesHeader asStringRangesHeader() {
		return new BasicStringRangesHeader(getName(), getValue());
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
