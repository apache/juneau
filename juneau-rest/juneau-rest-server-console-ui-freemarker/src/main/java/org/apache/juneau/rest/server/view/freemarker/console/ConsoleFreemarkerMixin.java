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
 * <h5 class='section'>Consumer-supplied {@code Configuration} is honored, not augmented:</h5>
 * <p>
 * When the request's {@code BeanStore} already has a {@code Configuration} bean (Spring/Spring-Boot autoconfig, or
 * a microservice {@code BasicBeanStore.put}), {@link #resolveConfiguration} returns that <b>same instance</b>
 * ({@code ==}), completely untouched &mdash; the console loader is not spliced in, and
 * {@code org/apache/juneau/console/base.ftlh} does not resolve unless the consumer adds it themselves. This is a
 * documented v1 caveat, not an oversight: augmenting an externally-owned singleton {@code Configuration} silently
 * would be a surprising side effect for a consumer.
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
	}

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	// Double-checked-locking identity cache (Phase 5 should-fix S2 gate 1): resolveConfiguration is called once
	// per request, but super.resolveConfiguration(req) returns the SAME cached Configuration object on every
	// call for a given mixin instance when no consumer Configuration bean is registered (FreemarkerDispatcher's
	// own lazy default-Configuration cache). Tracking that object's identity here means "have I already wrapped
	// THIS exact object" is answered in O(1) without re-wrapping (which would otherwise nest
	// Multi(Multi(base, console), console) on every subsequent call).
	@SuppressWarnings({
		"java:S3077" // volatile is required here for correct double-checked-locking safe-publication; the reference is publish-once (fully constructed/wrapped before assignment) and never compound-mutated.
	})
	private volatile Configuration wrappedConfiguration;

	/**
	 * Resolves the active {@link Configuration}, augmented with the reserved-path console loader when no
	 * consumer-supplied {@code Configuration} bean is present.
	 *
	 * <p>
	 * Two independent guards, both should-fix S2 gates:
	 * <ol class='spaced-list'>
	 * 	<li><b>Consumer identity honored:</b> if the request's {@code BeanStore} already has a {@code Configuration}
	 * 		bean, it is returned {@code ==}-identical and completely untouched &mdash; no console loader is spliced
	 * 		in, and {@link #BASE_TEMPLATE_PATH} will not resolve through it unless the consumer adds it themselves.
	 * 	<li><b>Wrap-once:</b> otherwise, the mixin-instance-owned bridge-default {@code Configuration} is augmented
	 * 		exactly once (identity-gated double-checked locking below); every subsequent call on the same instance
	 * 		returns the same already-augmented object without re-wrapping its {@code TemplateLoader}.
	 * </ol>
	 *
	 * @param req The current REST request.
	 * @return The active FreeMarker configuration. Never {@code null}.
	 */
	@Override
	public Configuration resolveConfiguration(RestRequest req) {
		if (req.getContext().getBeanStore().getBean(Configuration.class).isPresent())
			return super.resolveConfiguration(req);
		var base = super.resolveConfiguration(req);
		if (base == wrappedConfiguration)
			return base;
		synchronized (this) {
			if (base != wrappedConfiguration) {
				base.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[]{
					base.getTemplateLoader(),
					new ClassTemplateLoader(getClass().getClassLoader(), "")
				}));
				base.setSharedVariable(TagMethodModel.NAME, new TagMethodModel());
				wrappedConfiguration = base;
			}
		}
		return base;
	}

	/**
	 * Builder for {@link ConsoleFreemarkerMixin}.
	 */
	public static class Builder extends FreemarkerMixin.Builder {

		/** Constructor &mdash; package access for {@link ConsoleFreemarkerMixin#create()}. */
		protected Builder() {}

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
