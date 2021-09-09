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
import org.apache.juneau.serializer.*;

/**
 * Base class for implementations of {@link HttpPartSerializer}
 */
public abstract class BaseHttpPartSerializer extends BeanContextable implements HttpPartSerializer {

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected BaseHttpPartSerializer(Builder builder) {
		super(builder);
	}

	/**
	 * The builder class for this object.
	 */
	public abstract static class Builder extends BeanContextableBuilder {

		/**
		 * Constructor.
		 */
		protected Builder() {
			super();
		}

		/**
		 * Copy constructor.
		 * @param builder The existing builder to copy.
		 */
		protected Builder(Builder builder) {
			super(builder);
		}
	}

	/**
	 * Converts the specified value to a string that can be used as an HTTP header value, query parameter value,
	 * form-data parameter, or URI path variable.
	 *
	 * <p>
	 * Returned values should NOT be URL-encoded.
	 *
	 * @param partType The category of value being serialized.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part serializers use the schema information.
	 * @param value The value being serialized.
	 * @return The serialized value.
	 * @throws SerializeException If a problem occurred while trying to parse the input.
	 * @throws SchemaValidationException If the output fails schema validation.
	 */
	public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SchemaValidationException, SerializeException {
		return createPartSession(null).serialize(partType, schema, value);
	}
}
