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

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;

/**
 * Themes: the stock console themes and one custom token theme, switchable with {@code ?theme=}.
 *
 * <p>
 * The chosen name reaches {@code base.ftlh} as the {@code petstoreTheme} view attribute, which feeds
 * {@code <@console theme=...>}.  {@code ?theme=custom} also sets {@code petstoreCustomTheme}, which turns on a
 * {@code <@theme name="light-brown">} block with two {@code <@token>} overrides: a leaf and an alias.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bftl'>
 * 	&lt;@console brand="My app" theme="light-brown"&gt;
 * 	&lt;@theme name="light-brown"&gt;
 * 	&lt;@token name="--jc-pill-red-bg" value="#fdeceb"/&gt;
 * 	&lt;@token name="--jc-tab-bar-bg" alias="var(--jc-card-bg)"/&gt;
 * 	&lt;/@theme&gt;
 * 	...
 * 	&lt;/@console&gt;
 * </p>
 * <p class='bcode'>
 * 	GET /console/dev/themes?theme=gray
 * </p>
 */
@Rest(path="/themes", title="Themes")
@SuppressWarnings({
	"java:S110", // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
	"java:S2386" // CHOICES is an immutable List.copyOf(); public so templates and tests can read it.
})
public class ThemesRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/** The name {@code ?theme=} takes for the custom token theme. */
	public static final String CUSTOM = "custom";

	/** The custom theme's stock seed. */
	public static final String CUSTOM_SEED = "light-brown";

	/** Every name {@code ?theme=} accepts, stock themes first. */
	public static final List<String> CHOICES = choices();

	private static List<String> choices() {
		var l = new ArrayList<>(ConsoleChromeMixin.BUILTIN_THEME_NAMES);
		l.add(CUSTOM);
		return List.copyOf(l);
	}

	/**
	 * @param theme The theme to render in, or <jk>null</jk> for the default.
	 * @return The page.
	 * @throws BadRequest If the name is not one of {@link #CHOICES}.
	 */
	@RestGet(path="/")
	public View page(@Query("theme") String theme) {
		var name = theme == null || theme.isEmpty() ? ConsoleChromeMixin.BUILTIN_THEME_NAMES.get(0) : theme;
		if (! CHOICES.contains(name))
			throw new BadRequest("Unknown theme '%s'; choose one of: '%s'", name, String.join(", ", CHOICES));
		var custom = CUSTOM.equals(name);
		return FreemarkerView.of("themes.ftlh")
			.attr("petstoreTheme", custom ? CUSTOM_SEED : name)
			.attr("petstoreCustomTheme", custom)
			.attr("themeChoices", CHOICES);
	}
}
