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
package org.apache.juneau.rest;

import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RestResponse#downloadAs(String)}.
 */
class RestResponse_DownloadAs_Test extends org.apache.juneau.TestBase {

	@Rest(serializers = JsonSerializer.class)
	public static class A {
		@RestGet("/file")
		public void file(RestResponse res) {
			res.downloadAs("my report.pdf");
			res.setContent("OK");
		}
	}

	@Test
	void a01_setsContentDispositionAttachment() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/file").run()
			.assertStatus(200)
			.assertHeader("Content-Disposition").isContains("attachment", "filename=", "my report.pdf");
	}
}
