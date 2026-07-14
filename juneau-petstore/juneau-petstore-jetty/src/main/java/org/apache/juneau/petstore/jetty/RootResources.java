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
package org.apache.juneau.petstore.jetty;

import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.microservice.examples.*;
import org.apache.juneau.petstore.rest.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.widget.*;

/**
 * Root router resource for the Jetty deployment.
 *
 * <p>
 * Mounts the deployment-agnostic {@link PetStoreResource} from {@code juneau-petstore-core} and the
 * Jetty/Microservice-specific admin resources ({@link ConfigResource}, {@link LogsResource},
 * {@link ShutdownResource}).  The admin trio is a documented Jetty-only non-parity feature — the Spring Boot
 * deployment in {@code juneau-petstore-springboot} does not mount it.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 *
 * @serial exclude
 */
@Rest(
	title="Juneau Petstore (Jetty)",
	description="Apache Juneau petstore sample application running under Jetty/Microservice.",
	children={
		PetStoreResource.class,
		PetSecureResource.class,
		PetMustacheViewResource.class,
		PetFreemarkerViewResource.class,
		PetstoreUiResource.class,
		ConfigResource.class,
		LogsResource.class,
		ShutdownResource.class
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
		"source: $C{Source/gitHub}/org/apache/juneau/petstore/jetty/RootResources.java"
	},
	aside={
		"<div class='text'>",
		"\t<p>Apache Juneau petstore sample application.</p>",
		"\t<p>This is a 'router' page that serves as a jumping-off point to the petstore CRUD resource and",
		"\t   the microservice admin endpoints.</p>",
		"\t<p>Click <span class='link'>API</span> for the generated swagger doc, or <span class='link'>STATS</span>",
		"\t   for runtime statistics.</p>",
		"</div>"
	},
	asideFloat="RIGHT"
)
@SerializerConfig(
	// For consistency with other Juneau samples — single quotes simplify HTML/string assertions in tests.
	quoteChar="'"
)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for example/demo code
})
public class RootResources extends BasicRestServletGroup {

	private static final long serialVersionUID = 1L;
}
