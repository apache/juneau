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
package org.apache.juneau.utils;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.junit.Assert.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.junit.*;

public class ReflectionUtilsTest {

	@Target({PARAMETER,TYPE})
	@Retention(RUNTIME)
	public static @interface IA {
		public String value();
	}

	public static interface A {
		public void doX(@IA("0") A01 x);
	}

	@IA("1") public static class A01 extends A02 {}
	@IA("2") public static class A02 implements A03, A04 {}
	@IA("3") public static interface A03 {}
	@IA("4") public static interface A04 {}

	@Test
	public void getAnnotationsOnParameter() throws Exception {
		ObjectList l = new ObjectList();
		for (IA ia : ReflectionUtils.getAnnotations(IA.class, A.class.getMethod("doX", A01.class), 0)) {
			l.add(ia.value());
		}
		assertEquals("['0','1','2','3','4']", l.toString());
	}

	@Target({PARAMETER,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface IB {
		public String value();
	}

	public static interface B {
		public void doX(@IB("0") B01 x);
	}

	@IB("1") public static class B01 extends B02 {}
	@IB("2") public static class B02 implements B03, B04 {}
	@IB("3") public static interface B03 {}
	@IB("4") public static interface B04 {}

	@Test
	public void getAnnotationsOnParameterInherited() throws Exception {
		ObjectList l = new ObjectList();
		for (IB ib : ReflectionUtils.getAnnotations(IB.class, B.class.getMethod("doX", B01.class), 0)) {
			l.add(ib.value());
		}
		assertEquals("['0','1','2','3','4']", l.toString());
	}
}
