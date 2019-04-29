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
package org.apache.juneau.json.annotation;

import static org.junit.Assert.*;
import static org.apache.juneau.serializer.Serializer.*;
import static org.apache.juneau.json.JsonSerializer.*;
import static org.apache.juneau.json.JsonParser.*;

import java.util.function.*;

import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @JsonConfig annotation.
 */
public class JsonConfigTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
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

	@JsonConfig(
		addBeanTypes="$true",
		escapeSolidus="$true",
		simpleMode="$true",
		validateEnd="$true"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicSerializer() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		JsonSerializer x = JsonSerializer.create().applyAnnotations(m, sr).build();
		check("true", x.getProperty(SERIALIZER_addBeanTypes));
		check("true", x.getProperty(JSON_escapeSolidus));
		check("true", x.getProperty(JSON_simpleMode));
		check(null, x.getProperty(JSON_validateEnd));
	}

	@Test
	public void basicParser() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		JsonParser x = JsonParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(JSON_escapeSolidus));
		check(null, x.getProperty(JSON_simpleMode));
		check("true", x.getProperty(JSON_validateEnd));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@JsonConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValuesSerializer() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		JsonSerializer x = JsonSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(JSON_escapeSolidus));
		check(null, x.getProperty(JSON_simpleMode));
		check(null, x.getProperty(JSON_validateEnd));
	}

	@Test
	public void noValuesParser() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		JsonParser x = JsonParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(JSON_escapeSolidus));
		check(null, x.getProperty(JSON_simpleMode));
		check(null, x.getProperty(JSON_validateEnd));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationSerializer() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		JsonSerializer x = JsonSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(JSON_escapeSolidus));
	}

	@Test
	public void noAnnotationParser() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		JsonParser x = JsonParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(JSON_validateEnd));
	}
}
