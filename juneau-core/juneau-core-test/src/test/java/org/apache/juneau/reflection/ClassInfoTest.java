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

import static org.apache.juneau.reflection.ClassInfo.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.*;

public class ClassInfoTest {

	private static void assertListEquals(String expected, List<?> l) {
		String actual = l
			.stream()
			.map(TO_STRING)
			.collect(Collectors.joining(","));
		assertEquals(expected, actual);
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getSimpleName();
			if (t instanceof MethodInfo)
				return ((MethodInfo)t).getDeclaringClassInfo().getSimpleName() + '.' + ((MethodInfo)t).getName();
			return t.toString();
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	public class A1 {}
	public class A2 extends Value<A1>{};

	@Test
	public void ofType() {
		assertEquals("A1", of(A1.class).getSimpleName());
	}

	@Test
	public void ofTypeOnObject() {
		assertEquals("A1", of(new A1()).getSimpleName());
	}

	@Test
	public void ofTypeOnNulls() {
		assertNull(of((Class<?>)null));
		assertNull(of((Type)null));
		assertNull(of((Object)null));
	}

	@Test
	public void inner() {
		assertTrue(of(A1.class).inner() instanceof Class);
		assertTrue(of(A1.class).innerType() instanceof Class);
	}

	@Test
	public void resolved() {
		assertEquals("A1", of(A1.class).resolved().getSimpleName());
		assertEquals("A1", of(A2.class).resolved().getSimpleName());
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Parent classes and interfaces.
	//-----------------------------------------------------------------------------------------------------------------

	static interface BI1 {}
	static interface BI2 extends BI1 {}
	static interface BI3 {}
	static interface BI4 {}
	static class BC1 implements BI1, BI2 {}
	static class BC2 extends BC1 implements BI3 {}
	static class BC3 extends BC2 {}

	@Test
	public void getInterfaces() {
		assertListEquals("", of(BI4.class).getInterfaces());
		assertListEquals("BI1,BI2", of(BC1.class).getInterfaces());
		assertListEquals("BI3", of(BC2.class).getInterfaces());
		assertListEquals("", of(BC3.class).getInterfaces());
	}

	@Test
	public void getInterfacesTwice() {
		ClassInfo bc2 = of(BC2.class);
		assertListEquals("BI3", bc2.getInterfaces());
		assertListEquals("BI3", bc2.getInterfaces());
	}

	@Test
	public void getInterfaceInfos() {
		assertListEquals("", of(BI4.class).getInterfaceInfos());
		assertListEquals("BI1,BI2", of(BC1.class).getInterfaceInfos());
		assertListEquals("BI3", of(BC2.class).getInterfaceInfos());
		assertListEquals("", of(BC3.class).getInterfaceInfos());
	}

	@Test
	public void getInterfaceInfosTwice() {
		ClassInfo bc2 = of(BC2.class);
		assertListEquals("BI3", bc2.getInterfaceInfos());
		assertListEquals("BI3", bc2.getInterfaceInfos());
	}

	@Test
	public void getParentInfos() {
		ClassInfo bc3 = of(BC3.class);
		assertListEquals("BC3,BC2,BC1", bc3.getParentInfos());
		assertListEquals("BC3,BC2,BC1", bc3.getParentInfos(false,false));
		assertListEquals("BC1,BC2,BC3", bc3.getParentInfos(true,false));

		assertListEquals("BC3,BC2,BI3,BC1,BI1,BI2", bc3.getParentInfos(false,true));
		assertListEquals("BI2,BI1,BC1,BI3,BC2,BC3", bc3.getParentInfos(true,true));
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Methods
	//-----------------------------------------------------------------------------------------------------------------

	static interface CI1 {
		void i1a();
		void i1b();
	}
	static interface CI2 extends CI1 {
		void i2b();
		void i2a();
	}
	static interface CI3 {}
	static interface CI4 {}
	static abstract class CC1 implements CI1, CI2 {
		@Override
		public void i1a() {}
		public void c1b() {}
		public void c1a() {}
	}
	static class CC2 extends CC1 implements CI3 {
		public void c2b() {}
		@Override
		public void i1b() {}
		@Override
		public void i2b() {}
		@Override
		public void i2a() {}
		public void c2a() {}
	}
	static class CC3 extends CC2 {
		@Override
		public void i2b() {}
		public void c3a() {}
		public void c3b() {}
	}

	@Test
	public void getAllMethodInfos() throws Exception {
		ClassInfo cc3 = of(CC3.class);
		assertListEquals("CC3.c3a,CC3.c3b,CC3.i2b,CC2.c2a,CC2.c2b,CC2.i1b,CC2.i2a,CC2.i2b,CC1.c1a,CC1.c1b,CC1.i1a,CI1.i1a,CI1.i1b,CI2.i2a,CI2.i2b", cc3.getAllMethodInfos());
		assertListEquals("CC3.c3a,CC3.c3b,CC3.i2b,CC2.c2a,CC2.c2b,CC2.i1b,CC2.i2a,CC2.i2b,CC1.c1a,CC1.c1b,CC1.i1a,CI1.i1a,CI1.i1b,CI2.i2a,CI2.i2b", cc3.getAllMethodInfos(false));
		assertListEquals("CI2.i2a,CI2.i2b,CI1.i1a,CI1.i1b,CC1.c1a,CC1.c1b,CC1.i1a,CC2.c2a,CC2.c2b,CC2.i1b,CC2.i2a,CC2.i2b,CC3.c3a,CC3.c3b,CC3.i2b", cc3.getAllMethodInfos(true));
	}

	//====================================================================================================
	// isParentClass(Class, Class)
	//====================================================================================================

	public interface B1x {}
	public static class B2x implements B1x {}
	public static class B3x extends B2x {}


	@Test
	public void testIsParentClass() throws Exception {

		// Strict
		assertTrue(of(B1x.class).isParentOf(B2x.class, true));
		assertTrue(of(B2x.class).isParentOf(B3x.class, true));
		assertTrue(of(Object.class).isParentOf(B3x.class, true));
		assertFalse(of(B1x.class).isParentOf(B1x.class, true));
		assertFalse(of(B2x.class).isParentOf(B2x.class, true));
		assertFalse(of(B3x.class).isParentOf(B3x.class, true));
		assertFalse(of(B3x.class).isParentOf(B2x.class, true));
		assertFalse(of(B2x.class).isParentOf(B1x.class, true));
		assertFalse(of(B3x.class).isParentOf(Object.class, true));

		// Not strict
		assertTrue(of(B1x.class).isParentOf(B2x.class, false));
		assertTrue(of(B2x.class).isParentOf(B3x.class, false));
		assertTrue(of(Object.class).isParentOf(B3x.class, false));
		assertTrue(of(B1x.class).isParentOf(B1x.class, false));
		assertTrue(of(B2x.class).isParentOf(B2x.class, false));
		assertTrue(of(B3x.class).isParentOf(B3x.class, false));
		assertFalse(of(B3x.class).isParentOf(B2x.class, false));
		assertFalse(of(B2x.class).isParentOf(B1x.class, false));
		assertFalse(of(B3x.class).isParentOf(Object.class, false));
	}


	//====================================================================================================
	// getAllMethodsParentFirst()
	//====================================================================================================

	//====================================================================================================
	// getAllFieldsParentFirst()
	//====================================================================================================
	@Test
	public void getParentFieldsParentFirst() throws Exception {
		Set<String> s = new TreeSet<>();
		ClassInfo ci = ClassInfo.of(EB.class);
		for (FieldInfo f : ci.getAllFieldInfos(true,false)) {
			if (! f.getName().startsWith("$"))
				s.add(f.getDeclaringClassInfo().getSimpleName() + '.' + f.getName());
		}
		assertObjectEquals("['EA.a1','EB.a1','EB.b1']", s);

		s = new TreeSet<>();
		for (FieldInfo f : ci.getAllFieldInfos()) {
			if (! f.getName().startsWith("$"))
				s.add(f.getDeclaringClassInfo().getSimpleName() + '.' + f.getName());
		}
		assertObjectEquals("['EA.a1','EB.a1','EB.b1']", s);
	}

	static class EA {
		int a1;
	}
	static class EB extends EA {
		int a1;
		int b1;
	}

	//====================================================================================================
	// getSimpleName()
	//====================================================================================================

	@Test
	public void getShortName() throws Exception {
		assertEquals("ClassInfoTest.G1", of(G1.class).getShortName());
		assertEquals("ClassInfoTest.G2", of(G2.class).getShortName());
	}

	public class G1 {}
	public static class G2 {}


}
