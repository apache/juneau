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
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.junit.*;

public class MethodInfoTest {

	//====================================================================================================
	// getMethodAnnotation
	//====================================================================================================
	@Test
	public void getMethodAnnotations() throws Exception {
		assertEquals("a1", MethodInfo.of(CI3.class.getMethod("a1")).getAnnotation(TestAnnotation.class).value());
		assertEquals("a2b", MethodInfo.of(CI3.class.getMethod("a2")).getAnnotation(TestAnnotation.class).value());
		assertEquals("a3", MethodInfo.of(CI3.class.getMethod("a3", CharSequence.class)).getAnnotation(TestAnnotation.class).value());
		assertEquals("a4", MethodInfo.of(CI3.class.getMethod("a4")).getAnnotation(TestAnnotation.class).value());
	}

	public static interface CI1 {
		@TestAnnotation("a1")
		void a1();
		@TestAnnotation("a2a")
		void a2();
		@TestAnnotation("a3")
		void a3(CharSequence foo);

		void a4();
	}

	public static class CI2 implements CI1 {
		@Override
		public void a1() {}
		@Override
		@TestAnnotation("a2b")
		public void a2() {}
		@Override
		public void a3(CharSequence s) {}
		@Override
		public void a4() {}
	}

	public static class CI3 extends CI2 {
		@Override
		public void a1() {}
		@Override public void a2() {}
		@Override
		@TestAnnotation("a4")
		public void a4() {}
	}

	@Target(METHOD)
	@Retention(RUNTIME)
	public @interface TestAnnotation {
		String value() default "";
	}

	//====================================================================================================
	// getAnnotations()
	//====================================================================================================

	@Target({PARAMETER,TYPE})
	@Retention(RUNTIME)
	public static @interface HI1 {
		public String value();
	}

	public static interface HA {
		public void doX(@HI1("0") HA01 x);
	}

	@HI1("1") public static class HA01 extends HA02 {}
	@HI1("2") public static class HA02 implements HA03, HA04 {}
	@HI1("3") public static interface HA03 {}
	@HI1("4") public static interface HA04 {}

	@Test
	public void getAnnotationsOnParameter() throws Exception {
		ObjectList l = new ObjectList();
		MethodParamInfo mpi = MethodInfo.of(HA.class.getMethod("doX", HA01.class)).getParam(0);
		for (HI1 ia : mpi.getAnnotations(HI1.class)) {
			l.add(ia.value());
		}
		assertEquals("['0','1','2','3','4']", l.toString());
	}

	@Target({PARAMETER,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface HI2 {
		public String value();
	}

	public static interface HB {
		public void doX(@HI2("0") HB01 x);
	}

	@HI2("1") public static class HB01 extends HB02 {}
	@HI2("2") public static class HB02 implements HB03, HB04 {}
	@HI2("3") public static interface HB03 {}
	@HI2("4") public static interface HB04 {}

	@Test
	public void getAnnotationsOnParameterInherited() throws Exception {
		ObjectList l = new ObjectList();
		MethodParamInfo mpi = MethodInfo.of(HB.class.getMethod("doX", HB01.class)).getParam(0);
		for (HI2 ib : mpi.getAnnotations(HI2.class)) {
			l.add(ib.value());
		}
		assertEquals("['0','1','2','3','4']", l.toString());
	}

	//====================================================================================================
	// findMatchingMethods()
	//====================================================================================================

	public static interface I1 {
		public int foo(int x);
		public int foo(String x);
		public int foo();
	}
	public static class I2 {
		public int foo(int x) { return 0; }
		public int foo(String x) {return 0;}
		public int foo() {return 0;}
	}
	public static class I3 extends I2 implements I1 {
		@Override
		public int foo(int x) {return 0;}
		@Override
		public int foo(String x) {return 0;}
		@Override
		public int foo() {return 0;}
	}

	@Test
	public void findMatchingMethods() throws Exception {
		MethodInfo mi = MethodInfo.of(I3.class.getMethod("foo", int.class));
		assertObjectEquals("['public int org.apache.juneau.reflection.MethodInfoTest$I3.foo(int)','public int org.apache.juneau.reflection.MethodInfoTest$I2.foo(int)','public abstract int org.apache.juneau.reflection.MethodInfoTest$I1.foo(int)']", mi.getMatching());
	}
}
