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

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.*;

import org.junit.jupiter.api.*;

/**
 * Tests the default exclusion pattern of {@link BasicStaticFiles}.
 */
class BasicStaticFiles_Exclude_Test {

	private static final Pattern P = Pattern.compile(BasicStaticFiles.EXCLUDE_PATTERN);

	@Test void a01_excludedNames() {
		for (var n : new String[]{"Foo.class", "a.properties", ".env", "sub/.git", "sub/.htpasswd", "app.yml", "app.YAML", "pom.xml", "store.jks", "k.p12", "k.pem", "id.key", "a.env", "a.cfg", "a.conf", "a.bak"})
			assertTrue(P.matcher(n).matches(), n);
	}

	@Test void a02_servedNames() {
		for (var n : new String[]{"themes/devops.css", "scripts/a.js", "images/a.png", "index.html", "sub/dir.name/file.txt"})
			assertFalse(P.matcher(n).matches(), n);
	}
}
