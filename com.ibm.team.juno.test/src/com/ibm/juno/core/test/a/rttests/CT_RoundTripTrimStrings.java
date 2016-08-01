/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2016. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.a.rttests;

import static com.ibm.juno.core.test.TestUtils.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;


/**
 * Tests for the {@link SerializerProperties#SERIALIZER_trimStrings} and {@link ParserProperties#PARSER_trimStrings}.
 */
public class CT_RoundTripTrimStrings extends RoundTripTest {

	public CT_RoundTripTrimStrings(String label, WriterSerializer s, ReaderParser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// test
	//====================================================================================================
	@SuppressWarnings("hiding")
	@Test
	public void test() throws Exception {
		if (isValidationOnly())
			return;
		WriterSerializer s = getSerializer();
		ReaderParser p = getParser();
		Object in, a, e;

		WriterSerializer s2 = s.clone().setProperty(SerializerProperties.SERIALIZER_trimStrings, true);
		ReaderParser p2 = p.clone().setProperty(ParserProperties.PARSER_trimStrings, true);

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
