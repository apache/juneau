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
package org.apache.juneau.soap;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.soap.annotation.*;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;

/**
 * Tests the @SoapXmlConfig annotation.
 */
class SoapXmlConfigAnnotationTest extends TestBase {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = Object::toString;

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@SoapXmlConfig(
		soapAction="$X{foo}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test void basic() {
		var al = AnnotationWorkList.of(sr, rstream(a.getAnnotations()));
		var x = SoapXmlSerializer.create().apply(al).build().getSession();
		check("foo", x.getSoapAction());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@SoapXmlConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test void noValues() {
		var al = AnnotationWorkList.of(sr, rstream(b.getAnnotations()));
		var x = SoapXmlSerializer.create().apply(al).build().getSession();
		check("http://www.w3.org/2003/05/soap-envelope", x.getSoapAction());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test void noAnnotation() {
		var al = AnnotationWorkList.of(sr, rstream(c.getAnnotations()));
		var x = SoapXmlSerializer.create().apply(al).build().getSession();
		check("http://www.w3.org/2003/05/soap-envelope", x.getSoapAction());
	}
}