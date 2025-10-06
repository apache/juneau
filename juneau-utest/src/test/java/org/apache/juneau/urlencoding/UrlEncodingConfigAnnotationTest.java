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
package org.apache.juneau.urlencoding;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.urlencoding.annotation.*;
import org.junit.jupiter.api.*;

/**
 * Tests the @UrlEncodingConfig annotation.
 */
class UrlEncodingConfigAnnotationTest extends TestBase {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = Object::toString;

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@UrlEncodingConfig(
		expandedParams="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test void basicSerializer() {
		var al = AnnotationWorkList.of(sr, a.getAnnotationList());
		var x = UrlEncodingSerializer.create().apply(al).build().getSession();
		check("true", x.isExpandedParams());
	}

	@Test void basicParser() {
		var al = AnnotationWorkList.of(sr, a.getAnnotationList());
		var x = UrlEncodingParser.create().apply(al).build().getSession();
		check("true", x.isExpandedParams());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@UrlEncodingConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test void noValuesSerializer() {
		var al = AnnotationWorkList.of(sr, b.getAnnotationList());
		var x = UrlEncodingSerializer.create().apply(al).build().getSession();
		check("false", x.isExpandedParams());
	}

	@Test void noValuesParser() {
		var al = AnnotationWorkList.of(sr, b.getAnnotationList());
		var x = UrlEncodingParser.create().apply(al).build().getSession();
		check("false", x.isExpandedParams());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test void noAnnotationSerializer() {
		var al = AnnotationWorkList.of(sr, c.getAnnotationList());
		var x = UrlEncodingSerializer.create().apply(al).build().getSession();
		check("false", x.isExpandedParams());
	}

	@Test void noAnnotationParser() {
		var al = AnnotationWorkList.of(sr, c.getAnnotationList());
		var x = UrlEncodingParser.create().apply(al).build().getSession();
		check("false", x.isExpandedParams());
	}
}