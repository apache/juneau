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

import org.apache.juneau.serializer.*;

/**
 * Session object that lives for the duration of a single use of {@link HttpPartSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
public interface HttpPartSerializerSession {

	/**
	 * Converts the specified value to a string that can be used as an HTTP header value, query parameter value,
	 * form-data parameter, or URI path variable.
	 *
	 * <p>
	 * Returned values should NOT be URL-encoded.
	 *
	 * @param type The category of value being serialized.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part serializers use the schema information.
	 * @param value The value being serialized.
	 * @return The serialized value.
	 * @throws SerializeException If a problem occurred while trying to parse the input.
	 * @throws SchemaValidationException If the output fails schema validation.
	 */
	public String serialize(HttpPartType type, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException;
}
