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

import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.console.dev.*;
import org.apache.juneau.petstore.console.ops.*;
import org.apache.juneau.petstore.console.pets.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

/**
 * Root of the petstore admin console, mounted at {@code /console} beside the {@code /petstore} API and sharing its
 * {@link org.apache.juneau.petstore.service.PetStore}.
 *
 * <p>
 * Every page renders through {@code base.ftlh} (the {@code <@console>} chrome and nav tree).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Mount it in a runner beside the API; both see the same @Bean PetStore.</jc>
 * 	<ja>@Rest</ja>(children={PetStoreResource.<jk>class</jk>, PetstoreConsoleResource.<jk>class</jk>})
 * 	<jk>public class</jk> RootResources <jk>extends</jk> BasicRestServletGroup {
 * 		<ja>@Bean</ja> <jk>public</jk> PetStore petStore() { <jk>return</jk> PetstoreSeed.<jsm>create</jsm>().populate(<jk>new</jk> PetStore()); }
 * 	}
 * </p>
 * <p class='bcode'>
 * 	&lt;#-- base.ftlh --&gt;
 * 	&lt;@console brand="Juneau Petstore"&gt;
 * 	  &lt;@navigation&gt;&lt;@node id="store" label="Store" href="/console/store"/&gt; ... &lt;/@navigation&gt;
 * 	  &lt;@main/&gt;
 * 	&lt;/@console&gt;
 * </p>
 */
@Rest(
	path="/console",
	title="Juneau Petstore console",
	children={
		StoreRest.class,
		PetsRest.class,
		OpsRest.class,
		DevRest.class,
		AboutRest.class,
		VendorRest.class
	}
)
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class PetstoreConsoleResource extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/**
	 * {@code GET /console} redirects to the store dashboard.
	 *
	 * @param req The request.
	 * @return A 303 to {@code /console/store}.
	 */
	@RestGet(path="/")
	public SeeOther index(RestRequest req) {
		return new SeeOther().setLocation(req.getUriResolver().resolve("servlet:/store"));
	}

	/** @return The Orders placeholder (see {@link ConsoleStubs}). */
	@RestGet(path="/orders")
	public View orders() { return ConsoleStubs.view("orders"); }

	/** @return The Users placeholder (see {@link ConsoleStubs}). */
	@RestGet(path="/users")
	public View users() { return ConsoleStubs.view("users"); }
}
