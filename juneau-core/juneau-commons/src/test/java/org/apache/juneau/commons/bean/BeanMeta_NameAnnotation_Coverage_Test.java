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
package org.apache.juneau.commons.bean;

import static org.junit.jupiter.api.Assertions.*;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

/**
 * Direct (reflection-driven) coverage tests for {@link BeanMeta}'s private static {@code bpName(List, List)}
 * and {@code name(AnnotationInfo)} helpers' documented-but-practically-unreachable {@link Name @Name}-annotation
 * handling.
 *
 * <p>
 * {@link Name @Name} is {@code @Target(ElementType.PARAMETER)}-only, so no real call site in this codebase can
 * ever pass a non-empty {@code List<Name>} into {@code bpName} (both call sites scan field/method-level
 * annotations only) - {@code BeanMeta.java} itself already documents this via {@code // HTT} markers on the
 * relevant guard conditions. These tests exercise the documented behavior directly via reflection, since the
 * javadoc on both methods explicitly describes {@code @Name} handling as part of their contract even though no
 * production caller can trigger it.
 */
class BeanMeta_NameAnnotation_Coverage_Test extends TestBase {

	// A dummy parameter-annotated holder - @Name can only ever be placed on a parameter, so this is the only
	// way to obtain a real (non-mock) Name annotation instance to hand to bpName()/name() directly.
	@SuppressWarnings("unused")
	private static void paramHolder(@Name("theName") String p) { /* never invoked - reflection target only */ }

	private static Name syntheticNameAnnotation() throws Exception {
		var m = BeanMeta_NameAnnotation_Coverage_Test.class.getDeclaredMethod("paramHolder", String.class);
		return (Name) m.getParameters()[0].getAnnotations()[0];
	}

	private static String invokeBpName(List<BeanProp> p, List<Name> n) throws Exception {
		var m = BeanMeta.class.getDeclaredMethod("bpName", List.class, List.class);
		m.setAccessible(true);
		return (String) m.invoke(null, p, n);
	}

	private static String invokeNameOf(AnnotationInfo<?> ai) throws Exception {
		var m = BeanMeta.class.getDeclaredMethod("name", AnnotationInfo.class);
		m.setAccessible(true);
		return (String) m.invoke(null, ai);
	}

	public static class DummyAnnotatable {
		public String x;
	}

	@Test
	void a01_bpName_nonEmptyNameList_returnsLastNamesValue() throws Exception {
		var n1 = syntheticNameAnnotation();
		// bpName() javadoc: "If @Name annotations are present, returns the value from the last one."
		var result = invokeBpName(List.of(), List.of(n1, n1));
		assertEquals("theName", result);
	}

	@Test
	void a02_name_nameAnnotationInfo_returnsValue() throws Exception {
		var n1 = syntheticNameAnnotation();
		var f = info(DummyAnnotatable.class.getField("x"));
		var ai = AnnotationInfo.of(f, n1);
		assertEquals("theName", invokeNameOf(ai));
	}

	// A @Name whose value() is blank - name(AnnotationInfo)'s Name-branch guard is `ine(n.value())`
	// (is-not-empty), so a blank value must return null rather than the empty string.
	@SuppressWarnings("unused")
	private static void blankParamHolder(@Name("") String p) { /* never invoked - reflection target only */ }

	@Test
	void a03_name_blankNameAnnotationValue_returnsNull() throws Exception {
		var m = BeanMeta_NameAnnotation_Coverage_Test.class.getDeclaredMethod("blankParamHolder", String.class);
		var blankName = (Name) m.getParameters()[0].getAnnotations()[0];
		var f = info(DummyAnnotatable.class.getField("x"));
		var ai = AnnotationInfo.of(f, blankName);
		assertNull(invokeNameOf(ai));
	}
}
