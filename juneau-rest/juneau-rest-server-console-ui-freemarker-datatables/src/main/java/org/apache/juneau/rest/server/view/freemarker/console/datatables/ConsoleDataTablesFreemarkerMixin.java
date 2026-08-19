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
package org.apache.juneau.rest.server.view.freemarker.console.datatables;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.freemarker.console.*;

import freemarker.template.*;

/**
 * A {@link ConsoleFreemarkerMixin} that additionally makes the reserved-classpath {@code <@datatable>} macro
 * template ({@code org/apache/juneau/console/datatables/datatable.ftlh}) resolvable from any consumer template.
 *
 * <h5 class='section'>Used INSTEAD OF {@link ConsoleFreemarkerMixin}/{@link org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin},
 * not beside it:</h5>
 * <p>
 * Same load-bearing-return-type rule as {@link ConsoleFreemarkerMixin}'s own class javadoc: register it typed as
 * {@code FreemarkerMixin} so {@code FreemarkerViewRenderer}'s exact-type {@code getBean(FreemarkerMixin.class)}
 * lookup finds it.
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja> <jk>public</jk> FreemarkerMixin freemarker() {
 * 		<jk>return</jk> ConsoleDataTablesFreemarkerMixin.<jsm>create</jsm>().basePath(<js>"/templates/"</js>).build();
 * 	}
 * </p>
 *
 * <h5 class='section'>No second {@code ClassTemplateLoader} needed:</h5>
 * <p>
 * {@link ConsoleFreemarkerMixin#resolveConfiguration} already splices in a classpath-<b>root</b>-rooted
 * {@code ClassTemplateLoader(getClass().getClassLoader(), "")}. Because {@code getClass()} resolves to <b>this</b>
 * subclass's runtime class, its classloader sees every module's classpath resources (this module's
 * {@code datatable.ftlh} included) &mdash; so the reserved {@link #DATATABLE_TEMPLATE_PATH} resolves through that
 * same loader without any additional wiring here.
 *
 * <h5 class='section'>Consumer-supplied {@code Configuration} is honored, not augmented (same S2 guard as the parent):</h5>
 * <p>
 * If the request's {@code BeanStore} already has a {@code Configuration} bean, {@link #resolveConfiguration} returns
 * it completely untouched (the {@code <@datatable>} shared variable is not spliced in either) &mdash; same
 * documented v1 caveat as {@link ConsoleFreemarkerMixin}.
 *
 * @since 10.0.0
 */
public class ConsoleDataTablesFreemarkerMixin extends ConsoleFreemarkerMixin {

	/** The reserved classpath-root-relative location of the shipped {@code <@datatable>} macro template. */
	public static final String DATATABLE_TEMPLATE_PATH = "org/apache/juneau/console/datatables/datatable.ftlh";

	/** No-arg constructor &mdash; mirrors {@link ConsoleFreemarkerMixin#ConsoleFreemarkerMixin()}. */
	public ConsoleDataTablesFreemarkerMixin() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 */
	protected ConsoleDataTablesFreemarkerMixin(Builder builder) {
		super(builder);
	}

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

	// Same wrap-once identity-cache pattern as ConsoleFreemarkerMixin's own field, kept as a SEPARATE field (not
	// shared with the parent's private one) since this class needs to answer "have I already registered the
	// <@datatable> shared variable on THIS config" independently of the parent's "have I already spliced in the
	// console loader" question -- the two guards happen to gate on the same underlying object in practice, but
	// are conceptually distinct augmentations and each class owns only its own idempotency.
	@SuppressWarnings({
		"java:S3077" // volatile required for correct double-checked-locking safe-publication; see ConsoleFreemarkerMixin's identical field.
	})
	private volatile Configuration wrappedConfiguration;

	/**
	 * Resolves the active {@link Configuration}: delegates to {@link ConsoleFreemarkerMixin#resolveConfiguration}
	 * for the console-loader augmentation, then additionally registers the {@code <@datatable>} shared variable
	 * &mdash; unless the request's {@code BeanStore} already has a consumer-supplied {@code Configuration} bean, in
	 * which case (mirroring the parent's own guard) it is returned untouched.
	 *
	 * @param req The current REST request.
	 * @return The active FreeMarker configuration. Never {@code null}.
	 */
	@SuppressWarnings({
		"resource" // False positive: req.getContext().getBeanStore() returns a borrowed, container-owned AutoCloseable, not a resource created/owned here.
	})
	@Override
	public Configuration resolveConfiguration(RestRequest req) {
		var cfg = super.resolveConfiguration(req);
		if (req.getContext().getBeanStore().getBean(Configuration.class).isPresent())
			return cfg;
		if (cfg == wrappedConfiguration)
			return cfg;
		synchronized (this) {
			if (cfg != wrappedConfiguration) {
				cfg.setSharedVariable(DataTableMethodModel.NAME, new DataTableMethodModel());
				wrappedConfiguration = cfg;
			}
		}
		return cfg;
	}

	/**
	 * Builder for {@link ConsoleDataTablesFreemarkerMixin}.
	 */
	public static class Builder extends ConsoleFreemarkerMixin.Builder {

		/** Constructor &mdash; package access for {@link ConsoleDataTablesFreemarkerMixin#create()}. */
		protected Builder() {}

		/**
		 * Builds the {@link ConsoleDataTablesFreemarkerMixin}.
		 *
		 * <p>
		 * Overrides {@link org.apache.juneau.rest.server.view.freemarker.console.ConsoleFreemarkerMixin.Builder#build()}
		 * &mdash; without this override, the inherited method would silently return a plain
		 * {@link ConsoleFreemarkerMixin} with no {@code <@datatable>} macro.
		 *
		 * @return A new {@link ConsoleDataTablesFreemarkerMixin} instance.
		 */
		@Override
		public ConsoleDataTablesFreemarkerMixin build() {
			return new ConsoleDataTablesFreemarkerMixin(this);
		}
	}
}
