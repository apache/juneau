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

import java.util.function.*;

import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @ParserConfig annotation.
 */
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

	static StringResolver sr = new StringResolver() {
		@Override
		public String resolve(String input) {
			if (input.startsWith("$"))
				input = input.substring(1);
			return input;
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	public static class AA extends ParserListener {}

	@ParserConfig(
		autoCloseStreams="$true",
		binaryFormat="HEX",
		debugOutputLines="$1",
		fileCharset="$foo",
		inputStreamCharset="$foo",
		listener=AA.class,
		strict="$true",
		trimStrings="$true",
		unbuffered="$true"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicReaderParser() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		JsonParserSession x = JsonParser.create().applyAnnotations(m, sr).build().createSession();
		check("true", x.isAutoCloseStreams());
		check("1", x.getDebugOutputLines());
		check("foo", x.getFileCharset());
		check("foo", x.getInputStreamCharset());
		check("AA", x.getListener());
		check("true", x.isStrict());
		check("true", x.isTrimStrings());
		check("true", x.isUnbuffered());
	}

	@Test
	public void basicInputStreamParser() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		MsgPackParserSession x = MsgPackParser.create().applyAnnotations(m, sr).build().createSession();
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
		AnnotationsMap m = b.getAnnotationsMap();
		JsonParserSession x = JsonParser.create().applyAnnotations(m, sr).build().createSession();
		check("false", x.isAutoCloseStreams());
		check("5", x.getDebugOutputLines());
		check("DEFAULT", x.getFileCharset());
		check("UTF-8", x.getInputStreamCharset());
		check(null, x.getListener());
		check("false", x.isStrict());
		check("false", x.isTrimStrings());
		check("false", x.isUnbuffered());
	}

	@Test
	public void noValuesInputStreamParser() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		MsgPackParserSession x = MsgPackParser.create().applyAnnotations(m, sr).build().createSession();
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
		AnnotationsMap m = c.getAnnotationsMap();
		JsonParserSession x = JsonParser.create().applyAnnotations(m, sr).build().createSession();
		check("false", x.isAutoCloseStreams());
		check("5", x.getDebugOutputLines());
		check("DEFAULT", x.getFileCharset());
		check("UTF-8", x.getInputStreamCharset());
		check(null, x.getListener());
		check("false", x.isStrict());
		check("false", x.isTrimStrings());
		check("false", x.isUnbuffered());
	}

	@Test
	public void noAnnotationInputStreamParser() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		MsgPackParserSession x = MsgPackParser.create().applyAnnotations(m, sr).build().createSession();
		check("false", x.isAutoCloseStreams());
		check("HEX", x.getBinaryFormat());
		check("5", x.getDebugOutputLines());
		check(null, x.getListener());
		check("false", x.isStrict());
		check("false", x.isTrimStrings());
		check("false", x.isUnbuffered());
	}
}
