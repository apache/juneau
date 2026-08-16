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
package org.apache.juneau.rest.springboot.logging;

import java.io.*;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.springboot.*;

/**
 * Top-level Spring Boot host resource used by logging propagation integration tests.
 *
 * @since 10.0.0
 */
@Rest
public class RestDebugLoggingPropagation_HostResource extends BasicSpringRestServlet {
	private static final long serialVersionUID = 1L;

	@RestPost(path="/echo")
	public String echo(RestRequest req) throws IOException {
		return req.getContent().asString();
	}

	@RestGet(path="/one")
	public String one() {
		return "one";
	}

	@RestGet(path="/two")
	public String two() {
		return "two";
	}
}
