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
package org.apache.juneau;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.junit.*;

@SuppressWarnings({"rawtypes"})
@FixMethodOrder(NAME_ASCENDING)
public class BeanContextTest {

	BeanContext bc = BeanContext.DEFAULT;

	public static interface A1 {
		public int getF1();
		public void setF1(int f1);
	}

	@Test
	public void normalCachableBean() throws ExecutableException {
		ClassMeta cm1 = bc.getClassMeta(A1.class), cm2 = bc.getClassMeta(A1.class);
		assertTrue(cm1 == cm2);
	}

	interface A2 {
		void foo(int x);
	}

	@Test
	public void lambdaExpressionsNotCached() throws ExecutableException {
		BeanContext bc = BeanContext.DEFAULT;
		A2 fi = (x) -> System.out.println(x);
		ClassMeta cm1 = bc.getClassMeta(fi.getClass()), cm2 = bc.getClassMeta(fi.getClass());
		assertTrue(cm1 != cm2);
	}

	@Test
	public void proxiesNotCached() throws ExecutableException {
		A1 a1 = bc.createBeanSession().getBeanMeta(A1.class).newBean(null);
		ClassMeta cm1 = bc.getClassMeta(a1.getClass()), cm2 = bc.getClassMeta(a1.getClass());
		assertTrue(cm1 != cm2);
	}
}