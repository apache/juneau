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
package org.apache.juneau.parser;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.nio.charset.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.junit.*;

/**
 * Tests the @ParserConfig annotation.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ParserConfigAnnotationTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof AA)
				return "AA";
			return t.toString();
		}
	};

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	public static class AA extends ParserListener {}

	@ParserConfig(
		autoCloseStreams="$X{true}",
		binaryFormat="$X{HEX}",
		debugOutputLines="$X{1}",
		fileCharset="$X{US-ASCII}",
		streamCharset="$X{US-ASCII}",
		listener=AA.class,
		strict="$X{true}",
		trimStrings="$X{true}",
		unbuffered="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicReaderParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, a.getAnnotationList());
		JsonParserSession x = JsonParser.create().apply(al).build().getSession();
		check("true", x.isAutoCloseStreams());
		check("1", x.getDebugOutputLines());
		check("US-ASCII", x.getFileCharset());
		check("US-ASCII", x.getStreamCharset());
		check("AA", x.getListener());
		check("true", x.isStrict());
		check("true", x.isTrimStrings());
		check("true", x.isUnbuffered());
	}

	@Test
	public void basicInputStreamParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, a.getAnnotationList());
		MsgPackParserSession x = MsgPackParser.create().apply(al).build().getSession();
		check("true", x.isAutoCloseStreams());
		check("HEX", x.getBinaryFormat());
		check("1", x.getDebugOutputLines());
		check("AA", x.getListener());
		check("true", x.isStrict());
		check("true", x.isTrimStrings());
		check("true", x.isUnbuffered());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@ParserConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValuesReaderParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, b.getAnnotationList());
		JsonParserSession x = JsonParser.create().apply(al).build().getSession();
		check("false", x.isAutoCloseStreams());
		check("5", x.getDebugOutputLines());
		check(Charset.defaultCharset().toString(), x.getFileCharset());
		check("UTF-8", x.getStreamCharset());
		check(null, x.getListener());
		check("false", x.isStrict());
		check("false", x.isTrimStrings());
		check("false", x.isUnbuffered());
	}

	@Test
	public void noValuesInputStreamParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, b.getAnnotationList());
		MsgPackParserSession x = MsgPackParser.create().apply(al).build().getSession();
		check("false", x.isAutoCloseStreams());
		check("HEX", x.getBinaryFormat());
		check("5", x.getDebugOutputLines());
		check(null, x.getListener());
		check("false", x.isStrict());
		check("false", x.isTrimStrings());
		check("false", x.isUnbuffered());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationReaderParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, c.getAnnotationList());
		JsonParserSession x = JsonParser.create().apply(al).build().getSession();
		check("false", x.isAutoCloseStreams());
		check("5", x.getDebugOutputLines());
		check(Charset.defaultCharset().toString(), x.getFileCharset());
		check("UTF-8", x.getStreamCharset());
		check(null, x.getListener());
		check("false", x.isStrict());
		check("false", x.isTrimStrings());
		check("false", x.isUnbuffered());
	}

	@Test
	public void noAnnotationInputStreamParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, c.getAnnotationList());
		MsgPackParserSession x = MsgPackParser.create().apply(al).build().getSession();
		check("false", x.isAutoCloseStreams());
		check("HEX", x.getBinaryFormat());
		check("5", x.getDebugOutputLines());
		check(null, x.getListener());
		check("false", x.isStrict());
		check("false", x.isTrimStrings());
		check("false", x.isUnbuffered());
	}
}
