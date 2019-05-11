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
package org.apache.juneau.jsonschema;

import static org.junit.Assert.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.junit.*;

/**
 * Tests the @JsonSchemaConfig annotation.
 */
public class JsonSchemaConfigAnnotationTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t instanceof Collection)
				return ((Collection<?>)t)
					.stream()
					.map(TO_STRING)
					.collect(Collectors.joining(","));
			if (t instanceof BeanDefMapper)
				return ((BeanDefMapper)t).getClass().getSimpleName();
			return t.toString();
		}
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
		defaultSchemas=@CS(k=A.class,v="{foo:'bar'}"),
		ignoreTypes="$X{foo}",
		useBeanDefs="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basic() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		JsonSchemaGeneratorSession x = JsonSchemaGenerator.create().applyAnnotations(m, sr).build().createSession();
		check("BEAN", x.getAddDescriptionsTo());
		check("BEAN", x.getAddExamplesTo());
		check("true", x.isAllowNestedDescriptions());
		check("true", x.isAllowNestedExamples());
		check("BasicBeanDefMapper", x.getBeanDefMapper());
		check("{org.apache.juneau.jsonschema.JsonSchemaConfigAnnotationTest$A={foo:'bar'}}", x.getDefaultSchemas());
		check("foo", x.getIgnoreTypes());
		check("true", x.isUseBeanDefs());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@JsonSchemaConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValues() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		JsonSchemaGeneratorSession x = JsonSchemaGenerator.create().applyAnnotations(m, sr).build().createSession();
		check("", x.getAddDescriptionsTo());
		check("", x.getAddExamplesTo());
		check("false", x.isAllowNestedDescriptions());
		check("false", x.isAllowNestedExamples());
		check("BasicBeanDefMapper", x.getBeanDefMapper());
		check("{}", x.getDefaultSchemas());
		check("", x.getIgnoreTypes());
		check("false", x.isUseBeanDefs());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotation() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		JsonSchemaGeneratorSession x = JsonSchemaGenerator.create().applyAnnotations(m, sr).build().createSession();
		check("", x.getAddDescriptionsTo());
		check("", x.getAddExamplesTo());
		check("false", x.isAllowNestedDescriptions());
		check("false", x.isAllowNestedExamples());
		check("BasicBeanDefMapper", x.getBeanDefMapper());
		check("{}", x.getDefaultSchemas());
		check("", x.getIgnoreTypes());
		check("false", x.isUseBeanDefs());
	}
}
