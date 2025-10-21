/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.utils.*;
import org.junit.jupiter.api.*;

class ValueTest extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Value defined on parent class.
	//-----------------------------------------------------------------------------------------------------------------

	public static class A extends Value<A1>{}
	public static class A1 {}

	@Test void a01_testSubclass() {
		assertEquals(A1.class, ClassUtils.getParameterType(A.class));
	}
}