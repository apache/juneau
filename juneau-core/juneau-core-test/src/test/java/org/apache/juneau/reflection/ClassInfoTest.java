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
			return t.toString();
		}
	};

	//====================================================================================================
	// of(Type)
	//====================================================================================================

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

	//====================================================================================================
	// resolved(Type)
	//====================================================================================================

	@Test
	public void resolved() {
		assertEquals("A1", of(A1.class).resolved().getSimpleName());
		assertEquals("A1", of(A2.class).resolved().getSimpleName());
	}


	//====================================================================================================
	// getInterfaces()
	//====================================================================================================

	static interface CA1 {}
	static interface CA2 extends CA1 {}
	static interface CA3 {}
	static interface CA4 {}
	static class CB implements CA1, CA2 {}
	static class CC extends CB implements CA3 {}
	static class CD extends CC {}

	@Test
	public void getInterfaces() {
		assertListEquals("", of(CA4.class).getInterfaces());
		assertListEquals("CA1,CA2", of(CB.class).getInterfaces());
		assertListEquals("CA3", of(CC.class).getInterfaces());
		assertListEquals("", of(CD.class).getInterfaces());
	}

	@Test
	public void getInterfacesTwice() {
		ClassInfo cc = of(CC.class);
		assertListEquals("CA3", cc.getInterfaces());
		assertListEquals("CA3", cc.getInterfaces());
	}

	@Test
	public void getInterfaceInfos() {
		assertListEquals("", of(CA4.class).getInterfaceInfos());
		assertListEquals("CA1,CA2", of(CB.class).getInterfaceInfos());
		assertListEquals("CA3", of(CC.class).getInterfaceInfos());
		assertListEquals("", of(CD.class).getInterfaceInfos());
	}

	@Test
	public void getInterfaceInfosTwice() {
		ClassInfo cc = of(CC.class);
		assertListEquals("CA3", cc.getInterfaceInfos());
		assertListEquals("CA3", cc.getInterfaceInfos());
	}




	@Test
	public void getParentInfos() {
		ClassInfo cd = of(CD.class);
		assertListEquals("CD,CC,CB", cd.getParentInfos());
		assertListEquals("CD,CC,CB", cd.getParentInfos(false,false));
		assertListEquals("CB,CC,CD", cd.getParentInfos(true,false));

		assertListEquals("CD,CC,CA3,CB,CA1,CA2", cd.getParentInfos(false,true));
		assertListEquals("CA2,CA1,CB,CA3,CC,CD", cd.getParentInfos(true,true));
	}

	//====================================================================================================
	// isParentClass(Class, Class)
	//====================================================================================================

	public interface B1 {}
	public static class B2 implements B1 {}
	public static class B3 extends B2 {}


	@Test
	public void testIsParentClass() throws Exception {

		// Strict
		assertTrue(of(B1.class).isParentOf(B2.class, true));
		assertTrue(of(B2.class).isParentOf(B3.class, true));
		assertTrue(of(Object.class).isParentOf(B3.class, true));
		assertFalse(of(B1.class).isParentOf(B1.class, true));
		assertFalse(of(B2.class).isParentOf(B2.class, true));
		assertFalse(of(B3.class).isParentOf(B3.class, true));
		assertFalse(of(B3.class).isParentOf(B2.class, true));
		assertFalse(of(B2.class).isParentOf(B1.class, true));
		assertFalse(of(B3.class).isParentOf(Object.class, true));

		// Not strict
		assertTrue(of(B1.class).isParentOf(B2.class, false));
		assertTrue(of(B2.class).isParentOf(B3.class, false));
		assertTrue(of(Object.class).isParentOf(B3.class, false));
		assertTrue(of(B1.class).isParentOf(B1.class, false));
		assertTrue(of(B2.class).isParentOf(B2.class, false));
		assertTrue(of(B3.class).isParentOf(B3.class, false));
		assertFalse(of(B3.class).isParentOf(B2.class, false));
		assertFalse(of(B2.class).isParentOf(B1.class, false));
		assertFalse(of(B3.class).isParentOf(Object.class, false));
	}


	//====================================================================================================
	// getAllMethodsParentFirst()
	//====================================================================================================
	@Test
	public void getParentMethodsParentFirst() throws Exception {
		Set<String> s = new TreeSet<>();
		ClassInfo ci = ClassInfo.of(DD.class);
		for (MethodInfo m : ci.getAllMethodInfos(true, true))
			if (! m.getName().startsWith("$"))
				s.add(m.getDeclaringClassInfo().getSimpleName() + '.' + m.getName());
		assertObjectEquals("['DA1.da1','DA2.da2','DB.da1','DB.db','DC.da2','DC.dc','DD.da2','DD.dd']", s);

		s = new TreeSet<>();
		for (MethodInfo m : ci.getAllMethodInfos())
			if (! m.getName().startsWith("$"))
				s.add(m.getDeclaringClassInfo().getSimpleName() + '.' + m.getName());
		assertObjectEquals("['DA1.da1','DA2.da2','DB.da1','DB.db','DC.da2','DC.dc','DD.da2','DD.dd']", s);
	}

	static interface DA1 {
		void da1();
	}
	static interface DA2 extends DA1 {
		void da2();
	}
	static interface DA3 {}
	static interface DA4 {}
	static abstract class DB implements DA1, DA2 {
		@Override
		public void da1() {}
		public void db() {}
	}
	static class DC extends DB implements DA3 {
		@Override
		public void da2() {}
		public void dc() {}
	}
	static class DD extends DC {
		@Override
		public void da2() {}
		public void dd() {}
	}

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
