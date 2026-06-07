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
package org.apache.juneau.rest.server;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

class Rest_Mixins_Conflicts_Test extends TestBase {

	@Rest
	public static class A_Mixin {
		@RestGet(path="/same")
		public String sameFromMixin() {
			return "mixin";
		}
	}

	@Rest(mixins={A_Mixin.class})
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/same")
		public String sameFromResource() {
			return "resource";
		}
	}

	@Test void a01_resourceMethodWinsOnCollision() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/same").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("\"resource\"");
	}
}
