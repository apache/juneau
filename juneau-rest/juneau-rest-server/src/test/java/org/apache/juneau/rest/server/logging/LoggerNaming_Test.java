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
package org.apache.juneau.rest.server.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link LoggerNaming}.
 *
 * @since 10.0.0
 */
class LoggerNaming_Test {

	static class A00_UserClass {}
	static class A01_Nested {
		static class Inner {}
	}
	static class A02_User$$SpringCGLIB$$ extends A00_UserClass {}
	static class A03_User$$EnhancerBySpringCGLIB$$ extends A00_UserClass {}
	static class A04_User$$EnhancerByCGLIB$$ extends A00_UserClass {}
	static class A05_User$$FastClassBySpringCGLIB$$ extends A00_UserClass {}
	static class A06_User$ByteBuddy$ extends A00_UserClass {}
	static class A07_User$Proxy0 extends A00_UserClass {}
	static class A08_User$HibernateProxy$1 extends A00_UserClass {}
	static class A09_User$MockitoMock$1 extends A00_UserClass {}
	static class A10_ProxyChain$$SpringCGLIB$$ extends A06_User$ByteBuddy$ {}

	@Test void a01_knownProxyInfixes_resolveToUserSuperclassName() {
		var expected = A00_UserClass.class.getName();
		assertEquals(expected, LoggerNaming.userClassName(A02_User$$SpringCGLIB$$.class));
		assertEquals(expected, LoggerNaming.userClassName(A03_User$$EnhancerBySpringCGLIB$$.class));
		assertEquals(expected, LoggerNaming.userClassName(A04_User$$EnhancerByCGLIB$$.class));
		assertEquals(expected, LoggerNaming.userClassName(A05_User$$FastClassBySpringCGLIB$$.class));
		assertEquals(expected, LoggerNaming.userClassName(A06_User$ByteBuddy$.class));
		assertEquals(expected, LoggerNaming.userClassName(A07_User$Proxy0.class));
		assertEquals(expected, LoggerNaming.userClassName(A08_User$HibernateProxy$1.class));
		assertEquals(expected, LoggerNaming.userClassName(A09_User$MockitoMock$1.class));
	}

	@Test void a02_superclassWalk_handlesMultipleProxyLayers() {
		assertEquals(A00_UserClass.class.getName(), LoggerNaming.userClassName(A10_ProxyChain$$SpringCGLIB$$.class));
	}

	@Test void a03_nonProxyClassName_isUnchanged() {
		assertEquals(A00_UserClass.class.getName(), LoggerNaming.userClassName(A00_UserClass.class));
	}

	@Test void a04_nestedClassName_isPreserved() {
		assertEquals(A01_Nested.Inner.class.getName(), LoggerNaming.userClassName(A01_Nested.Inner.class));
	}
}
