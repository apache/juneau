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
package org.apache.juneau.reflect;

import static org.apache.juneau.reflect.ConstructorInfo.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ConstructorInfoTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof Iterable)
				return StreamSupport.stream(((Iterable<?>)t).spliterator(), false).map(this).collect(Collectors.joining(","));
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getSimpleName();
			if (t instanceof ConstructorInfo)
				return ((ConstructorInfo)t).getShortName();
			if (t instanceof Constructor)
				return ConstructorInfo.of((Constructor<?>)t).getShortName();
			return t.toString();
		}
	};

	private static ConstructorInfo ofc(Class<?> c, Class<?>...pt) {
		try {
			return of(c.getConstructor(pt));
		} catch (NoSuchMethodException | SecurityException e) {
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
	static ConstructorInfo a = ofc(A.class);

	@Test
	public void of_withDeclaringClass() throws Exception {
		check("A()", ConstructorInfo.of(ClassInfo.of(A.class), a.inner()));
	}

	@Test
	public void of_noDeclaringClass() throws Exception {
		check("A()", a.inner());
	}

	@Test
	public void getDeclaringClass() throws Exception {
		check("A", a.getDeclaringClass());
	}

	@Test
	public void of_null() throws Exception {
		check(null, ConstructorInfo.of(null));
		check(null, ConstructorInfo.of(null, null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	public static class B {
		private String f;
		public B() {}
		public B(String f) {
			this.f = f;
		}
		public B(String f, String f2) {
			this.f = f;
		}
		protected B(int f) {}
		@Override
		public String toString() {
			return f;
		}
	}
	static ClassInfo b = ClassInfo.of(B.class);
	static ConstructorInfo
		b_c1 = b.getPublicConstructor(x -> x.hasNoParams()),
		b_c2 = b.getPublicConstructor(x -> x.hasParamTypes(String.class)),
		b_c3 = b.getDeclaredConstructor(x -> x.hasParamTypes(int.class)),
		b_c4 = b.getPublicConstructor(x -> x.hasParamTypes(String.class, String.class));


	@Test
	public void invoke() throws Exception {
		assertEquals(null, b_c1.invokeFuzzy().toString());
		assertEquals("foo", b_c2.invokeFuzzy("foo").toString());
	}

	@Test
	public void accessible() throws Exception {
		b_c3.accessible(Visibility.PROTECTED);
		assertEquals(null, b_c3.invokeFuzzy(123).toString());
	}

	@Test
	public void compareTo() {
		Set<ConstructorInfo> s = new TreeSet<>(Arrays.asList(b_c1, b_c2, b_c3, b_c4, a));
		check("A(),B(),B(int),B(String),B(String,String)", s);

	}
}