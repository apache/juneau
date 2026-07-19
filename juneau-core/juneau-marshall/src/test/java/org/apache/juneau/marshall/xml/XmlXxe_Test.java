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
package org.apache.juneau.marshall.xml;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Verifies that {@link XmlReader} is not vulnerable to XML External Entity (XXE) attacks.
 *
 * <p>
 * The default (non-validating) parser disables DTD processing entirely, so any document carrying a
 * {@code <!DOCTYPE ...>} declaration (the vector for external/parameter-entity attacks) is rejected outright and no
 * external entity is ever resolved.
 */
class XmlXxe_Test extends TestBase {

	@Test void a01_externalEntityNotResolved(@TempDir Path tempDir) throws Exception {
		var secret = tempDir.resolve("secret.txt");
		Files.writeString(secret, "TOP-SECRET-CONTENTS");
		var uri = secret.toUri().toString();

		var xml = "<?xml version=\"1.0\"?>"
			+ "<!DOCTYPE A [<!ENTITY xxe SYSTEM \"" + uri + "\">]>"
			+ "<A>&xxe;</A>";

		// DTD processing is disabled, so parsing must fail rather than resolving the external entity.
		var e = assertThrows(Exception.class, () -> XmlParser.DEFAULT.read(xml, Json5Map.class));

		// The secret file contents must never appear anywhere in the failure.
		assertFalse(e.toString().contains("TOP-SECRET-CONTENTS"), "External entity was resolved");
	}

	@Test void a02_doctypeRejected() {
		var xml = "<?xml version=\"1.0\"?>"
			+ "<!DOCTYPE A [<!ELEMENT A ANY>]>"
			+ "<A>x</A>";

		// Any DOCTYPE declaration is rejected on the non-validating path.
		assertThrows(Exception.class, () -> XmlParser.DEFAULT.read(xml, Json5Map.class));
	}

	@Test void a03_normalDocumentStillParses() throws Exception {
		var xml = "<A b='1'><c>2</c></A>";
		var m = XmlParser.DEFAULT.read(xml, Json5Map.class);
		assertEquals("{b:'1',c:'2'}", m.toString());
	}
}
