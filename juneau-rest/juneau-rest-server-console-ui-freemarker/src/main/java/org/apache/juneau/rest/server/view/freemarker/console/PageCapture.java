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

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.marshall.marshaller.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * Typed, per-render capture of one console page: theme, header, footer, nav tree, active nav, cards
 * and slot templates, held as FreeMarker {@link Environment} custom state (see {@link #KEY}) so every
 * directive in the render shares one instance. {@code <@console>} serializes it as the {@code #juneau-page}
 * contract.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Inside a custom directive that contributes a card:</jc>
 * 	<jk>public void</jk> execute(Environment <jv>env</jv>, Map <jv>params</jv>, TemplateModel[] <jv>loop</jv>,
 * 			TemplateDirectiveBody <jv>body</jv>) <jk>throws</jk> TemplateException, IOException {
 * 		PageCapture <jv>page</jv> = PageCapture.<jsm>of</jsm>(<jv>env</jv>);
 * 		<jv>page</jv>.addCard(CardSpec.<jsm>html</jsm>(<js>"intro"</js>).title(<js>"Welcome"</js>),
 * 			PageCapture.<jsm>render</jsm>(<jv>body</jv>));
 * 	}
 *
 * 	<jc>// In a test, after rendering:</jc>
 * 	String <jv>json</jv> = PageCapture.<jsm>get</jsm>(<jv>env</jv>).toContractJson();
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicated literals read more clearly inline than as constants
})
public final class PageCapture {

	/** Environment custom-state key. */
	public static final String KEY = PageCapture.class.getName();

	/** The contract version this server writes. */
	public static final String CONTRACT_VERSION = "1";

	//-----------------------------------------------------------------------------------------------------------------
	// Header / footer specs
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Mutable header fields; mirrors {@code $defs/header}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jv>page</jv>.header(<jv>h</jv> -&gt; <jv>h</jv>.title(<js>"My App"</js>).logo(<js>"/img/logo.svg"</js>, <js>"/"</js>, <js>"My App"</js>).chrome(<jk>true</jk>));
	 * </p>
	 */
	public static final class HeaderSpec {
		String title;
		String subtitle;
		String logoSrc;
		String logoHref;
		String logoAlt;
		boolean chrome;
		final List<Map<String,Object>> links = new ArrayList<>();
		Map<String,Object> userMenu;

		/** @param v The header title. @return This object. */
		public HeaderSpec title(String v) { title = v; return this; }
		/** @param v The subtitle. @return This object. */
		public HeaderSpec subtitle(String v) { subtitle = v; return this; }
		/** @param src Logo URL. @param href Link target, or <jk>null</jk>. @param alt Alt text, or <jk>null</jk>. @return This object. */
		public HeaderSpec logo(String src, String href, String alt) { logoSrc = src; logoHref = href; logoAlt = alt; return this; }
		/** @param v Whether the header and nav share the sticky {@code .jc-chrome} wrapper. @return This object. */
		public HeaderSpec chrome(boolean v) { chrome = v; return this; }
		/** @param label Link label. @param href Link target. @return This object. */
		public HeaderSpec link(String label, String href) { links.add(link0(label, href)); return this; }
		/** @param v A {@code $defs/userMenu} map. @return This object. */
		public HeaderSpec userMenu(Map<String,Object> v) { userMenu = v; return this; }
	}

	/**
	 * Mutable footer fields; mirrors {@code $defs/footer}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jv>page</jv>.footer(<jv>f</jv> -&gt; <jv>f</jv>.text(<js>"Sandbox Support Console"</js>).link(<js>"Docs"</js>, <js>"/docs"</js>));
	 * </p>
	 */
	public static final class FooterSpec {
		String text;
		final List<Map<String,Object>> links = new ArrayList<>();

		/** @param v Trusted, author-supplied HTML rendered as-is by the console shell (entities decode, markup becomes real elements); must be a literal template/config value, never request data. @return This object. */
		public FooterSpec text(String v) { text = v; return this; }
		/** @param label Link label. @param href Link target. @return This object. */
		public FooterSpec link(String label, String href) { links.add(link0(label, href)); return this; }
	}

	private static Map<String,Object> link0(String label, String href) {
		var m = new LinkedHashMap<String,Object>();
		m.put("label", label);
		m.put("href", href);
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// State
	//-----------------------------------------------------------------------------------------------------------------

	private String title = "";
	private String theme;
	private String tab = "";
	private String navLayout;
	private String shellUrl;
	private final HeaderSpec header = new HeaderSpec();
	private final FooterSpec footer = new FooterSpec();
	private final NavNode navRoot = NavNode.root();
	private List<String> selected;
	private List<String> activeNav = List.of();
	private final List<CardSpec> cards = new ArrayList<>();
	private final Map<String,String> templates = new LinkedHashMap<>();
	private final Map<String,Map<String,String>> slots = new LinkedHashMap<>();
	private List<String> toolkits = List.of();
	private List<String> init = List.of();
	private List<String> css = List.of();
	private List<String> toolkitCss = List.of();
	private List<String> toolkitJs = List.of();
	private int segments;
	private int mainCount;
	private int autoCards;
	// Stable writers: <@page>/<@console> render their bodies straight into these, so flushSegment()/markMain()
	// reset them in place instead of swapping in new instances.
	private final StringWriter pageBuffer = new StringWriter();
	private final StringWriter consoleBuffer = new StringWriter();

	// Package-private server-side slots (not in the contract).
	String head;
	String headBeforePageCss;
	String scripts;
	String scriptsAfterToolkit;
	String bodyAttrs;
	String themeCssUrl;
	String themeOverrideBlock;
	ThemeBuildContext themeBuild;
	final Deque<NavNode> navCursor = new ArrayDeque<>();
	boolean navOpen;
	boolean consoleOpen;
	boolean pageOpen;
	boolean consoleDone;
	boolean cardOpen;

	PageCapture() {}

	/**
	 * Returns the capture for this render, creating it on first use.
	 *
	 * @param env The FreeMarker environment.
	 * @return The capture, never <jk>null</jk>.
	 */
	public static PageCapture of(Environment env) {
		var c = (PageCapture)env.getCustomState(KEY);
		if (c == null) {
			c = new PageCapture();
			env.setCustomState(KEY, c);
		}
		return c;
	}

	/**
	 * Returns the capture for this render, if one exists.
	 *
	 * @param env The FreeMarker environment.
	 * @return The capture, or <jk>null</jk>.
	 */
	public static PageCapture get(Environment env) {
		return (PageCapture)env.getCustomState(KEY);
	}

	/**
	 * Renders a directive body to a string.
	 *
	 * @param body The body, or <jk>null</jk>.
	 * @return The rendered markup, or {@code ""} when the body is <jk>null</jk>.
	 * @throws TemplateException On a template error.
	 * @throws IOException On an I/O error.
	 */
	public static String render(TemplateDirectiveBody body) throws TemplateException, IOException {
		if (body == null)
			return "";
		var sw = new StringWriter();
		body.render(sw);
		return sw.toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Setters
	//-----------------------------------------------------------------------------------------------------------------

	/** @param v The page title. @return This object. */
	public PageCapture title(String v) { title = v; return this; }

	/** @param name A stock theme name. @return This object. */
	public PageCapture theme(String name) { theme = name; return this; }

	/** @param v The {@code <@page tab>} value. @return This object. */
	public PageCapture tab(String v) { tab = v == null ? "" : v; return this; }

	/** @param names Toolkit names from {@code <@page toolkit>}. @return This object. */
	public PageCapture toolkit(List<String> names) { toolkits = List.copyOf(names); return this; }

	/** @param c Mutates the header. @return This object. */
	public PageCapture header(Consumer<HeaderSpec> c) { c.accept(header); return this; }

	/** @param c Mutates the footer. @return This object. */
	public PageCapture footer(Consumer<FooterSpec> c) { c.accept(footer); return this; }

	/** @param horizontalOrVertical The nav layout. @return This object. */
	public PageCapture navLayout(String horizontalOrVertical) { navLayout = horizontalOrVertical; return this; }

	/** @return The synthetic nav root; its children are the contract's {@code nav[]}. */
	public NavNode navRoot() { return navRoot; }

	/**
	 * Explicitly selects a nav path ({@code <@node selected="true">}).
	 *
	 * @param idPath The root-first id path.
	 * @return This object.
	 * @throws TemplateModelException If a path is already selected (E-6).
	 */
	public PageCapture select(List<String> idPath) throws TemplateModelException {
		if (selected != null)
			throw FtlAttrLists.reject(String.format("<@node id='%s'> selected=true but '%s' is already selected.",
				idPath.get(idPath.size() - 1), String.join("/", selected)));
		selected = List.copyOf(idPath);
		return this;
	}

	/**
	 * Appends a card.
	 *
	 * @param card The card.
	 * @param markupOrNull Markup emitted as {@code <template data-card="{id}">}, or <jk>null</jk>.
	 * @return This object.
	 * @throws TemplateModelException On a duplicate card id (E-9).
	 */
	public PageCapture addCard(CardSpec card, String markupOrNull) throws TemplateModelException {
		for (var c : cards)
			if (c.id().equals(card.id()))
				throw FtlAttrLists.reject(String.format("<@card id='%s'> duplicates an existing card id.", card.id()));
		if (markupOrNull != null) {
			card.template(card.id());
			templates.put(card.id(), markupOrNull);
		}
		cards.add(card);
		return this;
	}

	/**
	 * Adds a header or footer slot template.
	 *
	 * @param scope {@code header} or {@code footer}.
	 * @param name The slot name.
	 * @param markup The slot markup.
	 * @return This object.
	 */
	public PageCapture addSlot(String scope, String name, String markup) {
		var id = scope + "." + name;
		slots.computeIfAbsent(scope, k -> new LinkedHashMap<>()).put(name, id);
		templates.put(id, markup);
		return this;
	}

	void css(List<String> v) { css = List.copyOf(v); }
	void init(List<String> v) { init = List.copyOf(v); }
	void toolkitAssets(List<String> cssUrls, List<String> jsUrls) { toolkitCss = List.copyOf(cssUrls); toolkitJs = List.copyOf(jsUrls); }
	void shellUrl(String v) { shellUrl = v; }
	StringWriter pageBuffer() { return pageBuffer; }
	StringWriter consoleBuffer() { return consoleBuffer; }
	List<String> cssHrefs() { return css; }
	List<String> initScripts() { return init; }
	List<String> toolkitCss() { return toolkitCss; }
	List<String> toolkitJs() { return toolkitJs; }
	String title() { return title; }
	boolean hasHeaderTitle() { return header.title != null; }

	//-----------------------------------------------------------------------------------------------------------------
	// Lifecycle hooks used by the directives
	//-----------------------------------------------------------------------------------------------------------------

	/** Turns pending non-blank {@code <@page>} body markup into a bare {@code jc-seg-N} card. */
	void flushSegment() throws TemplateModelException {
		var s = pageBuffer.toString();
		pageBuffer.getBuffer().setLength(0);
		if (s.isBlank())
			return;
		var id = "jc-seg-" + (++segments);
		addCard(CardSpec.html(id).bare(true), s);
	}

	/** P17: the next {@code jc-card-N} id for a {@code <@card>} without {@code id=}. */
	String nextCardId() {
		return "jc-card-" + (++autoCards);
	}

	/** {@code <@main/>}: the console buffer so far is the banner. */
	void markMain() {
		mainCount++;
		var s = consoleBuffer.toString();
		consoleBuffer.getBuffer().setLength(0);
		if (! s.isBlank())
			addSlot("header", "banner", s);
	}

	/** {@code </@console>}: E-13 then E-12. */
	void checkConsoleClose() throws TemplateModelException {
		if (mainCount != 1)
			throw FtlAttrLists.reject(String.format("<@console> requires exactly one <@main/>; found '%s'.", mainCount));
		if (! consoleBuffer.toString().isBlank())
			throw FtlAttrLists.reject("<@console> has markup after <@main/>; move it into <@footer> or <@scripts>.");
	}

	/** §4.3: explicit selection, else tab (E-7 when it does not resolve), else empty. */
	void resolveActiveNav() throws TemplateModelException {
		if (selected != null) {
			activeNav = selected;
			return;
		}
		if (tab.isEmpty()) {
			activeNav = List.of();
			return;
		}
		var path = List.of(tab.split("/"));
		if (navRoot.find(path).isEmpty())
			throw FtlAttrLists.reject(String.format("<@page tab='%s'> does not match a visible <@node> path; known paths: '%s'.",
				tab, String.join(", ", navRoot.paths())));
		activeNav = path;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Getters / output
	//-----------------------------------------------------------------------------------------------------------------

	/** @return The {@code <@page tab>} value, {@code ""} when absent. */
	public String tab() { return tab; }

	/** @return The toolkit names. */
	public List<String> toolkits() { return toolkits; }

	/** @return The resolved active path (empty until {@code <@console>} closes, or when there is no selection). */
	public List<String> activeNav() { return activeNav; }

	/**
	 * Whether the page has a {@code datatables} card that may run in server mode, i.e. one that needs
	 * {@code window.JuneauDataTables} (the DataTables glue).
	 *
	 * <p>
	 * A card qualifies when its {@code table} is a bare URL string (the view envelope is fetched client-side, so its
	 * {@code dataMode} cannot be known here; assumed server-capable) or an envelope whose {@code view.dataMode} is
	 * {@code "server"}.  A catalog or envelope with any other {@code dataMode} is client-mode and does not qualify.
	 *
	 * @return <jk>true</jk> if the glue should be emitted.
	 */
	public boolean hasServerModeTable() {
		for (var c : cards) {
			var m = c.toMap();
			if (neq(m.get("type"), "datatables"))
				continue;
			var table = m.get("table");
			if (table instanceof CharSequence)
				return true;
			if (table instanceof Map<?,?> t && t.get("view") instanceof Map<?,?> v && eq(v.get("dataMode"), "server"))
				return true;
		}
		return false;
	}

	/** @return The cards, in page order. */
	public List<CardSpec> cards() { return Collections.unmodifiableList(cards); }

	/** @return Template id to markup, in emit order. */
	public Map<String,String> templates() { return Collections.unmodifiableMap(templates); }

	/**
	 * Serializes the contract.
	 *
	 * @return Script-safe contract JSON (every {@code <} written as the JSON unicode escape).
	 */
	public String toContractJson() {
		var m = new LinkedHashMap<String,Object>();
		m.put("contractVersion", CONTRACT_VERSION);
		m.put("title", title);
		if (theme != null)
			m.put("theme", Map.of("name", theme));
		var h = headerMap();
		if (! h.isEmpty())
			m.put("header", h);
		var f = footerMap();
		if (! f.isEmpty())
			m.put("footer", f);
		if (navLayout != null)
			m.put("navLayout", navLayout);
		m.put("nav", navRoot.toList());
		m.put("activeNav", activeNav);
		m.put("cards", cards.stream().map(CardSpec::toMap).toList());
		return scriptSafeJson(Json.DEFAULT.write(m));
	}

	/**
	 * Writes the contract island, the templates and the shell script tag.
	 *
	 * @param out The writer.
	 * @throws IOException On an I/O error.
	 */
	public void writeBody(Writer out) throws IOException {
		out.write("<script type=\"application/json\" id=\"juneau-page\">");
		out.write(toContractJson());
		out.write("</script>");
		for (var e : templates.entrySet()) {
			var attr = e.getKey().contains(".") ? "data-slot" : "data-card";
			out.write("<template " + attr + "=\"" + e.getKey() + "\">");
			out.write(e.getValue());
			out.write("</template>");
		}
		out.write("<script src=\"" + shellUrl + "\"></script>");
	}

	static String scriptSafeJson(String json) {
		return json.replace("<", "\\u003c");
	}

	private Map<String,Object> headerMap() {
		var m = new LinkedHashMap<String,Object>();
		if (header.title != null) m.put("title", header.title);
		if (header.subtitle != null) m.put("subtitle", header.subtitle);
		if (header.logoSrc != null) {
			var l = new LinkedHashMap<String,Object>();
			l.put("src", header.logoSrc);
			if (header.logoHref != null) l.put("href", header.logoHref);
			if (header.logoAlt != null) l.put("alt", header.logoAlt);
			m.put("logo", l);
		}
		if (! header.links.isEmpty()) m.put("links", header.links);
		if (header.userMenu != null) m.put("userMenu", header.userMenu);
		if (header.chrome) m.put("chrome", true);
		if (slots.containsKey("header")) m.put("slots", slots.get("header"));
		return m;
	}

	private Map<String,Object> footerMap() {
		var m = new LinkedHashMap<String,Object>();
		if (footer.text != null) m.put("text", footer.text);
		if (! footer.links.isEmpty()) m.put("links", footer.links);
		if (slots.containsKey("footer")) m.put("slots", slots.get("footer"));
		return m;
	}
}
