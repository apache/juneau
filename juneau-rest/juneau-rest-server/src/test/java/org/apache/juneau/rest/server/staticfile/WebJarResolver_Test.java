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
package org.apache.juneau.rest.server.staticfile;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class WebJarResolver_Test extends TestBase {

	private static final String G = "org.example.test";

	@Test void a01_version_readsPomProperties() {
		assertString("1.2.3", WebJarResolver.version(G, "fakejar"));
	}

	@Test void a02_resolvePath_fillsInVersion() {
		assertString("fakejar/1.2.3/fake.js", WebJarResolver.resolvePath(WebJarResolver.asset(G, "fakejar", "fakejar/{version}/fake.js")));
	}

	@Test void a03_resolvePath_versionlessLayout() {
		assertString("fakejar/plain.js", WebJarResolver.resolvePath(WebJarResolver.asset(G, "fakejar", "fakejar/plain.js")));
	}

	@Test void a04_missingWebJar_namesCoordinates() {
		var e = assertThrows(IllegalStateException.class, () -> WebJarResolver.version(G, "nope"));
		assertString("needs org.example.test:nope on the classpath (no META-INF/maven/org.example.test/nope/pom.properties).", e.getMessage());
	}

	@Test void a05_blankVersion_fails() {
		var e = assertThrows(IllegalStateException.class, () -> WebJarResolver.version(G, "noversion"));
		assertString("needs org.example.test:noversion on the classpath (META-INF/maven/org.example.test/noversion/pom.properties has no version).", e.getMessage());
	}

	@Test void a06_missingFile_namesPath() {
		var asset = WebJarResolver.asset(G, "fakejar", "fakejar/{version}/missing.js");
		var e = assertThrows(IllegalStateException.class, () -> WebJarResolver.resolvePath(asset));
		assertString("needs WebJar file 'fakejar/1.2.3/missing.js' (org.example.test:fakejar:1.2.3), which is not on the classpath.", e.getMessage());
	}

	@Test void a07_malformedAsset_fails() {
		var e = assertThrows(IllegalArgumentException.class, () -> WebJarResolver.resolvePath("fakejar/fake.js"));
		assertString("WebJar asset must be 'groupId:artifactId:path'; got 'fakejar/fake.js'.", e.getMessage());
	}
}
