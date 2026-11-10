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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/** Node behavioral checks for page-level {@code ?state=} sources ({@code registerPageSource}, {@code syncPage}, {@code currentDirective}). */
class ViewsJs_UrlStatePageSource_Test extends TestBase {

	private static Map<?,?> report() {
		var r = NodeHarness.report("urlstate-pagesource.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.URLSTATE_JS_RESOURCE);
		assumeTrue(r != null, "node (or urlstate-pagesource.cjs) not available - behavioral layer skipped");
		return r;
	}

	@Test void p01_pageApiExists() {
		assertEquals(true, report().get("hasPageApi"));
	}

	@Test void p02_syncPageWritesSourceAndKeepsOtherParams() {
		var r = report();
		assertEquals(true, r.get("sourceRegistered"));
		assertEquals(true, r.get("keepsOthers"));
		assertTrue(String.valueOf(r.get("written")).contains("zz(hello)"), () -> String.valueOf(r.get("written")));
	}

	@Test void p03_duplicateAndUnregisteredSourcesRejected() {
		var r = report();
		assertEquals(true, r.get("dupRejected"));
		assertEquals(true, r.get("unregisteredNameRejected"));
	}

	@Test void p04_emptyValueDropsTheDirective() {
		assertEquals(true, report().get("emptyDropped"));
	}

	@Test void p05_decodeKeepsTildeAndColon() {
		assertEquals("{\"a\":\"b.c\",\"d\":\"e\"}", report().get("tildeColon"));
	}

	@Test void p06_currentDirectiveParsesLiveAddress() {
		var r = report();
		assertEquals("{\"a\":\"v2\",\"b\":\"v3.s2\"}", r.get("current"));
		assertEquals(true, r.get("currentAbsent"));
		assertEquals(true, r.get("currentUnregistered"));
	}
}
