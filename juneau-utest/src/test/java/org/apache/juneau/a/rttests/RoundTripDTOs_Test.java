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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.bean.jsonschema.*;
import org.apache.juneau.dto.jsonschema.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class RoundTripDTOs_Test extends BasicRoundTripTest {

	@ParameterizedTest
	@MethodSource("testers")
	void a01_jsonSchema1(RoundTripTester t) throws Exception {
		var x1 = JsonSchemaTest.getTest1();
		var x2 = t.roundTrip(x1, JsonSchema.class);
		assertEquals(json(x2), json(x1));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a02_jsonSchema2(RoundTripTester t) throws Exception {
		var x1 = JsonSchemaTest.getTest2();
		var x2 = t.roundTrip(x1, JsonSchema.class);
		assertEquals(json(x2), json(x1));
	}
}