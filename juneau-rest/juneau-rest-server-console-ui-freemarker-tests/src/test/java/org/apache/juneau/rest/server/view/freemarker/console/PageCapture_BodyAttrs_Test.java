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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class PageCapture_BodyAttrs_Test extends TestBase {

	private static PageCapture cap(String attrs) throws Exception {
		return new PageCapture().mergeBodyAttrs(attrs);
	}

	private static String strip(String attrs) throws Exception {
		return cap(attrs).bodyAttrsWithout("data-juneau-csrf");
	}

	@Test void a01_doubleQuoted() throws Exception {
		assertEquals("data-k=\"v\"", strip("data-juneau-csrf=\"tok\" data-k=\"v\""));
	}

	@Test void a02_singleQuoted() throws Exception {
		assertEquals("data-k=\"v\"", strip("data-juneau-csrf='tok' data-k=\"v\""));
	}

	@Test void a03_unquoted() throws Exception {
		assertEquals("data-k=\"v\"", strip("data-juneau-csrf=tok data-k=\"v\""));
	}

	@Test void a04_upperCaseName() throws Exception {
		assertEquals("data-k=\"v\"", strip("DATA-JUNEAU-CSRF=\"tok\" data-k=\"v\""));
	}

	@Test void a05_whitespaceAroundEquals() throws Exception {
		assertEquals("data-k=\"v\"", strip("data-juneau-csrf = 'tok' data-k=\"v\""));
	}

	@Test void a06_headerNameKept() throws Exception {
		assertEquals("data-juneau-csrf-header=\"X-H\"", strip("data-juneau-csrf=\"tok\" data-juneau-csrf-header=\"X-H\""));
	}

	@Test void a07_prefixedNameKept() throws Exception {
		assertEquals("x-data-juneau-csrf=\"keep\"", strip("x-data-juneau-csrf=\"keep\" data-juneau-csrf=tok"));
	}

	@Test void a08_valuelessKept() throws Exception {
		assertEquals("data-juneau-csrf data-k=\"v\"", strip("data-juneau-csrf data-k=\"v\""));
	}

	@Test void a09_valueWithGtAndSpacesInQuotes() throws Exception {
		assertEquals("data-k=\"v\"", strip("data-juneau-csrf='a > b' data-k=\"v\""));
	}

	@Test void a10_hasBodyAttr_allFormsAndCase() throws Exception {
		assertTrue(cap("data-juneau-csrf=\"t\"").hasBodyAttr("data-juneau-csrf"));
		assertTrue(cap("data-juneau-csrf='t'").hasBodyAttr("data-juneau-csrf"));
		assertTrue(cap("data-juneau-csrf=t").hasBodyAttr("data-juneau-csrf"));
		assertTrue(cap("Data-Juneau-Csrf = 't'").hasBodyAttr("data-juneau-csrf"));
		assertFalse(cap("data-juneau-csrf-header='t'").hasBodyAttr("data-juneau-csrf"));
		assertFalse(cap("x-data-juneau-csrf='t'").hasBodyAttr("data-juneau-csrf"));
		assertFalse(cap("data-juneau-csrf").hasBodyAttr("data-juneau-csrf"));
	}

	@Test void a11_noAttrs() {
		assertNull(new PageCapture().bodyAttrsWithout("data-juneau-csrf"));
	}

	@Test void a12_nameInsideAnotherValueIsNotAnAttribute() throws Exception {
		assertEquals("title=\"data-juneau-csrf=x\" data-k=\"v\"", strip("title=\"data-juneau-csrf=x\" data-k=\"v\""));
		assertEquals("title='a data-juneau-csrf=x'", strip("title='a data-juneau-csrf=x'"));
		assertEquals("title=\"data-juneau-csrf=x\"", strip("data-juneau-csrf=tok title=\"data-juneau-csrf=x\""));
	}

	@Test void a13_hasBodyAttr_ignoresNameInsideAnotherValue() throws Exception {
		assertFalse(cap("title=\"data-juneau-csrf=x\"").hasBodyAttr("data-juneau-csrf"));
	}
}
