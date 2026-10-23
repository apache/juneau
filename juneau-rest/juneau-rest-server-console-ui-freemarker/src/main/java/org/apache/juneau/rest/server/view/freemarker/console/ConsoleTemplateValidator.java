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

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.commons.collections.Maps;
import org.apache.juneau.rest.server.staticfile.WebJarResolver;
import org.apache.juneau.rest.server.views.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * Static lint for console FreeMarker templates. Scans each template for console directive calls and reports unknown
 * directives, unknown attributes, dangling template references, duplicate nav and card ids, non-strict booleans,
 * removed card types and globals, a chart card whose ECharts WebJar is missing (E-70), and a wrong {@code <@main/>} count, without rendering any page.
 *
 * <p>
 * The attribute sets are the same ones the directives enforce at render time. Interpolated values
 * ({@code visible="${x}"}) and unquoted expressions ({@code visible=flag}) are skipped; the render-time errors cover
 * them. Macros defined in the template, in literally {@code <#include>}d templates and in the
 * {@link #chromeTemplate(String) chrome template} are in scope, as are {@code <@ns.x>} calls on an
 * {@code <#import … as ns>} namespace.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Test</ja> <jk>void</jk> templatesLint() {
 * 		List&lt;ConsoleTemplateValidator.Finding&gt; <jv>f</jv> = ConsoleTemplateValidator.<jsm>create</jsm>()
 * 			.templateRoot(Paths.<jsm>get</jsm>(<js>"src/main/resources/templates"</js>))
 * 			.chromeTemplate(<js>"base.ftlh"</js>)
 * 			.allowDirective(<js>"pageHero"</js>)
 * 			.validateAll();
 * 		<jsm>assertTrue</jsm>(<jv>f</jv>.isEmpty(), <jv>f</jv>::toString);
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1075", // "/" is the template-name separator, not a file-system path.
	"java:S3776" // Validation and rendering chains many independent guards; splitting them adds no clarity.
})
public final class ConsoleTemplateValidator {

	/**
	 * One lint finding.
	 *
	 * @param template The template name, relative to the root, with {@code /} separators.
	 * @param line The 1-based line.
	 * @param column The 1-based column.
	 * @param rule The rule id, for example {@code unknown-attribute}.
	 * @param message The human-readable message.
	 */
	public record Finding(String template, int line, int column, String rule, String message) {
		@Override
		public String toString() {
			return template + ":" + line + ":" + column + ": [" + rule + "] " + message;
		}
	}

	@SuppressWarnings({
		"java:S5843", // Tag grammar needs this complexity; it is one regex by design.
		"java:S5998", // Lint runs on authored templates at build time, never untrusted input.
		"java:S8786" // Same: the lazy attribute scan is linear on well-formed tags.
	})
	private static final Pattern TAG = Pattern.compile("<(/?)@([A-Za-z_]\\w*(?:\\.[A-Za-z_]\\w*)?)((?:\"[^\"]*\"|'[^']*'|[^>\"'])*?)(/?)>");
	private static final Pattern ATTR = Pattern.compile("([A-Za-z_]\\w*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'/>]+))");
	private static final Pattern MACRO = Pattern.compile("<#macro\\s+([A-Za-z_]\\w*)");
	private static final Pattern INCLUDE = Pattern.compile("<#include\\s+\"([^\"]+)\"");
	private static final Pattern IMPORT = Pattern.compile("<#import\\s+\"[^\"]+\"\\s+as\\s+([A-Za-z_]\\w*)");
	private static final Pattern COMMENT = Pattern.compile("(?s)<#--.*?-->");
	private static final Pattern GLOBAL = Pattern.compile("\\b(pageBody|pageTab|pageInit|pageCss|pageToolkitCss|pageToolkitJs|pageToolkit|pageThemeCss)\\b");
	private static final Pattern VIEW_KEY = Pattern.compile("\\bview\\s*:\\s*\\{");
	private static final Pattern VIEW_VERSION = Pattern.compile("^\\s*[\"']?contractVersion[\"']?\\s*:\\s*[\"']([^\"']*)[\"']");
	private static final Pattern ASSET_REF = Pattern.compile("\\b(?:src|href)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
	private static final Pattern JUNEAU_ASSET = Pattern.compile("(?:^|/)(?:juneau-[\\w-]+\\.(?:js|css|svg)|juneau-console/chrome\\.css)(?:[?#].*)?$");
	private static final Set<String> REMOVED_TYPES = Set.of("js", "json", "calendar");
	private static final Map<String,Set<String>> STRICT = Map.of(NodeDirectiveModel.NAME, Set.of("visible", "selected"), ConsoleDirectiveModel.NAME, Set.of("chrome", "schemeToggle"));
	private static final Map<String,Set<String>> DIRECTIVES = directives();

	private static Map<String,Set<String>> directives() {
		Maps<String,Set<String>> m = mb();
		m.add(PageDirectiveModel.NAME, PageDirectiveModel.ATTRS);
		m.add(ConsoleDirectiveModel.NAME, ConsoleDirectiveModel.ATTRS);
		m.add(MainDirectiveModel.NAME, MainDirectiveModel.ATTRS);
		m.add(NavigationDirectiveModel.NAME, NavigationDirectiveModel.ATTRS);
		m.add(NodeDirectiveModel.NAME, NodeDirectiveModel.ATTRS);
		m.add(ThemeDirectiveModel.NAME, ThemeDirectiveModel.ATTRS);
		m.add(TokenDirectiveModel.NAME, TokenDirectiveModel.ATTRS);
		m.add(CardDirectiveModel.NAME, CardDirectiveModel.ATTRS);
		m.add(TopicDirectiveModel.NAME, TopicDirectiveModel.ATTRS);
		m.add(BridgeDirectiveModel.NAME, BridgeDirectiveModel.ATTRS);
		m.add(BadgeDirectiveModel.NAME, BadgeDirectiveModel.ATTRS);
		m.add(FactsDirectiveModel.NAME, Set.of());
		for (var s : ConsoleSlotDirectiveModel.SLOT_NAMES)
			m.add(s, new ConsoleSlotDirectiveModel(s).attrs());
		return u(m.build());
	}

	private static final Configuration PARSE_CFG = parseConfig();

	private static Configuration parseConfig() {
		var c = new Configuration(Configuration.VERSION_2_3_34);
		c.setRecognizeStandardFileExtensions(true);
		return c;
	}

	private Path templateRoot;
	private String classpathRoot;
	private String chromeTemplate;
	private final Set<String> allowed = new TreeSet<>();
	private UnaryOperator<String> webJarProbe = WebJarResolver::resolvePath;
	private final ToolkitPackRegistry packs = new ToolkitPackRegistry();
	private String echartsProblem;         // cached E-70 (or invalid-pack-graph) message; null when the probe passed
	private String echartsRule;            // the rule id the cached problem is reported under
	private boolean echartsProbed;

	private ConsoleTemplateValidator() {}

	/** @return A new validator with no roots. */
	public static ConsoleTemplateValidator create() {
		return new ConsoleTemplateValidator();
	}

	/**
	 * @param dir The file-system directory templates are resolved against and {@link #validateAll()} walks.
	 * @return This object.
	 */
	public ConsoleTemplateValidator templateRoot(Path dir) {
		templateRoot = dir;
		return this;
	}

	/**
	 * @param prefix The classpath prefix templates are resolved against when not found under the template root, for
	 * 	example {@code "/templates/"}.
	 * @return This object.
	 */
	public ConsoleTemplateValidator classpathRoot(String prefix) {
		classpathRoot = prefix.endsWith("/") ? prefix : prefix + "/";
		return this;
	}

	/**
	 * @param name The chrome template the {@code <@page>} directive renders. Its macros are in scope for every template.
	 * @return This object.
	 */
	public ConsoleTemplateValidator chromeTemplate(String name) {
		chromeTemplate = name;
		return this;
	}

	/**
	 * @param name An adopter-defined directive or macro name to accept, for example {@code "pageHero"}.
	 * @return This object.
	 */
	public ConsoleTemplateValidator allowDirective(String name) {
		allowed.add(name);
		return this;
	}

	// Package-private (no production caller; the mixin owns its own registry).  Names packs the app's chrome already
	// loads; a provided "echarts" pack suppresses the missing-webjar rule.
	// @throws IllegalArgumentException If a name is not a registered pack.
	ConsoleTemplateValidator providedPacks(String...names) {
		var list = Arrays.asList(names);
		packs.requireKnown(list, "providedPacks");
		packs.provide(list);
		echartsProbed = false;
		return this;
	}

	// Package-private: registers an app-supplied toolkit pack; a replacement "echarts" pack (for example one served
	// from a CDN) suppresses the missing-webjar rule.
	// The mixin's registerToolkitPack(String, List, List) overload is deliberately absent: it builds a pack from asset
	// lists, which cannot change whether the "charts" WebJars resolve, so no lint rule depends on it.
	ConsoleTemplateValidator registerToolkitPack(ToolkitPack pack) {
		packs.register(rnn(pack));
		echartsProbed = false;
		return this;
	}

	// Package-private: tests simulate an absent WebJar.
	ConsoleTemplateValidator webJarProbe(UnaryOperator<String> probe) {
		webJarProbe = rnn(probe);
		echartsProbed = false;
		return this;
	}

	// The E-70 message when the WebJars the "charts" pack reaches are missing, or null.  Probes the pack the chart card
	// resolves at runtime (so a replaced "charts" or "echarts" pack is honoured) through the registry's own provided and
	// replaced-pack knowledge; probed at most once per configuration, not once per chart card.  A broken pack graph
	// (unknown dependsOn, cycle) is reported as an invalid-pack-graph finding instead of crashing the lint.
	private String echartsProblem() {
		if (! echartsProbed) {
			echartsProblem = null;
			echartsRule = "missing-webjar";
			try {
				packs.checkWebJars(List.of(ToolkitPackRegistry.PACK_CHARTS), webJarProbe);
			} catch (IllegalStateException e) {
				echartsProblem = e.getMessage();
			} catch (IllegalArgumentException e) {
				echartsProblem = e.getMessage();
				echartsRule = "invalid-pack-graph";
			}
			echartsProbed = true;
		}
		return echartsProblem;
	}

	/**
	 * @param templateName The template to lint, relative to the template root or classpath root.
	 * @return The findings, sorted by line and column. Empty when the template is clean.
	 * @throws IllegalArgumentException If the template cannot be found.
	 */
	public List<Finding> validate(String templateName) {
		var src = load(templateName);
		if (n(src))
			throw iaex("Template '%s' not found under templateRoot or classpathRoot.", templateName);
		return validateSource(templateName, src);
	}

	/**
	 * Lints every {@code *.ftlh} and {@code *.ftl} file under the template root.
	 *
	 * @return The findings, sorted by template, line and column. Empty when every template is clean.
	 * @throws IllegalStateException If no template root is set.
	 */
	public List<Finding> validateAll() {
		if (n(templateRoot))
			throw isex("validateAll() requires templateRoot(Path); a classpath root cannot be listed.");
		try (Stream<Path> s = Files.walk(templateRoot)) {
			var names = s.filter(Files::isRegularFile)
				.map(p -> templateRoot.relativize(p).toString().replace(File.separatorChar, '/'))
				.filter(n -> n.endsWith(".ftlh") || n.endsWith(".ftl"))
				.sorted()
				.toList();
			var out = new ArrayList<Finding>();
			for (var n : names)
				out.addAll(validate(n));
			return out;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// Package-private: lints a source string as if it were the template with the given name (unit tests).
	@SuppressWarnings({
		"java:S3776" // One pass over the directive tags; splitting it would scatter the lint rules.
	})
	List<Finding> validateSource(String name, String source) {
		var out = new ArrayList<Finding>();
		var lines = lineStarts(source);
		parse(name, source, out);
		var text = blankComments(source);

		var macros = new HashSet<String>(allowed);
		var namespaces = new HashSet<String>();
		collectScope(name, text, macros, namespaces, new HashSet<>());
		if (nn(chromeTemplate) && neq(chromeTemplate, name)) {
			var chrome = load(chromeTemplate);
			if (n(chrome))
				throw iaex("Chrome template '%s' not found under templateRoot or classpathRoot.", chromeTemplate);
			collectScope(chromeTemplate, blankComments(chrome), macros, namespaces, new HashSet<>());
		}

		var siblings = new ArrayDeque<Set<String>>();
		siblings.push(new HashSet<>());
		var cardIds = new HashSet<String>();
		var navDepth = 0;
		var templateRefs = new ArrayList<Object[]>();   // each entry pairs a template name with its source offset
		var consoleAt = -1;
		var mains = 0;

		var m = TAG.matcher(text);
		while (m.find()) {
			var closing = ! m.group(1).isEmpty();
			var dname = m.group(2);
			var selfClosing = ! m.group(4).isEmpty();
			var at = m.start();
			var known = closing ? null : DIRECTIVES.get(dname);
			if (closing) {
				if ((dname.equals(NodeDirectiveModel.NAME) || dname.equals(NavigationDirectiveModel.NAME)) && siblings.size() > 1)
					siblings.pop();
				if (dname.equals(NavigationDirectiveModel.NAME) && navDepth > 0)
					navDepth--;
			} else if (n(known)) {
				var dot = dname.indexOf('.');
				var ok = dot > 0 ? namespaces.contains(dname.substring(0, dot)) : macros.contains(dname);
				if (! ok)
					add(out, name, lines, at, "unknown-directive", f("<@%s> is not a console directive, a macro in scope, or allow-listed.", dname));
			} else {
				var attrs = attrs(m.group(3));
				for (var a : attrs.keySet())
					if (! known.contains(a))
						add(out, name, lines, at, "unknown-attribute", f("<@%s> unknown attribute '%s'.", dname, a));
				for (var a : STRICT.getOrDefault(dname, Set.of())) {
					var v = attrs.get(a);
					if (nn(v) && v.literal && !eqa(v.value, "true", "false"))
						add(out, name, lines, at, "strict-boolean", f("<@%s> %s= must be true or false; got '%s'.", dname, a, v.value));
				}
				var id = attrs.get("id");
				switch (dname) {
					case NavigationDirectiveModel.NAME -> {
						if (! selfClosing) {
							siblings.push(new HashSet<>());
							navDepth++;
						}
					}
					case NodeDirectiveModel.NAME -> {
						if (nn(id) && id.literal && ! siblings.peek().add(id.value))
							add(out, name, lines, at, "duplicate-id", f("<@node id='%s'> duplicates a sibling id.", id.value));
						if (navDepth > 0 && attrs.containsKey("under"))
							add(out, name, lines, at, "unknown-attribute", f("<@%s> unknown attribute '%s'.", dname, "under"));
						if (! selfClosing)
							siblings.push(new HashSet<>());
					}
					case CardDirectiveModel.NAME -> {
						if (nn(id) && id.literal && ! cardIds.add(id.value))
							add(out, name, lines, at, "duplicate-id", f("<@card id='%s'> duplicates an existing card id.", id.value));
						var type = attrs.get("type");
						if (nn(type) && type.literal && REMOVED_TYPES.contains(type.value))
							add(out, name, lines, at, "removed-card-type", f(
								"<@card id='%s'> type='%s' was removed in 10.0.0; use type='html' with a <template>, or a registered card type.",
								n(id) ? "" : id.value, type.value));
						if (nn(type) && type.literal && eq(type.value, "chart")) {
							var problem = echartsProblem();
							if (nn(problem))
								add(out, name, lines, at, echartsRule, problem);
						}
						var tpl = attrs.get("template");
						if (nn(tpl) && tpl.literal)
							templateRefs.add(new Object[]{tpl.value, at});
						if (nn(type) && type.literal && eq(type.value, "datatables") && ! selfClosing)
							checkViewVersion(out, name, lines, text, m.end(), n(id) ? "" : id.value);
						if (! selfClosing)
							checkCardBody(out, name, lines, at, text, m.end(), id, attrs.get("src"), attrs.get("type"));
					}
					case ConsoleDirectiveModel.NAME -> {
						if (consoleAt < 0)
							consoleAt = at;
					}
					case MainDirectiveModel.NAME -> mains++;
					default -> { /* slots, theme, token, page: nothing beyond the attribute checks above */ }
				}
			}
		}

		for (var r : templateRefs) {
			var v = (String)r[0];
			if (! cardIds.contains(v) && ! v.startsWith("header.") && ! v.startsWith("footer."))
				add(out, name, lines, (Integer)r[1], "dangling-template", f("<@card> template='%s' names no <@card id> or slot in this template.", v));
		}
		if (consoleAt >= 0 && mains != 1)
			add(out, name, lines, consoleAt, "main-count", f("<@console> requires exactly one <@main/>; found '%s'.", mains));

		var g = GLOBAL.matcher(text);
		while (g.find()) {
			add(out, name, lines, g.start(), "removed-global", f("'%s' was removed in 10.0.0; %s", g.group(1),
				eq(g.group(1), "pageToolkit") ? "use jcHasToolkit(\"views\")." : "<@page> captures the page into the contract."));
		}

		var a = ASSET_REF.matcher(text);
		while (a.find()) {
			var url = a.group(nn(a.group(1)) ? 1 : 2);
			if (! url.contains("${") && ! url.contains("<#") && JUNEAU_ASSET.matcher(url).find())
				add(out, name, lines, a.start(), "hardcoded-asset-url", f(
					"Hard-coded Juneau asset URL '%s' is unversioned and goes stale in browser caches; use viewAssetUrl(...), consoleJsUrl(...), chromeCssUrl(...) or themeAssetUrl(...) so it carries ?v= (an adopter's own assets can use assetUrl(...)).", url));
		}

		out.sort(Comparator.comparingInt(Finding::line).thenComparingInt(Finding::column));
		return out;
	}

	/**
	 * Flags a literal {@code view.contractVersion} that differs from {@link ViewsMixin#CONTRACT_VERSION} in a
	 * {@code type="datatables"} card body. An omitted version is fine (the runtime injects it); only a pinned, stale
	 * value is reported. Scans the {@code view:{...}} object at its own nesting depth so nested {@code contractVersion}
	 * keys (quickStats, detail, ...) are never mistaken for it.
	 */
	private static void checkViewVersion(List<Finding> out, String name, int[] lines, String text, int bodyStart, String cardId) {
		var end = text.indexOf("</@card", bodyStart);
		var body = text.substring(bodyStart, end < 0 ? text.length() : end);
		var vm = VIEW_KEY.matcher(body);
		if (! vm.find())
			return;
		var depth = 1;
		for (var i = vm.end(); i < body.length() && depth > 0; i++) {
			var c = body.charAt(i);
			if (c == '{' || c == '[')
				depth++;
			else if (c == '}' || c == ']')
				depth--;
			else if (depth == 1 && c == 'c') {
				var v = VIEW_VERSION.matcher(body.substring(i));
				if (v.find() && neq(v.group(1), ViewsMixin.CONTRACT_VERSION)) {
					add(out, name, lines, bodyStart + i, "view-contract-version", f(
						"<@card id='%s'> view.contractVersion is '%s' but the runtime is '%s'; omit it to track the runtime automatically.",
						cardId, v.group(1), ViewsMixin.CONTRACT_VERSION));
					return;
				}
			}
		}
	}

	private record Value(String value, boolean literal) {}

	// Mirrors CardTypeRegistry.BASE_KEYS (E-23); visibleWhen is deliberately excluded, same as there.
	private static final Set<String> RESERVED_BODY_KEYS = Set.of("id", "type", "title", "src", "template", "ref");

	/**
	 * The C2 card-body rules: {@code card-src-and-body} and {@code card-reserved-key}.
	 * Reads the body the same way {@link #checkViewVersion} does, up to the next {@code </@card}.
	 */
	@SuppressWarnings({
		"java:S107" // Internal helper whose parameters are the render/validation context.
	})
	private static void checkCardBody(List<Finding> out, String name, int[] lines, int at, String text, int bodyStart, Value id, Value src, Value type) {
		var end = text.indexOf("</@card", bodyStart);
		var body = text.substring(bodyStart, end < 0 ? text.length() : end);
		var cardId = n(id) ? "" : id.value;
		var trimmed = body.trim();
		// A chart card legitimately takes src= (remote rows) plus a body (kind and options).
		var chart = nn(type) && type.literal && "chart".equals(type.value);
		if (! trimmed.isEmpty() && nn(src) && src.literal && ! chart)
			add(out, name, lines, at, "card-src-and-body", f(
				"<@card id='%s'> has both src='%s' and a body; use exactly one.", cardId, src.value));
		if (trimmed.isEmpty() || body.contains("${"))
			return;
		var keys = topLevelJson5Keys(trimmed);
		for (var k : keys)
			if (RESERVED_BODY_KEYS.contains(k))
				add(out, name, lines, at, "card-reserved-key", f(
					"<@card id='%s'> body key '%s' is reserved; set it as an attribute.", cardId, k));
	}

	// Hand-rolled, like attrs() below: finds JSON5 object keys at brace-depth 0, skipping quoted strings (backslash escapes honored). Not a
	// general parser — enough to catch literal top-level keys for the two card-body lint rules above.
	private static Set<String> topLevelJson5Keys(String body) {
		var s = body.trim();
		if (s.startsWith("{") && s.endsWith("}"))
			s = s.substring(1, s.length() - 1);
		var keys = new LinkedHashSet<String>();
		var depth = 0;
		var i = 0;
		while (i < s.length()) {
			var c = s.charAt(i);
			if (c == '"' || c == '\'') {
				// Honor backslash escapes in both quote styles: skip the escaped character wholesale.
				var j = i + 1;
				while (j < s.length() && s.charAt(j) != c)
					j += s.charAt(j) == '\\' ? 2 : 1;
				i = j >= s.length() ? s.length() : j + 1;
			} else if (c == '{' || c == '[') {
				depth++;
				i++;
			} else if (c == '}' || c == ']') {
				depth--;
				i++;
			} else if (depth == 0 && (Character.isLetter(c) || c == '_')) {
				var start = i;
				while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_'))
					i++;
				var word = s.substring(start, i);
				var j = i;
				while (j < s.length() && Character.isWhitespace(s.charAt(j)))
					j++;
				if (j < s.length() && s.charAt(j) == ':')
					keys.add(word);
			} else {
				i++;
			}
		}
		return keys;
	}

	private static Map<String,Value> attrs(String s) {
		var out = new LinkedHashMap<String,Value>();
		var m = ATTR.matcher(s);
		while (m.find()) {
			String v;
			boolean literal;
			if (nn(m.group(2)) || nn(m.group(3))) {
				v = m.group(nn(m.group(2)) ? 2 : 3);
				literal = ! v.contains("${");
			} else {
				v = m.group(4);
				literal = eqa(v, "true", "false");
			}
			out.put(m.group(1), new Value(v, literal));
		}
		return out;
	}

	private void collectScope(String name, String text, Set<String> macros, Set<String> namespaces, Set<String> seen) {
		if (! seen.add(name))
			return;
		var m = MACRO.matcher(text);
		while (m.find())
			macros.add(m.group(1));
		var i = IMPORT.matcher(text);
		while (i.find())
			namespaces.add(i.group(1));
		var inc = INCLUDE.matcher(text);
		while (inc.find()) {
			var target = resolve(name, inc.group(1));
			var src = load(target);
			if (nn(src))
				collectScope(target, blankComments(src), macros, namespaces, seen);
		}
	}

	private static String resolve(String from, String path) {
		if (path.startsWith("/"))
			return path;
		var slash = from.lastIndexOf('/');
		return slash < 0 ? path : from.substring(0, slash + 1) + path;
	}

	// Template root first, then the classpath root, then (for absolute names) the classpath itself.
	private String load(String name) {
		var rel = name.startsWith("/") ? name.substring(1) : name;
		try {
			if (nn(templateRoot)) {
				var p = templateRoot.resolve(rel);
				if (Files.isRegularFile(p))
					return Files.readString(p, UTF_8);
			}
			if (nn(classpathRoot)) {
				var s = resource(classpathRoot + rel);
				if (nn(s))
					return s;
			}
			return name.startsWith("/") ? resource(name) : null;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String resource(String absPath) throws IOException {
		var cl = Thread.currentThread().getContextClassLoader();
		if (n(cl))
			cl = ConsoleTemplateValidator.class.getClassLoader();
		try (var in = cl.getResourceAsStream(absPath.substring(1))) {
			return n(in) ? null : new String(in.readAllBytes(), UTF_8);
		}
	}

	@SuppressWarnings({
		"unused" // the Template is built only so its constructor parses (and may throw)
	})
	private static void parse(String name, String source, List<Finding> out) {
		try {
			new Template(name, new StringReader(source), PARSE_CFG);
		} catch (ParseException e) {
			out.add(new Finding(name, e.getLineNumber(), e.getColumnNumber(), "parse-error", e.getEditorMessage()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// Replaces each <#-- … --> with spaces (newlines kept), so offsets still map to the original line and column.
	private static String blankComments(String s) {
		var sb = new StringBuilder(s);
		var m = COMMENT.matcher(s);
		while (m.find())
			for (var i = m.start(); i < m.end(); i++)
				if (sb.charAt(i) != '\n')
					sb.setCharAt(i, ' ');
		return sb.toString();
	}

	@SuppressWarnings({
		"javabugs:S2259" // False positive: validate() throws on a null load() result via n(src), which Sonar's flow analysis does not track as a null guard.
	})
	private static int[] lineStarts(String s) {
		var starts = new ArrayList<Integer>();
		starts.add(0);
		for (var i = 0; i < s.length(); i++)
			if (s.charAt(i) == '\n')
				starts.add(i + 1);
		return starts.stream().mapToInt(Integer::intValue).toArray();
	}

	private static void add(List<Finding> out, String name, int[] lines, int offset, String rule, String message) {
		var i = Arrays.binarySearch(lines, offset);
		var line = i >= 0 ? i : -i - 2;
		out.add(new Finding(name, line + 1, offset - lines[line] + 1, rule, message));
	}
}
