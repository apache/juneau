// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.microservice.springboot.template;

import java.util.*;

import org.apache.juneau.html.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.BasicRestObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample REST resource that prints out a simple "Hello world!" message.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#my-springboot-microservice">my-springboot-microservice</a>
 * </ul>
 */
@Rest(
	title="Hello World",
	description="An example of the simplest-possible resource",
	path="/helloWorld"
)
@HtmlDocConfig(
	aside={
		"<div style='max-width:400px' class='text'>",
		"	<p>This page shows a resource that simply response with a 'Hello world!' message</p>",
		"	<p>The POJO serialized is a simple String.</p>",
		"</div>"
	}
)
public class HelloWorldResource extends BasicRestObject {

	private final String message;

	/**
	 * Optional message provider that can be injected into this object.
	 */
	@Autowired
	private Optional<HelloWorldMessageProvider> messageProvider;

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
		this.message = message;
	}

	/**
	 * GET request handler.
	 *
	 * @return A simple Hello-World message.
	 */
	@RestGet(path="/*", summary="Responds with \"Hello world!\"")
	public String sayHello() {
		String message = this.message;
		if (messageProvider != null && messageProvider.isPresent())
			message = messageProvider.get().get();
		return message;
	}
}
