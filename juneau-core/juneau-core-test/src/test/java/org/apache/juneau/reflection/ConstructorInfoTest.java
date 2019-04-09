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

import static org.apache.juneau.reflect.ConstructorInfo.*;
import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.reflect.*;
import org.junit.*;

public class ConstructorInfoTest {

	private static void check(String expected, Object o) {
		if (o instanceof List) {
			List<?> l = (List<?>)o;
			String actual = l
				.stream()
				.map(TO_STRING)
				.collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else if (o instanceof Iterable) {
			String actual = StreamSupport.stream(((Iterable<?>)o).spliterator(), false)
				.map(TO_STRING)
				.collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else {
			assertEquals(expected, TO_STRING.apply(o));
		}
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
			if (t instanceof Constructor) {
				Constructor<?> x = (Constructor<?>)t;
				return x.getDeclaringClass().getSimpleName() + '(' + argTypes(x.getParameterTypes()) + ')';
			}
//			if (t instanceof Package)
//				return ((Package)t).getName();
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getSimpleName();
//			if (t instanceof MethodInfo)
//				return ((MethodInfo)t).getDeclaringClass().getSimpleName() + '.' + ((MethodInfo)t).getLabel();
//			if (t instanceof ConstructorInfo)
//				return ((ConstructorInfo)t).getLabel();
//			if (t instanceof FieldInfo)
//				return ((FieldInfo)t).getDeclaringClass().getSimpleName() + '.' + ((FieldInfo)t).getLabel();
//			if (t instanceof AnnotationInfo)
//				return apply(((AnnotationInfo<?>)t).getAnnotation());
			return t.toString();
		}
	};

	private static String argTypes(Class<?>[] t) {
		return Arrays.asList(t).stream().map(x -> x.getSimpleName()).collect(Collectors.joining(","));
	}

	private static ConstructorInfo ofc(Class<?> c, Class<?>...pt) {
		try {
			return of(c.getConstructor(pt));
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

	static class A {
		public A() {}
	}
	ConstructorInfo a = ofc(A.class);

	@Test
	public void of_noDeclaringClass() throws Exception {
		check("A()", a.inner());
	}

	@Test
	public void getDeclaringClass() throws Exception {
		check("A", a.getDeclaringClass());
	}


}
