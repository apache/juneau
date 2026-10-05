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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;

/**
 * Byte-for-byte golden-file assertions for refactor safety nets.
 *
 * <p>
 * A missing golden is written and the test fails, so a first run never passes silently. Re-generate deliberately
 * with {@code -Djuneau.golden.update=true}, then review the diff before re-running without it.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	GoldenFiles.<jsm>assertGolden</jsm>(<js>"chrome-css"</js>, <js>"gray"</js>, <jv>body</jv>);
 * 	<jc>// compares against src/test/resources/golden/chrome-css/gray.txt</jc>
 * </p>
 */
final class GoldenFiles {

	private static final boolean UPDATE = Boolean.getBoolean("juneau.golden.update");

	private GoldenFiles() {}

	static void assertGolden(String dir, String name, String actual) throws IOException {
		var file = Path.of("src/test/resources/golden", dir, name + ".txt");
		if (UPDATE || ! Files.exists(file)) {
			Files.createDirectories(file.getParent());
			Files.writeString(file, actual, StandardCharsets.UTF_8);
			fail("Golden written: " + file.toAbsolutePath() + " - review it, then re-run without -Djuneau.golden.update.");
		}
		assertEquals(Files.readString(file, StandardCharsets.UTF_8), actual, () -> "Golden mismatch: " + file);
	}
}
