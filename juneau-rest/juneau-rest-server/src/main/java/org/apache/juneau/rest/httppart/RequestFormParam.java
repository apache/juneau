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

import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.*;

/**
 * Represents a single form-data parameter on an HTTP request.
 *
 * <p>
 * Typically accessed through the {@link RequestFormParams} class.
 *
 * <p>
 * 	Some important methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RequestFormParam}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for retrieving simple string values:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestFormParam#asString() asString()}
 * 			<li class='jm'>{@link RequestFormParam#get() get()}
 * 			<li class='jm'>{@link RequestFormParam#isPresent() isPresent()}
 * 			<li class='jm'>{@link RequestFormParam#orElse(String) orElse(String)}
 * 		</ul>
 * 		<li>Methods for retrieving as other common types:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestFormParam#asBoolean() asBoolean()}
 * 			<li class='jm'>{@link RequestFormParam#asBooleanPart() asBooleanPart()}
 * 			<li class='jm'>{@link RequestFormParam#asCsvArray() asCsvArray()}
 * 			<li class='jm'>{@link RequestFormParam#asCsvArrayPart() asCsvArrayPart()}
 * 			<li class='jm'>{@link RequestFormParam#asDate() asDate()}
 * 			<li class='jm'>{@link RequestFormParam#asDatePart() asDatePart()}
 * 			<li class='jm'>{@link RequestFormParam#asInteger() asInteger()}
 * 			<li class='jm'>{@link RequestFormParam#asIntegerPart() asIntegerPart()}
 * 			<li class='jm'>{@link RequestFormParam#asLong() asLong()}
 * 			<li class='jm'>{@link RequestFormParam#asLongPart() asLongPart()}
 * 			<li class='jm'>{@link RequestFormParam#asMatcher(Pattern) asMatcher(Pattern)}
 * 			<li class='jm'>{@link RequestFormParam#asMatcher(String) asMatcher(String)}
 * 			<li class='jm'>{@link RequestFormParam#asMatcher(String,int) asMatcher(String,int)}
 * 			<li class='jm'>{@link RequestFormParam#asStringPart() asStringPart()}
 * 			<li class='jm'>{@link RequestFormParam#asUriPart() asUriPart()}
 * 		</ul>
 * 		<li>Methods for retrieving as custom types:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestFormParam#as(Class) as(Class)}
 * 			<li class='jm'>{@link RequestFormParam#as(ClassMeta) as(ClassMeta)}
 * 			<li class='jm'>{@link RequestFormParam#as(Type,Type...) as(Type,Type...)}
 * 			<li class='jm'>{@link RequestFormParam#parser(HttpPartParserSession) parser(HttpPartParserSession)}
 * 			<li class='jm'>{@link RequestFormParam#schema(HttpPartSchema) schema(HttpPartSchema)}
 * 		</ul>
 * 		<li>Methods for performing assertion checks:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestFormParam#assertCsvArray() assertCsvArray()}
 * 			<li class='jm'>{@link RequestFormParam#assertDate() assertDate()}
 * 			<li class='jm'>{@link RequestFormParam#assertInteger() assertInteger()}
 * 			<li class='jm'>{@link RequestFormParam#assertLong() assertLong()}
 * 			<li class='jm'>{@link RequestFormParam#assertString() assertString()}
 * 		</ul>
 * 		<li>Other methods:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestFormParam#getName() getName()}
 * 			<li class='jm'>{@link RequestFormParam#getValue() getValue()}
* 		</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HttpParts">HTTP Parts</a>
 * </ul>
 */
public class RequestFormParam extends RequestHttpPart implements NameValuePair {

	private final javax.servlet.http.Part part;

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param part The HTTP part.
	 */
	public RequestFormParam(RestRequest request, javax.servlet.http.Part part) {
		super(FORMDATA, request, part.getName(), null);
		this.part = part;
	}

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public RequestFormParam(RestRequest request, String name, String value) {
		super(FORMDATA, request, name, value);
		this.part = null;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Retrievers
	//------------------------------------------------------------------------------------------------------------------

	@Override /* RequestHttpPart */
	public String getValue() {
		if (value == null && part != null)
			try {
				value = IOUtils.read(part.getInputStream());
			} catch (IOException e) {
				throw asRuntimeException(e);
			}
		return value;
	}

	/**
	 * Returns this part value as an input stream.
	 *
	 * @return This part value as an input stream.
	 * @throws IOException If an error occurs in retrieving the content.
	 */
	public InputStream getStream() throws IOException {
		if (value != null)
			return new ByteArrayInputStream(value.getBytes(IOUtils.UTF8));
		return part.getInputStream();
	}

	/**
	 * Returns the content type of this part.
	 *
	 * @return The content type of this part, or <jk>null</jk> if not known.
	 */
	public String getContentType() {
		return (part == null ? null : part.getContentType());
	}

	/**
	 * Returns the value of the specified mime header as a String.
	 *
	 * <p>
	 * If the Part did not include a header of the specified name, this method returns null.
	 * If there are multiple headers with the same name, this method returns the first header in the part.
	 * The header name is case insensitive.
	 * You can use this method with any request header.
	 *
	 * @param name The header name.
	 * @return The value of the specified mime header as a String.
	 */
	public String getHeader(String name) {
		return part.getHeader(name);
	}

	/**
	 * Returns the header names of this param.
	 *
	 * @return The header names of this param.
	 */
	public Collection<String> getHeaderNames() {
		return part.getHeaderNames();
	}

	/**
	 * Returns the values of the param header with the given name.
	 *
	 * @param name The param name.
	 * @return The values of the param header with the given name.
	 */
	public Collection<String> getHeaders(String name) {
		return part.getHeaders(name);
	}

	/**
	 * Returns the size of this file.
	 *
	 * @return A long specifying the size of this part, in bytes.
	 */
	public long getSize() {
		return part.getSize();
	}

	/**
	 * Returns the file name specified by the client.
	 *
	 * @return The file name specified by the client.
	 */
	public String getSubmittedFileName() {
		return part.getSubmittedFileName();
	}

	// <FluentSetters>

	@Override /* GENERATED */
	public RequestFormParam schema(HttpPartSchema value) {
		super.schema(value);
		return this;
	}

	@Override /* GENERATED */
	public RequestFormParam parser(HttpPartParserSession value) {
		super.parser(value);
		return this;
	}

	// </FluentSetters>
}
