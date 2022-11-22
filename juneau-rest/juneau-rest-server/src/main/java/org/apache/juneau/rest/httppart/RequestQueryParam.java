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
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.*;

/**
 * Represents a single query parameter on an HTTP request.
 *
 * <p>
 * Typically accessed through the {@link RequestQueryParams} class.
 *
 * <p>
 * 	Some important methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RequestQueryParam}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for retrieving simple string values:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestQueryParam#asString() asString()}
 * 			<li class='jm'>{@link RequestQueryParam#get() get()}
 * 			<li class='jm'>{@link RequestQueryParam#isPresent() isPresent()}
 * 			<li class='jm'>{@link RequestQueryParam#orElse(String) orElse(String)}
 * 		</ul>
 * 		<li>Methods for retrieving as other common types:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestQueryParam#asBoolean() asBoolean()}
 * 			<li class='jm'>{@link RequestQueryParam#asBooleanPart() asBooleanPart()}
 * 			<li class='jm'>{@link RequestQueryParam#asCsvArray() asCsvArray()}
 * 			<li class='jm'>{@link RequestQueryParam#asCsvArrayPart() asCsvArrayPart()}
 * 			<li class='jm'>{@link RequestQueryParam#asDate() asDate()}
 * 			<li class='jm'>{@link RequestQueryParam#asDatePart() asDatePart()}
 * 			<li class='jm'>{@link RequestQueryParam#asInteger() asInteger()}
 * 			<li class='jm'>{@link RequestQueryParam#asIntegerPart() asIntegerPart()}
 * 			<li class='jm'>{@link RequestQueryParam#asLong() asLong()}
 * 			<li class='jm'>{@link RequestQueryParam#asLongPart() asLongPart()}
 * 			<li class='jm'>{@link RequestQueryParam#asMatcher(Pattern) asMatcher(Pattern)}
 * 			<li class='jm'>{@link RequestQueryParam#asMatcher(String) asMatcher(String)}
 * 			<li class='jm'>{@link RequestQueryParam#asMatcher(String,int) asMatcher(String,int)}
 * 			<li class='jm'>{@link RequestQueryParam#asStringPart() asStringPart()}
 * 			<li class='jm'>{@link RequestQueryParam#asUriPart() asUriPart()}
 * 		</ul>
 * 		<li>Methods for retrieving as custom types:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestQueryParam#as(Class) as(Class)}
 * 			<li class='jm'>{@link RequestQueryParam#as(ClassMeta) as(ClassMeta)}
 * 			<li class='jm'>{@link RequestQueryParam#as(Type,Type...) as(Type,Type...)}
 * 			<li class='jm'>{@link RequestQueryParam#parser(HttpPartParserSession) parser(HttpPartParserSession)}
 * 			<li class='jm'>{@link RequestQueryParam#schema(HttpPartSchema) schema(HttpPartSchema)}
 * 		</ul>
 * 		<li>Methods for performing assertion checks:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestQueryParam#assertCsvArray() assertCsvArray()}
 * 			<li class='jm'>{@link RequestQueryParam#assertDate() assertDate()}
 * 			<li class='jm'>{@link RequestQueryParam#assertInteger() assertInteger()}
 * 			<li class='jm'>{@link RequestQueryParam#assertLong() assertLong()}
 * 			<li class='jm'>{@link RequestQueryParam#assertString() assertString()}
 * 		</ul>
 * 		<li>Other methods:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestQueryParam#getName() getName()}
 * 			<li class='jm'>{@link RequestQueryParam#getValue() getValue()}
* 		</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HttpParts">HTTP Parts</a>
 * </ul>
 */
public class RequestQueryParam extends RequestHttpPart implements NameValuePair {

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public RequestQueryParam(RestRequest request, String name, String value) {
		super(QUERY, request, name, value);
	}

	// <FluentSetters>

	@Override /* GENERATED */
	public RequestQueryParam schema(HttpPartSchema value) {
		super.schema(value);
		return this;
	}

	@Override /* GENERATED */
	public RequestQueryParam parser(HttpPartParserSession value) {
		super.parser(value);
		return this;
	}
	// </FluentSetters>
}
