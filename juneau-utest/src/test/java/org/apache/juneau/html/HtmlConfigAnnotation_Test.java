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
package org.apache.juneau.html;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;

/**
 * Tests the @HtmlConfig annotation.
 */
class HtmlConfigAnnotation_Test extends TestBase {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = t -> Objects.toString(t, null);

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@HtmlConfig(
		addBeanTypes="$X{true}",
		addKeyValueTableHeaders="$X{true}",
		disableDetectLabelParameters="$X{true}",
		disableDetectLinksInStrings="$X{true}",
		labelParameter="$X{foo}",
		uriAnchorText="$X{TO_STRING}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test void basicSerializer() {
		var al = AnnotationWorkList.of(sr, a.getAnnotationList());
		var x = HtmlSerializer.create().apply(al).build().getSession();
		check("true", x.isAddBeanTypes());
		check("true", x.isAddKeyValueTableHeaders());
		check("false", x.isDetectLabelParameters());
		check("false", x.isDetectLinksInStrings());
		check("foo", x.getLabelParameter());
		check("TO_STRING", x.getUriAnchorText());
	}

	@Test void basicParser() {
		var al = AnnotationWorkList.of(sr, a.getAnnotationList());
		assertDoesNotThrow(()->HtmlParser.create().apply(al).build().createSession());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@HtmlConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test void defaultsSerializer() {
		var al = AnnotationWorkList.of(sr, b.getAnnotationList());
		var x = HtmlSerializer.create().apply(al).build().getSession();
		check("false", x.isAddBeanTypes());
		check("false", x.isAddKeyValueTableHeaders());
		check("true", x.isDetectLabelParameters());
		check("true", x.isDetectLinksInStrings());
		check("label", x.getLabelParameter());
		check("TO_STRING", x.getUriAnchorText());
	}

	@Test void defaultsParser() {
		var al = AnnotationWorkList.of(sr, b.getAnnotationList());
		assertDoesNotThrow(()->HtmlParser.create().apply(al).build().createSession());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test void noAnnotationSerializer() {
		var al = AnnotationWorkList.of(sr, c.getAnnotationList());
		var x = HtmlSerializer.create().apply(al).build().getSession();
		check("false", x.isAddBeanTypes());
		check("false", x.isAddKeyValueTableHeaders());
		check("true", x.isDetectLabelParameters());
		check("true", x.isDetectLinksInStrings());
		check("label", x.getLabelParameter());
		check("TO_STRING", x.getUriAnchorText());
	}

	@Test void noAnnotationParser() {
		var al = AnnotationWorkList.of(sr, c.getAnnotationList());
		assertDoesNotThrow(()->HtmlParser.create().apply(al).build().createSession());
	}
}