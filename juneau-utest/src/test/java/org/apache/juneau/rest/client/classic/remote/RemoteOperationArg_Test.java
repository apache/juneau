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

import static org.apache.juneau.commons.httppart.HttpPartType.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link RemoteOperationArg#create(ParameterInfo)} factory,
 * specifically the branches not yet covered by integration tests.
 */
class RemoteOperationArg_Test {

	// Interface whose single method covers the PathRemainder branch
	interface PathRemainderIface {
		void get(@PathRemainder String remainder);
	}

	@Test
	void a01_create_pathRemainder_returnsPATHWithSlashStar() throws Exception {
		var m = MethodInfo.of(PathRemainderIface.class.getMethod("get", String.class));
		var pi = m.getParameter(0);
		var arg = RemoteOperationArg.create(pi);
		assertNotNull(arg, "create() should return non-null for @PathRemainder param");
		assertEquals(PATH, arg.getPartType());
		assertEquals("/*", arg.getName());
	}

	// Verify null is returned for unannotated parameters
	interface NoAnnotationIface {
		void get(String plain);
	}

	@Test
	void a02_create_noAnnotation_returnsNull() throws Exception {
		var m = MethodInfo.of(NoAnnotationIface.class.getMethod("get", String.class));
		var pi = m.getParameter(0);
		assertNull(RemoteOperationArg.create(pi));
	}

	// Verify @Path branch (already covered upstream, but check getName / getSchema)
	interface PathIface {
		void get(@Path("id") String id);
	}

	@Test
	void a03_create_path_returnsCorrectArg() throws Exception {
		var m = MethodInfo.of(PathIface.class.getMethod("get", String.class));
		var pi = m.getParameter(0);
		var arg = RemoteOperationArg.create(pi);
		assertNotNull(arg);
		assertEquals(PATH, arg.getPartType());
		assertEquals("id", arg.getName());
		assertEquals(0, arg.getIndex());
		assertNotNull(arg.getSchema());
	}
}
