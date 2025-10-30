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
package org.apache.juneau.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.jar.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ManifestFile_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// test - Basic tests
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() throws Exception {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(new java.util.jar.Attributes.Name("Bundle-Name"), "Test Bundle");
		manifest.getMainAttributes().put(new java.util.jar.Attributes.Name("Bundle-Version"), "1.0.0");

		ManifestFile mf = new ManifestFile(manifest);

		assertEquals("1.0", mf.get("Manifest-Version"));
		assertEquals("Test Bundle", mf.get("Bundle-Name"));
		assertEquals("1.0.0", mf.get("Bundle-Version"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// test - Fluent setters
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a02_fluentSetters() throws Exception {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");

		ManifestFile mf = new ManifestFile(manifest);

		// Test inner() returns same instance for fluent chaining
		var innerMap = new HashMap<String,Object>();
		innerMap.put("test", "value");
		assertSame(mf, mf.inner(innerMap));

		// Test session() returns same instance
		BeanSession session = BeanContext.DEFAULT.getSession();
		assertSame(mf, mf.session(session));

		// Test append(String, Object) returns same instance
		assertSame(mf, mf.append("Custom-Key", "custom-value"));
		assertEquals("custom-value", mf.get("Custom-Key"));

		// Test append(Map) returns same instance
		var appendMap = new HashMap<String,Object>();
		appendMap.put("Another-Key", "another-value");
		assertSame(mf, mf.append(appendMap));
		assertEquals("another-value", mf.get("Another-Key"));

		// Test appendIf() returns same instance
		assertSame(mf, mf.appendIf(true, "Conditional-Key", "conditional-value"));
		assertEquals("conditional-value", mf.get("Conditional-Key"));
		assertSame(mf, mf.appendIf(false, "Skipped-Key", "skipped-value"));
		assertNull(mf.get("Skipped-Key"));

		// Test filtered() returns same instance
		assertSame(mf, mf.filtered(x -> x != null));

		// Test keepAll() returns same instance
		assertSame(mf, mf.keepAll("Custom-Key", "Another-Key"));

		// Test setBeanSession() returns same instance
		assertSame(mf, mf.setBeanSession(session));

		// Test modifiable() returns same instance
		assertSame(mf, mf.modifiable());

		// Test unmodifiable() returns same instance
		assertSame(mf, mf.unmodifiable());
	}

	@Test void a03_fluentChaining() throws Exception {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");

		// Test multiple fluent calls can be chained
		ManifestFile mf = new ManifestFile(manifest)
			.append("Key1", "value1")
			.append("Key2", "value2")
			.appendIf(true, "Key3", "value3");

		assertEquals("value1", mf.get("Key1"));
		assertEquals("value2", mf.get("Key2"));
		assertEquals("value3", mf.get("Key3"));
	}
}