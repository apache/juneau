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
import java.util.regex.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.*;

import freemarker.core.*;
import freemarker.template.*;
import freemarker.template.utility.*;

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
	private boolean themeFromDirective;
	private String tab = "";
	private String navLayout;
	private String shellUrl;
	private final HeaderSpec header = new HeaderSpec();
	private final FooterSpec footer = new FooterSpec();
	private final NavNode navRoot = NavNode.root();
	private List<String> selected;
	private List<String> activeNav = List.of();
	private final List<CardSpec> cards = new ArrayList<>();
	private final Map<String,Map<String,Object>> topics = new LinkedHashMap<>();
	private final Map<String,String> topicWhere = new HashMap<>();
	private final Map<String,Map<String,Object>> bridges = new LinkedHashMap<>();
	// Cards from a PageSpec, held until placed or the page closes.  builtCards: not yet placed, in declaration order.
	// builtCardIds: every id ever registered here, kept after placement.
	private final Map<String,CardSpec> builtCards = new LinkedHashMap<>();
	private final Set<String> builtCardIds = new LinkedHashSet<>();
	private final Map<String,String> templates = new LinkedHashMap<>();
	private final Map<String,Map<String,String>> slots = new LinkedHashMap<>();
	private List<String> toolkits = List.of();
	private List<String> init = List.of();
	private List<String> css = List.of();
	private List<String> vendorCss = List.of();
	private List<String> vendorJs = List.of();
	private List<String> runtimeCss = List.of();
	private List<String> runtimeJs = List.of();
	private final Set<String> requiredPacks = new LinkedHashSet<>();
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
	String ftlBodyAttrs;
	String themeCssUrl;
	String themeOverrideBlock;
	ThemeBuildContext themeBuild;
	final Deque<NavNode> navCursor = new ArrayDeque<>();
	boolean navOpen;
	boolean consoleOpen;
	boolean pageOpen;
	boolean consoleDone;
	boolean cardOpen;
	boolean underRootOpen;
	boolean slotOpen;
	String underRootParent = "";
	NavNode underRootSelected;

	/**
	 * A nav-tree mutation deferred until the chrome's nav tree exists.  Unlike a plain {@link Consumer}, it may
	 * throw {@link TemplateModelException}, which {@link NavNode#add(String, String, String)} does on a duplicate
	 * sibling id.
	 */
	@FunctionalInterface
	public interface NavAdder {
		/**
		 * @param parent The resolved ancestor node.
		 * @throws TemplateModelException If adding under {@code parent} fails.
		 */
		void accept(NavNode parent) throws TemplateModelException;
	}

	/** One pending nav addition: a child to add under an existing chrome node, resolved at {@code </@console>}
	 * because the chrome's own nav tree may not exist yet when the addition is recorded. */
	record PendingNavAdd(String parentIdPath, NavAdder adder) {}

	final List<PendingNavAdd> pendingNavAdds = new ArrayList<>();

	PageCapture() {}

	/**
	 * Returns the capture for this render, creating it on first use.
	 *
	 * <p>
	 * On create, a {@link PageSpec} carried in the data model under {@link PageSpec#ATTR} is applied to the new
	 * capture before any directive writes to it, and marked adopted.
	 *
	 * @param env The FreeMarker environment.
	 * @return The capture, never <jk>null</jk>.
	 * @throws TemplateModelException If seeding a {@link PageSpec} from the data model fails.
	 */
	public static PageCapture of(Environment env) throws TemplateModelException {
		var c = (PageCapture)env.getCustomState(KEY);
		if (n(c)) {
			c = new PageCapture();
			var tm = env.getDataModel().get(PageSpec.ATTR);
			if (nn(tm) && DeepUnwrap.unwrap(tm) instanceof PageSpec.Seed seed) {
				var cardModel = env.getConfiguration().getSharedVariable(CardDirectiveModel.NAME);
				seed.adoptInto(c, cardModel instanceof CardDirectiveModel m ? m : null);
			}
			env.setCustomState(KEY, c);  // Only after a successful seed, so a failed one leaves nothing half-applied.
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
		if (n(body))
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

	/**
	 * Sets the title only when none has been set yet.  A page-spec title always wins over the chrome's own
	 * {@code <@console title>} or {@code brand=} default; this is the one deliberate silent override in the merge rules.
	 *
	 * @param fallback The chrome-resolved default.  A <jk>null</jk> is treated as blank.
	 * @return This object.
	 */
	PageCapture titleDefault(String fallback) {
		if (title.isEmpty())
			title = fallback == null ? "" : fallback;
		return this;
	}

	/** @param name A stock theme name. @return This object. */
	public PageCapture theme(String name) { theme = name; return this; }

	/** @param v The {@code <@page tab>} value. @return This object. */
	public PageCapture tab(String v) { tab = n(v) ? "" : v; return this; }

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
		if (nn(selected))
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
		if (builtCardIds.contains(card.id()))
			throw FtlAttrLists.reject(String.format("<@card id='%s'> duplicates an existing card id.", card.id()));
		if (nn(markupOrNull)) {
			card.template(card.id());
			templates.put(card.id(), markupOrNull);
		}
		cards.add(card);
		return this;
	}

	/**
	 * Registers a card built by a {@code PageSpec}.  It is held apart from the authored cards until placed or the
	 * page closes, so a spec's cards follow the template's cards in declaration order.
	 *
	 * @param card The card.
	 * @param markupOrNull Markup emitted as {@code <template data-card="{id}">}, or <jk>null</jk>.
	 * @throws TemplateModelException On a duplicate card id, checked against placed and unplaced cards.
	 */
	void addBuiltCard(CardSpec card, String markupOrNull) throws TemplateModelException {
		for (var c : cards)
			if (c.id().equals(card.id()))
				throw FtlAttrLists.reject(String.format("<@card id='%s'> duplicates an existing card id.", card.id()));
		if (builtCards.containsKey(card.id()) || builtCardIds.contains(card.id()))
			throw FtlAttrLists.reject(String.format("<@card id='%s'> duplicates an existing card id.", card.id()));
		if (nn(markupOrNull)) {
			card.template(card.id());
			templates.put(card.id(), markupOrNull);
		}
		builtCards.put(card.id(), card);
		builtCardIds.add(card.id());
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

	/**
	 * Adds a top-level {@code topics} entry.  Java ({@code PageSpec.topic}) and FTL ({@code <@topic>}) declarations
	 * union; the same topic again with the same {@code retain} is a no-op.
	 *
	 * @param decl A {@code TopicDecl.toMap()} entry.
	 * @param where Where it was declared, for the E-46 message, for example {@code "<@topic>"}.
	 * @return This object.
	 * @throws IllegalArgumentException E-46 if the topic is already declared with a different {@code retain}.
	 */
	PageCapture addTopic(Map<String,Object> decl, String where) {
		var topic = (String)decl.get("topic");
		var prior = topics.get(topic);
		if (prior == null) {
			topics.put(topic, decl);
			topicWhere.put(topic, where);
		} else if (! Objects.equals(prior.get("retain"), decl.get("retain"))) {
			throw new IllegalArgumentException(String.format("topic '%s' is declared with retain=%s here and retain=%s at %s",
				topic, decl.get("retain"), prior.get("retain"), topicWhere.get(topic)));
		} else if (! Objects.equals(prior.get("publisher"), decl.get("publisher"))) {
			throw new IllegalArgumentException(String.format("topic '%s' is declared with publisher=%s here and publisher=%s at %s",
				topic, decl.get("publisher"), prior.get("publisher"), topicWhere.get(topic)));
		}
		return this;
	}

	/**
	 * Adds a top-level {@code bridges} entry, with {@code session} already resolved.
	 *
	 * @param bridge A {@code BridgeDecl.toMap()} entry.
	 * @return This object.
	 * @throws IllegalArgumentException E-51 on a duplicate bridge id.
	 */
	PageCapture addBridge(Map<String,Object> bridge) {
		var id = (String)bridge.get("id");
		if (bridges.containsKey(id))
			throw new IllegalArgumentException(String.format("bridge '%s': %s", id, "duplicate id"));
		bridges.put(id, bridge);
		return this;
	}

	/**
	 * Runs R-10 and R-11 over the captured page; called at {@code </@console>} and by {@code PageSpec.view}.
	 *
	 * @param registry The card types the page may use: the {@code ConsoleFreemarkerMixin}'s registry, so custom
	 * 	{@code Builder.cardType(...)} handlers are seen.
	 * @throws IllegalArgumentException With the first problem's message.
	 */
	void checkWiring(CardTypeRegistry registry) {
		List<BusWiringValidator.Problem> problems;
		try {
			problems = BusWiringValidator.validate(JsonMap.ofString(toContractJson()), registry);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		if (! problems.isEmpty())
			throw new IllegalArgumentException(problems.get(0).message());
	}

	/**
	 * Records a child to add under an existing chrome nav node, resolved later by {@link #applyPendingNavAdds()}.
	 *
	 * @param parentIdPath The root-first id path to the existing ancestor node.
	 * @param adder Receives the resolved ancestor and adds its children to it.
	 */
	public void addPendingNavUnder(String parentIdPath, NavAdder adder) {
		pendingNavAdds.add(new PendingNavAdd(parentIdPath, adder));
	}

	/**
	 * Resolves every pending nav addition recorded by {@link #addPendingNavUnder}, in declaration order.  The
	 * pending list is cleared first, so a second call does nothing.
	 *
	 * @throws TemplateModelException If a {@code parentIdPath} does not match an existing node.
	 */
	public void applyPendingNavAdds() throws TemplateModelException {
		var batch = List.copyOf(pendingNavAdds);
		pendingNavAdds.clear();
		for (var p : batch) {
			var parent = navRoot.find(List.of(p.parentIdPath().split("/")));
			if (parent.isEmpty())
				throw FtlAttrLists.reject(String.format(
					"Nav addition under '%s' does not match a <@node> path in the chrome; known paths: '%s'.",
					p.parentIdPath(), String.join(", ", navRoot.paths())));
			p.adder().accept(parent.get());
		}
	}

	/**
	 * Merge-aware twin of {@link #tab(String)}: first writer wins, and any second non-blank writer is rejected, even with the
	 * same value.
	 *
	 * @param tab The {@code <@page tab>} value.  Blank is a no-op.
	 * @return This object.
	 * @throws TemplateModelException If a non-blank tab was already set.
	 */
	public PageCapture mergeTab(String tab) throws TemplateModelException {
		if (tab == null || tab.isBlank())
			return this;
		if (this.tab.isEmpty()) {
			this.tab = tab;
			return this;
		}
		throw FtlAttrLists.reject(String.format(
			"PageSpec sets tab='%s' and <@page tab='%s'> also sets it; set it in one place.", this.tab, tab));
	}

	/**
	 * Merge-aware twin of {@link #theme(String)}: first writer wins, a repeat of the same value is a no-op, and a
	 * differing second writer is rejected.
	 *
	 * @param name The {@code <@theme name>} value.  Blank is a no-op.
	 * @return This object.
	 * @throws TemplateModelException If a different, non-blank theme was already set.
	 */
	public PageCapture mergeTheme(String name) throws TemplateModelException {
		if (name == null || name.isBlank())
			return this;
		if (theme == null || theme.isBlank()) {
			theme = name;
			themeFromDirective = true;
			return this;
		}
		if (theme.equals(name))
			return this;
		if (themeFromDirective)
			throw FtlAttrLists.reject(String.format(
				"<@theme name='%s'> conflicts with an earlier <@theme name='%s'> on this page; use one.", name, theme));
		throw FtlAttrLists.reject(String.format(
			"PageSpec theme '%s' conflicts with <@theme name='%s'>; remove name= from <@theme> to inherit the "
				+ "page theme.", theme, name));
	}

	/** @return The current theme name, or <jk>null</jk> if neither a spec nor a directive has set one yet. */
	String themeOrNull() { return theme; }

	/**
	 * Merge-aware setter for the raw {@code <@body>} attribute text: union by attribute name, existing (spec) text
	 * first and then the new text.  The same attribute name on both sides is always rejected, even when the values
	 * agree.  Called once, from {@link #checkConsoleClose()}, with the text {@code <@body>} captured into
	 * {@link #ftlBodyAttrs}.
	 *
	 * @param attrs The raw {@code <@body>} attribute text.  Blank is a no-op.
	 * @return This object.
	 * @throws TemplateModelException If an attribute name already set is set again.
	 */
	public PageCapture mergeBodyAttrs(String attrs) throws TemplateModelException {
		if (attrs == null || attrs.isBlank())
			return this;
		if (bodyAttrs == null || bodyAttrs.isBlank()) {
			bodyAttrs = attrs;
			return this;
		}
		var existing = bodyAttrNames(bodyAttrs);
		for (var name : bodyAttrNames(attrs))
			if (existing.contains(name))
				throw FtlAttrLists.reject(String.format(
					"PageSpec body attribute '%s' is also set by <@body>; set it in one place.", name));
		bodyAttrs = (bodyAttrs + " " + attrs).strip();
		return this;
	}

	// Matches only double-quoted attributes and assumes <@body> markup is well-formed by construction, as the
	// spec side always is, so a targeted name scan is enough here.
	private static final Pattern BODY_ATTR_NAME = Pattern.compile("([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*\"");

	private static Set<String> bodyAttrNames(String text) {
		var names = new LinkedHashSet<String>();
		var m = BODY_ATTR_NAME.matcher(text);
		while (m.find())
			names.add(m.group(1));
		return names;
	}

	/**
	 * Whether the merged body-attribute text already sets {@code name}.  Used so a spec- or {@code <@body>}-authored
	 * CSRF attribute is not emitted a second time by the request-attribute auto-detection.
	 *
	 * @param name An HTML attribute name.
	 * @return Whether the merged text sets it.
	 */
	boolean hasBodyAttr(String name) {
		return bodyAttrs != null && bodyAttrNames(bodyAttrs).contains(name);
	}

	/**
	 * Union merge for {@link #toolkit(List)}: appends any name not already present, in first-seen order.
	 *
	 * @param names The names to union in.
	 * @return This object.
	 */
	public PageCapture mergeToolkit(List<String> names) {
		toolkits = mergeUnion(toolkits, names);
		return this;
	}

	/**
	 * Union merge for the page stylesheets: appends any URL not already present, in first-seen order.
	 *
	 * @param hrefs The URLs to union in.
	 * @return This object.
	 */
	public PageCapture mergeCss(List<String> hrefs) {
		css = mergeUnion(css, hrefs);
		return this;
	}

	/**
	 * Union merge for the page init scripts: appends any URL not already present, in first-seen order.
	 *
	 * @param srcs The URLs to union in.
	 * @return This object.
	 */
	public PageCapture mergeInit(List<String> srcs) {
		init = mergeUnion(init, srcs);
		return this;
	}

	// Dedupes within a single list too, not only across the two sources: css=["a.css","a.css"] yields one link.
	private static List<String> mergeUnion(List<String> existing, List<String> additions) {
		var out = new ArrayList<>(existing);
		for (var a : additions)
			if (! out.contains(a))
				out.add(a);
		return List.copyOf(out);
	}

	void css(List<String> v) { css = List.copyOf(v); }
	void init(List<String> v) { init = List.copyOf(v); }
	void toolkitAssets(ToolkitPackRegistry.Resolved r) {
		vendorCss = r.vendorCss();
		vendorJs = r.vendorJs();
		runtimeCss = r.runtimeCss();
		runtimeJs = r.runtimeJs();
	}
	void shellUrl(String v) { shellUrl = v; }
	StringWriter pageBuffer() { return pageBuffer; }
	StringWriter consoleBuffer() { return consoleBuffer; }
	List<String> cssHrefs() { return css; }
	List<String> initScripts() { return init; }
	List<String> vendorCss() { return vendorCss; }
	List<String> vendorJs() { return vendorJs; }
	List<String> runtimeCss() { return runtimeCss; }
	List<String> runtimeJs() { return runtimeJs; }

	/**
	 * Records toolkit packs the page needs, in first-seen order.  Called by {@code <@card>} for each authored card
	 * and by other directives that emit markup needing a pack.
	 *
	 * @param packs The pack names.
	 * @return This object.
	 */
	public PageCapture require(Collection<String> packs) {
		requiredPacks.addAll(packs);
		return this;
	}

	/** @return The recorded pack names, in first-seen order. */
	public Set<String> requiredPacks() { return Collections.unmodifiableSet(requiredPacks); }

	/** @return <jk>true</jk> while a {@code <@page>} body is rendering. */
	public boolean inPage() { return pageOpen; }
	String title() { return title; }
	List<CardSpec> unplacedCards() { return List.copyOf(builtCards.values()); }
	String theme() { return theme; }
	String headerSubtitle() { return header.subtitle; }
	Map<String,Object> headerUserMenu() { return header.userMenu; }
	boolean hasHeaderTitle() { return nn(header.title); }

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

	/** {@code </@console>}: E-13 then E-12, then merge the {@code <@body>} attributes with any spec-supplied ones, then
	 * resolve pending nav additions (E-B8), all before the caller's own {@code resolveActiveNav()}. */
	void checkConsoleClose() throws TemplateModelException {
		if (mainCount != 1)
			throw FtlAttrLists.reject(String.format("<@console> requires exactly one <@main/>; found '%s'.", mainCount));
		if (! consoleBuffer.toString().isBlank())
			throw FtlAttrLists.reject("<@console> has markup after <@main/>; move it into <@footer> or <@scripts>.");
		if (ftlBodyAttrs != null)
			mergeBodyAttrs(ftlBodyAttrs);
		applyPendingNavAdds();
	}

	/** §4.3: explicit selection, else tab (E-7 when it does not resolve), else empty. */
	void resolveActiveNav() throws TemplateModelException {
		if (nn(selected)) {
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
	 * <p>
	 * No longer used by the renderer: every {@code type="datatables"} card now requires the {@code "datatables-glue"}
	 * pack.  Kept because it is public.
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
		if (nn(theme))
			m.put("theme", Map.of("name", theme));
		var h = headerMap();
		if (! h.isEmpty())
			m.put("header", h);
		var f = footerMap();
		if (! f.isEmpty())
			m.put("footer", f);
		if (nn(navLayout))
			m.put("navLayout", navLayout);
		m.put("nav", navRoot.toList());
		m.put("activeNav", activeNav);
		m.put("cards", cards.stream().map(CardSpec::toMap).toList());
		if (! topics.isEmpty())
			m.put("topics", List.copyOf(topics.values()));
		if (! bridges.isEmpty())
			m.put("bridges", List.copyOf(bridges.values()));
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
		if (nn(header.title)) m.put("title", header.title);
		if (nn(header.subtitle)) m.put("subtitle", header.subtitle);
		if (nn(header.logoSrc)) {
			var l = new LinkedHashMap<String,Object>();
			l.put("src", header.logoSrc);
			if (nn(header.logoHref)) l.put("href", header.logoHref);
			if (nn(header.logoAlt)) l.put("alt", header.logoAlt);
			m.put("logo", l);
		}
		if (! header.links.isEmpty()) m.put("links", header.links);
		if (nn(header.userMenu)) m.put("userMenu", header.userMenu);
		if (header.chrome) m.put("chrome", true);
		if (slots.containsKey("header")) m.put("slots", slots.get("header"));
		return m;
	}

	private Map<String,Object> footerMap() {
		var m = new LinkedHashMap<String,Object>();
		if (nn(footer.text)) m.put("text", footer.text);
		if (! footer.links.isEmpty()) m.put("links", footer.links);
		if (slots.containsKey("footer")) m.put("slots", slots.get("footer"));
		return m;
	}
}
