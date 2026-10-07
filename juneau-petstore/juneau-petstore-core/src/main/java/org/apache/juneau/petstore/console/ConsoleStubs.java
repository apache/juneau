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

import java.util.*;

import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;

/**
 * The console's placeholder pages: every nav node whose real page has not landed yet is served from here as a
 * 200 "Coming soon" page inside the normal chrome, so no nav link 404s.
 *
 * <p>
 * This is the one place that lists the stubs.  When a real page lands, delete its row here and the
 * {@code stub(...)} route that served it.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(path=<js>"/orders"</js>)
 * 	<jk>public</jk> View orders() { <jk>return</jk> ConsoleStubs.<jsm>view</jsm>(<js>"orders"</js>); }
 * </p>
 */
public final class ConsoleStubs {

	/**
	 * One placeholder page.
	 *
	 * @param tab The nav path it sits at, e.g. {@code "pets/sold"}.
	 * @param href The URL it is served at.
	 * @param title The page title.
	 * @param pending One line naming what is still to come.
	 */
	public record Stub(String tab, String href, String title, String pending) {}

	/** Every stub, in nav order. */
	public static final List<Stub> ALL = List.of(
		new Stub("pets", "/console/pets", "Pets", "The pets table with search, add, edit and bulk actions."),
		new Stub("pets/sold", "/console/pets/sold", "Sold pets", "The list of pets that have been sold."),
		new Stub("pets/changes", "/console/pets/changes", "Pending changes", "Review and apply staged pet edits."),
		new Stub("orders", "/console/orders", "Orders", "The orders table and order details."),
		new Stub("users", "/console/users", "Users", "The users table and user details.")
	);

	private ConsoleStubs() {}

	/**
	 * Renders the placeholder for a nav path.
	 *
	 * @param tab The nav path; must be one of {@link #ALL}.
	 * @return The page view.
	 * @throws IllegalArgumentException If no stub is registered for it.
	 */
	public static View view(String tab) {
		var s = ALL.stream().filter(x -> x.tab().equals(tab)).findFirst()
			.orElseThrow(() -> new IllegalArgumentException("No console stub for '" + tab + "'"));
		return FreemarkerView.of("stub.ftlh").attr("tab", s.tab()).attr("title", s.title()).attr("pending", s.pending());
	}
}
