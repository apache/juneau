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
package org.apache.juneau.jsonschema;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;

/**
 * Tests the @JsonSchemaConfig annotation.
 */
class JsonSchemaConfigAnnotationTest extends TestBase {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = t -> {
		if (t instanceof Collection)
			return ((Collection<?>)t)
				.stream()
				.map(JsonSchemaConfigAnnotationTest.TO_STRING)
				.collect(Collectors.joining(","));
		if (t instanceof BeanDefMapper)
			return t.getClass().getSimpleName();
		return t.toString();
	};

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@JsonSchemaConfig(
		addDescriptionsTo="$X{BEAN}",
		addExamplesTo="$X{BEAN}",
		allowNestedDescriptions="$X{true}",
		allowNestedExamples="$X{true}",
		beanDefMapper=BasicBeanDefMapper.class,
		ignoreTypes="$X{foo}",
		useBeanDefs="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test void basic() {
		var al = AnnotationWorkList.of(sr, rstream(a.getAnnotationInfos()).collect(Collectors.toCollection(AnnotationList::new)));
		var x = JsonSchemaGenerator.create().apply(al).build().getSession();
		check("BEAN", x.getAddDescriptionsTo());
		check("BEAN", x.getAddExamplesTo());
		check("true", x.isAllowNestedDescriptions());
		check("true", x.isAllowNestedExamples());
		check("BasicBeanDefMapper", x.getBeanDefMapper());
		check("foo", x.getIgnoreTypes());
		check("true", x.isUseBeanDefs());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@JsonSchemaConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test void noValues() {
		var al = AnnotationWorkList.of(sr, rstream(b.getAnnotationInfos()).collect(Collectors.toCollection(AnnotationList::new)));
		var x = JsonSchemaGenerator.create().apply(al).build().getSession();
		check("", x.getAddDescriptionsTo());
		check("", x.getAddExamplesTo());
		check("false", x.isAllowNestedDescriptions());
		check("false", x.isAllowNestedExamples());
		check("BasicBeanDefMapper", x.getBeanDefMapper());
		check("", x.getIgnoreTypes());
		check("false", x.isUseBeanDefs());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test void noAnnotation() {
		var al = AnnotationWorkList.of(sr, rstream(c.getAnnotationInfos()).collect(Collectors.toCollection(AnnotationList::new)));
		var x = JsonSchemaGenerator.create().apply(al).build().getSession();
		check("", x.getAddDescriptionsTo());
		check("", x.getAddExamplesTo());
		check("false", x.isAllowNestedDescriptions());
		check("false", x.isAllowNestedExamples());
		check("BasicBeanDefMapper", x.getBeanDefMapper());
		check("", x.getIgnoreTypes());
		check("false", x.isUseBeanDefs());
	}
}