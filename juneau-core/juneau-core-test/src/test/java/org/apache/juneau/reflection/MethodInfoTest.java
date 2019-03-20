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

import java.lang.annotation.*;

import org.junit.*;

public class MethodInfoTest {

	//====================================================================================================
	// getMethodAnnotation
	//====================================================================================================
	@Test
	public void getMethodAnnotations() throws Exception {
		assertEquals("a1", MethodInfo.create(CI3.class.getMethod("a1")).getAnnotation(TestAnnotation.class).value());
		assertEquals("a2b", MethodInfo.create(CI3.class.getMethod("a2")).getAnnotation(TestAnnotation.class).value());
		assertEquals("a3", MethodInfo.create(CI3.class.getMethod("a3", CharSequence.class)).getAnnotation(TestAnnotation.class).value());
		assertEquals("a4", MethodInfo.create(CI3.class.getMethod("a4")).getAnnotation(TestAnnotation.class).value());
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


}
