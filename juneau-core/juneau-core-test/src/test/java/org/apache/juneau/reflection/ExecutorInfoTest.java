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

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

public class ExecutorInfoTest {
//
//	private static void check(String expected, Object o) {
//		if (o instanceof List) {
//			List<?> l = (List<?>)o;
//			String actual = l
//				.stream()
//				.map(TO_STRING)
//				.collect(Collectors.joining(","));
//			assertEquals(expected, actual);
//		} else if (o instanceof Iterable) {
//			String actual = StreamSupport.stream(((Iterable<?>)o).spliterator(), false)
//				.map(TO_STRING)
//				.collect(Collectors.joining(","));
//			assertEquals(expected, actual);
//		} else {
//			assertEquals(expected, TO_STRING.apply(o));
//		}
//	}
//
//	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
//		@Override
//		public String apply(Object t) {
//			if (t == null)
//				return null;
//			if (t instanceof Class)
//				return ((Class<?>)t).getSimpleName();
//			if (t instanceof Constructor) {
//				Constructor<?> x = (Constructor<?>)t;
//				return x.getDeclaringClass().getSimpleName() + '(' + argTypes(x.getParameterTypes()) + ')';
//			}
////			if (t instanceof Package)
////				return ((Package)t).getName();
//			if (t instanceof ClassInfo)
//				return ((ClassInfo)t).getSimpleName();
////			if (t instanceof MethodInfo)
////				return ((MethodInfo)t).getDeclaringClass().getSimpleName() + '.' + ((MethodInfo)t).getLabel();
////			if (t instanceof ConstructorInfo)
////				return ((ConstructorInfo)t).getLabel();
////			if (t instanceof FieldInfo)
////				return ((FieldInfo)t).getDeclaringClass().getSimpleName() + '.' + ((FieldInfo)t).getLabel();
////			if (t instanceof AnnotationInfo)
////				return apply(((AnnotationInfo<?>)t).getAnnotation());
//			return t.toString();
//		}
//	};
//
//	private static String argTypes(Class<?>[] t) {
//		return Arrays.asList(t).stream().map(x -> x.getSimpleName()).collect(Collectors.joining(","));
//	}
//

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// Labels
	//-----------------------------------------------------------------------------------------------------------------

	static class X {
		public X() {}
		public X(String foo) {}
		public X(Map<String,Object> foo) {}
		public void foo(){}
		public void foo(String foo){}
		public void foo(Map<String,Object> foo){}
	}
	ClassInfo x = ClassInfo.of(X.class);

	@Test
	public void getFullName_method() {
		assertEquals("org.apache.juneau.reflection.ExecutorInfoTest$X.foo()", x.getPublicMethod("foo").getFullName());
		assertEquals("org.apache.juneau.reflection.ExecutorInfoTest$X.foo(java.lang.String)", x.getPublicMethod("foo", String.class).getFullName());
		assertEquals("org.apache.juneau.reflection.ExecutorInfoTest$X.foo(java.util.Map<java.lang.String,java.lang.Object>)", x.getPublicMethod("foo", Map.class).getFullName());
	}

	@Test
	public void getFullName_constructor() {
		assertEquals("org.apache.juneau.reflection.ExecutorInfoTest$X()", x.getPublicConstructor().getFullName());
		assertEquals("org.apache.juneau.reflection.ExecutorInfoTest$X(java.lang.String)", x.getPublicConstructor(String.class).getFullName());
		assertEquals("org.apache.juneau.reflection.ExecutorInfoTest$X(java.util.Map<java.lang.String,java.lang.Object>)", x.getPublicConstructor(Map.class).getFullName());
	}

	@Test
	public void getShortName_method() {
		assertEquals("foo()", x.getPublicMethod("foo").getShortName());
		assertEquals("foo(String)", x.getPublicMethod("foo", String.class).getShortName());
		assertEquals("foo(Map)", x.getPublicMethod("foo", Map.class).getShortName());
	}

	@Test
	public void getShortName_constructor() {
		assertEquals("X()", x.getPublicConstructor().getShortName());
		assertEquals("X(String)", x.getPublicConstructor(String.class).getShortName());
		assertEquals("X(Map)", x.getPublicConstructor(Map.class).getShortName());
	}

	@Test
	public void getSimpleName_method() {
		assertEquals("foo", x.getPublicMethod("foo").getSimpleName());
		assertEquals("foo", x.getPublicMethod("foo", String.class).getSimpleName());
		assertEquals("foo", x.getPublicMethod("foo", Map.class).getSimpleName());
	}

	@Test
	public void getSimpleName_constructor() {
		assertEquals("X", x.getPublicConstructor().getSimpleName());
		assertEquals("X", x.getPublicConstructor(String.class).getSimpleName());
		assertEquals("X", x.getPublicConstructor(Map.class).getSimpleName());
	}
}
