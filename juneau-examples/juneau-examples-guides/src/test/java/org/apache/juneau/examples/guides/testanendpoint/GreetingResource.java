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
package org.apache.juneau.examples.guides.testanendpoint;

// region resource
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * A small greeting service: one GET, one POST that validates its body.
 */
@Rest(path = "/greetings", title = "Greeting service")
public class GreetingResource extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * Greets the name in the path.
	 *
	 * @param name The name from the URL.
	 * @return The greeting.
	 */
	@RestGet("/{name}")
	public Greeting greet(@Path("name") String name) {
		return new Greeting(name);
	}

	/**
	 * Greets the name in the request body.
	 *
	 * @param body A greeting with only {@code name} set.
	 * @return The completed greeting.
	 * @throws BadRequest If the name is missing or blank.
	 */
	@RestPost("/")
	public Greeting create(@Content Greeting body) throws BadRequest {
		if (body.name == null || body.name.isBlank())
			throw new BadRequest("name is required");
		return new Greeting(body.name);
	}
}
// endregion resource
