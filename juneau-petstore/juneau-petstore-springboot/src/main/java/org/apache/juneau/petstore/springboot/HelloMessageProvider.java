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
package org.apache.juneau.petstore.springboot;

import java.util.function.*;

/**
 * Sample Spring bean injected into {@link HelloResource} to demonstrate the {@code @Autowired} pattern under
 * Juneau {@code SpringRestServlet}-based deployments.
 */
public class HelloMessageProvider implements Supplier<String> {

	private final String message;

	/**
	 * Constructor.
	 *
	 * @param message The message to surface from {@link #get()}.
	 */
	public HelloMessageProvider(String message) {
		this.message = message;
	}

	@Override
	public String get() {
		return message;
	}
}
