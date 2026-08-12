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

import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.http.remote.*;
import org.junit.jupiter.api.*;

/**
 * Exercises the {@code @Remote} annotation-parsing branches of {@link RemoteMeta}'s constructor that aren't
 * reached by other remote-proxy tests: {@code value()} (as opposed to {@code path()}), a non-empty
 * {@code versionHeader()}, and the {@code headerList()} handling -- both the "concrete class specified" branch
 * and the "left at its {@code Void.class} default" branch (the latter needs an {@code @Remote} annotation to be
 * present at all, which the no-annotation case below doesn't provide, since the whole {@code headerList()} check
 * is inside the per-{@code @Remote} loop).
 */
class RemoteMeta_Test {

	@Remote(value = "myPath", version = "1.0", versionHeader = "X-Client-Version", headerList = HeaderList.class)
	interface RemoteWithValueAndCustomVersionHeader {}

	@Test void a01_pathFromValue_and_customVersionHeader_and_headerListClass() {
		var meta = new RemoteMeta(RemoteWithValueAndCustomVersionHeader.class);
		var versionHeader = meta.getHeaders().stream().filter(h -> h.getName().equals("X-Client-Version")).findFirst().orElse(null);
		assertNotNull(versionHeader, "Custom version header not found");
		assertEquals("1.0", versionHeader.getValue());
	}

	interface NoRemoteAnnotation {}

	@Test void a02_noRemoteAnnotation_emptyHeaders() {
		var meta = new RemoteMeta(NoRemoteAnnotation.class);
		assertTrue(meta.getHeaders().isEmpty());
	}

	@Remote(path = "myPath")
	interface RemoteWithDefaultHeaderList {}

	@Test void a03_remoteAnnotationPresent_headerListLeftAtDefault_isNoOp() {
		var meta = new RemoteMeta(RemoteWithDefaultHeaderList.class);
		assertTrue(meta.getHeaders().isEmpty());
	}

	@Remote(path = "myPath", headerList = String.class)
	interface RemoteWithNonHeaderListClass {}

	@Test void a04_headerListClass_notAHeaderListSubtype_isNoOp() {
		var meta = new RemoteMeta(RemoteWithNonHeaderListClass.class);
		assertTrue(meta.getHeaders().isEmpty());
	}
}
