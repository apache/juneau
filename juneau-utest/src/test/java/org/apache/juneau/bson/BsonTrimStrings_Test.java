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
package org.apache.juneau.bson;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests BSON parser trimStrings behavior.
 */
class BsonTrimStrings_Test {

	@Test
	void testTrimStrings() throws Exception {
		var p = BsonParser.create().build();
		var p2 = BsonParser.create().trimStrings().build();
		var s = BsonSerializer.create().build();
		var in = " foo bar ";
		// Parser without trimStrings should preserve spaces
		var a = p.parse(s.serialize(in), String.class);
		assertEquals(" foo bar ", a);
		// Parser with trimStrings should trim
		var a2 = p2.parse(s.serialize(in), String.class);
		assertEquals("foo bar", a2);
	}

	@Test
	void testTrimStringsWithRoundTripTesterConfig() throws Exception {
		// Match RoundTrip_Tester BSON config exactly
		var s = BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().build();
		var p = BsonParser.create().build();
		var p2 = p.copy().trimStrings().build();
		var in = " foo bar ";
		var a = p2.parse(s.serialize(in), String.class);
		assertEquals("foo bar", a, "Parser with trimStrings should trim when using RoundTrip test config");
	}

}
