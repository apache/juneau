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
package org.apache.juneau.rest.server.terminal;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.runreport.*;
import org.apache.juneau.rest.server.terminal.TerminalProcess.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.io.*;

/**
 * {@link TerminalProcess} over the real {@code juneau_run.py --pty}: the child sees a terminal of the given size, the
 * log keeps every byte, and the events carry {@code rawOffset}, the exit code and {@code done}.
 */
@DisabledOnOs(OS.WINDOWS)
class TerminalProcessPty_Test extends TestBase {

	@TempDir Path dir;

	/** {@code juneau-run/src/main/python/juneau_run.py}, from {@code basedir} or the working directory. */
	static Path script() {
		var basedir = System.getProperty("basedir");
		for (var p : List.of(Path.of(basedir == null ? "." : basedir, "../../juneau-run/src/main/python/juneau_run.py"), Path.of("juneau-run/src/main/python/juneau_run.py")))
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		throw new AssertionError("cannot find juneau-run/src/main/python/juneau_run.py");
	}

	@Test void a01_realRunnerUnderAPty() throws Exception {
		var path = System.getenv("PATH");
		assumeTrue(TerminalProcess.findOnPath("python3", path) != null, "python3 not on the PATH - skipped");
		var host = new Host(System.getProperty("os.name"), script().toString(), null, path);
		var cmd = List.of("sh", "-c", "printf '\\033[31mred\\033[0m\\n'; stty size; printf '10%%\\r100%%\\n'; exit 3");
		try (var p = TerminalProcess.start("generic", cmd, 100, 30, dir, host)) {
			assertEquals(3, p.waitFor(60, TimeUnit.SECONDS));
			var chunk = p.terminal().read(0, 1 << 20);
			var text = new String(chunk.bytes(), UTF_8);
			assertTrue(text.startsWith("\u001b[31mred\u001b[0m\r\n30 100\r\n10%\r100%\r\n"), text);
			assertTrue(chunk.done(), "done once the process exits");
			assertEquals(100, chunk.cols());
			assertEquals(30, chunk.rows());
			var events = p.events().page(null, 100).events().stream().map(RunEvent::toContractMap).toList();
			var kinds = events.stream().map(e -> e.get("ev")).toList();
			assertEquals("step", kinds.get(0), events::toString);
			assertEquals(0L, ((Number)events.get(0).get("rawOffset")).longValue(), events::toString);
			assertTrue(events.stream().anyMatch(e -> "end".equals(e.get("ev")) && Objects.equals(3L, ((Number)e.getOrDefault("exit", -1L)).longValue())), events::toString);
			var last = events.get(events.size() - 1);
			assertEquals(List.of("done", "fail"), List.of(last.get("ev"), last.get("status")), events::toString);
		}
	}
}
