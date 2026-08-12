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
package org.apache.juneau.rest.client.classic.remote;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Exercises both branches of the private {@code getMessage(Class, Method, String)} helper via the two public
 * constructors that call it: the {@code Method}-arg constructor (method name included in the message) and the
 * {@code Class}-arg constructor (method name omitted, since it passes a {@code null} method).
 */
class RemoteMetadataException_Test {

	interface Foo {
		void bar();
	}

	@Test void a01_methodConstructor_messageIncludesMethodName() throws Exception {
		var m = Foo.class.getMethod("bar");
		var e = new RemoteMetadataException(m, "Bad thing: %s", "reason");
		assertTrue(e.getMessage().contains("on method bar"), "Unexpected message: " + e.getMessage());
		assertTrue(e.getMessage().contains("Bad thing: reason"), "Unexpected message: " + e.getMessage());
	}

	@Test void a02_classConstructor_messageOmitsMethodName() {
		var e = new RemoteMetadataException(Foo.class, "Bad thing: %s", "reason");
		assertFalse(e.getMessage().contains("on method"), "Unexpected message: " + e.getMessage());
		assertTrue(e.getMessage().contains("Bad thing: reason"), "Unexpected message: " + e.getMessage());
	}
}
