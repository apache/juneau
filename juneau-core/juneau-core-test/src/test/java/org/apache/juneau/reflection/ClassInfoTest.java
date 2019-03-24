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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.*;

public class ClassInfoTest {

	//====================================================================================================
	// isParentClass(Class, Class)
	//====================================================================================================
	@Test
	public void testIsParentClass() throws Exception {

		// Strict
		assertTrue(getClassInfo(A.class).isParentOf(A1.class, true));
		assertTrue(getClassInfo(A1.class).isParentOf(A2.class, true));
		assertTrue(getClassInfo(Object.class).isParentOf(A2.class, true));
		assertFalse(getClassInfo(A.class).isParentOf(A.class, true));
		assertFalse(getClassInfo(A1.class).isParentOf(A1.class, true));
		assertFalse(getClassInfo(A2.class).isParentOf(A2.class, true));
		assertFalse(getClassInfo(A2.class).isParentOf(A1.class, true));
		assertFalse(getClassInfo(A1.class).isParentOf(A.class, true));
		assertFalse(getClassInfo(A2.class).isParentOf(Object.class, true));

		// Not strict
		assertTrue(getClassInfo(A.class).isParentOf(A1.class, false));
		assertTrue(getClassInfo(A1.class).isParentOf(A2.class, false));
		assertTrue(getClassInfo(Object.class).isParentOf(A2.class, false));
		assertTrue(getClassInfo(A.class).isParentOf(A.class, false));
		assertTrue(getClassInfo(A1.class).isParentOf(A1.class, false));
		assertTrue(getClassInfo(A2.class).isParentOf(A2.class, false));
		assertFalse(getClassInfo(A2.class).isParentOf(A1.class, false));
		assertFalse(getClassInfo(A1.class).isParentOf(A.class, false));
		assertFalse(getClassInfo(A2.class).isParentOf(Object.class, false));
	}

	public interface A {}

	public static class A1 implements A {}

	public static class A2 extends A1 {}

	//====================================================================================================
	// getAllMethodsParentFirst()
	//====================================================================================================
	@Test
	public void getParentMethodsParentFirst() throws Exception {
		Set<String> s = new TreeSet<>();
		ClassInfo ci = ClassInfo.of(DD.class);
		for (MethodInfo m : ci.getAllMethods(true, true))
			if (! m.getName().startsWith("$"))
				s.add(m.getDeclaringClass().getSimpleName() + '.' + m.getName());
		assertObjectEquals("['DA1.da1','DA2.da2','DB.da1','DB.db','DC.da2','DC.dc','DD.da2','DD.dd']", s);

		s = new TreeSet<>();
		for (MethodInfo m : ci.getAllMethods())
			if (! m.getName().startsWith("$"))
				s.add(m.getDeclaringClass().getSimpleName() + '.' + m.getName());
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
		for (FieldInfo f : ci.getAllFieldsParentFirst()) {
			if (! f.getName().startsWith("$"))
				s.add(f.getDeclaringClass().getSimpleName() + '.' + f.getName());
		}
		assertObjectEquals("['EA.a1','EB.a1','EB.b1']", s);

		s = new TreeSet<>();
		for (FieldInfo f : ci.getAllFields()) {
			if (! f.getName().startsWith("$"))
				s.add(f.getDeclaringClass().getSimpleName() + '.' + f.getName());
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
	// getParentClassesParentFirst()
	//====================================================================================================
	@Test
	public void getParentClassesParentFirst() throws Exception {
		ClassInfo ci = getClassInfo(CD.class);

		Set<String> s = new TreeSet<>();
		for (ClassInfo c : ci.getParents(true, true)) {
			s.add(c.getSimpleName());
		}
		assertObjectEquals("['CA1','CA2','CA3','CB','CC','CD']", s);

		s = new TreeSet<>();
		for (ClassInfo c : ci.getParents(true, false)) {
			s.add(c.getSimpleName());
		}
		assertObjectEquals("['CB','CC','CD']", s);

		s = new TreeSet<>();
		for (ClassInfo c : ci.getParents(false, true)) {
			s.add(c.getSimpleName());
		}
		assertObjectEquals("['CA1','CA2','CA3','CB','CC','CD']", s);

		s = new TreeSet<>();
		for (ClassInfo c : ci.getParents(false, false)) {
			s.add(c.getSimpleName());
		}
		assertObjectEquals("['CB','CC','CD']", s);
	}

	static interface CA1 {}
	static interface CA2 extends CA1 {}
	static interface CA3 {}
	static interface CA4 {}
	static class CB implements CA1, CA2 {}
	static class CC extends CB implements CA3 {}
	static class CD extends CC {}

	//====================================================================================================
	// getSimpleName()
	//====================================================================================================

	@Test
	public void getShortName() throws Exception {
		assertEquals("ClassInfoTest.G1", getClassInfo(G1.class).getShortName());
		assertEquals("ClassInfoTest.G2", getClassInfo(G2.class).getShortName());
	}

	public class G1 {}
	public static class G2 {}


}
