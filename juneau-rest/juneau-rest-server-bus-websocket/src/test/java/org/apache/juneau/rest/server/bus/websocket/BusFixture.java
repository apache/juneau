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
package org.apache.juneau.rest.server.bus.websocket;

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.JsonParser;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;

/** Opens bus sessions through the real session POST, the only place a session id is ever visible. */
final class BusFixture {

	private BusFixture() {}

	/** The session POST body. */
	static String body(String transport, List<String> downstream, List<String> upstream) {
		return Json.of(JsonMap.of("v", 1, "bridge", "ops", "transport", transport, "downstream", downstream, "upstream", upstream));
	}

	/**
	 * Posts a session request and returns the grant.
	 *
	 * @param headers Extra request headers, as name/value pairs.
	 */
	static JsonMap post(MockRestClient client, Principal principal, String body, String...headers) throws Exception {
		var r = client.post("/juneau-bus/session").userPrincipal(principal).contentString(body).contentType("application/json")
			.header("Accept", "application/json");
		for (var i = 0; i < headers.length; i += 2)
			r = r.header(headers[i], headers[i + 1]);
		return JsonMap.ofString(r.run().assertStatus(200).getContent().asString(), JsonParser.DEFAULT);
	}

	/** Posts a session request and returns the new session's capability id. */
	static String open(MockRestClient client, Principal principal, String transport, List<String> downstream, List<String> upstream, String...headers) throws Exception {
		return post(client, principal, body(transport, downstream, upstream), headers).getString("sessionId");
	}
}
