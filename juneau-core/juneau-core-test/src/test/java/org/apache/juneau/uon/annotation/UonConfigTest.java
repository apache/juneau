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
package org.apache.juneau.uon.annotation;

import static org.junit.Assert.*;
import static org.apache.juneau.serializer.Serializer.*;
import static org.apache.juneau.uon.UonSerializer.*;
import static org.apache.juneau.uon.UonParser.*;

import java.util.function.*;

import org.apache.juneau.reflect.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @UonConfig annotation.
 */
public class UonConfigTest {

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

	@UonConfig(
		addBeanTypes="$true",
		decoding="$true",
		encoding="$true",
		paramFormat="$UON",
		validateEnd="$true"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicSerializer() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		UonSerializer x = UonSerializer.create().applyAnnotations(m, sr).build();
		check("true", x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(UON_decoding));
		check("true", x.getProperty(UON_encoding));
		check("UON", x.getProperty(UON_paramFormat));
		check(null, x.getProperty(UON_validateEnd));
	}

	@Test
	public void basicParser() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		UonParser x = UonParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check("true", x.getProperty(UON_decoding));
		check(null, x.getProperty(UON_encoding));
		check(null, x.getProperty(UON_paramFormat));
		check("true", x.getProperty(UON_validateEnd));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@UonConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValuesSerializer() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		UonSerializer x = UonSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}

	@Test
	public void noValuesParser() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		UonParser x = UonParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationSerializer() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		UonSerializer x = UonSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}

	@Test
	public void noAnnotationParser() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		UonParser x = UonParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}
}
