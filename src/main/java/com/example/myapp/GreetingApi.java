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
package com.example.myapp;

import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;

/**
 * Typed REST client for {@link HelloWorldResource}, used by the example test.
 *
 * <p>Obtain a proxy with {@code restClient.remote(GreetingApi.class)}; each method call is translated
 * to an HTTP request and the JSON response is parsed back into a {@link Greeting} POJO.
 */
@Remote
public interface GreetingApi {

	/**
	 * Calls {@code GET /greet/{name}} and parses the JSON response into a {@link Greeting}.
	 *
	 * @param name The user name (bound to the path variable).
	 * @param loud Whether to upper-case the message (bound to the query parameter).
	 * @return The parsed greeting.
	 */
	@RemoteGet("/greet/{name}")
	Greeting greet(@Path("name") String name, @Query("loud") boolean loud);
}
