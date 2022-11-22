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
 * Represents a single path parameter on an HTTP request.
 *
 * <p>
 * Typically accessed through the {@link RequestPathParams} class.
 *
 * <p>
 * 	Some important methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RequestPathParam}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for retrieving simple string values:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestPathParam#asString() asString()}
 * 			<li class='jm'>{@link RequestPathParam#get() get()}
 * 			<li class='jm'>{@link RequestPathParam#isPresent() isPresent()}
 * 			<li class='jm'>{@link RequestPathParam#orElse(String) orElse(String)}
 * 		</ul>
 * 		<li>Methods for retrieving as other common types:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestPathParam#asBoolean() asBoolean()}
 * 			<li class='jm'>{@link RequestPathParam#asBooleanPart() asBooleanPart()}
 * 			<li class='jm'>{@link RequestPathParam#asCsvArray() asCsvArray()}
 * 			<li class='jm'>{@link RequestPathParam#asCsvArrayPart() asCsvArrayPart)}
 * 			<li class='jm'>{@link RequestPathParam#asDate() asDate()}
 * 			<li class='jm'>{@link RequestPathParam#asDatePart() asDatePart()}
 * 			<li class='jm'>{@link RequestPathParam#asInteger() asInteger()}
 * 			<li class='jm'>{@link RequestPathParam#asIntegerPart() asIntegerPart()}
 * 			<li class='jm'>{@link RequestPathParam#asLong() asLong()}
 * 			<li class='jm'>{@link RequestPathParam#asLongPart() asLongPart()}
 * 			<li class='jm'>{@link RequestPathParam#asMatcher(Pattern) asMatcher(Pattern)}
 * 			<li class='jm'>{@link RequestPathParam#asMatcher(String) asMatcher(String)}
 * 			<li class='jm'>{@link RequestPathParam#asMatcher(String,int) asMatcher(String,int)}
 * 			<li class='jm'>{@link RequestPathParam#asStringPart() asStringPart()}
 * 			<li class='jm'>{@link RequestPathParam#asUriPart() asUriPart()}
 * 		</ul>
 * 		<li>Methods for retrieving as custom types:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestPathParam#as(Class) as(Class)}
 * 			<li class='jm'>{@link RequestPathParam#as(ClassMeta) as(ClassMeta)}
 * 			<li class='jm'>{@link RequestPathParam#as(Type,Type...) as(Type,Type...)}
 * 			<li class='jm'>{@link RequestPathParam#parser(HttpPartParserSession) parser(HttpPartParserSession)}
 * 			<li class='jm'>{@link RequestPathParam#schema(HttpPartSchema) schema(HttpPartSchema)}
 * 		</ul>
 * 		<li>Methods for performing assertion checks:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestPathParam#assertCsvArray() assertCsvArray()}
 * 			<li class='jm'>{@link RequestPathParam#assertDate() assertDate()}
 * 			<li class='jm'>{@link RequestPathParam#assertInteger() assertInteger()}
 * 			<li class='jm'>{@link RequestPathParam#assertLong() assertLong()}
 * 			<li class='jm'>{@link RequestPathParam#assertString() assertString()}
 * 		</ul>
 * 		<li>Other methods:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestPathParam#getName() getName()}
 * 			<li class='jm'>{@link RequestPathParam#getValue() getValue()}
 * 		</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HttpParts">HTTP Parts</a>
 * </ul>
 */
public class RequestPathParam extends RequestHttpPart implements NameValuePair {

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public RequestPathParam(RestRequest request, String name, String value) {
		super(PATH, request, name, value);
		this.value = value;
	}

	// <FluentSetters>

	@Override /* GENERATED */
	public RequestPathParam schema(HttpPartSchema value) {
		super.schema(value);
		return this;
	}

	@Override /* GENERATED */
	public RequestPathParam parser(HttpPartParserSession value) {
		super.parser(value);
		return this;
	}
	// </FluentSetters>
}
