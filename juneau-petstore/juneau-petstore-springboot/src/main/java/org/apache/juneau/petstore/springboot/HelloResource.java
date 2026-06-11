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

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.springframework.beans.factory.annotation.*;

/**
 * Tiny REST resource that demonstrates Spring {@code @Autowired} injection into a Juneau {@code @Rest} servlet
 * under a Spring Boot deployment.  Mirrors the legacy {@code HelloWorldResource} demo.
 *
 * <p>
 * The {@link HelloMessageProvider} bean is wired by Spring; the {@code GET /hello/*} endpoint surfaces its message.
 * Resolution of the {@code HelloMessageProvider} bean works because the parent router extends
 * {@code BasicSpringRestServletGroup} (i.e. {@code SpringRestServlet}), which lets child resources be Spring beans.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstoreOverview">juneau-petstore</a>
 * </ul>
 */
@Rest(
	path="/hello",
	title="Hello (Spring injection demo)",
	description="A trivial REST resource demonstrating Spring @Autowired injection of a HelloMessageProvider bean."
)
public class HelloResource extends BasicRestResource {

	private final HelloMessageProvider messageProvider;

	/**
	 * Constructor.
	 *
	 * @param messageProvider The Spring-injected message provider bean.
	 */
	@Autowired
	public HelloResource(HelloMessageProvider messageProvider) {
		this.messageProvider = messageProvider;
	}

	/**
	 * GET /hello/* — returns the message provided by the injected Spring bean.
	 *
	 * @return The injected message.
	 */
	@RestGet(path="/*", summary="Responds with the Spring-injected message.")
	public String sayHello() {
		return messageProvider.get();
	}
}
