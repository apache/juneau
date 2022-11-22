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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;

/**
 * Represents a single request attribute on an HTTP request.
 *
 * <p>
 * Typically accessed through the {@link RequestAttributes} class.
 *
 * <p>
 * 	Some important methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RequestAttribute}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for retrieving simple string values:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestAttribute#asString() asString()}
 * 			<li class='jm'>{@link RequestAttribute#get() get()}
 * 			<li class='jm'>{@link RequestAttribute#isPresent() isPresent()}
 * 			<li class='jm'>{@link RequestAttribute#orElse(Object) orElse(Object)}
 * 		</ul>
 * 		<li>Methods for retrieving as custom types:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestAttribute#as(Class) as(Class)}
 * 		</ul>
 * 		<li>Methods for performing assertion checks:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestAttribute#assertName() assertName()}
 * 			<li class='jm'>{@link RequestAttribute#assertValue() assertValue()}
 * 		</ul>
 * 		<li>Other methods:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestAttribute#getName() getName()}
 * 			<li class='jm'>{@link RequestAttribute#getValue() getValue()}
* 		</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HttpParts">HTTP Parts</a>
 * </ul>
 */
public class RequestAttribute extends BasicNamedAttribute {

	private final RestRequest req;

	/**
	 * Constructor.
	 *
	 * @param request The request object.
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public RequestAttribute(RestRequest request, String name, Object value) {
		super(name, value);
		this.req = request;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Retrievers
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the value of this part as a string.
	 *
	 * @return The value of this part as a string, or {@link Optional#empty()} if the part was not present.
	 */
	public Optional<String> asString() {
		return optional(stringify(getValue()));
	}

	/**
	 * Converts this part to the specified POJO.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted type, or {@link Optional#empty()} if the part is not present.
	 * @throws BasicHttpException If value could not be parsed.
	 */
	public <T> Optional<T> as(Class<T> type) {
		return optional(req.getBeanSession().convertToType(getValue(), type));
	}
}
