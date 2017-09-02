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
package org.apache.juneau.serializer;

import org.apache.juneau.*;
import org.apache.juneau.remoteable.*;
import org.apache.juneau.urlencoding.*;

/**
 * Interface used to convert POJOs to simple strings in HTTP headers, query parameters, form-data parameters, and URI
 * path variables.
 *
 * <p>
 * By default, the {@link UrlEncodingSerializer} class implements this interface so that it can be used to serialize
 * these HTTP parts.
 * However, the interface is provided to allow custom serialization of these objects by providing your own implementation
 * class and using it in any of the following locations:
 * <ul>
 * 	<li>{@link FormData#serializer()}
 * 	<li>{@link FormDataIfNE#serializer()}
 * 	<li>{@link Query#serializer()}
 * 	<li>{@link QueryIfNE#serializer()}
 * 	<li>{@link Header#serializer()}
 * 	<li>{@link HeaderIfNE#serializer()}
 * 	<li>{@link Path#serializer()}
 * 	<li>{@link RequestBean#serializer()}
 * 	<li><code>RestClientBuilder.partSerializer(Class)</code>
 * </ul>
 *
 * <p>
 * Implementations must include a no-arg constructor.
 */
public interface PartSerializer {

	/**
	 * Converts the specified value to a string that can be used as an HTTP header value, query parameter value,
	 * form-data parameter, or URI path variable.
	 *
	 * <p>
	 * Returned values should NOT be URL-encoded.  This will happen automatically.
	 *
	 * @param type The category of value being serialized.
	 * @param value The value being serialized.
	 * @return The serialized value.
	 */
	public String serialize(PartType type, Object value);
}
