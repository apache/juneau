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

import static org.apache.juneau.AssertionHelpers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests for the {@link Serializer#SERIALIZER_trimStrings} and {@link Parser#PARSER_trimStrings}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class RoundTripTrimStringsTest extends RoundTripTest {

	public RoundTripTrimStringsTest(String label, Serializer.Builder s, Parser.Builder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// test
	//====================================================================================================
	@Test
	public void test() throws Exception {
		if (isValidationOnly())
			return;
		Serializer s = getSerializer();
		Parser p = getParser();
		Object in, a, e;

		Serializer s2 = s.copy().trimStrings().build();
		Parser p2 = p.copy().trimStrings().build();

		in = " foo bar ";
		e = "foo bar";
		a = p.parse(s2.serialize(in), String.class);
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
			f2 = new String[]{" f2a ", " f2b "};
			f3 = JsonList.ofJson("[' f3a ',' f3b ']");
			f4 = JsonMap.ofJson("{' foo ':' bar '}");
			return this;
		}

		public A init2() throws Exception {
			f1 = "f1";
			f2 = new String[]{"f2a", "f2b"};
			f3 = JsonList.ofJson("['f3a','f3b']");
			f4 = JsonMap.ofJson("{'foo':'bar'}");
			return this;
		}
	}
}