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
package org.apache.juneau.http.header;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ContentDisposition} and {@link ContentDisposition.Builder}.
 */
class ContentDisposition_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A — ContentDisposition header bean
	//------------------------------------------------------------------------------------------------------------------

	@Nested class A_bean extends TestBase {

		@Test void a01_name() {
			assertEquals("Content-Disposition", ContentDisposition.NAME);
		}

		@Test void a02_ofEager() {
			var x = ContentDisposition.of("attachment");
			assertEquals("Content-Disposition", x.getName());
			assertEquals("attachment", x.getValue());
		}

		@Test void a03_attachmentHelperStillWorks() {
			assertEquals("attachment; filename=\"genome.jpeg\"", ContentDisposition.attachment("genome.jpeg").getValue());
		}

		@Test void a04_attachmentHelperEscapes() {
			assertEquals("attachment; filename=\"a\\\"b.txt\"", ContentDisposition.attachment("a\"b.txt").getValue());
		}

		@Test void a05_attachmentHelperEscapesBackslash() {
			assertEquals("attachment; filename=\"a\\\\b.txt\"", ContentDisposition.attachment("a\\b.txt").getValue());
		}

		@Test void a06_attachmentBlankThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "Attachment filename must not be null or blank.", () -> ContentDisposition.attachment(" "));
		}

		@Test void a07_attachmentCrThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "Attachment filename must not contain CR or LF characters.", () -> ContentDisposition.attachment("a\rb"));
		}

		@Test void a08_attachmentLfThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "Attachment filename must not contain CR or LF characters.", () -> ContentDisposition.attachment("a\nb"));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// B — Disposition-type presets + typed params
	//------------------------------------------------------------------------------------------------------------------

	@Nested class B_params extends TestBase {

		@Test void b01_attachmentWithFilenameAndSize() {
			assertEquals("attachment; filename=\"genome.jpeg\"; size=12345",
				ContentDisposition.create().attachment().filename("genome.jpeg").size(12345).build());
		}

		@Test void b02_inline() {
			assertEquals("inline", ContentDisposition.create().inline().build());
		}

		@Test void b03_formDataWithName() {
			assertEquals("form-data; name=\"field1\"", ContentDisposition.create().formData().name("field1").build());
		}

		@Test void b04_filenameExtRaw() {
			assertEquals("attachment; filename*=UTF-8''%e2%82%ac%20rates",
				ContentDisposition.create().attachment().filenameExt("UTF-8''%e2%82%ac%20rates").build());
		}

		@Test void b05_dates() {
			assertEquals("attachment; creation-date=\"Wed, 12 Feb 1997 16:29:51 -0500\"",
				ContentDisposition.create().attachment().creationDate("Wed, 12 Feb 1997 16:29:51 -0500").build());
		}

		@Test void b06_arbitraryType() {
			assertEquals("custom-type", ContentDisposition.create().type("custom-type").build());
		}

		@Test void b07_paramReplaceKeepsPosition() {
			assertEquals("attachment; filename=\"b\"", ContentDisposition.create().attachment().filename("a").filename("b").build());
		}

		@Test void b08_modificationAndReadDates() {
			assertEquals(
				"attachment; modification-date=\"Wed, 12 Feb 1997 16:29:51 -0500\"; read-date=\"Tue, 11 Feb 1997 16:29:51 -0500\"",
				ContentDisposition.create().attachment().modificationDate("Wed, 12 Feb 1997 16:29:51 -0500").readDate("Tue, 11 Feb 1997 16:29:51 -0500").build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// C — Generic escape hatch + quoting
	//------------------------------------------------------------------------------------------------------------------

	@Nested class C_escapeHatch extends TestBase {

		@Test void c01_genericParamQuoted() {
			assertEquals("attachment; custom=\"v\"", ContentDisposition.create().attachment().param("custom", "v").build());
		}

		@Test void c02_quotingEscapes() {
			assertEquals("attachment; filename=\"a\\\"b\\\\c\"", ContentDisposition.create().attachment().filename("a\"b\\c").build());
		}

		@Test void c03_emptyBuild() {
			assertEquals("", ContentDisposition.create().build());
		}

		@Test void c04_paramsWithoutType() {
			assertEquals("name=\"x\"", ContentDisposition.create().name("x").build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// D — Validation
	//------------------------------------------------------------------------------------------------------------------

	@Nested class D_validation extends TestBase {

		@Test void d01_blankTypeThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "disposition-type must not be blank", () -> ContentDisposition.create().type(" "));
		}

		@Test void d02_negativeSizeThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "size must be non-negative", () -> ContentDisposition.create().size(-1));
		}

		@Test void d03_nullParamNameThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "name", () -> ContentDisposition.create().param(null, "v"));
		}

		@Test void d04_blankParamNameThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "parameter name must not be blank", () -> ContentDisposition.create().param("  ", "v"));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// E — toString / toHeader round-trips
	//------------------------------------------------------------------------------------------------------------------

	@Nested class E_rendering extends TestBase {

		@Test void e01_toStringEqualsBuild() {
			var b = ContentDisposition.create().attachment().filename("x.txt");
			assertEquals(b.build(), b.toString());
		}

		@Test void e02_toHeader() {
			var h = ContentDisposition.create().attachment().filename("x.txt").toHeader();
			assertEquals("Content-Disposition", h.getName());
			assertEquals("attachment; filename=\"x.txt\"", h.getValue());
		}
	}
}
