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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.dto.jsonschema.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@FixMethodOrder(NAME_ASCENDING)
public class RoundTripDTOsTest extends RoundTripTest {

	public RoundTripDTOsTest(String label, SerializerBuilder s, ParserBuilder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// org.apache.juneau.test.dto.jsonschema
	//====================================================================================================
	@Test
	public void testJsonSchema1() throws Exception {
		JsonSchema s = JsonSchemaTest.getTest1();
		JsonSchema s2 = roundTrip(s, JsonSchema.class);
		assertObject(s).sameAs(s2);
	}

	@Test
	public void testJsonSchema2() throws Exception {
		JsonSchema s = JsonSchemaTest.getTest2();
		JsonSchema s2 = roundTrip(s, JsonSchema.class);
		assertObject(s).sameAs(s2);
	}
}
