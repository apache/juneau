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

import java.util.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;

/**
 * The four petstore rendering flavors shown under Developer &gt; Rendering flavors.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// The console page for one flavor.</jc>
 * 	View <jv>page</jv> = Flavor.<jsm>of</jsm>(<js>"mustache"</js>).page();
 * </p>
 */
public enum Flavor {

	/** HTML5 bean builder, serialized by {@code HtmlSerializer}. */
	HTML("html", "Html", "The pet list built in Java with the HTML5 bean builder (div, table, tr, td) and serialized as a bare fragment.", "console/dev/flavors/HtmlFlavorRest.java"),

	/** FreeMarker view rendering. */
	FREEMARKER("freemarker", "Freemarker", "The pet list rendered from a FreeMarker template through the FreeMarker view bridge.", "console/dev/flavors/FreemarkerFlavorRest.java"),

	/** Mustache view rendering. */
	MUSTACHE("mustache", "Mustache", "The pet list rendered from a Mustache template through the Mustache view bridge.", "console/dev/flavors/MustacheFlavorRest.java"),

	/** The standalone React app at /petstore-ui. */
	REACT("react", "React", "The standalone React single-page app, served at /petstore-ui and embedded here unchanged.", "rest/PetstoreUiResource.java");

	private static final String SOURCE_ROOT =
		"https://github.com/apache/juneau/blob/master/juneau-petstore/juneau-petstore-core/src/main/java/org/apache/juneau/petstore/";

	private final String id;
	private final String label;
	private final String caption;
	private final String sourceFile;

	Flavor(String id, String label, String caption, String sourceFile) {
		this.id = id;
		this.label = label;
		this.caption = caption;
		this.sourceFile = sourceFile;
	}

	/**
	 * Looks up a flavor by its URL id.
	 *
	 * @param id The id, for example {@code "html"}.
	 * @return The flavor.
	 * @throws NotFound If the id names no flavor.
	 */
	public static Flavor of(String id) {
		return Arrays.stream(values()).filter(x -> x.id.equals(id)).findFirst()
			.orElseThrow(() -> new NotFound("Unknown rendering flavor '%s'", id));
	}

	/** @return The URL id. */
	public String id() { return id; }

	/** @return The caption shown above the rendered output. */
	public String caption() { return caption; }

	/** @return The GitHub URL of the class that renders this flavor. */
	public String sourceUrl() { return SOURCE_ROOT + sourceFile; }

	/** @return The console page for this flavor. */
	public View page() {
		// React embeds the standalone app; the server flavors load their fragment with src=.
		return FreemarkerView.of(this == REACT ? "flavors-react.ftlh" : "flavors.ftlh")
			.attr("engine", id)
			.attr("label", label)
			.attr("caption", caption)
			.attr("source", sourceUrl());
	}
}
