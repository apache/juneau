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

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.dev.*;
import org.apache.juneau.petstore.console.dev.flavors.*;
import org.apache.juneau.console.*;
import org.apache.juneau.console.TopicDecl.*;
import org.apache.juneau.console.freemarker.*;
import org.apache.juneau.console.freemarker.DataSourceDef.*;
import org.apache.juneau.console.views.*;
import org.apache.juneau.console.views.TableSpec.*;

/**
 * Builder twins of the Developer pages: Custom cards ({@code cards.ftlh}), Rendering flavors ({@code flavors.ftlh},
 * {@code flavors-react.ftlh}), Secure ({@code secure.ftlh}), Themes ({@code themes.ftlh}), Charts (builder only),
 * Data sources (builder only) and Dashboard (builder only).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>return</jk> <jsm>twin</jsm>(<jv>req</jv>, DevPageBuilders.<jsm>secure</jsm>());
 * </p>
 */
@SuppressWarnings({
	"java:S1192" // Page-definition ids and labels repeat as inline literals by design.
})
public final class DevPageBuilders {

	private DevPageBuilders() {}

	/**
	 * Custom cards.
	 *
	 * <p>
	 * The gauge card's body is the same {@code {species, low, high}} the FTL page authors: {@link PageSpec} runs it
	 * through the registered {@link InventoryGaugeCardType}, so both pages carry the same validated fragment.
	 *
	 * @return The page spec.
	 */
	public static PageSpec cards() {
		var gauge = CardSpec.of(InventoryGaugeCardType.TYPE, "gauge").title("Dog inventory")
			.body("species", "DOG").body("low", 20).body("high", 120);
		return PageSpec.create().tab("dev/cards").init("/console/dev/cards/petstore-cards.js")
			.card(CardSpec.of("kpi", "available").title("Pets available")
				.body("label", "Pets available").body("dataUrl", "/console/dev/cards/stats/available"))
			.card(gauge)
			.card(ChartDef.bar("bands").title("Stock by species").src("/console/dev/cards/inventory/all")
				.x("species").series("available", "Available").series("pending", "Pending").series("sold", "Sold")
				.stacked(true).horizontal().decorate("petstore.stockTotals", null).on("click", "app.band-picked").toCardSpec())
			.card(ChartDef.scatter("prices").title("Prices by pet").src("/console/dev/cards/prices")
				.x("id", "number", "Pet").series("price", "Price")
				.option("{dataZoom:[{type:'inside'},{type:'slider'}]}").publishZoom().toCardSpec())
			.html("toolbar", "Card API", """
				<div class="jc-prose">
				<p>
				<button type="button" class="jc-btn jc-btn-outline" data-cards-action="refresh">Refresh the gauge</button>
				<button type="button" class="jc-btn jc-btn-outline" data-cards-action="api">Read the gauge API</button>
				<button type="button" class="jc-btn jc-btn-outline" data-cards-action="cat">Show cats</button>
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
		return p.htmlSrc("flavor", "Pets", "/console/dev/flavors/" + flavor.id() + "/fragment");
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
				</div>""".formatted(escapeHtml(theme), custom ? ", with custom tokens" : ""))
			.html("picker", "Pick a theme", """
				<div class="jc-prose">
				<ul>
				%s
				</ul>
				</div>""".formatted(links))
			.html("authoring", "Writing a custom theme", """
				<div class="jc-prose">
				<p>Seed from the page theme and override tokens. A <code>value</code> sets a token; an <code>alias</code> makes it
				follow another token. Add <code>scheme="dark"</code> to set a token for the dark scheme only.</p>
				<pre><code>&lt;@theme&gt;
				&lt;@token name="--jc-pill-red-bg" value="#fdeceb"/&gt;
				&lt;@token name="--jc-tab-bar-bg" alias="var(--jc-card-bg)"/&gt;
				&lt;@token name="--jc-link" value="#d9a65c" scheme="dark"/&gt;
				&lt;/@theme&gt;</code></pre>
				</div>""");
	}

	/**
	 * Charts (builder only): a line chart with markers, a threshold and a click topic, and a table whose ribbon highlights a point.
	 *
	 * @return The page spec.
	 */
	public static PageSpec charts() {
		var rows = "/console/dev/charts/daily";
		return PageSpec.create().tab("dev").toolkit("views")
			.card(ChartDef.line("trend").title("Orders this week").src(rows).x("day")
				.series("placed", "Placed").series("delivered", "Delivered")
				.decorate(Markers.values(List.of(Map.of("at", "Wed", "label", "Restock"))))
				.decorate(Thresholds.at(6).label("Target").tone("ok"))
				.on("click", "app.day-picked")
				.toCardSpec())
			.table(TableSpec.create("days").dataMode(DataMode.CLIENT).dataUrl(rows)
				.columns(Column.create("day").label("Day"), Column.create("placed").label("Placed"), Column.create("delivered").label("Delivered"))
				.ribbon(RibbonItem.publish("Highlight Monday", "cmd:trend",
					Map.of("schemaVersion", 1, "op", "highlight", "options", Map.of("seriesId", "placed", "dataIndex", 0)))),
				"Days");
	}

	/**
	 * Data sources (builder only): an hourly CSV source chunked by UTC day and cached in IndexedDB, over a range the
	 * page script publishes, drawn by a line chart.
	 *
	 * @return The page spec.
	 */
	public static PageSpec data() {
		var range = Topics.custom("petstore", "range");
		return PageSpec.create().tab("dev").toolkit("views").init("/console/dev/data/data.js")
			.topic(TopicDecl.of(range).retain(true).publisher(Publisher.SCRIPT))
			.data(DataSourceDef.create("orders").url("/console/dev/data/orders").format(Format.CSV)
				.columns("t:time", "placed:number")
				.params(Subscription.to(range).as("params").map("start", "start").map("end", "end"))
				.chunk(Chunking.byDay("start", "end")).cache(Cache.indexedDb()))
			.card(ChartDef.line("ordersChart").title("Orders by hour").source(Topics.data("orders")).x("t")
				.series("placed", "Placed").toCardSpec());
	}

	/**
	 * Dashboard (builder only): a rail of every section kind over a 2-column grid of charts, bound to one CSV source.
	 *
	 * @param variant The source variant: {@code ok}, {@code slow}, {@code truncated}, {@code error} or {@code right} (the {@code ok} source with the rail on the right).
	 * @param version The persist version; bumping it discards saved state.
	 * @return The page spec.
	 */
	public static PageSpec dashboard(String variant, int version) {
		var base = "/console/dev/dashboard";
		var share = ChartDef.donut("share").title("Share by reason").src(base + "/reasons").label("reason").value("orders");
		return PageSpec.create().tab("dev").toolkit("views").init(base + "/dashboard.js")
			// An array param (reasons) repeats its key in the query, which the endpoint reads with getAll.
			.data(DataSourceDef.create("orders").url(base + "/series/" + variant).format(Format.CSV)
				.columns("day:time", "reason:string", "orders:number")
				.params(
					Subscription.to(Topics.filter("range")).as("params").map("start", "start").map("end", "end"),
					Subscription.to(Topics.filter("q")).as("params").map("q", "search"),
					Subscription.to(Topics.filter("reasons")).as("params").map("reasons", "columns.reason"),
					Subscription.to(Topics.filter("late")).as("params").map("lateOnly", "options.lateOnly"))
				.chunk(Chunking.none()).cache(Cache.none()))
			.dashboard(DashboardSpec.create("overview").title("Order issues")
				.rail(("right".equals(variant) ? Rail.right() : Rail.left()).sections(
					RangeSection.quick("7d", "30d", "ytd").custom(true).defaultPreset("30d").publishes(Topics.filter("range")),
					SearchSection.create().placeholder("Reason contains").publishes(Topics.filter("q")),
					ChoiceSection.fromDim("orders", "reason").label("Reasons").publishes(Topics.filter("reasons")),
					CardSection.of(share),
					Section.custom("petstore.late", JsonMap.of("label", "Late only")).publishes(Topics.filter("late"))))
				.grid(2)
				.panel(ChartDef.line("daily").title("Orders per day").source(Topics.data("orders")).x("day", "time", "Day")
					.series("orders", "Orders").publishZoom().patch("{dataZoom:[{type:'inside'},{type:'slider'}]}"), 2)
				.panel(ChartDef.bar("by-reason").title("Orders by reason").src(base + "/reasons").x("reason").series("orders", "Orders"))
				.panel(ChartDef.bar("by-weekday").title("Orders by weekday").src(base + "/weekdays").x("weekday").series("orders", "Orders"))
				.status(Topics.data("orders"))
				.persist(PersistSpec.page("petstore.dashboard").version(version)
					.topics(Topics.filter("range"), Topics.filter("q"), Topics.filter("reasons"), Topics.zoom("daily")).shareLink()));
	}
}
