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
package org.apache.juneau.json;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO metadata to HTTP responses as JSON.
 * 
 * <h5 class='topic'>Media types</h5>
 * 
 * Handles <code>Accept</code> types:  <code><b>application/json+schema, text/json+schema</b></code>
 * <p>
 * Produces <code>Content-Type</code> types:  <code><b>application/json</b></code>
 * 
 * <h5 class='topic'>Description</h5>
 * 
 * Produces the JSON-schema for the JSON produced by the {@link JsonSerializer} class with the same properties.
 */
public final class JsonSchemaSerializer extends JsonSerializer {

	/**
	 * Constructor.
	 * 
	 * @param ps Initialize with the specified config property store.
	 */
	public JsonSchemaSerializer(PropertyStore ps) {
		super(
			ps.builder()
				.set(SERIALIZER_detectRecursions, true)
				.set(SERIALIZER_ignoreRecursions, true)
				.build(),
			"application/json",
			"application/json+schema", "text/json+schema"
		);
	}

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new JsonSchemaSerializerSession(this, args);
	}
}