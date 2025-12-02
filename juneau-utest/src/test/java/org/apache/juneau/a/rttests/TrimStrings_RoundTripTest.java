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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests for the {@link Serializer#SERIALIZER_trimStrings} and {@link Parser#PARSER_trimStrings}.
 */
class TrimStrings_RoundTripTest extends RoundTripTest_Base {

	//====================================================================================================
	// test
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_basic(RoundTrip_Tester t) throws Exception {
		if (t.isValidationOnly())
			return;
		var s = t.getSerializer();
		var p = t.getParser();

		var s2 = s.copy().trimStrings().build();
		var p2 = p.copy().trimStrings().build();

		var in = (Object)" foo bar ";
		var e = (Object)"foo bar";
		var a = (Object)p.parse(s2.serialize(in), String.class);
		assertEquals(json(a), json(e));
		a = p2.parse(s.serialize(in), String.class);
		assertEquals(json(a), json(e));

		in = JsonMap.ofJson("{' foo ': ' bar '}");
		e = JsonMap.ofJson("{foo:'bar'}");
		a = p.parse(s2.serialize(in), JsonMap.class);
		assertEquals(json(a), json(e));
		a = p2.parse(s.serialize(in), JsonMap.class);
		assertEquals(json(a), json(e));

		in = new JsonList("[' foo ', {' foo ': ' bar '}]");
		e = new JsonList("['foo',{foo:'bar'}]");
		a = p.parse(s2.serialize(in), JsonList.class);
		assertEquals(json(a), json(e));
		a = p2.parse(s.serialize(in), JsonList.class);
		assertEquals(json(a), json(e));

		in = new A().init1();
		e = new A().init2();
		a = p.parse(s2.serialize(in), A.class);
		assertEquals(json(a), json(e));
		a = p2.parse(s.serialize(in), A.class);
		assertEquals(json(a), json(e));
	}

	public static class A {
		public String f1;
		public String[] f2;
		public JsonList f3;
		public JsonMap f4;

		public A init1() throws Exception {
			f1 = " f1 ";
			f2 = a(" f2a ", " f2b ");
			f3 = JsonList.ofJson("[' f3a ',' f3b ']");
			f4 = JsonMap.ofJson("{' foo ':' bar '}");
			return this;
		}

		public A init2() throws Exception {
			f1 = "f1";
			f2 = a("f2a", "f2b");
			f3 = JsonList.ofJson("['f3a','f3b']");
			f4 = JsonMap.ofJson("{'foo':'bar'}");
			return this;
		}
	}
}