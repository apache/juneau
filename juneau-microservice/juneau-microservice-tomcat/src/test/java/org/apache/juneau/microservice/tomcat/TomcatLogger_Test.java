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
package org.apache.juneau.microservice.tomcat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link TomcatLogger}.
 */
class TomcatLogger_Test extends TestBase {

	@Test void a01_defaultConstructor() {
		var l = new TomcatLogger();
		assertEquals("org.apache.juneau.marshall.microservice.tomcat", l.getName());
	}

	@Test void a02_namedConstructor() {
		var l = new TomcatLogger("test.tomcat.logger.name");
		assertEquals("test.tomcat.logger.name", l.getName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — isXEnabled level checks
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_levelChecks_atAll() {
		var l = new TomcatLogger("test.tomcat.b01");
		Logger.getLogger("test.tomcat.b01").setLevel(Level.ALL);
		assertTrue(l.isTraceEnabled());
		assertTrue(l.isDebugEnabled());
		assertTrue(l.isInfoEnabled());
		assertTrue(l.isWarnEnabled());
		assertTrue(l.isErrorEnabled());
		assertTrue(l.isFatalEnabled());
	}

	@Test void b02_levelChecks_atOff() {
		var l = new TomcatLogger("test.tomcat.b02");
		Logger.getLogger("test.tomcat.b02").setLevel(Level.OFF);
		assertFalse(l.isTraceEnabled());
		assertFalse(l.isDebugEnabled());
		assertFalse(l.isInfoEnabled());
		assertFalse(l.isWarnEnabled());
		assertFalse(l.isErrorEnabled());
		assertFalse(l.isFatalEnabled());
	}

	@Test void b03_levelChecks_atInfo() {
		var l = new TomcatLogger("test.tomcat.b03");
		Logger.getLogger("test.tomcat.b03").setLevel(Level.INFO);
		assertFalse(l.isTraceEnabled());
		assertFalse(l.isDebugEnabled());
		assertTrue(l.isInfoEnabled());
		assertTrue(l.isWarnEnabled());
		assertTrue(l.isErrorEnabled());
		assertTrue(l.isFatalEnabled());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — log methods are loggable (true branch of the level guard)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_loggable_message() {
		var l = new TomcatLogger("test.tomcat.c01");
		Logger.getLogger("test.tomcat.c01").setLevel(Level.ALL);
		assertDoesNotThrow(() -> l.trace("trace msg"));
		assertDoesNotThrow(() -> l.debug("debug msg"));
		assertDoesNotThrow(() -> l.info("info msg"));
		assertDoesNotThrow(() -> l.warn("warn msg"));
		assertDoesNotThrow(() -> l.error("error msg"));
		assertDoesNotThrow(() -> l.fatal("fatal msg"));
	}

	@Test void c02_loggable_messageAndThrowable() {
		var l = new TomcatLogger("test.tomcat.c02");
		Logger.getLogger("test.tomcat.c02").setLevel(Level.ALL);
		var t = new RuntimeException("boom");
		assertDoesNotThrow(() -> l.trace("trace msg", t));
		assertDoesNotThrow(() -> l.debug("debug msg", t));
		assertDoesNotThrow(() -> l.info("info msg", t));
		assertDoesNotThrow(() -> l.warn("warn msg", t));
		assertDoesNotThrow(() -> l.error("error msg", t));
		assertDoesNotThrow(() -> l.fatal("fatal msg", t));
	}

	@Test void c03_loggable_nullMessage() {
		var l = new TomcatLogger("test.tomcat.c03");
		Logger.getLogger("test.tomcat.c03").setLevel(Level.ALL);
		assertDoesNotThrow(() -> l.info(null));
		assertDoesNotThrow(() -> l.error(null, new RuntimeException("x")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — log methods are not loggable (false branch of the level guard)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_notLoggable_message() {
		var l = new TomcatLogger("test.tomcat.d01");
		Logger.getLogger("test.tomcat.d01").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.trace("trace msg"));
		assertDoesNotThrow(() -> l.debug("debug msg"));
		assertDoesNotThrow(() -> l.info("info msg"));
		assertDoesNotThrow(() -> l.warn("warn msg"));
		assertDoesNotThrow(() -> l.error("error msg"));
		assertDoesNotThrow(() -> l.fatal("fatal msg"));
	}

	@Test void d02_notLoggable_messageAndThrowable() {
		var l = new TomcatLogger("test.tomcat.d02");
		Logger.getLogger("test.tomcat.d02").setLevel(Level.OFF);
		var t = new RuntimeException("boom");
		assertDoesNotThrow(() -> l.trace("trace msg", t));
		assertDoesNotThrow(() -> l.debug("debug msg", t));
		assertDoesNotThrow(() -> l.info("info msg", t));
		assertDoesNotThrow(() -> l.warn("warn msg", t));
		assertDoesNotThrow(() -> l.error("error msg", t));
		assertDoesNotThrow(() -> l.fatal("fatal msg", t));
	}
}
