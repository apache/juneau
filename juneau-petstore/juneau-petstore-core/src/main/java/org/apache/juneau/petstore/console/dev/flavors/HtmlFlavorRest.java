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

import static org.apache.juneau.bean.html5.HtmlBuilder.*;

import java.util.*;

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

/**
 * The Html rendering flavor: the pet list built with the HTML5 bean builder.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// GET /console/dev/flavors/html/fragment returns a bare table, no page chrome:</jc>
 * 	<jc>// &lt;table class="petstore-flavor" data-flavor="html"&gt;&lt;tr&gt;&lt;th&gt;Name&lt;/th&gt;...</jc>
 * </p>
 */
@Rest(path="/html", title="Html flavor")
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class HtmlFlavorRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/** @return The console page. */
	@RestGet(path="/")
	public View page() {
		return Flavor.HTML.page();
	}

	/** @return The pet list as a bare HTML table. */
	@RestGet(path="/fragment", serializers=HtmlSerializer.class)
	public Table fragment() {
		var rows = new ArrayList<Object>();
		rows.add(tr(th("Name"), th("Species"), th("Price"), th("Status")));
		for (var r : FlavorPets.rows(store()))
			rows.add(tr(td(r.get("name")), td(r.get("species")), td(r.get("price")), td(r.get("status"))));
		return table(rows.toArray()).class_("petstore-flavor").attr("data-flavor", "html");
	}
}
