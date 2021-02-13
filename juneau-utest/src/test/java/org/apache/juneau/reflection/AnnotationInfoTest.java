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
package org.apache.juneau.reflection;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.assertions.Assertions.*;

import org.apache.juneau.annotation.*;
import java.lang.annotation.*;
import java.util.function.*;

import org.apache.juneau.reflect.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class AnnotationInfoTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t instanceof A)
				return "@A(" + ((A)t).value() + ")";
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getSimpleName();
			return t.toString();
		}
	};

	private static ClassInfo of(Class<?> c) {
		try {
			return ClassInfo.of(c);
		} catch (SecurityException e) {
			fail(e.getLocalizedMessage());
		}
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

	@Documented
	@Target(TYPE)
	@Retention(RUNTIME)
	@Inherited
	public static @interface A {
		int value();
	}

	@A(1)
	static class B {}
	static ClassInfo b = of(B.class);

	@Test
	public void b01_getClassOn() {
		check("B", b.getAnnotationInfos(A.class).get(0).getClassOn());
	}

	@Test
	public void b02_getAnnotation() {
		check("@A(1)", b.getAnnotationInfos(A.class).get(0).getAnnotation());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Get value.
	//-----------------------------------------------------------------------------------------------------------------

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface C {
		String foo() default "x";
	}

	@C(foo="bar")
	public static class C1 {}

	@Test
	public void c01_getValue() {
		ClassInfo c1 = ClassInfo.of(C1.class);
		AnnotationInfo<?> ai = c1.getAnnotationInfos(C.class).get(0);
		assertString(ai.getValue(String.class, "foo")).is("bar");
		assertObject(ai.getValue(Integer.class, "foo")).isNull();
		assertObject(ai.getValue(String.class, "bar")).isNull();
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Is in group.
	//-----------------------------------------------------------------------------------------------------------------

	@Target(TYPE)
	@Retention(RUNTIME)
	@AnnotationGroup(D1.class)
	public static @interface D1 {}

	@Target(TYPE)
	@Retention(RUNTIME)
	@AnnotationGroup(D1.class)
	public static @interface D2 {}

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface D3 {}

	@D1 @D2 @D3
	public static class D {}

	@Test
	public void d01_isInGroup() {
		ClassInfo d = ClassInfo.of(D.class);
		AnnotationList l = d.getAnnotationGroupList(D1.class);
		assertList(l).isSize(2);
	}
}