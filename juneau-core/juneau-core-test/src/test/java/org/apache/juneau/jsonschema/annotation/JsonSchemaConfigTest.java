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
package org.apache.juneau.jsonschema.annotation;

import static org.junit.Assert.*;
import static org.apache.juneau.jsonschema.JsonSchemaGenerator.*;

import java.util.function.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @JsonSchemaConfig annotation.
 */
public class JsonSchemaConfigTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
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

	@JsonSchemaConfig(
		addDescriptionsTo="$BEAN",
		addExamplesTo="$BEAN",
		allowNestedDescriptions="$true",
		allowNestedExamples="$true",
		beanDefMapper=BasicBeanDefMapper.class,
		defaultSchemas=@CSEntry(key=A.class,value="${foo:'bar'}"),
		ignoreTypes="$foo",
		useBeanDefs="$true"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basic() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		JsonSchemaGenerator x = JsonSchemaGenerator.create().applyAnnotations(m, sr).build();
		check("BEAN", x.getProperty(JSONSCHEMA_addDescriptionsTo));
		check("BEAN", x.getProperty(JSONSCHEMA_addExamplesTo));
		check("true", x.getProperty(JSONSCHEMA_allowNestedDescriptions));
		check("true", x.getProperty(JSONSCHEMA_allowNestedExamples));
		check("BasicBeanDefMapper", x.getProperty(JSONSCHEMA_beanDefMapper));
		check("{org.apache.juneau.jsonschema.annotation.JsonSchemaConfigTest$A={foo:'bar'}}", x.getProperty(JSONSCHEMA_defaultSchemas));
		check("foo", x.getProperty(JSONSCHEMA_ignoreTypes));
		check("true", x.getProperty(JSONSCHEMA_useBeanDefs));
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
		JsonSchemaGenerator x = JsonSchemaGenerator.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(JSONSCHEMA_addDescriptionsTo));
		check(null, x.getProperty(JSONSCHEMA_addExamplesTo));
		check(null, x.getProperty(JSONSCHEMA_allowNestedDescriptions));
		check(null, x.getProperty(JSONSCHEMA_allowNestedExamples));
		check(null, x.getProperty(JSONSCHEMA_beanDefMapper));
		check(null, x.getProperty(JSONSCHEMA_defaultSchemas));
		check(null, x.getProperty(JSONSCHEMA_ignoreTypes));
		check(null, x.getProperty(JSONSCHEMA_useBeanDefs));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotation() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		JsonSchemaGenerator x = JsonSchemaGenerator.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(JSONSCHEMA_addDescriptionsTo));
	}
}
