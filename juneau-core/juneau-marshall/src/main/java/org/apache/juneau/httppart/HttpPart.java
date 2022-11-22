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

import static org.apache.juneau.common.internal.ThrowableUtils.*;

import org.apache.http.*;

/**
 * Represents an instance of an HTTP part.
 *
 * <p>
 * Can be used to represent both request and response parts.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
public class HttpPart implements NameValuePair {
	private final String name;
	private final Object opart;
	private final String spart;
	private final HttpPartType partType;
	private final HttpPartSchema schema;
	private final HttpPartSerializerSession serializer;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Used when the part is in POJO form and needs to be converted to a String.
	 *
	 * @param name The HTTP part name (e.g. the header name).
	 * @param partType The HTTP part type.
	 * @param schema Schema information about the part.
	 * @param serializer The part serializer to use to serialize the part.
	 * @param part The part POJO being serialized.
	 */
	public HttpPart(String name, HttpPartType partType, HttpPartSchema schema, HttpPartSerializerSession serializer, Object part) {
		this.name = name;
		this.partType = partType;
		this.schema = schema;
		this.serializer = serializer;
		this.opart = part;
		this.spart = null;
	}

	@Override /* NameValuePair */
	public String getName() {
		return name;
	}

	@Override /* NameValuePair */
	public String getValue() {
		if (spart != null)
			return spart;
		try {
			return serializer.serialize(partType, schema, opart);
		} catch (Exception e) {
			throw asRuntimeException(e);
		}
	}
}
