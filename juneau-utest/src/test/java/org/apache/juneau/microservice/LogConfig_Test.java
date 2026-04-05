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
package org.apache.juneau.microservice;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

import org.apache.juneau.*;
import org.apache.juneau.microservice.resources.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link LogConfig}.
 */
class LogConfig_Test extends TestBase {

	@Test void a01_create() {
		var x = LogConfig.create();
		assertNotNull(x);
	}

	@Test void a02_append() {
		var x = LogConfig.create().append();
		assertSame(Boolean.TRUE, x.append);
	}

	@Test void a03_consoleLevel() {
		assertBean(LogConfig.create().consoleLevel(Level.WARNING), "consoleLevel", "WARNING");
	}

	@Test void a04_count() {
		assertBean(LogConfig.create().count(5), "count", "5");
	}

	@Test void a05_fileLevel() {
		assertBean(LogConfig.create().fileLevel(Level.SEVERE), "fileLevel", "SEVERE");
	}

	@Test void a06_formatter() {
		Formatter f = new LogEntryFormatter("[{date} {level}] {msg}%n", "yyyy.MM.dd hh:mm:ss", false);
		var x = LogConfig.create().formatter(f);
		assertSame(f, x.formatter);
	}

	@Test void a07_level() {
		var x = LogConfig.create().level("org.apache.juneau", Level.FINE);
		assertEquals(Level.FINE, x.levels.get("org.apache.juneau"));
	}

	@Test void a08_levels_map() {
		var map = Map.of("foo.bar", Level.INFO, "baz.qux", Level.FINE);
		var x = LogConfig.create().levels(map);
		assertEquals(Level.INFO, x.levels.get("foo.bar"));
		assertEquals(Level.FINE, x.levels.get("baz.qux"));
	}

	@Test void a09_limit() {
		assertBean(LogConfig.create().limit(1024 * 1024), "limit", "1048576");
	}

	@Test void a10_logDir() {
		assertBean(LogConfig.create().logDir("/var/log/myservice"), "logDir", "/var/log/myservice");
	}

	@Test void a11_logFile() {
		assertBean(LogConfig.create().logFile("myservice.log"), "logFile", "myservice.log");
	}

	@Test void a12_copy() {
		var x = LogConfig.create()
			.append()
			.consoleLevel(Level.INFO)
			.count(3)
			.fileLevel(Level.WARNING)
			.level("test.logger", Level.FINE)
			.limit(500000)
			.logDir("/logs")
			.logFile("service.log");
		var copy = x.copy();
		assertNotSame(x, copy);
		assertSame(Boolean.TRUE, copy.append);
		assertEquals(Level.FINE, copy.levels.get("test.logger"));
		assertBean(copy, "consoleLevel,count,fileLevel,limit,logDir,logFile", "INFO,3,WARNING,500000,/logs,service.log");
	}

	@Test void a13_fluentChaining() {
		var x = LogConfig.create()
			.logFile("app.log")
			.logDir("/tmp")
			.limit(10000)
			.count(2)
			.consoleLevel(Level.INFO)
			.fileLevel(Level.FINE)
			.append();
		assertBean(x, "append,consoleLevel,count,fileLevel,limit,logDir,logFile", "true,INFO,2,FINE,10000,/tmp,app.log");
	}
}
