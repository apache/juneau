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
package org.apache.juneau.commons.template;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.svl.*;

/**
 * Renders SVL templates written by authors who are less trusted than the application.
 *
 * <p>
 * A template resolver is a locked-down {@link VarResolver}: it registers no system vars, permits only the functions in
 * {@link SafeFunctions}, and reads data only from the map passed to each render.  The built-in functions give lists, tables
 * and per-row projections; see the package documentation for the grammar and the encoding rule.
 *
 * <p>
 * Inside a projection over a list of maps, <c>size</c> and <c>empty</c> resolve to the row map's own size and emptiness
 * even when the row has no such key, so they do not fall back to a scope entry of the same name.
 *
 * <h2 class='section'>Example:</h2>
 * <p class='bjava'>
 * 	<jk>var</jk> <jv>resolver</jv> = TemplateResolver.<jsm>create</jsm>().destination(Destinations.<jsf>HTML</jsf>).build();
 * 	<jk>var</jk> <jv>template</jv> = <jv>resolver</jv>.parse(<js>"Failed: ${n}\n#{ul(failed.![id + ' - ' + msg])}"</js>);
 * 	<jk>var</jk> <jv>html</jv> = <jv>resolver</jv>.render(<jv>template</jv>, Map.<jsm>of</jsm>(<js>"n"</js>, 2, <js>"failed"</js>, <jv>rows</jv>));
 * </p>
 *
 * <h2 class='section'>Notes:</h2><ul>
 * 	<li class='note'>Inside a projection (<c>.![...]</c>, <c>.?[...]</c>) row fields are bare identifiers.  A
 * 		<c>${id}</c> there reads the render scope, not the row.
 * 	<li class='note'>Functions added with {@link Builder#functions(VarFunction...)} are subject to the allowlist; add them
 * 		with {@link Builder#allowFunctions(String...)}.
 * 	<li class='note'>Instances are immutable and thread-safe.  {@link ParsedTemplate} instances are too.
 * </ul>
 */
public final class TemplateResolver {

	/**
	 * A resolver with the default configuration: lenient, no destination.
	 *
	 * <p>
	 * It does <b>no encoding</b> and renders structures with {@link Structure#toString()}.  Resolved values reach the
	 * output unescaped, so it is not for HTML, Markdown, JSON or URLs; build a resolver with a {@link Destinations}
	 * preset (or an explicit encoder) for those.
	 */
	public static final TemplateResolver DEFAULT = create().build();

	/**
	 * Creates a builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() { return new Builder(); }

	/** Builder for {@link TemplateResolver}. */
	public static final class Builder {
		private final List<Var> vars = new ArrayList<>();
		private final List<VarFunction> functions = new ArrayList<>();
		private final List<String> extra = new ArrayList<>();
		private final List<String> only = new ArrayList<>();
		private boolean replaced;
		private boolean strict;
		private int maxCollectionSize = VarResolver.DEFAULT_MAX_COLLECTION_SIZE;
		private StructureRenderer renderer;
		private Destination destination;

		private Builder() {}

		/**
		 * Registers additional vars.  None are registered by default except the internal scope var.
		 *
		 * @param values The vars.  Must not be <jk>null</jk>.  A var named <c>P</c> replaces the scope var, which rebinds
		 * 	the <c>${name}</c> shortcut.
		 * @return This object.
		 */
		public Builder vars(Var...values) {
			for (var v : values)
				vars.add(Objects.requireNonNull(v, "var must not be null"));
			return this;
		}

		/**
		 * Registers additional functions.
		 *
		 * @param values The functions.  Must not be <jk>null</jk>.  They are also subject to the allowlist.
		 * @return This object.
		 */
		public Builder functions(VarFunction...values) {
			for (var f : values)
				functions.add(Objects.requireNonNull(f, "function must not be null"));
			return this;
		}

		/**
		 * Adds function-name globs to the default allowlist ({@link SafeFunctions#patterns()}).
		 *
		 * <p>
		 * Repeated calls are additive.  A bare <c>*</c> is rejected because it would switch default-deny off; use
		 * {@link #allowOnlyFunctions(String...)} to replace the defaults.
		 *
		 * @param patterns The patterns.  Must not be <jk>null</jk>, and must not be <c>*</c>.
		 * @return This object.
		 * @throws IllegalArgumentException If a pattern is <c>*</c>.
		 */
		public Builder allowFunctions(String...patterns) {
			addPatterns(extra, patterns);
			return this;
		}

		/**
		 * Replaces the default function allowlist ({@link SafeFunctions#patterns()}) with exactly these globs.
		 *
		 * <p>
		 * Include <c>$op.*</c> to keep the operators.  Patterns from {@link #allowFunctions(String...)} are still added.
		 * A bare <c>*</c> is rejected.
		 *
		 * @param patterns The patterns.  Must not be <jk>null</jk>, and must not be <c>*</c>.
		 * @return This object.
		 * @throws IllegalArgumentException If a pattern is <c>*</c>.
		 */
		public Builder allowOnlyFunctions(String...patterns) {
			replaced = true;
			addPatterns(only, patterns);
			return this;
		}

		private static void addPatterns(List<String> to, String[] patterns) {
			for (var p : patterns) {
				Objects.requireNonNull(p, "pattern must not be null");
				if (p.trim().equals("*"))
					throw iaex("allowFunctions pattern '*' would disable default-deny");
				to.add(p);
			}
		}

		/**
		 * Enables strict mode: an unknown or denied function, or an expression error, throws instead of being emitted
		 * as written.
		 *
		 * @param value Whether to enable strict mode.
		 * @return This object.
		 */
		public Builder strict(boolean value) {
			strict = value;
			return this;
		}

		/**
		 * Sets the maximum number of elements one expression may iterate, and the maximum per container when formatting.
		 *
		 * @param value The limit.  Must be positive.
		 * @return This object.
		 */
		public Builder maxCollectionSize(int value) {
			if (value <= 0)
				throw iaex("maxCollectionSize must be positive: %s", value);
			maxCollectionSize = value;
			return this;
		}

		/**
		 * Sets how lists, blocks and tables are rendered.  Overrides the renderer of {@link #destination(Destination)}.
		 *
		 * <p>
		 * Without a renderer {@link Structure#toString()} is used, and its items are not encoded.
		 *
		 * @param value The renderer.  <jk>null</jk> clears it.
		 * @return This object.
		 */
		public Builder structureRenderer(StructureRenderer value) {
			renderer = value;
			return this;
		}

		/**
		 * Sets the default value encoder and structure renderer from a destination preset.
		 *
		 * @param value The destination.  <jk>null</jk> clears it.
		 * @return This object.
		 */
		public Builder destination(Destination value) {
			destination = value;
			return this;
		}

		/**
		 * Builds the resolver.
		 *
		 * @return A new resolver.
		 */
		public TemplateResolver build() {
			var b = VarResolver.create().defaultFunctions().strict(strict).maxCollectionSize(maxCollectionSize);
			b.vars(new ScopeVar());
			b.vars(vars.toArray(new Var[0]));
			b.functions(functions.toArray(new VarFunction[0]));
			var patterns = new ArrayList<>(replaced ? only : SafeFunctions.patterns());
			patterns.addAll(extra);
			b.allowFunctions(patterns.toArray(new String[0]));
			var r = renderer;
			if (r == null && destination != null)
				r = destination.renderer();
			var e = destination == null ? null : destination.encoder();
			return new TemplateResolver(b.build(), r, e, new JsonValueFormatter(maxCollectionSize, strict));
		}
	}

	private final VarResolver resolver;
	private final UnaryOperator<String> encoder;
	private final Function<Object,String> formatter;

	private TemplateResolver(VarResolver resolver, StructureRenderer renderer, UnaryOperator<String> encoder, Function<Object,String> baseFormatter) {
		this.resolver = resolver;
		this.encoder = encoder;
		this.formatter = renderer == null ? null : new StructureFormatter(renderer, baseFormatter);
	}

	/** Sends structures to the renderer and everything else to the base formatter. */
	private record StructureFormatter(StructureRenderer renderer, Function<Object,String> base) implements Function<Object,String> {
		@Override public String apply(Object o) {
			return o instanceof Structure s ? renderer.render(s, base) : base.apply(o);
		}
	}

	/**
	 * Parses a template.
	 *
	 * @param template The template text.  May be <jk>null</jk>.
	 * @return The parsed template.  Never <jk>null</jk>.
	 * @throws ExprException In strict mode, if the template is malformed or uses an unknown or denied function.
	 * 	In lenient mode nothing is thrown.
	 */
	public ParsedTemplate parse(String template) {
		return new ParsedTemplate(resolver.compile(template), this);
	}

	/**
	 * Renders a template using the default value encoder of the configured destination (none if there is no destination).
	 *
	 * @param template The template.  Must not be <jk>null</jk>, and must have been parsed by this resolver.
	 * @param scope The names the template can read.  May be <jk>null</jk> (no names).
	 * @return The rendered text.
	 * @throws IllegalArgumentException If the template was parsed by a different resolver.
	 */
	public String render(ParsedTemplate template, Map<String,?> scope) {
		return render(template, scope, null);
	}

	/**
	 * Renders a template.
	 *
	 * @param template The template.  Must not be <jk>null</jk>, and must have been parsed by this resolver.
	 * @param scope The names the template can read.  May be <jk>null</jk> (no names).
	 * @param valueEncoder The value encoder.  Overrides the destination's encoder; <jk>null</jk> uses the destination's.
	 * @return The rendered text.
	 */
	public String render(ParsedTemplate template, Map<String,?> scope, UnaryOperator<String> valueEncoder) {
		Objects.requireNonNull(template, "template must not be null");
		if (template.owner() != this)
			throw iaex("template was parsed by a different TemplateResolver");
		Function<String,Object> lookup = n -> n == null ? null : scope.get(n);
		if (scope == null)
			lookup = n -> null;
		var session = resolver.createSession().scope(lookup);
		if (formatter != null)
			session.valueFormatter(formatter);
		return template.template().resolve(session, valueEncoder != null ? valueEncoder : encoder);
	}

	/**
	 * Checks a template against a catalog without rendering it.  Never throws.
	 *
	 * <p>
	 * Reports, in source order: an identifier not in the catalog, a row field not in its list's catalog, a row field read
	 * through a name that is not a declared list, an unknown or denied function (unless the catalog allows them), an
	 * unknown var, and a marker that does not compile.  The key of a <c>${name}</c> or <c>${name:default}</c> reference is
	 * checked like an identifier, so <c>${secret}</c> is a problem when <c>secret</c> is not declared; a key that is
	 * itself computed (<c>${${x}}</c>) is read as written and is reported as unknown.  The default text after <c>:</c> is
	 * not checked.
	 *
	 * @param template The template text.  May be <jk>null</jk>.
	 * @param catalog The catalog.  Must not be <jk>null</jk>.
	 * @return The problems.  Empty if the template is clean.
	 */
	public List<TemplateProblem> check(String template, TemplateCatalog catalog) {
		Objects.requireNonNull(catalog, "catalog must not be null");
		return check(compileOrProblems(template), catalog);
	}

	/** A compiled template, or the one problem that stopped it compiling. */
	private record Compiled(VarTemplate template, List<TemplateProblem> failure) {}

	private Compiled compileOrProblems(String template) {
		try {
			return new Compiled(resolver.compile(template), null);
		} catch (ExprException e) {
			var at = Math.max(0, e.getPos());
			return new Compiled(null, List.of(new TemplateProblem(at, at + 1, e.getBaseMessage())));
		}
	}

	private List<TemplateProblem> check(Compiled c, TemplateCatalog catalog) {
		if (c.failure() != null)
			return c.failure();
		var t = c.template();
		var out = new ArrayList<TemplateProblem>();
		for (var r : t.references())
			switch (r.kind()) {
				case SYNTAX -> out.add(new TemplateProblem(r.start(), r.end(), r.name()));
				case VAR -> {
					if (! r.bound())
						out.add(new TemplateProblem(r.start(), r.end(), "unknown var '" + r.name() + "'"));
				}
				case FUNCTION -> {
					if (! r.bound() && ! catalog.allowUnknownFunctions())
						out.add(new TemplateProblem(r.start(), r.end(), "unknown or denied function '" + r.name() + "'"));
				}
				case IDENT -> checkIdent(r, catalog, out);
			}
		out.sort(Comparator.comparingInt(TemplateProblem::start));
		return out;
	}

	private static void checkIdent(TemplateReference r, TemplateCatalog catalog, List<TemplateProblem> out) {
		var name = r.name();
		var list = r.listName();
		if (list == null) {
			if (! catalog.hasName(name))
				out.add(new TemplateProblem(r.start(), r.end(), "unknown name '" + name + "'"));
		} else if (catalog.isList(list)) {
			var rows = catalog.rowFields(list);
			if (rows != null && ! rows.has(name) && ! catalog.hasName(name))
				out.add(new TemplateProblem(r.start(), r.end(), "unknown field '" + name + "' in list '" + list + "'"));
		} else if (catalog.hasName(list)) {
			out.add(new TemplateProblem(r.start(), r.end(), "'" + list + "' is not a declared list"));
		}
		// An unknown list name is reported where it is read, so it is not repeated for each row field.
	}

	/**
	 * Like {@link #check(String, TemplateCatalog)} but throws.
	 *
	 * @param template The template text.  May be <jk>null</jk>.
	 * @param catalog The catalog.  Must not be <jk>null</jk>.
	 * @return The parsed template, if it is clean.
	 * @throws TemplateValidationException If there are problems.
	 */
	public ParsedTemplate validate(String template, TemplateCatalog catalog) {
		Objects.requireNonNull(catalog, "catalog must not be null");
		var c = compileOrProblems(template);
		var problems = check(c, catalog);
		if (! problems.isEmpty())
			throw new TemplateValidationException(problems);
		return new ParsedTemplate(c.template(), this);
	}
}
