/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.httppart.HttpPartDataType.*;
import static org.apache.juneau.httppart.HttpPartSchema.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.api.*;

class SerializedPart_Test extends TestBase {

	private static final OpenApiSerializerSession OAPI_SESSION = OpenApiSerializer.DEFAULT.getSession();
	private static final OpenApiSerializer OAPI_SERIALIZER = OpenApiSerializer.DEFAULT;

	@Test void a01_basic() {
		var x1 = new SerializedPart("Foo",l("bar","baz"),HEADER,OAPI_SESSION,T_ARRAY_PIPES,true);
		assertString("Foo=bar|baz", x1);
	}

	@Test void a02_type() {
		var x1 = serializedPart("Foo",2).type(HEADER).serializer(OAPI_SERIALIZER).schema(schema(INTEGER).maximum(1).build());
		assertThrowsWithMessage(RuntimeException.class, "Validation error on request HEADER part 'Foo'='2'", x1::toString);
	}

	@Test void a03_serializer() {
		var x1 = serializedPart("Foo",l("bar","baz")).serializer((HttpPartSerializer)null);
		assertEquals("[bar, baz]", x1.getValue());
		var x2 = serializedPart("Foo",l("bar","baz")).serializer((HttpPartSerializer)null).serializer(OAPI_SERIALIZER);
		assertEquals("bar,baz", x2.getValue());
		var x3 = serializedPart("Foo",l("bar","baz")).serializer(OAPI_SERIALIZER).serializer((HttpPartSerializerSession)null);
		assertEquals("[bar, baz]", x3.getValue());
		var x4 = serializedPart("Foo",l("bar","baz")).serializer(OAPI_SERIALIZER).copyWith(null,null);
		assertEquals("bar,baz", x4.getValue());
		var x5 = serializedPart("Foo",l("bar","baz")).copyWith(OAPI_SERIALIZER.getPartSession(),null);
		assertEquals("bar,baz", x5.getValue());
	}

	@Test void a04_skipIfEmpty() {
		var x1 = serializedPart("Foo",null).skipIfEmpty();
		assertNull(x1.getValue());
		var x2 = serializedPart("Foo","").skipIfEmpty();
		assertNull(x2.getValue());
		var x3 = serializedPart("Foo","").schema(schema(STRING)._default("bar").build()).serializer(OAPI_SERIALIZER).skipIfEmpty();
		assertThrowsWithMessage(Exception.class, "Empty value not allowed.", x3::getValue);
	}

	@Test void a05_getValue_defaults() {
		var x1 = serializedPart("Foo",null).schema(schema(INTEGER)._default("1").build()).serializer(OAPI_SESSION);
		assertEquals("1", x1.getValue());

		var x2 = serializedPart("Foo",null).schema(schema(STRING).required().allowEmptyValue().build()).serializer(OAPI_SESSION);
		assertNull(x2.getValue());

		var x3 = serializedPart("Foo",null).schema(schema(STRING).required(false).build()).serializer(OAPI_SESSION);
		assertNull(x3.getValue());

		var x4 = serializedPart("Foo",null).schema(schema(STRING).required().build()).serializer(OAPI_SESSION);
		assertThrowsWithMessage(Exception.class, "Required value not provided.", x4::getValue);

		var x5 = serializedPart("Foo",null).schema(schema(STRING).required().build()).serializer(new BadPartSerializerSession());
		assertThrowsWithMessage(Exception.class, "Bad", x5::getValue);
	}

	private static class BadPartSerializerSession implements HttpPartSerializerSession {
		@Override
		public String serialize(HttpPartType type, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
			throw new SerializeException("Bad");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private static HttpPartSchema.Builder schema(HttpPartDataType dataType) {
		return HttpPartSchema.create().type(dataType);
	}
}