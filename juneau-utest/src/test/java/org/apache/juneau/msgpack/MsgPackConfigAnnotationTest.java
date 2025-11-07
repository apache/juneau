/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.msgpack;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.msgpack.annotation.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;
import java.util.stream.*;

/**
 * Tests the @MsgPackConfig annotation.
 */
class MsgPackConfigAnnotationTest extends TestBase {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = Object::toString;

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	 @MsgPackConfig(
		addBeanTypes="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test void basicSerializer() {
		var al = AnnotationWorkList.of(sr, rstream(a.getAnnotationInfos()).map(ai -> (AnnotationInfo<?>)ai));
		var x = MsgPackSerializer.create().apply(al).build().getSession();
		check("true", x.isAddBeanTypes());
	}

	@Test void basicParser() {
		var al = AnnotationWorkList.of(sr, rstream(a.getAnnotationInfos()).map(ai -> (AnnotationInfo<?>)ai));
		assertDoesNotThrow(()->MsgPackParser.create().apply(al).build().createSession());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@MsgPackConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test void noValuesSerializer() {
		var al = AnnotationWorkList.of(sr, rstream(b.getAnnotationInfos()).map(ai -> (AnnotationInfo<?>)ai));
		var x = MsgPackSerializer.create().apply(al).build().getSession();
		check("false", x.isAddBeanTypes());
	}

	@Test void noValuesParser() {
		var al = AnnotationWorkList.of(sr, rstream(b.getAnnotationInfos()).map(ai -> (AnnotationInfo<?>)ai));
		assertDoesNotThrow(()->MsgPackParser.create().apply(al).build().createSession());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test void noAnnotationSerializer() {
		var al = AnnotationWorkList.of(sr, rstream(c.getAnnotationInfos()).map(ai -> (AnnotationInfo<?>)ai));
		var x = MsgPackSerializer.create().apply(al).build().getSession();
		check("false", x.isAddBeanTypes());
	}

	@Test void noAnnotationParser() {
		var al = AnnotationWorkList.of(sr, rstream(c.getAnnotationInfos()).map(ai -> (AnnotationInfo<?>)ai));
		assertDoesNotThrow(()->MsgPackParser.create().apply(al).build().createSession());
	}
}