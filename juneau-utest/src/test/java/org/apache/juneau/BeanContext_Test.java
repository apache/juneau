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

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.json.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.jupiter.api.*;

class BeanContext_Test extends SimpleTestBase {

	BeanContext bc = BeanContext.DEFAULT;
	BeanSession bs = BeanContext.DEFAULT_SESSION;

	public interface A1 {
		int getF1();
		void setF1(int f1);
	}

	@Test void a01_normalCachableBean() throws ExecutableException {
		var cm1 = bc.getClassMeta(A1.class);
		var cm2 = bc.getClassMeta(A1.class);
		assertSame(cm1, cm2);
	}

	interface A2 {
		void foo(int x);
	}

	@Test void a02_lambdaExpressionsNotCached() throws ExecutableException {
		var bc2 = BeanContext.DEFAULT;
		var fi = (A2)System.out::println;
		var cm1 = bc2.getClassMeta(fi.getClass());
		var cm2 = bc2.getClassMeta(fi.getClass());
		assertNotSame(cm1, cm2);
	}

	@Test void a03_proxiesNotCached() throws ExecutableException {
		var a1 = bs.getBeanMeta(A1.class).newBean(null);
		var cm1 = bc.getClassMeta(a1.getClass());
		var cm2 = bc.getClassMeta(a1.getClass());
		assertNotSame(cm1, cm2);
	}

	@Test void b01_ignoreUnknownEnumValues() {
		var p1 = JsonParser.DEFAULT;
		assertThrowsWithMessage(Exception.class, "Could not resolve enum value 'UNKNOWN' on class 'org.apache.juneau.testutils.pojos.TestEnum'", () -> p1.parse("'UNKNOWN'", TestEnum.class));

		var p2 = JsonParser.create().ignoreUnknownEnumValues().build();
		assertNull(p2.parse("'UNKNOWN'", TestEnum.class));
	}
}