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
package org.apache.juneau.petstore.console.dev;

import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.dev.flavors.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

/**
 * The petstore console's Developer section: custom cards, themes, rendering flavors and the secure API demo.
 *
 * <p>
 * Mounted under {@link PetstoreConsoleResource} at {@code /console/dev}.  Each page is its own child resource; routes
 * whose page has not landed yet answer with a {@link ConsoleStubs} "Coming soon" page so every nav link resolves.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Mounted as a child of the console root.</jc>
 * 	<ja>@Rest</ja>(path=<js>"/console"</js>, children={StoreRest.<jk>class</jk>, DevRest.<jk>class</jk>})
 * 	<jk>public class</jk> PetstoreConsoleResource <jk>extends</jk> PetstoreConsolePage { ... }
 *
 * 	<jc>// GET /console/dev  -&gt;  303 to /console/dev/cards</jc>
 * </p>
 */
@Rest(path="/dev", title="Developer", children={FlavorsRest.class, SecureRest.class, ThemesRest.class})
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class DevRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/**
	 * Redirects the section root to its first child.
	 *
	 * @param req The request.
	 * @return A 303 to {@code cards}.
	 */
	@RestGet(path="/")
	public SeeOther index(RestRequest req) {
		return new SeeOther().setLocation(req.getUriResolver().resolve("servlet:/cards"));
	}

	/** @return The Custom cards placeholder (see {@link ConsoleStubs}). */
	@RestGet(path="/cards")
	public View customCards() { return ConsoleStubs.view("dev/cards"); }
}
