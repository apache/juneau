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

import java.util.*;

import org.apache.juneau.rest.server.*;
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
 * Register it typed as the parent so {@code FreemarkerViewRenderer}'s exact-type
 * {@code getBean(FreemarkerMixin.class)} lookup finds it &mdash; the return type below is load-bearing:
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja> <jk>public</jk> FreemarkerMixin freemarker() {
 * 		<jk>return</jk> ConsoleFreemarkerMixin.<jsm>create</jsm>().basePath(<js>"/templates/"</js>).build();
 * 	}
 * </p>
 *
 * <p>
 * A {@code @Bean public ConsoleFreemarkerMixin freemarker() {...}} (the subtype as the declared return type) is
 * stored under {@code ConsoleFreemarkerMixin.class} and is <b>invisible</b> to the renderer's exact-type lookup,
 * which then falls back to a plain {@code new FreemarkerMixin()} (default {@code basePath="/"}) &mdash; composed
 * chrome silently does not render. See {@code ConsoleFreemarkerMixin_Test}'s anti-pattern gate.
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
 * ({@code page}, {@code card}, {@code navigation}, {@code node}, {@code theme}, {@code jcTagHtml}) and splices the
 * reserved-path console loader only when {@link #BASE_TEMPLATE_PATH} would not already resolve. A name the
 * consumer already set is left alone. There is no opt-out flag on this class &mdash; opt out by registering
 * {@link FreemarkerMixin} instead of {@code ConsoleFreemarkerMixin}.
 *
 * @since 10.0.0
 */
public class ConsoleFreemarkerMixin extends FreemarkerMixin {

	/**
	 * The reserved classpath-root-relative location of the shipped console chrome template
	 * (defines the {@code <@tag>} macro). Load-bearing for the collision-free-namespacing argument in the class
	 * javadoc &mdash; do not shorten.
	 */
	public static final String BASE_TEMPLATE_PATH = "org/apache/juneau/console/base.ftlh";

	/** Default consumer chrome template included by {@code <@page>} when none is configured. */
	public static final String DEFAULT_CHROME_TEMPLATE = "base.ftlh";

	private final String chromeTemplate;
	private final List<ExtraPack> extraPacks;

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
	 */
	protected ConsoleFreemarkerMixin(Builder builder) {
		super(builder);
		this.chromeTemplate = builder.chromeTemplate;
		this.extraPacks = List.copyOf(builder.extraPacks);
	}

	/** Extra toolkit pack registered through the builder, applied when {@code resolveConfiguration} builds the registry. */
	private record ExtraPack(String name, List<String> cssPaths, List<String> jsPaths) {}

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
	 * 		{@code theme}, {@code jcTagHtml} are set only when {@code getSharedVariable(name)} is unset. A
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
		if (existing == null)
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
		if (loader == null)
			return false;
		try {
			var source = loader.findTemplateSource(BASE_TEMPLATE_PATH);
			if (source == null)
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
		if (cfg.getSharedVariable(TagMethodModel.NAME) == null)
			cfg.setSharedVariable(TagMethodModel.NAME, new TagMethodModel());
		if (cfg.getSharedVariable(PageDirectiveModel.NAME) == null) {
			var packs = new ToolkitPackRegistry();
			for (var extra : extraPacks)
				packs.register(extra.name(), extra.cssPaths(), extra.jsPaths());
			cfg.setSharedVariable(PageDirectiveModel.NAME, new PageDirectiveModel(chromeTemplate, packs));
		}
		if (cfg.getSharedVariable(CardDirectiveModel.NAME) == null)
			cfg.setSharedVariable(CardDirectiveModel.NAME, new CardDirectiveModel());
		if (cfg.getSharedVariable(NavigationDirectiveModel.NAME) == null)
			cfg.setSharedVariable(NavigationDirectiveModel.NAME, new NavigationDirectiveModel());
		if (cfg.getSharedVariable(NodeDirectiveModel.NAME) == null)
			cfg.setSharedVariable(NodeDirectiveModel.NAME, new NodeDirectiveModel());
		if (cfg.getSharedVariable(ThemeDirectiveModel.NAME) == null)
			cfg.setSharedVariable(ThemeDirectiveModel.NAME, new ThemeDirectiveModel());
	}

	/**
	 * Builder for {@link ConsoleFreemarkerMixin}.
	 */
	public static class Builder extends FreemarkerMixin.Builder {

		String chromeTemplate = DEFAULT_CHROME_TEMPLATE;
		final List<ExtraPack> extraPacks = new ArrayList<>();

		/** Constructor &mdash; package access for {@link ConsoleFreemarkerMixin#create()}. */
		protected Builder() {}

		/**
		 * {@inheritDoc}
		 *
		 * <p>
		 * Covariantly narrowed to this {@code Builder} so a fluent chain can mix inherited setters
		 * (e.g. {@link #basePath(String)}) with the console-specific {@link #chromeTemplate(String)} /
		 * {@link #registerToolkitPack(String, java.util.List, java.util.List)} in any order.
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
		 * Registers an app-supplied toolkit pack (Q7 A / I2 public seam).
		 *
		 * <p>
		 * Extra packs are applied when {@code resolveConfiguration} constructs the
		 * {@link ToolkitPackRegistry}, alongside the built-in {@code "views"} pack. A second pack after
		 * GA must not need a new public method.
		 *
		 * @param name The pack name.
		 * @param cssPaths Ordered CSS asset paths.
		 * @param jsPaths Ordered JS asset paths.
		 * @return This object.
		 */
		public Builder registerToolkitPack(String name, List<String> cssPaths, List<String> jsPaths) {
			extraPacks.add(new ExtraPack(name, List.copyOf(cssPaths), List.copyOf(jsPaths)));
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
