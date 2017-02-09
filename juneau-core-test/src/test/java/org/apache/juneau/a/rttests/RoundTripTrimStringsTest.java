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

import static org.apache.juneau.TestUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests for the {@link SerializerContext#SERIALIZER_trimStrings} and {@link ParserContext#PARSER_trimStrings}.
 */
@SuppressWarnings("javadoc")
public class RoundTripTrimStringsTest extends RoundTripTest {

	public RoundTripTrimStringsTest(String label, Serializer s, Parser p, int flags) throws Exception {
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

		Serializer s2 = s.clone().setTrimStrings(true);
		Parser p2 = p.clone().setTrimStrings(true);

		in = " foo bar ";
		e = "foo bar";
		a = p.parse(s2.serialize(in), String.class);
		assertEqualObjects(e, a);
		a = p2.parse(s.serialize(in), String.class);
		assertEqualObjects(e, a);

		in = new ObjectMap("{' foo ': ' bar '}");
		e = new ObjectMap("{foo:'bar'}");
		a = p.parse(s2.serialize(in), ObjectMap.class);
		assertEqualObjects(e, a);
		a = p2.parse(s.serialize(in), ObjectMap.class);
		assertEqualObjects(e, a);

		in = new ObjectList("[' foo ', {' foo ': ' bar '}]");
		e = new ObjectList("['foo',{foo:'bar'}]");
		a = p.parse(s2.serialize(in), ObjectList.class);
		assertEqualObjects(e, a);
		a = p2.parse(s.serialize(in), ObjectList.class);
		assertEqualObjects(e, a);

		in = new A().init1();
		e = new A().init2();
		a = p.parse(s2.serialize(in), A.class);
		assertEqualObjects(e, a);
		a = p2.parse(s.serialize(in), A.class);
		assertEqualObjects(e, a);
	}

	public static class A {
		public String f1;
		public String[] f2;
		public ObjectList f3;
		public ObjectMap f4;

		public A init1() throws Exception {
			f1 = " f1 ";
			f2 = new String[]{" f2a ", " f2b "};
			f3 = new ObjectList("[' f3a ',' f3b ']");
			f4 = new ObjectMap("{' foo ':' bar '}");
			return this;
		}

		public A init2() throws Exception {
			f1 = "f1";
			f2 = new String[]{"f2a", "f2b"};
			f3 = new ObjectList("['f3a','f3b']");
			f4 = new ObjectMap("{'foo':'bar'}");
			return this;
		}
	}
}
