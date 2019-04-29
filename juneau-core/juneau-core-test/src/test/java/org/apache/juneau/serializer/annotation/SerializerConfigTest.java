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
package org.apache.juneau.serializer.annotation;

import static org.junit.Assert.*;
import static org.apache.juneau.serializer.Serializer.*;
import static org.apache.juneau.serializer.WriterSerializer.*;
import static org.apache.juneau.serializer.OutputStreamSerializer.*;

import java.util.function.*;

import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @SerializerConfig annotation.
 */
public class SerializerConfigTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
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

	static class AA extends SerializerListener {}

	@SerializerConfig(
		addBeanTypes="$true",
		addRootType="$true",
		binaryFormat="$HEX",
		listener=AA.class,
		maxIndent="$1",
		quoteChar="$'",
		sortCollections="$true",
		sortMaps="$true",
		trimEmptyCollections="$true",
		trimEmptyMaps="$true",
		trimNullProperties="$true",
		trimStrings="$true",
		uriContext="${}",
		uriRelativity="$RESOURCE",
		uriResolution="$ABSOLUTE",
		useWhitespace="$true"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicWriterSerializer() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		JsonSerializer x = JsonSerializer.create().applyAnnotations(m, sr).build();
		check("true", x.getProperty(SERIALIZER_addBeanTypes));
		check("true", x.getProperty(SERIALIZER_addRootType));
		check(null, x.getProperty(OSSERIALIZER_binaryFormat));
		check("AA", x.getProperty(SERIALIZER_listener));
		check("1", x.getProperty(WSERIALIZER_maxIndent));
		check("'", x.getProperty(WSERIALIZER_quoteChar));
		check("true", x.getProperty(SERIALIZER_sortCollections));
		check("true", x.getProperty(SERIALIZER_sortMaps));
		check("true", x.getProperty(SERIALIZER_trimEmptyCollections));
		check("true", x.getProperty(SERIALIZER_trimEmptyMaps));
		check("true", x.getProperty(SERIALIZER_trimNullProperties));
		check("true", x.getProperty(SERIALIZER_trimStrings));
		check("{}", x.getProperty(SERIALIZER_uriContext));
		check("RESOURCE", x.getProperty(SERIALIZER_uriRelativity));
		check("ABSOLUTE", x.getProperty(SERIALIZER_uriResolution));
		check("true", x.getProperty(SERIALIZER_useWhitespace));
	}

	@Test
	public void basicOutputStreamSerializer() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		MsgPackSerializer x = MsgPackSerializer.create().applyAnnotations(m, sr).build();
		check("true", x.getProperty(SERIALIZER_addBeanTypes));
		check("true", x.getProperty(SERIALIZER_addRootType));
		check("HEX", x.getProperty(OSSERIALIZER_binaryFormat));
		check("AA", x.getProperty(SERIALIZER_listener));
		check(null, x.getProperty(WSERIALIZER_maxIndent));
		check(null, x.getProperty(WSERIALIZER_quoteChar));
		check("true", x.getProperty(SERIALIZER_sortCollections));
		check("true", x.getProperty(SERIALIZER_sortMaps));
		check("true", x.getProperty(SERIALIZER_trimEmptyCollections));
		check("true", x.getProperty(SERIALIZER_trimEmptyMaps));
		check("true", x.getProperty(SERIALIZER_trimNullProperties));
		check("true", x.getProperty(SERIALIZER_trimStrings));
		check("{}", x.getProperty(SERIALIZER_uriContext));
		check("RESOURCE", x.getProperty(SERIALIZER_uriRelativity));
		check("ABSOLUTE", x.getProperty(SERIALIZER_uriResolution));
		check("true", x.getProperty(SERIALIZER_useWhitespace));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@SerializerConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValuesWriterSerializer() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		JsonSerializer x = JsonSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(SERIALIZER_addRootType));
		check(null, x.getProperty(OSSERIALIZER_binaryFormat));
		check(null, x.getProperty(SERIALIZER_listener));
		check(null, x.getProperty(WSERIALIZER_maxIndent));
		check(null, x.getProperty(WSERIALIZER_quoteChar));
		check(null, x.getProperty(SERIALIZER_sortCollections));
		check(null, x.getProperty(SERIALIZER_sortMaps));
		check(null, x.getProperty(SERIALIZER_trimEmptyCollections));
		check(null, x.getProperty(SERIALIZER_trimEmptyMaps));
		check(null, x.getProperty(SERIALIZER_trimNullProperties));
		check(null, x.getProperty(SERIALIZER_trimStrings));
		check(null, x.getProperty(SERIALIZER_uriContext));
		check(null, x.getProperty(SERIALIZER_uriRelativity));
		check(null, x.getProperty(SERIALIZER_uriResolution));
		check(null, x.getProperty(SERIALIZER_useWhitespace));
	}

	@Test
	public void noValuesOutputStreamSerializer() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		MsgPackSerializer x = MsgPackSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(SERIALIZER_addRootType));
		check(null, x.getProperty(OSSERIALIZER_binaryFormat));
		check(null, x.getProperty(SERIALIZER_listener));
		check(null, x.getProperty(WSERIALIZER_maxIndent));
		check(null, x.getProperty(WSERIALIZER_quoteChar));
		check(null, x.getProperty(SERIALIZER_sortCollections));
		check(null, x.getProperty(SERIALIZER_sortMaps));
		check(null, x.getProperty(SERIALIZER_trimEmptyCollections));
		check(null, x.getProperty(SERIALIZER_trimEmptyMaps));
		check(null, x.getProperty(SERIALIZER_trimNullProperties));
		check(null, x.getProperty(SERIALIZER_trimStrings));
		check(null, x.getProperty(SERIALIZER_uriContext));
		check(null, x.getProperty(SERIALIZER_uriRelativity));
		check(null, x.getProperty(SERIALIZER_uriResolution));
		check(null, x.getProperty(SERIALIZER_useWhitespace));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationWriterSerializer() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		JsonSerializer x = JsonSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}

	@Test
	public void noAnnotationOutputStreamSerializer() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		MsgPackSerializer x = MsgPackSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}
}
