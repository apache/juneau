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
 * <h5 class='section'>Consumer-supplied {@code Configuration} is the same instance, then fill-missing stamped:</h5>
 * <p>
 * Same rule as {@link ConsoleFreemarkerMixin}: {@link #resolveConfiguration} returns the consumer bean
 * {@code ==}-identical so settings win, then fill-missing-stamps {@code jcDataTableHtml} (and the parent
 * reserved names) and splices the console loader only when {@link ConsoleFreemarkerMixin#BASE_TEMPLATE_PATH}
 * would not already resolve. Opt out by registering {@link org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin}
 * instead of this class.
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
	// shared with the parent's private one) since this class needs to answer "have I already fill-missing-stamped
	// jcDataTableHtml on THIS config" independently of the parent's loader / reserved-name stamp. Presence
	// checks still run on a cache miss so two mixin instances sharing one Spring singleton do not re-set.
	@SuppressWarnings({
		"java:S3077" // volatile required for correct double-checked-locking safe-publication; see ConsoleFreemarkerMixin's identical field.
	})
	private volatile Configuration wrappedConfiguration;

	/**
	 * Resolves the active {@link Configuration}: delegates to {@link ConsoleFreemarkerMixin#resolveConfiguration}
	 * for console-loader / reserved-name stamping, then fill-missing-registers {@code jcDataTableHtml}.
	 *
	 * <p>
	 * A consumer {@code Configuration} bean is the same {@code ==} instance (settings win). {@code jcDataTableHtml}
	 * is set only when unset. Identity wrap-once plus the parent's loader presence check keep a shared Spring
	 * singleton from nesting {@code MultiTemplateLoader}.
	 *
	 * @param req The current REST request.
	 * @return The active FreeMarker configuration. Never {@code null}.
	 */
	@Override
	public Configuration resolveConfiguration(RestRequest req) {
		var cfg = super.resolveConfiguration(req);
		if (cfg == wrappedConfiguration)
			return cfg;
		synchronized (this) {
			if (cfg != wrappedConfiguration) {
				if (cfg.getSharedVariable(DataTableMethodModel.NAME) == null)
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
