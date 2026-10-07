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
package org.apache.juneau.petstore.console.dev.flavors;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.mustache.*;

/**
 * The Mustache rendering flavor: the pet list rendered from {@code console/fragments/pets.mustache}.
 *
 * <p>
 * The page itself is a console page (FreeMarker); only the fragment is Mustache, so this resource adds the Mustache
 * renderer beside the inherited FreeMarker one.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>return</jk> MustacheView.<jsm>of</jsm>(<js>"pets"</js>).attr(<js>"pets"</js>, FlavorPets.<jsm>rows</jsm>(store()));
 * </p>
 */
@Rest(
	path="/mustache",
	title="Mustache flavor",
	responseProcessors={MustacheViewRenderer.class}
)
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class MustacheFlavorRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/** Where the Mustache fragment lives. */
	public static final String FRAGMENTS = "/org/apache/juneau/petstore/console/fragments/";

	/** @return The Mustache engine for the fragment. */
	@Bean
	public MustacheMixin mustache() {
		return MustacheMixin.create().basePath(FRAGMENTS).templateSuffix(".mustache").build();
	}

	/** @return The console page. */
	@RestGet(path="/")
	public View page() {
		return Flavor.MUSTACHE.page();
	}

	/** @return The pet list fragment. */
	@RestGet(path="/fragment")
	public View fragment() {
		return MustacheView.of("pets").attr("pets", FlavorPets.rows(store()));
	}
}
