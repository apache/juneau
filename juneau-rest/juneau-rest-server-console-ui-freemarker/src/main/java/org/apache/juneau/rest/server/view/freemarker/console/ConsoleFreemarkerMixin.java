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

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.view.freemarker.*;

import freemarker.cache.*;
import freemarker.template.*;

/**
 * A {@link FreemarkerMixin} that additionally makes the reserved-classpath admin-console chrome template
 * ({@code org/apache/juneau/console/base.ftlh}) &mdash; and the {@code <@tag>} macro it defines &mdash; resolvable
 * from any consumer template, without shadowing the consumer's own {@code basePath}-rooted template tree.
 *
 * <h5 class='section'>Used INSTEAD OF {@link FreemarkerMixin}, not beside it:</h5>
 * <p>
 * Register it typed as either {@link FreemarkerMixin} or {@code ConsoleFreemarkerMixin} &mdash; both declared
 * return types work. {@code FreemarkerViewRenderer} finds the exact-type {@code FreemarkerMixin} bean when one is
 * registered, and otherwise also looks for a bean declared under any registered subtype (P8), which this class
 * registers via a static initializer:
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja> <jk>public</jk> FreemarkerMixin freemarker() {
 * 		<jk>return</jk> ConsoleFreemarkerMixin.<jsm>create</jsm>().basePath(<js>"/templates/"</js>).build();
 * 	}
 * </p>
 *
 * <p>
 * or equivalently:
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja> <jk>public</jk> ConsoleFreemarkerMixin freemarker() {
 * 		<jk>return</jk> ConsoleFreemarkerMixin.<jsm>create</jsm>().basePath(<js>"/templates/"</js>).build();
 * 	}
 * </p>
 *
 * <h5 class='section'>Consumer {@code /templates} is never shadowed:</h5>
 * <p>
 * The augmented {@link Configuration}'s {@code TemplateLoader} tries the consumer's own {@code basePath}-derived
 * loader <b>first</b>; the reserved template is addressed by its full classpath-root-relative path
 * ({@link #BASE_TEMPLATE_PATH}) on a second, classpath-<b>root</b>-rooted {@link ClassTemplateLoader}, so it cannot
 * collide with a consumer path such as {@code /templates/base.ftlh}. Consumer templates include it with a
 * loader-root-absolute path:
 *
 * <p class='bftl'>
 * 	&lt;#include "/org/apache/juneau/console/base.ftlh"&gt;
 * </p>
 *
 * <h5 class='section'>Consumer-supplied {@code Configuration} is the same instance, then fill-missing stamped:</h5>
 * <p>
 * When the request's {@code BeanStore} already has a {@code Configuration} bean (Spring/Spring-Boot autoconfig, or
 * a microservice {@code BasicBeanStore.put}), {@link #resolveConfiguration} still returns that <b>same instance</b>
 * ({@code ==}) so consumer settings win (encoding, {@code ObjectWrapper}, {@code exposeFields}, template-update
 * delay, output format). It then fill-missing-stamps the reserved console shared variables
 * ({@code page}, {@code card}, {@code navigation}, {@code node}, {@code theme}, {@code token}, {@code console},
 * {@code main}, the {@code <@console>} slot names {@code head}/{@code scripts}/{@code brand}/{@code actions}/
 * {@code title}/{@code footer}/{@code body}, and {@code jcTagHtml}) and splices the
 * reserved-path console loader only when {@link #BASE_TEMPLATE_PATH} would not already resolve. A name the
 * consumer already set is left alone. There is no opt-out flag on this class &mdash; opt out by registering
 * {@link FreemarkerMixin} instead of {@code ConsoleFreemarkerMixin}.
 *
 * @since 10.0.0
 */
public class ConsoleFreemarkerMixin extends FreemarkerMixin {

	static {
		FreemarkerMixin.registerSubtype(ConsoleFreemarkerMixin.class);
	}

	/**
	 * The reserved classpath-root-relative location of the shipped console chrome template
	 * (defines the {@code <@tag>} macro). Load-bearing for the collision-free-namespacing argument in the class
	 * javadoc &mdash; do not shorten.
	 */
	public static final String BASE_TEMPLATE_PATH = "org/apache/juneau/console/base.ftlh";

	/** Default consumer chrome template included by {@code <@page>} when none is configured. */
	public static final String DEFAULT_CHROME_TEMPLATE = "base.ftlh";

	private final String chromeTemplate;
	private final ToolkitPackRegistry packs;
	private final CardRequirements cardRequirements;
	private final CardTypeRegistry cardTypes;
	private final boolean devMode;
	private final ClassLoader adopterLoader;
	private final String adopterRoot;

	/**
	 * No-arg constructor &mdash; mirrors {@link FreemarkerMixin#FreemarkerMixin()} so the mixin walk's
	 * {@code BeanInstantiator} is not forced to depend on builder-detection alone (S3).
	 */
	public ConsoleFreemarkerMixin() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 * @throws IllegalArgumentException If the toolkit packs, provided packs or card requirements don't validate.
	 */
	protected ConsoleFreemarkerMixin(Builder builder) {
		super(builder);
		this.chromeTemplate = builder.chromeTemplate;
		this.packs = new ToolkitPackRegistry();
		builder.extraPacks.forEach(packs::register);
		packs.provide(builder.providedPacks).validate();
		this.cardRequirements = builder.cardRequirements.build(packs);
		this.cardTypes = builder.cardTypes.build();
		this.devMode = builder.devMode;
		this.adopterLoader = builder.adopterLoader;
		this.adopterRoot = builder.adopterRoot;
	}

	/** @return The configured consumer chrome template name; read by {@code PageSpec.view()} when no template is set. */
	String chromeTemplate() { return chromeTemplate; }

	/** @return The card-type registry, including custom {@code Builder.cardType(...)} handlers. */
	CardTypeRegistry cardTypes() { return cardTypes; }

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S9149" // Intentional per-subclass builder-factory override matching FreemarkerMixin.create()'s own convention; each mixin subclass returns its own nested Builder type.
	})
	public static Builder create() {
		return new Builder();
	}

	// Double-checked-locking identity cache: resolveConfiguration is called once per request, and
	// super.resolveConfiguration(req) returns the SAME Configuration object on every call for a given mixin
	// instance (consumer bean or FreemarkerDispatcher's lazy default cache). Tracking that object's identity
	// here means "have I already stamped THIS exact object" is answered in O(1). Presence checks on the loader
	// and shared-variable names still run on a cache miss so N Rest resources sharing one Spring singleton
	// do not nest Multi(Multi(base, console), console).
	@SuppressWarnings({
		"java:S3077" // volatile is required here for correct double-checked-locking safe-publication; the reference is publish-once (fully constructed/wrapped before assignment) and never compound-mutated.
	})
	private volatile Configuration wrappedConfiguration;

	/**
	 * Resolves the active {@link Configuration} and fill-missing-stamps console shared variables onto it.
	 *
	 * <p>
	 * Always delegates to {@code super.resolveConfiguration(req)} first so a consumer {@code Configuration} bean
	 * wins for settings. Then:
	 * <ol class='spaced-list'>
	 * 	<li><b>Fill-missing names:</b> {@code page}, {@code card}, {@code navigation}, {@code node},
	 * 		{@code theme}, {@code token}, {@code console}, {@code main}, the {@code <@console>} slot names
	 * 		({@code head}, {@code scripts}, {@code brand}, {@code actions}, {@code title}, {@code footer},
	 * 		{@code body}), and {@code jcTagHtml} are set only when {@code getSharedVariable(name)} is unset. A
	 * 		consumer value is kept.
	 * 	<li><b>Loader splice:</b> the classpath-root console {@link ClassTemplateLoader} is wrapped in only when
	 * 		{@link #BASE_TEMPLATE_PATH} would not already resolve through the current loader.
	 * 	<li><b>Wrap-once + presence:</b> identity-cached per mixin instance so a second call does not
	 * 		re-wrap; presence checks so N Rest resources sharing one Spring singleton {@code Configuration} do
	 * 		not nest {@link MultiTemplateLoader}.
	 * </ol>
	 *
	 * @param req The current REST request.
	 * @return The active FreeMarker configuration. Never {@code null}.
	 */
	@Override
	public Configuration resolveConfiguration(RestRequest req) {
		var base = super.resolveConfiguration(req);
		if (base == wrappedConfiguration)
			return base;
		synchronized (this) {
			if (base != wrappedConfiguration) {
				spliceConsoleLoaderIfNeeded(base);
				fillMissingConsoleSharedVariables(base);
				wrappedConfiguration = base;
			}
		}
		return base;
	}

	/**
	 * Wraps the current {@link TemplateLoader} with a classpath-root console loader only when
	 * {@link #BASE_TEMPLATE_PATH} would not resolve. Presence-checked so a second mixin instance sharing the
	 * same {@code Configuration} does not nest {@link MultiTemplateLoader}.
	 */
	private void spliceConsoleLoaderIfNeeded(Configuration cfg) {
		if (reservedTemplateResolves(cfg))
			return;
		var console = new ClassTemplateLoader(getClass().getClassLoader(), "");
		var existing = cfg.getTemplateLoader();
		if (n(existing))
			cfg.setTemplateLoader(console);
		else
			cfg.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[]{ existing, console }));
	}

	/**
	 * Whether the reserved console chrome template is already visible on {@code cfg}'s loader. Uses
	 * {@link TemplateLoader#findTemplateSource(String)} so a miss is not cached as a failed
	 * {@link Configuration#getTemplate(String)}.
	 */
	private static boolean reservedTemplateResolves(Configuration cfg) {
		var loader = cfg.getTemplateLoader();
		if (n(loader))
			return false;
		try {
			var source = loader.findTemplateSource(BASE_TEMPLATE_PATH);
			if (n(source))
				return false;
			loader.closeTemplateSource(source);
			return true;
		} catch (@SuppressWarnings("unused") Exception e) {
			return false; // Loader probe failed; treat as unresolved and splice.
		}
	}

	/**
	 * Sets each reserved console shared variable only when that name is unset on {@code cfg}.
	 */
	private void fillMissingConsoleSharedVariables(Configuration cfg) {
		if (n(cfg.getSharedVariable(TagMethodModel.NAME)))
			cfg.setSharedVariable(TagMethodModel.NAME, new TagMethodModel());
		if (n(cfg.getSharedVariable(PageDirectiveModel.NAME))) {
			cfg.setSharedVariable(PageDirectiveModel.NAME, new PageDirectiveModel(chromeTemplate, packs));
		}
		if (n(cfg.getSharedVariable(CardDirectiveModel.NAME)))
			cfg.setSharedVariable(CardDirectiveModel.NAME, new CardDirectiveModel(cardRequirements, cardTypes));
		if (n(cfg.getSharedVariable(NavigationDirectiveModel.NAME)))
			cfg.setSharedVariable(NavigationDirectiveModel.NAME, new NavigationDirectiveModel());
		if (n(cfg.getSharedVariable(NodeDirectiveModel.NAME)))
			cfg.setSharedVariable(NodeDirectiveModel.NAME, new NodeDirectiveModel());
		if (n(cfg.getSharedVariable(ThemeDirectiveModel.NAME)))
			cfg.setSharedVariable(ThemeDirectiveModel.NAME, new ThemeDirectiveModel());
		if (n(cfg.getSharedVariable(TokenDirectiveModel.NAME)))
			cfg.setSharedVariable(TokenDirectiveModel.NAME, new TokenDirectiveModel());
		if (n(cfg.getSharedVariable(ConsoleDirectiveModel.NAME)))
			cfg.setSharedVariable(ConsoleDirectiveModel.NAME, new ConsoleDirectiveModel(devMode));
		if (n(cfg.getSharedVariable(MainDirectiveModel.NAME)))
			cfg.setSharedVariable(MainDirectiveModel.NAME, new MainDirectiveModel());
		if (n(cfg.getSharedVariable(HasToolkitMethodModel.NAME)))
			cfg.setSharedVariable(HasToolkitMethodModel.NAME, new HasToolkitMethodModel());
		if (nn(adopterLoader) && n(cfg.getSharedVariable(AssetUrlMethodModel.NAME)))
			cfg.setSharedVariable(AssetUrlMethodModel.NAME, new AssetUrlMethodModel(adopterLoader, adopterRoot, devMode));
		// The seven capture-only slot directives share one parameterized class, one instance registered per slot name.
		for (var slot : ConsoleSlotDirectiveModel.SLOT_NAMES)
			if (n(cfg.getSharedVariable(slot)))
				cfg.setSharedVariable(slot, new ConsoleSlotDirectiveModel(slot));
	}

	/**
	 * Builder for {@link ConsoleFreemarkerMixin}.
	 */
	public static class Builder extends FreemarkerMixin.Builder {

		String chromeTemplate = DEFAULT_CHROME_TEMPLATE;
		final List<ToolkitPack> extraPacks = new ArrayList<>();
		final List<String> providedPacks = new ArrayList<>();
		final CardRequirements.Builder cardRequirements = CardRequirements.create();
		final CardTypeRegistry.Builder cardTypes = CardTypeRegistry.standard().copy();
		boolean devMode = Boolean.getBoolean("juneau.console.devMode");
		ClassLoader adopterLoader;
		String adopterRoot = "";

		/** Constructor &mdash; package access for {@link ConsoleFreemarkerMixin#create()}. */
		protected Builder() {}

		/**
		 * {@inheritDoc}
		 *
		 * <p>
		 * Covariantly narrowed to this {@code Builder} so a fluent chain can mix inherited setters
		 * (e.g. {@link #basePath(String)}) with the console-specific {@link #chromeTemplate(String)} /
		 * {@link #registerToolkitPack(ToolkitPack)} in any order.
		 */
		@Override
		public Builder basePath(String value) {
			super.basePath(value);
			return this;
		}

		/**
		 * Sets the consumer chrome template that {@code <@page>} includes after capturing its body.
		 *
		 * <p>
		 * Resolved on the consumer's own {@code basePath}-rooted loader. Defaults to
		 * {@link ConsoleFreemarkerMixin#DEFAULT_CHROME_TEMPLATE}.
		 *
		 * @param chromeTemplate The chrome template name. Must not be {@code null}.
		 * @return This object.
		 */
		public Builder chromeTemplate(String chromeTemplate) {
			this.chromeTemplate = chromeTemplate;
			return this;
		}

		/**
		 * Registers an app-supplied {@link ToolkitPack.Kind#RUNTIME} toolkit pack with no dependencies.
		 *
		 * <p>
		 * Same as {@link #registerToolkitPack(ToolkitPack)} with {@code kind(RUNTIME)} and the default resolver.
		 *
		 * @param name The pack name.
		 * @param cssPaths Ordered CSS asset paths.
		 * @param jsPaths Ordered JS asset paths.
		 * @return This object.
		 */
		public Builder registerToolkitPack(String name, List<String> cssPaths, List<String> jsPaths) {
			return registerToolkitPack(ToolkitPack.create(name).css(cssPaths).js(jsPaths).kind(ToolkitPack.Kind.RUNTIME).build());
		}

		/**
		 * Registers an app-supplied toolkit pack.
		 *
		 * <p>
		 * A pack with the name of a built-in or earlier pack replaces it completely, including its
		 * {@code dependsOn}.  {@link #build()} validates the pack graph.
		 *
		 * @param pack The pack.
		 * @return This object.
		 */
		public Builder registerToolkitPack(ToolkitPack pack) {
			extraPacks.add(rnn(pack));
			return this;
		}

		/**
		 * Names packs the app's chrome already loads.  A provided pack contributes no URLs; its dependencies still load
		 * unless they are provided too.
		 *
		 * @param names Registered pack names.
		 * @return This object.
		 */
		public Builder providedPacks(String...names) {
			providedPacks.addAll(Arrays.asList(names));
			return this;
		}

		/**
		 * Adds packs every card of a type requires, after the built-in one
		 * ({@code datatables} &rarr; {@code "datatables-glue"}).
		 *
		 * @param type The card type.
		 * @param packs Registered pack names.
		 * @return This object.
		 */
		public Builder cardRequires(String type, String...packs) {
			cardRequirements.add(type, packs);
			return this;
		}

		/**
		 * Registers a server-side card type handler for this mixin's {@code <@card>} directive, on top of
		 * {@link CardTypeRegistry#standard()}.
		 *
		 * <p>
		 * Most custom cards need only a JS handler; register a {@link CardTypeHandler} here when the server must
		 * validate or enrich the body. To also load toolkit packs for the type, use {@link #cardRequires(String, String...)}.
		 *
		 * @param handler The handler. Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException E-21 bad type name; E-22 reserved type; E-20 already registered.
		 */
		public Builder cardType(CardTypeHandler handler) {
			cardTypes.add(handler);
			return this;
		}

		/**
		 * Validates every page contract against {@code juneau-page.schema.json} when {@code <@console>} closes, and
		 * fails the render with E-14 on any finding.
		 *
		 * <p>
		 * Defaults to the {@code juneau.console.devMode} system property. Intended for development and tests; it parses
		 * and validates the contract on every render.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<ja>@Bean</ja> <jk>public</jk> FreemarkerMixin freemarker() {
		 * 		<jk>return</jk> ConsoleFreemarkerMixin.<jsm>create</jsm>().devMode(<jk>true</jk>).basePath(<js>"/templates/"</js>).build();
		 * 	}
		 * </p>
		 *
		 * @param value Whether to validate.
		 * @return This object.
		 */
		public Builder devMode(boolean value) {
			devMode = value;
			return this;
		}

		/**
		 * Enables the {@code assetUrl(path)} template function that versions the adopter's own static assets.
		 *
		 * <p>
		 * {@code assetUrl('/js/app.js')} returns {@code /js/app.js?v=<crc32 of the bundled bytes>}, where the bytes are
		 * read once (and the token cached) from {@code resourceRoot + path} on {@code loader}. This is the adopter-side
		 * counterpart of Juneau's own {@code viewAssetUrl}/{@code consoleJsUrl}/{@code chromeCssUrl}/{@code themeAssetUrl}
		 * cache-busters, for files the servlet container's static handler serves. A path with a query string, a
		 * non-root-absolute path (e.g. an external URL), or a path naming no bundled resource is returned unchanged
		 * (the last also logs a warning in {@link #devMode(boolean) dev mode}). The URL path itself is never rewritten,
		 * so any context-path prefix must be added by the template.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<ja>@Bean</ja> <jk>public</jk> FreemarkerMixin freemarker() {
		 * 		<jk>return</jk> ConsoleFreemarkerMixin.<jsm>create</jsm>()
		 * 			.basePath(<js>"/templates/"</js>)
		 * 			.adopterAssets(getClass().getClassLoader(), <js>"static"</js>)  <jc>// /js/app.js -&gt; classpath static/js/app.js</jc>
		 * 			.build();
		 * 	}
		 * </p>
		 * <p class='bftl'>
		 * 	&lt;script src="${assetUrl('/js/app.js')}"&gt;&lt;/script&gt;
		 * </p>
		 *
		 * @param loader The class loader that holds the assets. Must not be {@code null}.
		 * @param resourceRoot The classpath directory the URL paths are relative to (e.g. {@code "static"}); a leading
		 * 	and trailing slash are optional. Must not be {@code null}.
		 * @return This object.
		 */
		public Builder adopterAssets(ClassLoader loader, String resourceRoot) {
			// Q:  Use Shorts here and in this module.
			this.adopterLoader = rnn(loader);
			this.adopterRoot = rnn(resourceRoot);
			return this;
		}

		/**
		 * Builds the {@link ConsoleFreemarkerMixin}.
		 *
		 * <p>
		 * Overrides {@link org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin.Builder#build()} &mdash;
		 * without this override, the inherited method would silently return a plain {@link FreemarkerMixin}, not
		 * a {@link ConsoleFreemarkerMixin}.
		 *
		 * @return A new {@link ConsoleFreemarkerMixin} instance.
		 */
		@Override
		public ConsoleFreemarkerMixin build() {
			return new ConsoleFreemarkerMixin(this);
		}
	}
}
