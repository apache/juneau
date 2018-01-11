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
package org.apache.juneau.httppart;

import org.apache.juneau.*;
import org.apache.juneau.remoteable.*;

/**
 * Interface used to convert POJOs to simple strings in HTTP headers, query parameters, form-data parameters, and URI
 * path variables.
 *
 * <p>
 * The following default implementations are provided:
 * <ul class='doctree'>
 * 	<li class='jc'>{@link org.apache.juneau.httppart.UonPartSerializer} - Parts encoded in UON notation.
 * 	<li class='jc'>{@link org.apache.juneau.httppart.SimpleUonPartSerializer} - Parts encoded in UON notation, but
 * 		strings are treated as plain-text and arrays/collections are serialized as comma-delimited lists.
 * 	<li class='jc'>{@link org.apache.juneau.httppart.SimplePartSerializer} - Parts encoded in plain text.
 * </ul>
 * 
 * <p>
 * This class is used in the following locations:
 * <ul>
 * 	<li class='ja'>{@link FormData#serializer()}
 * 	<li class='ja'>{@link FormDataIfNE#serializer()}
 * 	<li class='ja'>{@link Query#serializer()}
 * 	<li class='ja'>{@link QueryIfNE#serializer()}
 * 	<li class='ja'>{@link Header#serializer()}
 * 	<li class='ja'>{@link HeaderIfNE#serializer()}
 * 	<li class='ja'>{@link Path#serializer()}
 * 	<li class='ja'>{@link RequestBean#serializer()}
 * 	<li class='jc'><code>RestClientBuilder.partSerializer(Class)</code>
 * </ul>
 *
 * <p>
 * Implementations must include either a public no-args constructor or a public constructor that takes in a single
 * {@link PropertyStore} object.
 */
public interface HttpPartSerializer {

	/**
	 * Represent "no" part part serializer.
	 * 
	 * <li>
	 * Used to represent the absence of a part serializer in annotations.
	 */
	public static interface Null extends HttpPartSerializer {}

	/**
	 * Converts the specified value to a string that can be used as an HTTP header value, query parameter value,
	 * form-data parameter, or URI path variable.
	 *
	 * <p>
	 * Returned values should NOT be URL-encoded.
	 *
	 * @param type The category of value being serialized.
	 * @param value The value being serialized.
	 * @return The serialized value.
	 */
	public String serialize(HttpPartType type, Object value);
}
