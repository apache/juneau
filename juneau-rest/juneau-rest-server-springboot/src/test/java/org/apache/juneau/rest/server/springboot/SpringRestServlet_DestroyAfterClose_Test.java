// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.server.springboot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Tests that destroying a {@link SpringRestServlet} after its Spring context has closed neither throws nor logs.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Contexts here are test-local.
})
class SpringRestServlet_DestroyAfterClose_Test extends TestBase {

	public static class SpringOnly {}

	@Rest(path="/child")
	public static class Child {}

	@Rest(children={Child.class})
	public static class Servlet extends BasicSpringRestServletGroup {
		private static final long serialVersionUID = 1L;

		@RestDestroy
		public void onDestroy() {
			// No-arg: the failure under test is context access by the framework, not by user code.
		}
	}

	private static class Capture extends Handler {
		final List<LogRecord> records = new ArrayList<>();
		@Override public void publish(LogRecord r) { records.add(r); }
		@Override public void flush() { /* no-op */ }
		@Override public void close() { /* no-op */ }
	}

	@Test void a01_destroyAfterContextClosed_noExceptionNoLog() throws Exception {
		var ctx = new AnnotationConfigApplicationContext();
		ctx.registerBean(SpringOnly.class);
		ctx.registerBean(Child.class);
		ctx.refresh();
		var servlet = new Servlet();
		ctx.getAutowireCapableBeanFactory().autowireBean(servlet);
		var rc = new RestContext(new RestContext.Args(Servlet.class, null, null, () -> servlet, null, null, null, null, null, null));

		var capture = new Capture();
		var root = Logger.getLogger("");
		root.addHandler(capture);
		try {
			ctx.close();
			assertDoesNotThrow(rc::destroy);
		} finally {
			root.removeHandler(capture);
		}
		var bad = capture.records.stream().filter(r -> r.getLevel().intValue() >= Level.WARNING.intValue()).toList();
		assertTrue(bad.isEmpty(), () -> "Unexpected log records: " + bad.stream().map(r -> r.getMessage() + " " + r.getThrown()).toList());
	}
}
