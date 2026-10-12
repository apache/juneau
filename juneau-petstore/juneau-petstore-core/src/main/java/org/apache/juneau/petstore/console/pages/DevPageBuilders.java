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
package org.apache.juneau.petstore.console.pages;

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.petstore.console.dev.*;
import org.apache.juneau.petstore.console.dev.flavors.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.view.freemarker.console.*;

/**
 * Builder twins of the Developer pages: Custom cards ({@code cards.ftlh}), Rendering flavors ({@code flavors.ftlh},
 * {@code flavors-react.ftlh}), Secure ({@code secure.ftlh}) and Themes ({@code themes.ftlh}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>return</jk> <jsm>twin</jsm>(<jv>req</jv>, DevPageBuilders.<jsm>secure</jsm>());
 * </p>
 */
public final class DevPageBuilders {

	private DevPageBuilders() {}

	/**
	 * Custom cards.
	 *
	 * <p>
	 * {@link PageSpec#card(CardSpec)} seeds a custom-type card as written, without running its {@link CardTypeHandler},
	 * so the gauge card is shaped here by {@link InventoryGaugeCardType} itself: the FTL page and this twin then carry
	 * the same validated fragment.
	 *
	 * @return The page spec.
	 */
	public static PageSpec cards() {
		var gauge = CardSpec.of(InventoryGaugeCardType.TYPE, "gauge").title("Dog inventory");
		new InventoryGaugeCardType().toFragment(CardSource.create(InventoryGaugeCardType.TYPE, "gauge").body("{species:'DOG', low:20, high:120}").build())
			.forEach(gauge::body);
		return PageSpec.create().tab("dev/cards").init("/console/dev/cards/petstore-cards.js")
			.card(CardSpec.of("kpi", "available").title("Pets available")
				.body("label", "Pets available").body("dataUrl", "/console/dev/cards/stats/available"))
			.card(gauge)
			.html("toolbar", "Card API", """
				<div class="jc-prose">
				<p>
				<button type="button" class="jc-btn jc-btn-outline" data-cards-action="refresh">Refresh the gauge</button>
				<button type="button" class="jc-btn jc-btn-outline" data-cards-action="api">Read the gauge API</button>
				</p>
				<output id="cards-api-result" aria-live="polite"></output>
				</div>""")
			.html("how", "Writing a card type", """
				<div class="jc-prose">
				<p>A card type is a JS handler, and optionally a Java class that validates the body and shapes what the handler
				receives. Push handlers onto <code>window.JuneauConsoleCards</code>; the queue works whether your script loads before
				or after the console.</p>
				<pre><code>(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(['kpi', {
				  render(card, el, ctx) { ... }
				}]);</code></pre>
				<p>The gauge's Java side is <code>InventoryGaugeCardType</code>, registered with
				<code>ConsoleFreemarkerMixin.create().cardType(new InventoryGaugeCardType())</code>.</p>
				</div>""");
	}

	/**
	 * One Rendering flavors page.
	 *
	 * @param flavor The flavor.
	 * @return The page spec.
	 */
	public static PageSpec flavor(Flavor flavor) {
		var p = PageSpec.create().tab("dev/flavors/" + flavor.id()).toolkit("views")
			.html("caption", flavor.label(), "<div class=\"jc-prose\"><p>%s</p><p><a href=\"%s\">View source</a></p></div>"
				.formatted(escapeHtml(flavor.caption()), escapeHtml(flavor.sourceUrl())));
		if (flavor == Flavor.REACT)
			return p.html("flavor", "React app",
				"<iframe src=\"/petstore-ui\" title=\"Petstore React app\" class=\"petstore-flavor-frame\" width=\"100%\" height=\"640\"></iframe>");
		// Blocked: PageSpec cannot build an html card with src=, so the server flavors' "flavor" card
		// (src=/console/dev/flavors/<engine>/fragment) has no twin yet.
		return p;
	}

	/**
	 * Secure.
	 *
	 * @return The page spec.
	 */
	public static PageSpec secure() {
		return PageSpec.create().tab("dev/secure").init("/console/dev/secure/secure.js")
			.html("about", "Bearer-token guard", """
				<div class="jc-prose">
				<p>This page is open. The API under <code>/console/dev/secure/api</code> is not: every call needs an
				<code>Authorization: Bearer &lt;token&gt;</code> header, checked by a fail-closed guard.</p>
				<p>Demo tokens: <code>petstore-user</code> (alice) and <code>petstore-admin</code> (admin). Anything else is a 401.</p>
				<pre><code>curl -H 'Authorization: Bearer petstore-user' http://localhost:10000/console/dev/secure/api/pets</code></pre>
				</div>""")
			.html("try", "Try it", """
				<div class="jc-prose">
				<p>
				<button type="button" data-secure-call="none" data-secure-url="/console/dev/secure/api/whoami">Without a token</button>
				<button type="button" data-secure-call="petstore-user" data-secure-url="/console/dev/secure/api/whoami">With the demo token</button>
				<button type="button" data-secure-call="wrong-token" data-secure-url="/console/dev/secure/api/whoami">With a wrong token</button>
				</p>
				<pre><output id="secure-result" aria-live="polite"></output></pre>
				</div>""");
	}

	/**
	 * Themes.
	 *
	 * <p>
	 * The theme itself is drawn by the chrome ({@code base.ftlh}), so the spec carries the same
	 * {@code petstoreTheme} / {@code petstoreCustomTheme} attributes the FTL page does.
	 *
	 * @param theme The stock theme to draw in (the custom theme's seed when {@code custom}).
	 * @param custom Whether the custom tokens are applied.
	 * @param choices The theme names to offer.
	 * @return The page spec.
	 */
	public static PageSpec themes(String theme, boolean custom, List<String> choices) {
		var links = choices.stream().map(x -> escapeHtml(x)).map(x -> "<li><a href=\"/console/dev/themes?theme=" + x + "\">" + x + "</a></li>")
			.collect(Collectors.joining());
		return PageSpec.create().tab("dev/themes")
			.attr("petstoreTheme", theme).attr("petstoreCustomTheme", custom)
			.html("swatches", "This theme", """
				<div class="jc-prose">
				<p>The page is drawn in <strong>%s</strong>%s.</p>
				<p>
				<span class="tag stage released">Available</span>
				<span class="tag stage pending">Pending</span>
				<span class="tag stage failed">Sold</span>
				</p>
				</div>""".formatted(escapeHtml(theme), custom ? ", with two custom tokens" : ""))
			.html("picker", "Pick a theme", """
				<div class="jc-prose">
				<ul>
				%s
				</ul>
				</div>""".formatted(links))
			.html("authoring", "Writing a custom theme", """
				<div class="jc-prose">
				<p>Seed from a stock theme and override tokens. A <code>value</code> sets a token; an <code>alias</code> makes it
				follow another token.</p>
				<pre><code>&lt;@theme name="light-brown"&gt;
				&lt;@token name="--jc-pill-red-bg" value="#fdeceb"/&gt;
				&lt;@token name="--jc-tab-bar-bg" alias="var(--jc-card-bg)"/&gt;
				&lt;/@theme&gt;</code></pre>
				</div>""");
	}
}
