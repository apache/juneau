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
package org.apache.juneau.microservice.springboot.template;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.marshall.html.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Sample REST resource that prints out a simple "Hello world!" message.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MySpringBootMicroserviceBasics">My SpringBoot Microservice Basics</a>

 * </ul>
 */
@Rest(title = "Hello World", description = "An example of the simplest-possible resource", path = "/helloWorld")
@HtmlDocConfig(aside = { "<div style='max-width:400px' class='text'>", "\t<p>This page shows a resource that simply response with a 'Hello world!' message</p>",
	"\t<p>The POJO serialized is a simple String.</p>", "</div>" })
public class HelloWorldResource extends BasicRestResource {

	private final String message;
	private final Optional<HelloWorldMessageProvider> messageProvider;

	/**
	 * Default constructor.
	 * <p>
	 * Used by default if bean cannot be found in the Spring application context.
	 */
	public HelloWorldResource() {
		this("Hello world!");
	}

	/**
	 * Constructor.
	 *
	 * @param message The message to display.
	 */
	public HelloWorldResource(String message) {
		this(message, opte());
	}

	/**
	 * Constructor with an injectable message provider.
	 *
	 * @param message The message to display.
	 * @param messageProvider Optional message provider that can be injected into this object.
	 */
	public HelloWorldResource(String message, Optional<HelloWorldMessageProvider> messageProvider) {
		this.message = message;
		this.messageProvider = messageProvider;
	}

	/**
	 * GET request handler.
	 *
	 * @return A simple Hello-World message.
	 */
	@RestGet(path = "/*", summary = "Responds with \"Hello world!\"")
	public String sayHello() {
		var message2 = this.message;
		if (messageProvider.isPresent())
			message2 = messageProvider.get().get();
		return message2;
	}
}