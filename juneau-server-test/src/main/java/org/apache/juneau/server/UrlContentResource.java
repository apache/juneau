/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import org.apache.juneau.json.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testUrlContent",
	serializers={PlainTextSerializer.class},
	parsers={JsonParser.class}
)
public class UrlContentResource extends RestServlet {
	private static final long serialVersionUID = 1L;

	@RestMethod(name="GET", path="/testString")
	public String testString(@Content String content) {
		return String.format("class=%s, value=%s", content.getClass().getName(), content.toString());
	}

	@RestMethod(name="GET", path="/testEnum")
	public String testEnum(@Content TestEnum content) {
		return String.format("class=%s, value=%s", content.getClass().getName(), content.toString());
	}

	public static enum TestEnum {
		X1
	}

	@RestMethod(name="GET", path="/testBean")
	public String testBean(@Content TestBean content) throws Exception {
		return String.format("class=%s, value=%s", content.getClass().getName(), JsonSerializer.DEFAULT_LAX.serialize(content));
	}

	public static class TestBean {
		public int f1;
		public String f2;
	}

	@RestMethod(name="GET", path="/testInt")
	public String testString(@Content Integer content) {
		return String.format("class=%s, value=%s", content.getClass().getName(), content.toString());
	}
}
