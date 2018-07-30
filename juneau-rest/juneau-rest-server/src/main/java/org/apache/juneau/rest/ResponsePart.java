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
package org.apache.juneau.rest;

import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;

/**
 * Represents part of an HTTP response such as an HTTP response header.
 */
public class ResponsePart {
	private final String name;
	private final Object part;
	private final HttpPartType partType;
	private final HttpPartSchema schema;
	private final HttpPartSerializer serializer;
	private final SerializerSessionArgs args;

	/**
	 * Constructor.
	 *
	 * @param name The HTTP part name (e.g. the response header name).
	 * @param partType The HTTP part type.
	 * @param schema Schema information about the part.
	 * @param serializer The part serializer to use to serialize the part.
	 * @param part The part POJO being serialized.
	 * @param args Session arguments to pass to the serializer.
	 */
	public ResponsePart(String name, HttpPartType partType, HttpPartSchema schema, HttpPartSerializer serializer, Object part, SerializerSessionArgs args) {
		this.name = name;
		this.partType = partType;
		this.schema = schema;
		this.serializer = serializer;
		this.part = part;
		this.args = args;
	}

	/**
	 * Returns the name of the part.
	 *
	 * @return The name of the part.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the value of the part converted to a string.
	 *
	 * @return The value of the part converted to a string.
	 * @throws SchemaValidationException
	 * @throws SerializeException
	 */
	public String getValue() throws SchemaValidationException, SerializeException {
		return serializer.createSession(args).serialize(partType, schema, part);
	}
}
