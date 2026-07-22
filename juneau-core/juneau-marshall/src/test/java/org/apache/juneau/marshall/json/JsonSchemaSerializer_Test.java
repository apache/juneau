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
package org.apache.juneau.marshall.json;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.jupiter.api.*;

/**
 * Most of the heavy testing for JsonSchemaSerializer is done in JsonSchemaGenerator_Test.
 */
class JsonSchemaSerializer_Test extends TestBase {

	//====================================================================================================
	// Simple objects
	//====================================================================================================

	@Test void simpleObjects() throws Exception {
		var s = JsonSchemaSerializer.DEFAULT;

		assertEquals("{\"type\":\"integer\",\"format\":\"int16\"}", s.write((short)1));
		assertEquals("{\"type\":\"integer\",\"format\":\"int32\"}", s.write(1));
		assertEquals("{\"type\":\"integer\",\"format\":\"int64\"}", s.write(1L));
		assertEquals("{\"type\":\"number\",\"format\":\"float\"}", s.write(1f));
		assertEquals("{\"type\":\"number\",\"format\":\"double\"}", s.write(1d));
		assertEquals("{\"type\":\"boolean\"}", s.write(true));
		assertEquals("{\"type\":\"string\"}", s.write("foo"));
		assertEquals("{\"type\":\"string\"}", s.write(new StringBuilder("foo")));
		assertEquals("{\"type\":\"string\"}", s.write('c'));
		assertEquals("{\"type\":\"string\",\"enum\":[\"one\",\"two\",\"three\"]}", s.write(TestEnumToString.ONE));
		assertEquals("{\"type\":\"object\",\"properties\":{\"f1\":{\"type\":\"string\"}}}", s.write(new SimpleBean()));
	}

	public static class SimpleBean {
		public String f1;
	}
}