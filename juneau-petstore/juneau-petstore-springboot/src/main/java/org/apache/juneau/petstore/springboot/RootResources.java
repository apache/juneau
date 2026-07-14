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

import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.petstore.rest.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.springboot.*;
import org.apache.juneau.rest.server.widget.*;

/**
 * Root router resource for the Spring Boot deployment.
 *
 * <p>
 * Extends {@link BasicSpringRestServletGroup} (i.e. {@link SpringRestServlet}) so child resources can be resolved
 * as Spring beans — required for {@link HelloResource} to receive its {@code @Autowired} {@link HelloMessageProvider}.
 * Mounts the deployment-agnostic {@link PetStoreResource} from {@code juneau-petstore-core} alongside the local
 * {@link HelloResource} injection demo.
 *
 * <p>
 * Note: this Spring Boot deployment intentionally does NOT mount the Jetty/Microservice admin trio
 * ({@code ConfigResource}/{@code LogsResource}/{@code ShutdownResource}, now in the
 * {@code juneau-microservice-examples} module) — those are Jetty-only and have no meaningful analogue here
 * (Spring Boot has its own actuator surface).  This is the documented non-parity deployment-specific
 * difference between the two petstore deployments.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 *
 * @serial exclude
 */
@Rest(
	title="Juneau Petstore (Spring Boot)",
	description="Apache Juneau petstore sample application running under Spring Boot.",
	children={
		PetStoreResource.class,
		PetSecureResource.class,
		PetMustacheViewResource.class,
		PetFreemarkerViewResource.class,
		PetstoreUiResource.class,
		HelloResource.class
	}
)
@HtmlDocConfig(
	widgets={
		ContentTypeMenuItem.class
	},
	navlinks={
		"api: servlet:/api",
		"stats: servlet:/stats",
		"$W{ContentTypeMenuItem}",
		"source: $C{Source/gitHub}/org/apache/juneau/petstore/springboot/RootResources.java"
	},
	aside={
		"<div class='text'>",
		"\t<p>Apache Juneau petstore sample application — Spring Boot deployment.</p>",
		"\t<p>This is the same Juneau REST surface as the Jetty deployment, deployed unchanged under Spring Boot",
		"\t   (note: child resources resolve as Spring beans).</p>",
		"</div>"
	},
	asideFloat="RIGHT"
)
@SerializerConfig(
	// Single quotes for consistency with other Juneau samples — simplifies HTML/string assertions in tests.
	quoteChar="'"
)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for example/demo code
})
public class RootResources extends BasicSpringRestServletGroup {

	private static final long serialVersionUID = 1L;
}
