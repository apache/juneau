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

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RestResponse#setSerializer(org.apache.juneau.serializer.Serializer)}.
 */
class RestResponse_SetSerializer_Test extends TestBase {

	@Rest(serializers = { JsonSerializer.class, XmlSerializer.class })
	public static class A {

		public static class Bean {
			public String f;
		}

		@RestGet("/forcedXml")
		public void forcedXml(RestResponse res) {
			var b = new Bean();
			b.f = "x";
			res.setSerializer(XmlSerializer.DEFAULT);
			res.setContent(b);
		}

		@RestGet("/negotiatedJson")
		public Bean negotiatedJson() {
			var b = new Bean();
			b.f = "x";
			return b;
		}
	}

	@Test
	void a01_setSerializerForcesOutputDespiteAccept() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/forcedXml").header("Accept", "application/json").run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("xml")
			.assertContent().isContains("<f>", "x", "</f>");
	}

	@Test
	void a02_withoutSetSerializerAcceptSelectsSerializer() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/negotiatedJson").header("Accept", "application/json").run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("json")
			.assertContent().isContains("\"f\"", "\"x\"");
	}
}
