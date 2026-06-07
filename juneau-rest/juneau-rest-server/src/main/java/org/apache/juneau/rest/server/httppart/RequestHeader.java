/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.httppart;

import static org.apache.juneau.commons.httppart.HttpPartType.*;

import java.lang.reflect.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.server.*;

/**
 * Represents a single header on an HTTP request.
 *
 * <p>
 * Typically accessed through the {@link RequestHeaderList} class.
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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HttpParts">HTTP Parts</a>
 * </ul>
 */
public class RequestHeader extends RequestHttpPart implements HttpHeader {

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
	 * Returns the value of this header as an {@link HttpBooleanHeader}.
	 *
	 * @return The value of this header as an {@link HttpBooleanHeader}, never <jk>null</jk>.
	 */
	public HttpBooleanHeader asBooleanHeader() {
		return HttpBooleanHeader.of(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an {@link HttpCsvHeader}.
	 *
	 * @return The value of this header as an {@link HttpCsvHeader}, never <jk>null</jk>.
	 */
	public HttpCsvHeader asCsvHeader() {
		return HttpCsvHeader.of(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an {@link HttpDateHeader}.
	 *
	 * @return The value of this header as an {@link HttpDateHeader}, never <jk>null</jk>.
	 */
	public HttpDateHeader asDateHeader() {
		return HttpDateHeader.of(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an {@link HttpEntityTagHeader}.
	 *
	 * @return The value of this header as an {@link HttpEntityTagHeader}, never <jk>null</jk>.
	 */
	public HttpEntityTagHeader asEntityTagHeader() {
		return HttpEntityTagHeader.of(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an {@link HttpEntityTagsHeader}.
	 *
	 * @return The value of this header as an {@link HttpEntityTagsHeader}, never <jk>null</jk>.
	 */
	public HttpEntityTagsHeader asEntityTagsHeader() {
		return HttpEntityTagsHeader.of(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an {@link HttpIntegerHeader}.
	 *
	 * @return The value of this header as an {@link HttpIntegerHeader}, never <jk>null</jk>.
	 */
	public HttpIntegerHeader asIntegerHeader() {
		return HttpIntegerHeader.of(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an {@link HttpLongHeader}.
	 *
	 * @return The value of this header as an {@link HttpLongHeader}, never <jk>null</jk>.
	 */
	public HttpLongHeader asLongHeader() {
		return HttpLongHeader.of(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an {@link HttpStringHeader}.
	 *
	 * @return The value of this header as an {@link HttpStringHeader}, never <jk>null</jk>.
	 */
	public HttpStringHeader asStringHeader() {
		return HttpStringHeader.of(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an {@link HttpStringRangesHeader}.
	 *
	 * @return The value of this header as an {@link HttpStringRangesHeader}, never <jk>null</jk>.
	 */
	public HttpStringRangesHeader asStringRangesHeader() {
		return HttpStringRangesHeader.of(getName(), getValue());
	}

	/**
	 * Returns the value of this header as an {@link HttpUriHeader}.
	 *
	 * @return The value of this header as an {@link HttpUriHeader}, never <jk>null</jk>.
	 */
	public HttpUriHeader asUriHeader() {
		return HttpUriHeader.of(getName(), getValue());
	}

	@Override /* Overridden from RequestHttpPart */
	public RequestHeader def(String def) {
		super.def(def);
		return this;
	}

	@Override /* Overridden from RequestHttpPart */
	public RequestHeader parser(HttpPartParserSession value) {
		super.parser(value);
		return this;
	}

	@Override /* Overridden from RequestHttpPart */
	public RequestHeader schema(HttpPartSchema value) {
		super.schema(value);
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return getName() + ": " + getValue();
	}
}