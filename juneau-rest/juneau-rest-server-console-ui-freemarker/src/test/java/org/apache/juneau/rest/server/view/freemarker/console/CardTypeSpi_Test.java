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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

class CardTypeSpi_Test extends TestBase {

	private static final String SPI = "META-INF/services/org.apache.juneau.rest.server.console.CardTypeHandler";

	@Test void a01_mainServicesFileListsBothBuiltIns() throws Exception {
		var lines = Files.readAllLines(Path.of(System.getProperty("basedir", "."), "target/classes", SPI));
		assertTrue(lines.contains(DatatablesCardType.class.getName()), lines::toString);
		assertTrue(lines.contains(ConsoleOutputCardType.class.getName()), lines::toString);
	}

	@Test void a02_noTestClasspathServicesFile() {
		assertFalse(Files.exists(Path.of(System.getProperty("basedir", "."), "target/test-classes", SPI)),
			"a test-only SPI file would let a01/a03 pass without the packaged one");
	}

	@Test void a03_standardRegistryResolvesBuiltIns() {
		assertTrue(CardTypeRegistry.standard().handler("datatables") instanceof DatatablesCardType);
		assertTrue(CardTypeRegistry.standard().handler("console-output") instanceof ConsoleOutputCardType);
		var found = new ArrayList<String>();
		ServiceLoader.load(CardTypeHandler.class).forEach(h -> found.add(h.type()));
		assertTrue(found.containsAll(List.of("datatables", "console-output")), found::toString);
	}
}
