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
package org.apache.juneau.marshall.parser;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Tests that parse failure messages do not quote the input, unless the input snippet is explicitly enabled.
 */
class ParseException_NoInput_Test extends TestBase {

	@Test void a01_debugMessageOmitsInput() {
		var p = JsonParser.create().debug().build();
		var e = assertThrows(ParseException.class, () -> p.read("{secretValue:", Object.class));
		assertFalse(e.getMessage().contains("secretValue"), e.getMessage());
	}

	@Test void a02_defaultOmitsInput() {
		var p = JsonParser.create().build();
		var e = assertThrows(ParseException.class, () -> p.read("{secretValue:", Object.class));
		assertFalse(e.getMessage().contains("secretValue"), e.getMessage());
	}

	@Test void a03_optInIncludesInput() {
		var p = JsonParser.create().debug().includeInputInErrors().build();
		var e = assertThrows(ParseException.class, () -> p.read("{secretValue:", Object.class));
		assertTrue(e.getMessage().contains("secretValue"), e.getMessage());
	}

	@Test void a04_explicitOffWithDebugAddsNoSnippet() {
		var p = JsonParser.create().includeInputInErrors(false).debug().build();
		var e = assertThrows(ParseException.class, () -> p.read("{secretValue:", Object.class));
		assertFalse(e.getMessage().contains("secretValue"), e.getMessage());
	}

	@ParserConfig(includeInputInErrors="true")
	static class OptedIn {}

	@ParserConfig
	static class NotOptedIn {}

	@Test void a05_annotationOptInIncludesInput() {
		var al = AnnotationWorkList.of(VarResolver.DEFAULT.createSession(), rstream(ClassInfo.of(OptedIn.class).getAnnotations()));
		var p = JsonParser.create().debug().apply(al).build();
		var e = assertThrows(ParseException.class, () -> p.read("{secretValue:", Object.class));
		assertTrue(e.getMessage().contains("secretValue"), e.getMessage());
	}

	@Test void a06_annotationWithoutAttributeOmitsInput() {
		var al = AnnotationWorkList.of(VarResolver.DEFAULT.createSession(), rstream(ClassInfo.of(NotOptedIn.class).getAnnotations()));
		var p = JsonParser.create().debug().apply(al).build();
		var e = assertThrows(ParseException.class, () -> p.read("{secretValue:", Object.class));
		assertFalse(e.getMessage().contains("secretValue"), e.getMessage());
	}
}
