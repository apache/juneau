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
package org.apache.juneau.petstore.console;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.view.freemarker.console.*;

/**
 * Base class for every petstore console page resource: registers the console FreeMarker mixin with
 * {@code base.ftlh} as chrome, and injects the shared {@link PetStore}.
 *
 * <p>
 * Children don't inherit the parent's mixins, so every page class extends this one instead of re-declaring the
 * mixin, which E-B12 would reject.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/ops/audit"</js>)
 * 	<jk>public class</jk> AuditRest <jk>extends</jk> PetstoreConsolePage {
 * 		<ja>@RestGet</ja>(path=<js>"/"</js>)
 * 		<jk>public</jk> View page() { <jk>return</jk> FreemarkerView.<jsm>of</jsm>(<js>"audit.ftlh"</js>); }
 * 	}
 * </p>
 */
@Rest(mixins=FreemarkerMixin.class, responseProcessors=FreemarkerViewRenderer.class)
public abstract class PetstoreConsolePage extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	/** Classpath folder holding the console templates. */
	public static final String TEMPLATES = "/org/apache/juneau/petstore/console/templates/";

	@SuppressWarnings({
		"java:S2226" // the field is set by the framework's @Bean injection after construction, so it cannot be final.
	})
	@Bean
	private transient PetStore store;

	/**
	 * The console FreeMarker mixin: petstore templates, {@code base.ftlh} chrome.
	 *
	 * @return The mixin.
	 */
	@Bean
	public FreemarkerMixin freemarker() {
		return ConsoleFreemarkerMixin.create().basePath(TEMPLATES).chromeTemplate("base.ftlh").build();
	}

	/**
	 * @return The shared store.
	 * @throws IllegalStateException If no {@link PetStore} {@code @Bean} was wired into the host.
	 */
	protected synchronized PetStore store() {
		if (store == null)
			throw new IllegalStateException("No PetStore bean is wired: declare a @Bean PetStore on the host that mounts " + getClass().getSimpleName());
		return store;
	}
}
