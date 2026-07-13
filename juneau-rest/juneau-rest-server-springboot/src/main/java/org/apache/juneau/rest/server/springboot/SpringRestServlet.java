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
package org.apache.juneau.rest.server.springboot;

import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.servlet.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.*;

/**
 * Subclass of a {@link RestServlet} that hooks into Spring Boot for using Spring Beans.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		Users will typically extend from {@link BasicSpringRestServlet} or {@link BasicSpringRestServletGroup}
 * 		instead of this class directly.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestServerSpringboot">juneau-rest-server-springboot Basics</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClasses">@Rest-Annotated Class Basics</a>
 * </ul>
 *
 * @serial exclude
 */
public abstract class SpringRestServlet extends RestServlet {

	private static final long serialVersionUID = 1L;

	@Autowired
	@SuppressWarnings({
		"java:S6813" // Field injection is structurally required: this abstract base servlet is extended by user subclasses that declare no constructors; Spring instantiates them via the no-arg constructor and injects this transient field afterward. Constructor injection would force every subclass to thread ApplicationContext through a boilerplate super(...) call.
	})
	private transient Optional<ApplicationContext> appContext;

	/**
	 * Hook into Spring bean injection framework.
	 *
	 * @param parent Optional parent resource bean store, used as a fallback after Spring's context.
	 * @return A {@link WritableBeanStore} backed by Spring's {@link ApplicationContext}.
	 */
	@Bean
	@SuppressWarnings({
		"resource" // Spring takes ownership of the returned bean and manages its lifecycle
	})
	public WritableBeanStore createBeanStore(Optional<BeanStore> parent) {
		return new SpringBeanStore(appContext.orElse(null), parent.orElse(null));
	}
}