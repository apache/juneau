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
package org.apache.juneau.microservice.jetty;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.slf4j.Marker;

/**
 * Tests for {@link JettyLogger}.
 */
class JettyLogger_Test extends TestBase {

	@Test void a01_defaultConstructor() {
		var l = new JettyLogger();
		assertEquals("org.eclipse.jetty.util.log.javautil", l.getName());
	}

	@Test void a02_namedConstructor() {
		var l = new JettyLogger("test.logger.name");
		assertEquals("test.logger.name", l.getName());
	}

	@Test void a03_getLogger() {
		var l = new JettyLogger("parent.logger");
		var child = l.getLogger("child.logger");
		assertNotNull(child);
		assertEquals("child.logger", child.getName());
	}

	@Test void a04_isDebugEnabled_whenFine() {
		var l = new JettyLogger("test.debug.logger");
		Logger.getLogger("test.debug.logger").setLevel(Level.FINE);
		assertTrue(l.isDebugEnabled());
	}

	@Test void a05_isDebugEnabled_whenInfo() {
		var l = new JettyLogger("test.info.logger");
		Logger.getLogger("test.info.logger").setLevel(Level.INFO);
		assertFalse(l.isDebugEnabled());
	}

	@Test void a06_setDebugEnabled_true() {
		var l = new JettyLogger("test.setdebug.logger");
		Logger.getLogger("test.setdebug.logger").setLevel(Level.INFO);
		l.setDebugEnabled(true);
		assertTrue(l.isDebugEnabled());
	}

	@Test void a07_setDebugEnabled_false_restoresPreviousLevel() {
		var jul = Logger.getLogger("test.setdebug2.logger");
		jul.setLevel(Level.INFO);
		var l = new JettyLogger("test.setdebug2.logger");
		l.setDebugEnabled(true);
		assertTrue(l.isDebugEnabled());
		l.setDebugEnabled(false);
		assertFalse(l.isDebugEnabled());
	}

	@Test void a08_debug_withArgs() {
		var l = new JettyLogger("test.debug.args.logger");
		Logger.getLogger("test.debug.args.logger").setLevel(Level.FINE);
		assertDoesNotThrow(() -> l.debug("Test message {}", "arg1"));
	}

	@Test void a09_debug_withArgs_notLoggable() {
		var l = new JettyLogger("test.debug.nolog.logger");
		Logger.getLogger("test.debug.nolog.logger").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.debug("Test message {}", "arg1"));
	}

	@Test void a10_debug_withThrowable() {
		var l = new JettyLogger("test.debug.thrown.logger");
		Logger.getLogger("test.debug.thrown.logger").setLevel(Level.FINE);
		assertDoesNotThrow(() -> l.debug("Error occurred", new RuntimeException("test")));
	}

	@Test void a11_debug_withThrowable_notLoggable() {
		var l = new JettyLogger("test.debug.thrown.nolog.logger");
		Logger.getLogger("test.debug.thrown.nolog.logger").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.debug("Error occurred", new RuntimeException("test")));
	}

	@Test void a12_debug_throwableOnly() {
		var l = new JettyLogger("test.debug.throwonly.logger");
		Logger.getLogger("test.debug.throwonly.logger").setLevel(Level.FINE);
		assertDoesNotThrow(() -> l.debug(new RuntimeException("test")));
	}

	@Test void a13_debug_longArg() {
		var l = new JettyLogger("test.debug.long.logger");
		Logger.getLogger("test.debug.long.logger").setLevel(Level.FINE);
		assertDoesNotThrow(() -> l.debug("Value: {}", 42L));
	}

	@Test void a14_info_withArgs() {
		var l = new JettyLogger("test.info.args.logger");
		Logger.getLogger("test.info.args.logger").setLevel(Level.INFO);
		assertDoesNotThrow(() -> l.info("Info message {}", "arg1"));
	}

	@Test void a15_info_withArgs_notLoggable() {
		var l = new JettyLogger("test.info.nolog.logger");
		Logger.getLogger("test.info.nolog.logger").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.info("Info message {}", "arg1"));
	}

	@Test void a16_info_withThrowable() {
		var l = new JettyLogger("test.info.thrown.logger");
		Logger.getLogger("test.info.thrown.logger").setLevel(Level.INFO);
		assertDoesNotThrow(() -> l.info("Info error", new RuntimeException("test")));
	}

	@Test void a17_info_withThrowable_notLoggable() {
		var l = new JettyLogger("test.info.thrown.nolog.logger");
		Logger.getLogger("test.info.thrown.nolog.logger").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.info("Info error", new RuntimeException("test")));
	}

	@Test void a18_info_throwableOnly() {
		var l = new JettyLogger("test.info.throwonly.logger");
		Logger.getLogger("test.info.throwonly.logger").setLevel(Level.INFO);
		assertDoesNotThrow(() -> l.info(new RuntimeException("test")));
	}

	@Test void a19_warn_withArgs() {
		var l = new JettyLogger("test.warn.args.logger");
		Logger.getLogger("test.warn.args.logger").setLevel(Level.WARNING);
		assertDoesNotThrow(() -> l.warn("Warn message {}", "arg1"));
	}

	@Test void a20_warn_withArgs_notLoggable() {
		var l = new JettyLogger("test.warn.nolog.logger");
		Logger.getLogger("test.warn.nolog.logger").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.warn("Warn message {}", "arg1"));
	}

	@Test void a21_warn_withThrowable() {
		var l = new JettyLogger("test.warn.thrown.logger");
		Logger.getLogger("test.warn.thrown.logger").setLevel(Level.WARNING);
		assertDoesNotThrow(() -> l.warn("Warn error", new RuntimeException("test")));
	}

	@Test void a22_warn_withThrowable_notLoggable() {
		var l = new JettyLogger("test.warn.thrown.nolog.logger");
		Logger.getLogger("test.warn.thrown.nolog.logger").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.warn("Warn error", new RuntimeException("test")));
	}

	@Test void a23_warn_throwableOnly() {
		var l = new JettyLogger("test.warn.throwonly.logger");
		Logger.getLogger("test.warn.throwonly.logger").setLevel(Level.WARNING);
		assertDoesNotThrow(() -> l.warn(new RuntimeException("test")));
	}

	@Test void a24_ignore() {
		var l = new JettyLogger("test.ignore.logger");
		Logger.getLogger("test.ignore.logger").setLevel(Level.FINEST);
		assertDoesNotThrow(() -> l.ignore(new RuntimeException("ignored")));
	}

	@Test void a25_ignore_notLoggable() {
		var l = new JettyLogger("test.ignore.nolog.logger");
		Logger.getLogger("test.ignore.nolog.logger").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.ignore(new RuntimeException("ignored")));
	}

	@Test void a26_isErrorEnabled() {
		var l = new JettyLogger("test.error.logger");
		assertFalse(l.isErrorEnabled());
	}

	@Test void a27_isInfoEnabled() {
		var l = new JettyLogger("test.isinfo.logger");
		assertFalse(l.isInfoEnabled());
	}

	@Test void a28_isTraceEnabled() {
		var l = new JettyLogger("test.trace.logger");
		assertFalse(l.isTraceEnabled());
	}

	@Test void a29_isWarnEnabled() {
		var l = new JettyLogger("test.iswarn.logger");
		assertFalse(l.isWarnEnabled());
	}

	@Test void a30_log_withThrown() {
		// Tests the log() private method via debug() which logs a non-null Throwable
		var l = new JettyLogger("test.log.thrown.logger");
		Logger.getLogger("test.log.thrown.logger").setLevel(Level.ALL);
		assertDoesNotThrow(() -> l.debug("msg with thrown", new RuntimeException("err")));
	}

	// --- Coverage for empty/TODO stubs (debug Marker variants) ---

	@Test void b01_debug_marker_msg() {
		var l = new JettyLogger("test.b01");
		assertDoesNotThrow(() -> l.debug((Marker)null, "msg"));
	}

	@Test void b02_debug_marker_format_arg() {
		var l = new JettyLogger("test.b02");
		assertDoesNotThrow(() -> l.debug((Marker)null, "fmt", (Object)"arg"));
	}

	@Test void b03_debug_marker_format_varargs() {
		var l = new JettyLogger("test.b03");
		assertDoesNotThrow(() -> l.debug((Marker)null, "fmt", (Object[])new Object[]{"a", "b"}));
	}

	@Test void b04_debug_marker_format_arg1_arg2() {
		var l = new JettyLogger("test.b04");
		assertDoesNotThrow(() -> l.debug((Marker)null, "fmt", "a", "b"));
	}

	@Test void b05_debug_marker_msg_throwable() {
		var l = new JettyLogger("test.b05");
		assertDoesNotThrow(() -> l.debug((Marker)null, "msg", new RuntimeException()));
	}

	@Test void b06_debug_string_msg() {
		var l = new JettyLogger("test.b06");
		assertDoesNotThrow(() -> l.debug("simple msg"));
	}

	@Test void b07_debug_string_format_arg() {
		var l = new JettyLogger("test.b07");
		assertDoesNotThrow(() -> l.debug("fmt {}", (Object)"arg"));
	}

	@Test void b08_debug_string_format_arg1_arg2() {
		var l = new JettyLogger("test.b08");
		assertDoesNotThrow(() -> l.debug("fmt {} {}", "a", "b"));
	}

	@Test void b09_debug_varargs_notLoggable() {
		// debug(String, Object...) with not-loggable level
		var l = new JettyLogger("test.b09");
		Logger.getLogger("test.b09").setLevel(Level.OFF);
		// Cast to Object[] to ensure varargs overload is called
		assertDoesNotThrow(() -> l.debug("fmt", (Object[])new Object[]{"a", "b"}));
	}

	@Test void b10_debug_varargs_loggable() {
		// debug(String, Object...) with loggable level - force varargs via explicit array
		var l = new JettyLogger("test.b10");
		Logger.getLogger("test.b10").setLevel(Level.ALL);
		assertDoesNotThrow(() -> l.debug("fmt {}", (Object[])new Object[]{"x"}));
	}

	@Test void b11_debug_long_notLoggable() {
		var l = new JettyLogger("test.b11");
		Logger.getLogger("test.b11").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.debug("val {}", 42L));
	}

	@Test void b12_debug_throwableOnly_notLoggable() {
		var l = new JettyLogger("test.b12");
		Logger.getLogger("test.b12").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.debug(new RuntimeException("not logged")));
	}

	// --- Coverage for empty/TODO stubs (error variants) ---

	@Test void c01_error_marker_msg() {
		var l = new JettyLogger("test.c01");
		assertDoesNotThrow(() -> l.error((Marker)null, "msg"));
	}

	@Test void c02_error_marker_format_arg() {
		var l = new JettyLogger("test.c02");
		assertDoesNotThrow(() -> l.error((Marker)null, "fmt", (Object)"arg"));
	}

	@Test void c03_error_marker_format_varargs() {
		var l = new JettyLogger("test.c03");
		assertDoesNotThrow(() -> l.error((Marker)null, "fmt", (Object[])new Object[]{"a"}));
	}

	@Test void c04_error_marker_format_arg1_arg2() {
		var l = new JettyLogger("test.c04");
		assertDoesNotThrow(() -> l.error((Marker)null, "fmt", "a", "b"));
	}

	@Test void c05_error_marker_msg_throwable() {
		var l = new JettyLogger("test.c05");
		assertDoesNotThrow(() -> l.error((Marker)null, "msg", new RuntimeException()));
	}

	@Test void c06_error_string_msg() {
		var l = new JettyLogger("test.c06");
		assertDoesNotThrow(() -> l.error("msg"));
	}

	@Test void c07_error_string_format_arg() {
		var l = new JettyLogger("test.c07");
		assertDoesNotThrow(() -> l.error("fmt {}", (Object)"arg"));
	}

	@Test void c08_error_string_format_varargs() {
		var l = new JettyLogger("test.c08");
		assertDoesNotThrow(() -> l.error("fmt", (Object[])new Object[]{"a"}));
	}

	@Test void c09_error_string_format_arg1_arg2() {
		var l = new JettyLogger("test.c09");
		assertDoesNotThrow(() -> l.error("fmt {} {}", "a", "b"));
	}

	@Test void c10_error_string_msg_throwable() {
		var l = new JettyLogger("test.c10");
		assertDoesNotThrow(() -> l.error("msg", new RuntimeException()));
	}

	// --- Coverage for empty/TODO stubs (info Marker variants) ---

	@Test void d01_info_marker_msg() {
		var l = new JettyLogger("test.d01");
		assertDoesNotThrow(() -> l.info((Marker)null, "msg"));
	}

	@Test void d02_info_marker_format_arg() {
		var l = new JettyLogger("test.d02");
		assertDoesNotThrow(() -> l.info((Marker)null, "fmt", (Object)"arg"));
	}

	@Test void d03_info_marker_format_varargs() {
		var l = new JettyLogger("test.d03");
		assertDoesNotThrow(() -> l.info((Marker)null, "fmt", (Object[])new Object[]{"a"}));
	}

	@Test void d04_info_marker_format_arg1_arg2() {
		var l = new JettyLogger("test.d04");
		assertDoesNotThrow(() -> l.info((Marker)null, "fmt", "a", "b"));
	}

	@Test void d05_info_marker_msg_throwable() {
		var l = new JettyLogger("test.d05");
		assertDoesNotThrow(() -> l.info((Marker)null, "msg", new RuntimeException()));
	}

	@Test void d06_info_string_msg() {
		var l = new JettyLogger("test.d06");
		assertDoesNotThrow(() -> l.info("simple msg"));
	}

	@Test void d07_info_string_format_arg() {
		var l = new JettyLogger("test.d07");
		assertDoesNotThrow(() -> l.info("fmt {}", (Object)"arg"));
	}

	@Test void d08_info_string_format_arg1_arg2() {
		var l = new JettyLogger("test.d08");
		assertDoesNotThrow(() -> l.info("fmt {} {}", "a", "b"));
	}

	@Test void d09_info_varargs_notLoggable() {
		// Use 3 args to avoid ambiguity with info(String, Object) single-arg overload
		var l = new JettyLogger("test.d09");
		Logger.getLogger("test.d09").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.info("{} {} {}", "a", "b", "c"));
	}

	@Test void d09b_info_varargs_loggable() {
		// Cover info(String, Object...) true branch with 3 args
		var l = new JettyLogger("test.d09b");
		Logger.getLogger("test.d09b").setLevel(Level.INFO);
		assertDoesNotThrow(() -> l.info("{} {} {}", "a", "b", "c"));
	}

	@Test void d10_info_throwableOnly_notLoggable() {
		var l = new JettyLogger("test.d10");
		Logger.getLogger("test.d10").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.info(new RuntimeException("not logged")));
	}

	// --- Coverage for empty/TODO stubs (warn Marker variants) ---

	@Test void e01_warn_marker_msg() {
		var l = new JettyLogger("test.e01");
		assertDoesNotThrow(() -> l.warn((Marker)null, "msg"));
	}

	@Test void e02_warn_marker_format_arg() {
		var l = new JettyLogger("test.e02");
		assertDoesNotThrow(() -> l.warn((Marker)null, "fmt", (Object)"arg"));
	}

	@Test void e03_warn_marker_format_varargs() {
		var l = new JettyLogger("test.e03");
		assertDoesNotThrow(() -> l.warn((Marker)null, "fmt", (Object[])new Object[]{"a"}));
	}

	@Test void e04_warn_marker_format_arg1_arg2() {
		var l = new JettyLogger("test.e04");
		assertDoesNotThrow(() -> l.warn((Marker)null, "fmt", "a", "b"));
	}

	@Test void e05_warn_marker_msg_throwable() {
		var l = new JettyLogger("test.e05");
		assertDoesNotThrow(() -> l.warn((Marker)null, "msg", new RuntimeException()));
	}

	@Test void e06_warn_string_msg() {
		var l = new JettyLogger("test.e06");
		assertDoesNotThrow(() -> l.warn("simple msg"));
	}

	@Test void e07_warn_string_format_arg() {
		var l = new JettyLogger("test.e07");
		assertDoesNotThrow(() -> l.warn("fmt {}", (Object)"arg"));
	}

	@Test void e08_warn_string_format_arg1_arg2() {
		var l = new JettyLogger("test.e08");
		assertDoesNotThrow(() -> l.warn("fmt {} {}", "a", "b"));
	}

	@Test void e09_warn_varargs_notLoggable() {
		// Use 3 args to avoid ambiguity with warn(String, Object) single-arg overload
		var l = new JettyLogger("test.e09");
		Logger.getLogger("test.e09").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.warn("{} {} {}", "a", "b", "c"));
	}

	@Test void e09b_warn_varargs_loggable() {
		// Cover warn(String, Object...) true branch with 3 args
		var l = new JettyLogger("test.e09b");
		Logger.getLogger("test.e09b").setLevel(Level.WARNING);
		assertDoesNotThrow(() -> l.warn("{} {} {}", "a", "b", "c"));
	}

	@Test void e10_warn_throwableOnly_notLoggable() {
		var l = new JettyLogger("test.e10");
		Logger.getLogger("test.e10").setLevel(Level.OFF);
		assertDoesNotThrow(() -> l.warn(new RuntimeException("not logged")));
	}

	// --- Coverage for empty/TODO stubs (trace variants) ---

	@Test void f01_trace_marker_msg() {
		var l = new JettyLogger("test.f01");
		assertDoesNotThrow(() -> l.trace((Marker)null, "msg"));
	}

	@Test void f02_trace_marker_format_arg() {
		var l = new JettyLogger("test.f02");
		assertDoesNotThrow(() -> l.trace((Marker)null, "fmt", (Object)"arg"));
	}

	@Test void f03_trace_marker_format_varargs() {
		var l = new JettyLogger("test.f03");
		assertDoesNotThrow(() -> l.trace((Marker)null, "fmt", (Object[])new Object[]{"a"}));
	}

	@Test void f04_trace_marker_format_arg1_arg2() {
		var l = new JettyLogger("test.f04");
		assertDoesNotThrow(() -> l.trace((Marker)null, "fmt", "a", "b"));
	}

	@Test void f05_trace_marker_msg_throwable() {
		var l = new JettyLogger("test.f05");
		assertDoesNotThrow(() -> l.trace((Marker)null, "msg", new RuntimeException()));
	}

	@Test void f06_trace_string_msg() {
		var l = new JettyLogger("test.f06");
		assertDoesNotThrow(() -> l.trace("msg"));
	}

	@Test void f07_trace_string_format_arg() {
		var l = new JettyLogger("test.f07");
		assertDoesNotThrow(() -> l.trace("fmt {}", (Object)"arg"));
	}

	@Test void f08_trace_string_format_varargs() {
		var l = new JettyLogger("test.f08");
		assertDoesNotThrow(() -> l.trace("fmt", (Object[])new Object[]{"a"}));
	}

	@Test void f09_trace_string_format_arg1_arg2() {
		var l = new JettyLogger("test.f09");
		assertDoesNotThrow(() -> l.trace("fmt {} {}", "a", "b"));
	}

	@Test void f10_trace_string_msg_throwable() {
		var l = new JettyLogger("test.f10");
		assertDoesNotThrow(() -> l.trace("msg", new RuntimeException()));
	}

	// --- Marker-based isEnabled stubs ---

	@Test void g01_isDebugEnabled_marker() {
		var l = new JettyLogger("test.g01");
		assertFalse(l.isDebugEnabled(null));
	}

	@Test void g02_isErrorEnabled_marker() {
		var l = new JettyLogger("test.g02");
		assertFalse(l.isErrorEnabled(null));
	}

	@Test void g03_isInfoEnabled_marker() {
		var l = new JettyLogger("test.g03");
		assertFalse(l.isInfoEnabled(null));
	}

	@Test void g04_isTraceEnabled_marker() {
		var l = new JettyLogger("test.g04");
		assertFalse(l.isTraceEnabled(null));
	}

	@Test void g05_isWarnEnabled_marker() {
		var l = new JettyLogger("test.g05");
		assertFalse(l.isWarnEnabled(null));
	}

	@Test void g06_log_marker() {
		var l = new JettyLogger("test.g06");
		assertDoesNotThrow(() -> l.log(null, "fqcn", 0, "msg", null, null));
	}
}
