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
package org.apache.juneau.rest.staticfiles;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.staticfile.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@code ${juneau.staticfiles.path:static}} SVL override on
 * {@link BasicStaticFilesResource}.
 *
 * <p>
 * Per the FINISHED-101 multi-path collapse, the historical {@code /htdocs/*} alias is now
 * reached by setting the override to {@code htdocs} &mdash; the same path the classpath
 * {@link BasicStaticFiles} root walks by default. The {@code a02} test exercises that exact
 * migration scenario.
 *
 * <p>
 * Uses a fresh inner-class resource per scenario because {@link MockRestClient} caches
 * {@link RestContext} per resource class &mdash; SVL substitution is captured at
 * context-construction time.
 *
 * @since 9.5.0
 */
class BasicStaticFilesResource_SvlPathOverride_Test extends TestBase {

	@Rest(mixins=BasicStaticFilesResource.class)
	public static class A01_OverridePath extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_svlOverrideChangesPath() throws Exception {
		var key = "juneau.staticfiles.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "assets");
		try {
			var c = MockRestClient.buildLax(A01_OverridePath.class);

			c.get("/static/javadoc.css").run().assertStatus(404);

			c.get("/assets/javadoc.css")
				.run()
				.assertStatus(200)
				.assertContent().asString().isContains("Licensed to the Apache Software Foundation");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=BasicStaticFilesResource.class)
	public static class A02_HtdocsAliasMigration extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_htdocsLegacyAliasReachableViaOverride() throws Exception {
		// Migration scenario: the historical dual default that included /htdocs/* is now reached
		// by setting the SVL override to htdocs. The classpath search root (BasicStaticFiles
		// default) walks both static/ and htdocs/ so the file is reachable either way.
		var key = "juneau.staticfiles.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "htdocs");
		try {
			var c = MockRestClient.buildLax(A02_HtdocsAliasMigration.class);

			c.get("/static/javadoc.css").run().assertStatus(404);

			c.get("/htdocs/javadoc.css")
				.run()
				.assertStatus(200)
				.assertContent().asString().isContains("Licensed to the Apache Software Foundation");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=BasicStaticFilesResource.class)
	public static class A03_BareToken extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a03_overrideBareToken() throws Exception {
		var key = "juneau.staticfiles.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "xxx");
		try {
			var c = MockRestClient.buildLax(A03_BareToken.class);
			c.get("/xxx/javadoc.css").run().assertStatus(200).assertContent().asString().isContains("Licensed to the Apache Software Foundation");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=BasicStaticFilesResource.class)
	public static class A04_LeadingSlash extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a04_overrideLeadingSlash() throws Exception {
		var key = "juneau.staticfiles.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx");
		try {
			var c = MockRestClient.buildLax(A04_LeadingSlash.class);
			c.get("/xxx/javadoc.css").run().assertStatus(200).assertContent().asString().isContains("Licensed to the Apache Software Foundation");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=BasicStaticFilesResource.class)
	public static class A05_TrailingSlash extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a05_overrideTrailingSlash() throws Exception {
		var key = "juneau.staticfiles.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "xxx/");
		try {
			var c = MockRestClient.buildLax(A05_TrailingSlash.class);
			c.get("/xxx/javadoc.css").run().assertStatus(200).assertContent().asString().isContains("Licensed to the Apache Software Foundation");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=BasicStaticFilesResource.class)
	public static class A06_BothSlashes extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a06_overrideBothSlashes() throws Exception {
		var key = "juneau.staticfiles.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx/");
		try {
			var c = MockRestClient.buildLax(A06_BothSlashes.class);
			c.get("/xxx/javadoc.css").run().assertStatus(200).assertContent().asString().isContains("Licensed to the Apache Software Foundation");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=BasicStaticFilesResource.class)
	public static class A07_WildcardSuffix extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a07_overrideWildcardSuffix() throws Exception {
		var key = "juneau.staticfiles.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx/*");
		try {
			var c = MockRestClient.buildLax(A07_WildcardSuffix.class);
			c.get("/xxx/javadoc.css").run().assertStatus(200).assertContent().asString().isContains("Licensed to the Apache Software Foundation");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=BasicStaticFilesResource.class)
	public static class A08_MultiSegment extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a08_overrideMultiSegment() throws Exception {
		var key = "juneau.staticfiles.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/api/v1/xxx/*");
		try {
			var c = MockRestClient.buildLax(A08_MultiSegment.class);
			c.get("/api/v1/xxx/javadoc.css").run().assertStatus(200).assertContent().asString().isContains("Licensed to the Apache Software Foundation");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}
}
