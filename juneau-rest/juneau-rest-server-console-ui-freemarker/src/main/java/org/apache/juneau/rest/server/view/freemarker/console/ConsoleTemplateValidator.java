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
import java.util.regex.*;
import java.util.stream.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * Static lint for console FreeMarker templates. Scans each template for console directive calls and reports unknown
 * directives, unknown attributes, dangling template references, duplicate nav and card ids, non-strict booleans,
 * removed card types and globals, and a wrong {@code <@main/>} count, without rendering any page.
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
	"java:S1075" // "/" is the template-name separator, not a file-system path.
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
	private static final Set<String> REMOVED_TYPES = Set.of("js", "json", "calendar");
	private static final Map<String,Set<String>> STRICT = Map.of(NodeDirectiveModel.NAME, Set.of("visible", "selected"), ConsoleDirectiveModel.NAME, Set.of("chrome"));
	private static final Map<String,Set<String>> DIRECTIVES = directives();

	private static Map<String,Set<String>> directives() {
		var m = new LinkedHashMap<String,Set<String>>();
		m.put(PageDirectiveModel.NAME, PageDirectiveModel.ATTRS);
		m.put(ConsoleDirectiveModel.NAME, ConsoleDirectiveModel.ATTRS);
		m.put(MainDirectiveModel.NAME, MainDirectiveModel.ATTRS);
		m.put(NavigationDirectiveModel.NAME, NavigationDirectiveModel.ATTRS);
		m.put(NodeDirectiveModel.NAME, NodeDirectiveModel.ATTRS);
		m.put(ThemeDirectiveModel.NAME, ThemeDirectiveModel.ATTRS);
		m.put(TokenDirectiveModel.NAME, TokenDirectiveModel.ATTRS);
		m.put(CardDirectiveModel.NAME, CardDirectiveModel.ATTRS);
		for (var s : ConsoleSlotDirectiveModel.SLOT_NAMES)
			m.put(s, new ConsoleSlotDirectiveModel(s).attrs());
		return Collections.unmodifiableMap(m);
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

	/**
	 * @param templateName The template to lint, relative to the template root or classpath root.
	 * @return The findings, sorted by line and column. Empty when the template is clean.
	 * @throws IllegalArgumentException If the template cannot be found.
	 */
	public List<Finding> validate(String templateName) {
		var src = load(templateName);
		if (src == null)
			throw new IllegalArgumentException(String.format("Template '%s' not found under templateRoot or classpathRoot.", templateName));
		return validateSource(templateName, src);
	}

	/**
	 * Lints every {@code *.ftlh} and {@code *.ftl} file under the template root.
	 *
	 * @return The findings, sorted by template, line and column. Empty when every template is clean.
	 * @throws IllegalStateException If no template root is set.
	 */
	public List<Finding> validateAll() {
		if (templateRoot == null)
			throw new IllegalStateException("validateAll() requires templateRoot(Path); a classpath root cannot be listed.");
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
		if (chromeTemplate != null && neq(chromeTemplate, name)) {
			var chrome = load(chromeTemplate);
			if (chrome == null)
				throw new IllegalArgumentException(String.format("Chrome template '%s' not found under templateRoot or classpathRoot.", chromeTemplate));
			collectScope(chromeTemplate, blankComments(chrome), macros, namespaces, new HashSet<>());
		}

		var siblings = new ArrayDeque<Set<String>>();
		siblings.push(new HashSet<>());
		var cardIds = new HashSet<String>();
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
			} else if (known == null) {
				var dot = dname.indexOf('.');
				var ok = dot > 0 ? namespaces.contains(dname.substring(0, dot)) : macros.contains(dname);
				if (! ok)
					add(out, name, lines, at, "unknown-directive", String.format("<@%s> is not a console directive, a macro in scope, or allow-listed.", dname));
			} else {
				var attrs = attrs(m.group(3));
				for (var a : attrs.keySet())
					if (! known.contains(a))
						add(out, name, lines, at, "unknown-attribute", String.format("<@%s> unknown attribute '%s'.", dname, a));
				for (var a : STRICT.getOrDefault(dname, Set.of())) {
					var v = attrs.get(a);
					if (v != null && v.literal && !eqa(v.value, "true", "false"))
						add(out, name, lines, at, "strict-boolean", String.format("<@%s> %s= must be true or false; got '%s'.", dname, a, v.value));
				}
				var id = attrs.get("id");
				switch (dname) {
					case NavigationDirectiveModel.NAME -> {
						if (! selfClosing)
							siblings.push(new HashSet<>());
					}
					case NodeDirectiveModel.NAME -> {
						if (id != null && id.literal && ! siblings.peek().add(id.value))
							add(out, name, lines, at, "duplicate-id", String.format("<@node id='%s'> duplicates a sibling id.", id.value));
						if (! selfClosing)
							siblings.push(new HashSet<>());
					}
					case CardDirectiveModel.NAME -> {
						if (id != null && id.literal && ! cardIds.add(id.value))
							add(out, name, lines, at, "duplicate-id", String.format("<@card id='%s'> duplicates an existing card id.", id.value));
						var type = attrs.get("type");
						if (type != null && type.literal && REMOVED_TYPES.contains(type.value))
							add(out, name, lines, at, "removed-card-type", String.format(
								"<@card id='%s'> type='%s' was removed in 10.0.0; use type='html' with a <template>, or a registered card type.",
								id == null ? "" : id.value, type.value));
						var tpl = attrs.get("template");
						if (tpl != null && tpl.literal)
							templateRefs.add(new Object[]{tpl.value, at});
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
				add(out, name, lines, (Integer)r[1], "dangling-template", String.format("<@card> template='%s' names no <@card id> or slot in this template.", v));
		}
		if (consoleAt >= 0 && mains != 1)
			add(out, name, lines, consoleAt, "main-count", String.format("<@console> requires exactly one <@main/>; found '%s'.", mains));

		var g = GLOBAL.matcher(text);
		while (g.find()) {
			add(out, name, lines, g.start(), "removed-global", String.format("'%s' was removed in 10.0.0; %s", g.group(1),
				eq(g.group(1), "pageToolkit") ? "use jcHasToolkit(\"views\")." : "<@page> captures the page into the contract."));
		}

		out.sort(Comparator.comparingInt(Finding::line).thenComparingInt(Finding::column));
		return out;
	}

	private record Value(String value, boolean literal) {}

	private static Map<String,Value> attrs(String s) {
		var out = new LinkedHashMap<String,Value>();
		var m = ATTR.matcher(s);
		while (m.find()) {
			String v;
			boolean literal;
			if (m.group(2) != null || m.group(3) != null) {
				v = m.group(2) != null ? m.group(2) : m.group(3);
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
			if (src != null)
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
			if (templateRoot != null) {
				var p = templateRoot.resolve(rel);
				if (Files.isRegularFile(p))
					return Files.readString(p, UTF_8);
			}
			if (classpathRoot != null) {
				var s = resource(classpathRoot + rel);
				if (s != null)
					return s;
			}
			return name.startsWith("/") ? resource(name) : null;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String resource(String absPath) throws IOException {
		var cl = Thread.currentThread().getContextClassLoader();
		if (cl == null)
			cl = ConsoleTemplateValidator.class.getClassLoader();
		try (var in = cl.getResourceAsStream(absPath.substring(1))) {
			return in == null ? null : new String(in.readAllBytes(), UTF_8);
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
