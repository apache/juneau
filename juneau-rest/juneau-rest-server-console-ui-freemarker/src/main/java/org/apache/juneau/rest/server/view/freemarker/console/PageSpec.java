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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;

import freemarker.template.*;

/**
 * Java description of one console page: the active tab, toolkits, page CSS and init scripts, per-request header
 * fields, the stock theme by name, and the page's cards (tables, html and registered custom types) and body attributes.
 *
 * <p>
 * A {@code PageSpec} renders nothing itself.  Its values are seeded into the request's {@link PageCapture} before any
 * directive runs, so a built page goes through the same {@code <@console>} chrome and emits the same
 * {@code #juneau-page} contract and the same toolkit asset tags as an FTL page.  Chrome (brand, nav tree, footer, theme tokens) stays in the app's
 * chrome template.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>var</jk> <jv>spec</jv> = PageSpec.<jsm>create</jsm>()
 * 		.title(<js>"SLO"</js>)
 * 		.tab(<js>"slo"</js>)
 * 		.toolkit(<js>"views"</js>)
 * 		.css(<js>"/css/slo.css"</js>)
 * 		.csrf(<jv>token</jv>, <js>"X-CSRF"</js>)
 * 		.html(<js>"intro"</js>, <js>"&lt;p&gt;Queue health&lt;/p&gt;"</js>)
 * 		.table(<jv>sloTable</jv>);
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>return</jk> PageSpec.<jsm>create</jsm>()
 * 		.tab(<js>"changes"</js>)
 * 		.table(ChangesView.<jsm>def</jsm>(), <jv>c</jv> -&gt; <jv>c</jv>.title(<js>"Changes"</js>))
 * 		.table(TasksView.<jsm>def</jsm>(), <jv>c</jv> -&gt; <jv>c</jv>.title(<js>"Tasks for the selected change"</js>)
 * 			.subscribes(Subscription.<jsm>to</jsm>(Topics.<jsm>selection</jsm>(<js>"changes"</js>)).as(<js>"params"</js>).map(<js>"changeId"</js>, <js>"ids.0"</js>)))
 * 		.card(ChartDef.<jsm>bar</jsm>(<js>"byOwner"</js>).title(<js>"Open changes by owner"</js>).src(<js>"/rest/changes/by-owner?q={q}"</js>)
 * 			.x(<js>"owner"</js>).series(<js>"open"</js>, <js>"Open"</js>).toCardSpec()
 * 			.subscribes(Subscription.<jsm>to</jsm>(Topics.<jsm>filter</jsm>(<js>"changes"</js>)).as(<js>"params"</js>).map(<js>"q"</js>, <js>"search"</js>)))
 * 		.view(<jv>req</jv>);
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1845", // Fluent-builder setters intentionally mirror field names (Juneau DSL convention)
	"java:S3776", // Validation and rendering chains many independent guards; splitting them adds no clarity.
	"javabugs:S2259" // False positive: each card.accept(c) is guarded by nn(card), which Sonar's flow analysis does not track as a null guard.
})
public final class PageSpec {

	/** Data-model attribute that carries the spec into the render. */
	public static final String ATTR = "jcPageSpec";

	private static final Set<String> USER_MENU_KEYS = Set.of("label", "initials", "avatar", "items");
	/**
	 * The three reserved types ({@code chart}, {@code job-log}, {@code terminal}) {@link #applyTo} lifts through their registered handler,
	 * with their own {@code <@card>} checks.  The other reserved types are seeded as written; adopter types are lifted
	 * through the registry too.
	 */
	private static final Set<String> LIFTED_TYPES = Set.of("chart", "job-log", "terminal");
	private static final Pattern ATTR_NAME_RE = Pattern.compile("[A-Za-z_:][-A-Za-z0-9_:.]*");

	/** One {@code header(String,String)} passthrough entry. */
	record HeaderEntry(String name, String value) {}

	private String template;
	private String title;
	private String tab;
	String theme;
	private String scheme;
	private boolean schemeToggle;
	final List<String> toolkits = new ArrayList<>();
	final List<String> css = new ArrayList<>();
	final List<String> init = new ArrayList<>();
	final Map<String,Object> attrs = new LinkedHashMap<>();
	final List<HeaderEntry> headers = new ArrayList<>();
	String headerSubtitle;
	Map<String,Object> headerUserMenu;
	final List<CardSpec> cards = new ArrayList<>();
	/** Card id to inline markup; an absent key means no markup. */
	final Map<String,String> cardMarkup = new LinkedHashMap<>();
	/** Card id to the unwrapped datatables body of each {@link #table} card, handed to the card-type registry at seed time. */
	final Map<String,Map<String,Object>> tableBodies = new LinkedHashMap<>();
	final List<TopicDecl> topics = new ArrayList<>();
	private final Map<String,Boolean> topicRetain = new LinkedHashMap<>();
	final Map<String,BridgeDecl> bridges = new LinkedHashMap<>();
	final Map<String,JsonMap> dataSources = new LinkedHashMap<>();
	/** The {@code dashboard} contract object, or <jk>null</jk> (see {@link #dashboard(DashboardSpec)}). */
	private Map<String,Object> dashboard;
	/** Body attribute name to its HTML-attribute-escaped value; the last call for a name wins. */
	final Map<String,String> bodyAttrs = new LinkedHashMap<>();
	final List<PageCapture.PendingNavAdd> navUnders = new ArrayList<>();
	final List<BadgeDef> badges = new ArrayList<>();
	Map<String,Object> facts = Map.of();

	private PageSpec() {}

	/**
	 * Creates a new, empty spec.
	 *
	 * @return A new spec.
	 */
	public static PageSpec create() {
		return new PageSpec();
	}

	/**
	 * Sets the page template, overriding the mixin's configured chrome template.
	 *
	 * @param pageTemplateName The template name (no {@code .ftlh}).
	 * @return This object.
	 */
	public PageSpec template(String pageTemplateName) {
		template = pageTemplateName;
		return this;
	}

	/**
	 * Adds a passthrough attribute, applied to the view this spec builds.
	 *
	 * @param key The attribute name.
	 * @param value The attribute value.
	 * @return This object.
	 */
	public PageSpec attr(String key, Object value) {
		if (ATTR.equals(key))
			throw iaex("PageSpec.attr key '%s' is reserved for the spec itself.", key);
		attrs.put(key, value);
		return this;
	}

	/**
	 * Adds a passthrough HTTP response header, applied to the view this spec builds.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public PageSpec header(String name, String value) {
		headers.add(new HeaderEntry(name, value));
		return this;
	}

	/**
	 * Sets the page title.  The spec always wins over {@code <@console title>}.
	 *
	 * @param v The title.
	 * @return This object.
	 */
	public PageSpec title(String v) {
		title = v;
		return this;
	}

	/**
	 * Sets the {@code <@page tab>}-equivalent active-nav path.
	 *
	 * @param idPath A {@code '/'}-separated id path, e.g. {@code "home/skills/core"}.
	 * @return This object.
	 * @throws IllegalArgumentException If blank, or any segment is blank.
	 */
	public PageSpec tab(String idPath) {
		tab = checkIdPath("tab", idPath);
		return this;
	}

	/**
	 * Validates a {@code '/'}-separated id path: the whole string and every segment must be non-blank.
	 *
	 * @param method The calling method's name, used in the error message.
	 * @param idPath The path to validate.
	 * @return {@code idPath}, unchanged.
	 * @throws IllegalArgumentException If blank, or any {@code '/'}-separated segment is blank.
	 */
	static String checkIdPath(String method, String idPath) {
		if (ib(idPath))
			throw iaex("PageSpec %s path must be a non-empty '/'-separated id path; got '%s'.", method, idPath);
		for (var segment : idPath.split("/", -1))
			if (segment.isBlank())
				throw iaex("PageSpec %s path must be a non-empty '/'-separated id path; got '%s'.", method, idPath);
		return idPath;
	}

	/**
	 * Selects a stock theme by name.  Validated immediately via {@link ConsoleChromeMixin#stockTheme(String)}.
	 *
	 * @param stockName One of {@link ConsoleChromeMixin#BUILTIN_THEME_NAMES}.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code stockName} is not a built-in stock-theme name.
	 */
	public PageSpec theme(String stockName) {
		ConsoleChromeMixin.stockTheme(stockName);
		theme = stockName;
		return this;
	}

	/**
	 * Pins the page's colour scheme.
	 *
	 * <p>
	 * {@code auto} (the default) follows the browser's {@code prefers-color-scheme}; {@code light} and {@code dark}
	 * pin the page. An explicit {@code <@console scheme="…">} on the page template wins over this value.
	 *
	 * @param name One of {@link ConsoleChromeMixin#SCHEMES}.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code name} is not a known scheme.
	 */
	public PageSpec scheme(String name) {
		if (name == null || ! ConsoleChromeMixin.SCHEMES.contains(name))
			throw iaex("Unknown scheme '%s'.  Schemes: %s.", name, String.join(", ", ConsoleChromeMixin.SCHEMES));
		scheme = name;
		return this;
	}

	/**
	 * Lets the visitor override the page's colour scheme with a stored choice.
	 *
	 * <p>
	 * An explicit {@code <@console schemeToggle="…">} on the page template wins over this value.
	 *
	 * @param value Whether the stored choice is honoured.
	 * @return This object.
	 */
	public PageSpec schemeToggle(boolean value) {
		schemeToggle = value;
		return this;
	}

	/**
	 * Adds toolkit names, appending any not already present.
	 *
	 * @param names The toolkit names.
	 * @return This object.
	 */
	public PageSpec toolkit(String...names) {
		addAllDeduped(toolkits, names);
		return this;
	}

	/**
	 * Adds page CSS URLs, appending any not already present.
	 *
	 * @param urls The CSS URLs.
	 * @return This object.
	 */
	public PageSpec css(String...urls) {
		addAllDeduped(css, urls);
		return this;
	}

	/**
	 * Adds page init-script URLs, appending any not already present.
	 *
	 * @param urls The script URLs.
	 * @return This object.
	 */
	public PageSpec init(String...urls) {
		addAllDeduped(init, urls);
		return this;
	}

	private static void addAllDeduped(List<String> target, String...values) {
		for (var v : values)
			if (! target.contains(v))
				target.add(v);
	}

	/**
	 * Sets the header's {@code subtitle} and/or {@code userMenu}.  These are the only two header fields this builder
	 * may set; every other field belongs in the chrome template.
	 *
	 * @param c Mutates a scratch {@link PageCapture.HeaderSpec}.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code c} sets any field other than {@code subtitle}/{@code userMenu}, or the
	 * 	user menu has a key the page contract does not allow.
	 */
	public PageSpec header(Consumer<PageCapture.HeaderSpec> c) {
		var scratch = new PageCapture.HeaderSpec();
		c.accept(scratch);
		checkHeaderScratch(scratch);
		if (scratch.subtitle != null)
			headerSubtitle = scratch.subtitle;
		if (scratch.userMenu != null)
			for (var k : scratch.userMenu.keySet())
				if (! USER_MENU_KEYS.contains(k))
					throw iaex("PageSpec.header userMenu key '%s' is not allowed; allowed: label, initials, avatar, items.", k);
		if (scratch.userMenu != null)
			headerUserMenu = new LinkedHashMap<>(scratch.userMenu);
		return this;
	}

	private static void checkHeaderScratch(PageCapture.HeaderSpec h) {
		if (h.title != null)
			throw headerFieldRejected("title");
		if (h.logoSrc != null || h.logoHref != null || h.logoAlt != null)
			throw headerFieldRejected("logo");
		if (h.chrome)
			throw headerFieldRejected("chrome");
		if (! h.links.isEmpty())
			throw headerFieldRejected("link");
	}

	private static IllegalArgumentException headerFieldRejected(String field) {
		return iaex("PageSpec.header may set only subtitle and userMenu; '%s' belongs in the chrome template.", field);
	}

	/**
	 * Appends a card, enforcing unique ids across every spec-declared card.
	 *
	 * @param card The card.
	 * @return {@code card}, for fluent chaining by the caller.
	 * @throws IllegalArgumentException If a card with this id was already declared.
	 */
	CardSpec addCard(CardSpec card) {
		for (var c : cards)
			if (c.id().equals(card.id()))
				throw iaex("PageSpec card id '%s' is already declared.", card.id());
		cards.add(card);
		return card;
	}

	/**
	 * Adds a {@code datatables} card for {@code table}, with no title.
	 *
	 * @param table The table.  Validated and snapshotted immediately; later changes to it do not affect this page.
	 * @return This object.
	 */
	public PageSpec table(TableSpec table) {
		return table(table, (Consumer<CardSpec>)null);
	}

	/**
	 * Adds a {@code datatables} card for {@code table}, with a title.
	 *
	 * @param table The table.
	 * @param title The card title.
	 * @return This object.
	 */
	public PageSpec table(TableSpec table, String title) {
		return table(table, c -> c.title(title));
	}

	/**
	 * Adds a {@code datatables} card for {@code table}, letting {@code card} adjust the card.
	 *
	 * <p>
	 * The table's CSS class, if set, is carried onto the card wrapper before {@code card} runs, so {@code card} may
	 * override it.  The card body is produced by the card-type registry when the spec is seeded into a page.
	 *
	 * @param table The table.
	 * @param card Mutates the card.  May be <jk>null</jk>.
	 * @return This object.
	 */
	public PageSpec table(TableSpec table, Consumer<CardSpec> card) {
		table.validate();
		var body = table.toCardBody();
		var c = CardSpec.of("datatables", table.id());
		if (nn(table.cssClass()))
			c.cssClass(table.cssClass());
		if (nn(card))
			card.accept(c);
		if (nn(c.src()) || nn(c.template()))
			throw iaex("PageSpec.table id '%s' sets src or template; the table supplies the card's data.", table.id());
		addCard(c);
		tableBodies.put(table.id(), body);
		return this;
	}

	/**
	 * Adds an {@code html} card whose body is {@code markup}, with no title.
	 *
	 * @param id The card id.
	 * @param markup The markup, emitted as {@code <template data-card=id>}.  Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code markup} is <jk>null</jk>.
	 */
	public PageSpec html(String id, String markup) {
		return html0(id, null, markup, null);
	}

	/**
	 * Adds an {@code html} card whose body is {@code markup}, with a title.
	 *
	 * @param id The card id.
	 * @param title The card title.
	 * @param markup The markup.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public PageSpec html(String id, String title, String markup) {
		return html0(id, title, markup, null);
	}

	/**
	 * Adds an {@code html} card whose body is {@code markup}, letting {@code card} adjust the card (title, CSS
	 * class, and so on).  {@code card} must not also set {@code src} or {@code template}.
	 *
	 * @param id The card id.
	 * @param markup The markup.  Must not be <jk>null</jk>.
	 * @param card Mutates the card.  May be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code markup} is <jk>null</jk>, or {@code card} sets {@code src} or {@code template}.
	 */
	public PageSpec html(String id, String markup, Consumer<CardSpec> card) {
		return html0(id, null, markup, card);
	}

	/**
	 * Adds an {@code html} card whose body is fetched from {@code src}, with a title, unsanitized.  See
	 * {@link #htmlSrc(String,String,String,boolean)} for the trust boundary.
	 *
	 * @param id The card id.
	 * @param title The card title.  May be <jk>null</jk> for no title.
	 * @param src The same-origin path of the HTML fragment endpoint.  Must not be blank.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code src} is <jk>null</jk> or blank.
	 */
	public PageSpec htmlSrc(String id, String title, String src) {
		return htmlSrc(id, title, src, false);
	}

	/**
	 * Adds an {@code html} card whose body is fetched from {@code src}, with a title, optionally rebuilt through the
	 * views attribute-allowlist sanitizer.  The equivalent of {@code <@card type="html" src="..." sanitize=true>}.
	 *
	 * <h5 class='section'>Trust boundary:</h5>
	 * <p>
	 * With {@code sanitize} <jk>false</jk> the shell fetches {@code src} and inserts the returned HTML as-is through a
	 * raw HTML-sink assignment, <b>unsanitized</b>, the same trust level as {@code footer.text} (see
	 * {@code HtmlCardType}).  The endpoint must therefore be a trusted first-party route, must never be derived from
	 * request data, and must escape any data it embeds in the fragment.  {@code src} must be a same-origin path; an
	 * absolute URL, a scheme, {@code //}, {@code ..} or a backslash is refused when the page is built, exactly as for
	 * every other card.
	 *
	 * <p>
	 * With {@code sanitize} <jk>true</jk> the fragment is rebuilt through the views attribute-allowlist sanitizer
	 * instead: script, {@code on*} handlers, {@code javascript:} and {@code data:} URLs and every attribute off the
	 * allowlist are dropped, and benign markup (text, tables, links, images) survives.  It needs the views toolkit on
	 * the page; without the toolkit nothing is painted (the shell fails closed rather than inserting the fragment
	 * unsanitized).  The sanitizer does not remove the need to escape any data you embed in the fragment.  Turn it on when the fragment's content is not fully under your control (it embeds user-supplied or
	 * third-party markup, or comes from an endpoint you do not own); leave it off for a fragment that needs
	 * {@code class}, {@code id}, {@code style}, forms, SVG or iframes, which the allowlist drops.  The default stays off.
	 *
	 * @param id The card id.
	 * @param title The card title.  May be <jk>null</jk> for no title.
	 * @param src The same-origin path of the HTML fragment endpoint.  Must not be blank.
	 * @param sanitize Whether to rebuild the fetched fragment through the sanitizer.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code src} is <jk>null</jk> or blank.
	 */
	public PageSpec htmlSrc(String id, String title, String src, boolean sanitize) {
		if (ib(src))
			throw iaex("PageSpec.htmlSrc id '%s' requires a non-blank src.", id);
		var c = CardSpec.html(id).src(src).sanitize(sanitize);
		if (nn(title))
			c.title(title);
		addCard(c);
		return this;
	}

	private PageSpec html0(String id, String title, String markup, Consumer<CardSpec> card) {
		if (markup == null)
			throw iaex("PageSpec.html id '%s' requires a non-null markup.", id);
		var c = CardSpec.html(id);
		if (nn(title))
			c.title(title);
		if (nn(card)) {
			card.accept(c);
			if (nn(c.src()) || nn(c.template()))
				throw iaex("PageSpec.html id '%s' sets a markup body; its Consumer<CardSpec> must not also set src or template.", id);
		}
		addCard(c);
		cardMarkup.put(id, markup);
		return this;
	}

	/**
	 * Adds a custom-type card with no inline markup.
	 *
	 * <p>
	 * When the page renders, a card of an adopter type (any type that is not reserved) is built as
	 * {@code <@card>} builds it: its body map is the authored body, the type's registered {@link CardTypeHandler}
	 * validates it and returns the fragment the card carries, and an invalid body fails the render with the
	 * {@code <@card>} message.  An unregistered type takes the generic passthrough, so its body map is carried as-is.
	 *
	 * @param card The card.  Its type must not be a removed type or one with a dedicated method.
	 * @return This object.
	 */
	public PageSpec card(CardSpec card) {
		return card(card, null);
	}

	/**
	 * Adds a custom-type card that also takes inline markup.
	 *
	 * @param card The card.  See {@link #card(CardSpec)}.
	 * @param markupOrNull The markup, or <jk>null</jk>.  For an adopter type it is the card's body, as the content of
	 * 	{@code <@card>} is: the handler parses it (JSON5) or captures it as {@code <template data-card=card.id()>}, and
	 * 	{@code subscribes} / {@code publishes} keys in JSON5 markup are wired onto the card.  A card takes at most one of a
	 * 	body map, non-blank markup, {@code template} or {@code src}.
	 * @return This object.
	 */
	public PageSpec card(CardSpec card, String markupOrNull) {
		checkCardType(card);
		addCard(card);
		if (nn(markupOrNull))
			cardMarkup.put(card.id(), markupOrNull);
		return this;
	}

	/**
	 * Adds the page's dashboard: a rail of filter sections beside a grid of card panels.
	 *
	 * <p>
	 * The panel and card-section cards built by the dashboard are added to this page as ordinary cards, and the
	 * {@code dashboard} toolkit pack is added (just before {@code charts} when that is listed).  A by-id panel
	 * ({@code panel("notes")}) must name a card added <i>before</i> this call, for example with {@link #html(String, String)}.
	 * </p>
	 *
	 * @param dashboard The dashboard.
	 * @return This object.
	 * @throws IllegalArgumentException E-77 on any dashboard problem, or if the page already has a dashboard.
	 */
	public PageSpec dashboard(DashboardSpec dashboard) {
		if (dashboard == null)
			throw iaex("PageSpec.dashboard requires a dashboard.");
		if (this.dashboard != null)
			throw Section.e77(dashboard.id(), "a page has at most one dashboard");
		var byId = new LinkedHashMap<String,CardSpec>();
		for (var c : cards)
			byId.put(c.id(), c);
		var json = dashboard.toJson(byId);
		for (var c : dashboard.ownedCards())
			card(c);
		var pack = ToolkitPackRegistry.PACK_DASHBOARD;
		if (! toolkits.contains(pack)) {
			var charts = toolkits.indexOf(ToolkitPackRegistry.PACK_CHARTS);
			if (charts >= 0)
				toolkits.add(charts, pack);
			else
				toolkits.add(pack);
		}
		this.dashboard = json;
		return this;
	}

	private static void checkCardType(CardSpec card) {
		if (card == null)
			throw iaex("PageSpec.card requires a non-null card.");
		var type = card.type();
		if (ib(type))
			throw iaex("PageSpec.card id '%s' requires a non-blank type.", card.id());
		if (CardDirectiveModel.REMOVED_TYPES.contains(type))
			throw iaex("PageSpec.card type '%s' was removed in 10.0.0; use PageSpec.html(...) with a markup/template, or a registered card type.", type);
		if ("html".equals(type))
			throw iaex("PageSpec.card type '%s' has a dedicated method; use PageSpec.%s(...).", type, "html");
		if ("datatables".equals(type))
			throw iaex("PageSpec.card type '%s' has a dedicated method; use PageSpec.%s(...).", type, "table");
		var registry = CardTypeRegistry.standard();
		if (CardTypeRegistry.RESERVED.contains(type) && ! registry.isRegistered(type))
			throw iaex("PageSpec.card type '%s' is reserved and has no card-type handler; it cannot be built with PageSpec.", type);
	}

	/**
	 * Sets the CSRF body attributes: {@code data-juneau-csrf} and, if {@code headerName} is non-blank,
	 * {@code data-juneau-csrf-header}.
	 *
	 * @param token The CSRF token value.
	 * @param headerName The CSRF header name, or <jk>null</jk>/blank to omit the header attribute.
	 * @return This object.
	 */
	public PageSpec csrf(String token, String headerName) {
		if (token == null)
			throw iaex("PageSpec.csrf requires a non-null token.");
		bodyAttrs.put("data-juneau-csrf", attrEscape(token));
		if (nn(headerName) && ! headerName.isBlank())
			bodyAttrs.put("data-juneau-csrf-header", attrEscape(headerName));
		return this;
	}

	/**
	 * Sets one {@code <body>} attribute.
	 *
	 * @param name The attribute name.
	 * @param value The attribute value; {@code & < > "} are escaped.
	 * @return This object.
	 */
	public PageSpec bodyAttr(String name, String value) {
		if (name == null || ! ATTR_NAME_RE.matcher(name).matches())
			throw iaex("PageSpec.bodyAttr name '%s' must match [A-Za-z_:][-A-Za-z0-9_:.]*.", name);
		bodyAttrs.put(name, attrEscape(value));
		return this;
	}

	private static String attrEscape(String s) {
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/** @return The declared cards, in declaration order; a snapshot. */
	public List<CardSpec> cards() {
		return List.copyOf(cards);
	}

	/**
	 * Looks up a declared card by id.
	 *
	 * @param id The card id.
	 * @return The card, or empty if none was declared with this id.
	 */
	public Optional<CardSpec> card(String id) {
		return cards.stream().filter(c -> c.id().equals(id)).findFirst();
	}

	/**
	 * Adds a header badge.  The Java twin of {@code <@badge>}.
	 *
	 * @param def The badge.  Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If {@link BadgeDef#validate()} rejects it.
	 */
	public PageSpec badge(BadgeDef def) {
		def.validate();
		badges.add(def);
		return this;
	}

	/**
	 * Seeds page-level facts, which {@code visibleWhen} rules test on the client.  Repeated calls merge, last wins, on
	 * this side only; the strict check against chrome-supplied facts runs once, at {@code </@console>}.
	 *
	 * @param v The facts.  Leaves must be strings, numbers, booleans, string lists or maps.
	 * @return This object.
	 * @throws IllegalArgumentException On a leaf of any other type.
	 */
	public PageSpec facts(Map<String,Object> v) {
		checkFactTypes("", v);
		facts = PageCapture.mergeFactsLastWins(facts, copyFacts(v));
		return this;
	}

	// Deep, unmodifiable copy, so a caller that later mutates its maps or lists cannot change the page.
	private static Map<String,Object> copyFacts(Map<String,Object> m) {
		var out = new LinkedHashMap<String,Object>();
		for (var e : m.entrySet())
			out.put(e.getKey(), copyFact(e.getValue()));
		return u(out);
	}

	@SuppressWarnings("unchecked")
	private static Object copyFact(Object v) {
		if (v instanceof Map<?,?> nested)
			return copyFacts((Map<String,Object>)nested);
		if (v instanceof List<?> list)
			return List.copyOf(list);
		return v;
	}

	static void checkFactTypes(String prefix, Map<?,?> m) {
		for (var e : m.entrySet()) {
			var path = prefix.isEmpty() ? String.valueOf(e.getKey()) : prefix + "." + e.getKey();
			var v = e.getValue();
			if (v instanceof Map<?,?> nested) {
				checkFactTypes(path, nested);
			} else if (v instanceof List<?> list) {
				for (var item : list)
					if (! (item instanceof String))
						throw badFact(path, item);
			} else if (! (v instanceof String || v instanceof Boolean || isPlainNumber(v))) {
				throw badFact(path, v);
			}
		}
	}

	private static boolean isPlainNumber(Object v) {
		return v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte || v instanceof Float
			|| v instanceof Double || v instanceof java.math.BigInteger || v instanceof java.math.BigDecimal;
	}

	private static IllegalArgumentException badFact(String path, Object v) {
		return iaex("Fact '%s' has unsupported value type '%s'; use string, number, boolean, string list or map.",
			path, v == null ? "null" : v.getClass().getSimpleName());
	}

	/**
	 * Java twin of the {@code </@console>} card-reference check, run early when this spec's own card set is final.  The
	 * nav-node check needs the resolved chrome, so it only runs at {@code </@console>}.
	 */
	private void checkBadgesAgainstOwnCards() {
		var cardIds = cards.stream().map(CardSpec::id).collect(Collectors.toSet());
		for (var b : badges) {
			var m = b.toContractMap();
			for (var card : PageCapture.badgeCardRefs(m))
				if (! cardIds.contains(card))
					throw isex(PageCapture.BADGE_CARD_MSG, m.get("id"), card);
		}
	}

	/**
	 * Queues a child to add under an existing chrome nav node, resolved once the chrome's own nav tree exists.
	 *
	 * <p>
	 * The Java twin of {@code <@node under="...">}.
	 *
	 * @param parentIdPath A {@code '/'}-separated id path to the existing ancestor node.
	 * @param adder Receives the resolved ancestor and adds its children to it.
	 * @return This object.
	 * @throws IllegalArgumentException If the path is blank, or any {@code '/'}-separated segment is blank.
	 */
	public PageSpec navUnder(String parentIdPath, PageCapture.NavAdder adder) {
		navUnders.add(new PageCapture.PendingNavAdd(checkIdPath("navUnder", parentIdPath), adder));
		return this;
	}

	/**
	 * Declares a page-level custom topic; repeatable.  Unions with FTL {@code <@topic>}.
	 *
	 * @param decl The declaration; needs {@code retain} and {@code publisher}.
	 * @return This object.
	 * @throws IllegalArgumentException E-46 if the topic is already declared with a different {@code retain} or {@code publisher}.
	 */
	public PageSpec topic(TopicDecl decl) {
		var m = decl.toMap();
		var retain = (Boolean)m.get("retain");
		var prior = topicRetain.putIfAbsent(decl.topic(), retain);
		if (prior != null && ! prior.equals(retain))
			throw iaex("topic '%s' is declared with retain=%s here and retain=%s at %s", decl.topic(), retain, prior, "PageSpec.topic");
		if (prior == null) {
			topics.add(decl);
		} else {
			var priorPub = topics.stream().filter(t -> t.topic().equals(decl.topic())).findFirst().get().toMap().get("publisher");
			if (! priorPub.equals(m.get("publisher")))
				throw iaex("topic '%s' is declared with publisher=%s here and publisher=%s at %s", decl.topic(), m.get("publisher"), priorPub, "PageSpec.topic");
		}
		return this;
	}

	/**
	 * Declares a server bridge; repeatable.  Unions with FTL {@code <@bridge>}.
	 *
	 * @param decl The bridge.
	 * @return This object.
	 * @throws IllegalArgumentException E-51 on a duplicate id, or from {@link BridgeDecl#toMap()}.
	 */
	public PageSpec bridge(BridgeDecl decl) {
		decl.toMap();   // fail at the call site, not at render
		if (bridges.putIfAbsent(decl.id(), decl) != null)
			throw iaex("bridge '%s': %s", decl.id(), "duplicate id");
		return this;
	}

	/**
	 * Declares a data source, published on {@code data:<id>}; repeatable.
	 *
	 * @param def The source.
	 * @return This object.
	 * @throws IllegalArgumentException E-76 on a duplicate id or a bad declaration; also on a null definition.
	 */
	public PageSpec data(DataSourceDef def) {
		if (def == null)
			throw iaex("PageSpec.data requires a non-null definition.");
		return addData(def.toMap());   // fail at the call site, not at render
	}

	/**
	 * Declares a derived data source, published on {@code data:<as>}; repeatable.
	 *
	 * @param def The derived source.
	 * @return This object.
	 * @throws IllegalArgumentException E-76 on a duplicate id or a bad declaration; also on a null definition.
	 * @throws IllegalStateException If {@link Derived#as(String)} was not called.
	 */
	public PageSpec data(Derived def) {
		if (def == null)
			throw iaex("PageSpec.data requires a non-null definition.");
		return addData(def.toMap());
	}

	private PageSpec addData(JsonMap m) {
		var id = (String)m.get("id");
		if (dataSources.putIfAbsent(id, m) != null)
			throw iaex(DataSourceDef.E76, id, "duplicate id");
		return this;
	}

	/** @return The template name, or <jk>null</jk> if unset (the mixin's chrome template applies). */
	public String template() {
		return template;
	}

	/** @return The tab path, or {@code ""} if unset. */
	public String tab() {
		return tab == null ? "" : tab;
	}

	/** @return The toolkit names, in declaration order. */
	public List<String> toolkits() {
		return List.copyOf(toolkits);
	}

	/**
	 * The per-render holder {@link #view(RestRequest)} puts in the data model.  It carries this spec and records
	 * whether the render adopted it, so a spec reused across requests never shares adoption state.
	 */
	static final class Seed implements FreemarkerView.MustConsume {

		private final PageSpec spec;
		private volatile boolean consumed;

		Seed(PageSpec spec) {
			this.spec = spec;
		}

		PageSpec spec() { return spec; }

		/** Applies the spec to a fresh capture and marks it adopted. */
		void adoptInto(PageCapture cap, CardDirectiveModel cardModel) throws TemplateModelException {
			spec.applyTo(cap, cardModel);
			consumed = true;
		}

		void markConsumed() { consumed = true; }

		@Override /* Overridden from FreemarkerView.MustConsume */
		public boolean consumed() { return consumed; }

		@Override /* Overridden from FreemarkerView.MustConsume */
		public String notConsumedMessage(String templateName) {
			return f("Template '%s' was rendered with a PageSpec, but the spec was never adopted; the "
				+ "template must use <@page> or include the console chrome.", templateName);
		}
	}

	/**
	 * Builds the view for this spec.
	 *
	 * <p>
	 * The template is {@link #template(String)} when set, else the {@code ConsoleFreemarkerMixin}'s chrome template.
	 * A built spec is not modified by rendering, so it may be cached and reused across requests.  Call
	 * {@code spec.view(req)} on each request: a reused {@link FreemarkerView} would share one adoption flag.
	 *
	 * @param req The current REST request.
	 * @return A view for the template, carrying this spec and every {@link #attr(String, Object)} /
	 * 	{@link #header(String, String)} passthrough.
	 * @throws IllegalStateException If no template was set and the request has no {@code ConsoleFreemarkerMixin} bean.
	 * @throws IllegalArgumentException If this spec has no page template and its bus wiring fails R-10 / R-11.
	 */
	public FreemarkerView view(RestRequest req) {
		if (req == null)
			throw iaex("PageSpec.view requires a non-null request.");
		String templateName;
		if (template != null) {
			templateName = template;
		} else {
			var ccm = resolveConsoleMixin(req);
			checkWiringEarly(ccm.cardTypes(), req);
			checkBadgesAgainstOwnCards();
			templateName = ccm.chromeTemplate();
		}
		var v = FreemarkerView.of(templateName).attr(ATTR, new Seed(this));
		for (var h : headers)
			v = v.header(h.name(), h.value());
		for (var e : attrs.entrySet())
			v = v.attr(e.getKey(), e.getValue());
		return v;
	}

	// The exact-type-else-registered-subtype lookup the renderer runs, without its two-beans ambiguity check (the
	// renderer still reports that itself at render time).
	@SuppressWarnings({
		"resource" // The request context owns its bean store.
	})
	private static ConsoleFreemarkerMixin resolveConsoleMixin(RestRequest req) {
		var store = req.getContext().getBeanStore();
		FreemarkerMixin found = store.getBean(FreemarkerMixin.class).orElse(null);
		if (found == null)
			for (var t : FreemarkerMixin.registeredSubtypes()) {
				var b = store.getBean(t).orElse(null);
				if (b != null) {
					found = b;
					break;
				}
			}
		if (! (found instanceof ConsoleFreemarkerMixin ccm))
			throw isex("PageSpec.view needs a ConsoleFreemarkerMixin bean; found '%s'.",
				found == null ? "none" : found.getClass().getSimpleName());
		return ccm;
	}

	/**
	 * Applies every field this spec has set to {@code cap}, via {@code cap}'s plain (non-merge-aware) setters.
	 *
	 * <p>
	 * Called once per render, on a freshly created capture before any directive has written to it, so these raw
	 * writes can never conflict with anything.  This spec is not modified: each card is copied before it is seeded.
	 * A table card, a chart, job-log or terminal card, and a card of any adopter (non-reserved) type are lifted through the
	 * card-type registry of {@code cardModel}, with the {@code <@card>} checks and messages.  Every card's required packs are recorded through {@code cardModel}.
	 *
	 * @param cap The freshly created, still-empty capture.
	 * @param cardModel The shared {@code <@card>} directive; may be <jk>null</jk> only when this spec has no cards.
	 * @throws TemplateModelException If a card is rejected.
	 */
	void applyTo(PageCapture cap, CardDirectiveModel cardModel) throws TemplateModelException {
		if (title != null)
			cap.title(title);
		if (tab != null)
			cap.tab(tab);
		if (theme != null)
			cap.theme(theme);
		if (scheme != null)
			cap.scheme(scheme);
		if (schemeToggle)
			cap.schemeToggle(true);
		if (! toolkits.isEmpty())
			cap.toolkit(toolkits);
		if (! css.isEmpty())
			cap.css(css);
		if (! init.isEmpty())
			cap.init(init);
		if (headerSubtitle != null || headerUserMenu != null)
			cap.header(h -> {
				if (headerSubtitle != null)
					h.subtitle(headerSubtitle);
				if (headerUserMenu != null)
					h.userMenu(headerUserMenu);
			});
		if (! cards.isEmpty() && n(cardModel))
			throw FtlAttrLists.reject("PageSpec cards need the console card directive; the render has no ConsoleFreemarkerMixin.");
		for (var card : cards) {
			var markup = cardMarkup.get(card.id());
			Built built;
			if (tableBodies.containsKey(card.id()))
				built = new Built(liftTable(card, cardModel), markup);
			else if (LIFTED_TYPES.contains(card.type()))
				built = new Built(liftChecked(card, cardModel), markup);
			else if (CardTypeRegistry.RESERVED.contains(card.type()))
				built = new Built(card.copy(), markup);
			else
				built = liftCustom(card, markup, cardModel);
			cap.addBuiltCard(built.card(), built.markupOrNull());
			cardModel.recordRequirements(cap, built.card().type(), built.card().id(), List.of(), built.card().bodyMap());
		}
		try {
			for (var t : topics)
				cap.addTopic(t.toMap(), "PageSpec.topic");
			var req = FreemarkerRenderScope.request();
			for (var b : bridges.values()) {
				cap.addBridge(resolvedBridge(b, req));
			}
			for (var d : dataSources.values())
				cap.addDataSource(new JsonMap(d));
			if (! dataSources.isEmpty())
				cap.require(List.of(ToolkitPackRegistry.PACK_DATA));
		} catch (IllegalArgumentException e) {
			throw FtlAttrLists.reject(e.getMessage());
		}
		if (! bodyAttrs.isEmpty()) {
			var sb = new StringBuilder();
			for (var e : bodyAttrs.entrySet())
				sb.append(e.getKey()).append("=\"").append(e.getValue()).append("\" ");
			cap.bodyAttrs = sb.toString().stripTrailing();
		}
		for (var p : navUnders)
			cap.addPendingNavUnder(p.parentIdPath(), p.adder());
		for (var b : badges)
			cap.addBadge(b);
		if (dashboard != null)
			cap.dashboard(dashboard);
		if (! facts.isEmpty())
			cap.seedJavaFacts(facts);
	}

	/** A card ready for {@link PageCapture#addBuiltCard}, with the markup it emits as its template, or <jk>null</jk>. */
	private record Built(CardSpec card, String markupOrNull) {}

	// sanitize= is refused before any other check, as <@card> refuses it: only an html card with src= takes it.
	private static void checkSanitize(CardSpec card) throws TemplateModelException {
		if (card.sanitize())
			throw FtlAttrLists.reject(f("<@card id='%s'> sanitize= applies only to an html card with src=.", card.id()));
	}

	private CardSpec liftTable(CardSpec card, CardDirectiveModel cardModel) throws TemplateModelException {
		checkSanitize(card);
		return liftVia(card, CardSource.create(card.type(), card.id()).bodyMap(tableBodies.get(card.id())), cardModel);
	}

	private static CardSpec liftChecked(CardSpec card, CardDirectiveModel cardModel) throws TemplateModelException {
		checkSanitize(card);
		if (eq(card.type(), "terminal"))
			return liftTerminal(card, cardModel);
		if (eq(card.type(), "job-log"))
			rejectSrcAndTemplate(card);
		else if (nn(card.template()))
			throw FtlAttrLists.reject(f("<@card id='%s'> type='chart' takes a JSON5 body (plus src= for remote data); template= is not allowed.", card.id()));
		var source = CardSource.create(card.type(), card.id()).bodyMap(card.bodyMap());
		if (nn(card.src()))
			source.src(card.src());
		return liftVia(card, source, cardModel);
	}

	/** Rejects {@code src=} and/or {@code template=} on a card whose options are its body, with the directive's message. */
	private static void rejectSrcAndTemplate(CardSpec card) throws TemplateModelException {
		var src = nn(card.src()) ? card.src() : "";
		var template = nn(card.template()) ? card.template() : "";
		if (! (src.isEmpty() && template.isEmpty()))
			throw FtlAttrLists.reject(f("<@card id='%s'> type='%s' takes its options as the body; %s.", card.id(), card.type(),
				CardDirectiveModel.notAllowed(src, template)));
	}

	/**
	 * A terminal card: its body map is the {@code {contractVersion: '1', terminal: {...}}} envelope {@code <@card>} takes,
	 * with {@code contractVersion} implied, so the registered handler validates it exactly as it does for the directive.
	 */
	private static CardSpec liftTerminal(CardSpec card, CardDirectiveModel cardModel) throws TemplateModelException {
		rejectSrcAndTemplate(card);
		var envelope = new LinkedHashMap<String,Object>();
		envelope.put("contractVersion", "1");
		envelope.putAll(card.bodyMap());
		return liftVia(card, CardSource.create(card.type(), card.id()).bodyMap(envelope), cardModel);
	}

	/**
	 * An adopter card type: the same checks {@code <@card>} runs, then its registered handler (or the generic
	 * passthrough for an unregistered type) builds the fragment from the card's body map, or from its non-blank markup
	 * when it has no body map.  The markup is emitted as the card's template only when the handler captures it, as
	 * {@code <@card>} emits it; JSON5 markup a handler parses is the body, not a template.
	 */
	private static Built liftCustom(CardSpec card, String markupOrNull, CardDirectiveModel cardModel) throws TemplateModelException {
		checkSanitize(card);
		var body = card.bodyMap();
		var hasMarkup = nn(markupOrNull) && ! markupOrNull.isBlank();
		if ((body.isEmpty() ? 0 : 1) + (hasMarkup ? 1 : 0) + (nn(card.src()) ? 1 : 0) + (nn(card.template()) ? 1 : 0) > 1)
			throw FtlAttrLists.reject(f("<@card id='%s'> type='%s' takes at most one of a body, template= or src=.", card.id(), card.type()));
		var captured = new boolean[1];
		var source = CardSource.create(card.type(), card.id()).templateSink((id, markup) -> captured[0] = true);
		if (nn(card.src()))
			source.src(card.src());
		if (nn(card.template()))
			source.template(card.template());
		if (! body.isEmpty())
			source.bodyMap(body);
		else if (hasMarkup)
			source.body(markupOrNull);
		var lifted = liftVia(card, source, cardModel);
		return new Built(lifted, captured[0] ? markupOrNull : null);
	}

	private static CardSpec liftVia(CardSpec card, CardSource.Builder source, CardDirectiveModel cardModel) throws TemplateModelException {
		if (nn(card.title()))
			source.title(card.title());
		var lifted = cardModel.lift(source.build(), card);
		if (nn(card.cssClass()))
			lifted.cssClass(card.cssClass());
		if (card.bare())
			lifted.bare(true);
		return lifted;
	}

	static Map<String,Object> resolvedBridge(BridgeDecl b, RestRequest req) {
		var m = b.toMap();
		var session = (String)m.get("session");
		if (nn(req))
			m.put("session", req.getUriResolver().resolve(session));
		else if (! session.startsWith("/"))
			throw iaex("BridgeDecl '%s' session '%s' needs a request to resolve.", b.id(), session);
		return m;
	}

	/**
	 * R-10 / R-11 over this spec alone, when it has no page template.  With a page template the FTL may add
	 * publishers or cards, so R-10 waits for {@code </@console>}.
	 *
	 * @param registry The console mixin's card-type registry.
	 * @param req The current request, used to resolve {@code servlet:} / {@code context:} bridge sessions; may be <jk>null</jk>
	 * 	only when no bridge uses one.
	 * @throws IllegalArgumentException With the first wiring problem.
	 */
	void checkWiringEarly(CardTypeRegistry registry, RestRequest req) {
		if (template != null)
			return;
		var early = new PageCapture();
		try {
			for (var c : cards) {
				var copy = c.copy();
				if (tableBodies.containsKey(c.id()))
					copy.body("table", tableBodies.get(c.id()));
				early.addCard(copy, null);
			}
		} catch (TemplateModelException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		for (var t : topics)
			early.addTopic(t.toMap(), "PageSpec.topic");
		for (var b : bridges.values())
			early.addBridge(resolvedBridge(b, req));
		for (var d : dataSources.values())
			early.addDataSource(new JsonMap(d));
		if (dashboard != null)
			early.dashboard(dashboard);
		early.checkWiring(registry);
	}
}
