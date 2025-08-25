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
package org.apache.juneau.json;

import static org.junit.Assert.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.json.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;

/**
 * Tests the @JsonConfig annotation.
 */
class JsonConfigAnnotationTest extends SimpleTestBase {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = Object::toString;

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@JsonConfig(
		addBeanTypes="$X{true}",
		escapeSolidus="$X{true}",
		simpleAttrs="$X{true}",
		validateEnd="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test void basicSerializer() {
		var al = AnnotationWorkList.of(sr, a.getAnnotationList());
		JsonSerializerSession x = JsonSerializer.create().apply(al).build().getSession();
		check("true", x.isAddBeanTypes());
		check("true", x.isEscapeSolidus());
		check("true", x.isSimpleAttrs());
	}

	@Test void basicParser() {
		var al = AnnotationWorkList.of(sr, a.getAnnotationList());
		JsonParserSession x = JsonParser.create().apply(al).build().getSession();
		check("true", x.isValidateEnd());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@JsonConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test void noValuesSerializer() {
		var al = AnnotationWorkList.of(sr, b.getAnnotationList());
		JsonSerializerSession x = JsonSerializer.create().apply(al).build().getSession();
		check("false", x.isAddBeanTypes());
		check("false", x.isEscapeSolidus());
		check("false", x.isSimpleAttrs());
	}

	@Test void noValuesParser() {
		var al = AnnotationWorkList.of(sr, b.getAnnotationList());
		JsonParserSession x = JsonParser.create().apply(al).build().getSession();
		check("false", x.isValidateEnd());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test void noAnnotationSerializer() {
		var al = AnnotationWorkList.of(sr, c.getAnnotationList());
		JsonSerializerSession x = JsonSerializer.create().apply(al).build().getSession();
		check("false", x.isAddBeanTypes());
		check("false", x.isEscapeSolidus());
		check("false", x.isSimpleAttrs());
	}

	@Test void noAnnotationParser() {
		var al = AnnotationWorkList.of(sr, c.getAnnotationList());
		JsonParserSession x = JsonParser.create().apply(al).build().getSession();
		check("false", x.isValidateEnd());
	}
}