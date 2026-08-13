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
package org.apache.juneau.rest.server.view.freemarker;

import java.io.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

import freemarker.template.*;

/**
 * Mixin that wires Apache FreeMarker view-rendering onto any Juneau REST resource.
 *
 * <p>
 * Compose into a host resource via
 * {@link Rest#mixins() @Rest(mixins=FreemarkerMixin.class)}; the host then:
 *
 * <ol class='spaced-list'>
 * 	<li>Gains a default mount at {@code /freemarker/*} that serves raw FreeMarker templates from
 * 		the importer's classpath by asking the configured {@link Configuration} for the named
 * 		template and rendering it with an empty data model (raw render path; callers who want
 * 		attributes use {@link FreemarkerView} from a typed handler instead).
 * 	<li>Registers {@link FreemarkerViewRenderer} for the mixin's <b>own</b> endpoints (e.g. the
 * 		{@code /freemarker/*} route above) via the mixin's own
 * 		{@link Rest#responseProcessors() @Rest(responseProcessors=...)} declaration.
 * </ol>
 *
 * <h5 class='section'>Auto-wiring the renderer into the host's own endpoints:</h5>
 *
 * <p>
 * A bare {@code @Rest(mixins=FreemarkerMixin.class)} is enough &mdash; a host whose own {@code @RestOp} method
 * returns {@link FreemarkerView} renders it through the FreeMarker engine with no extra wiring. That works
 * because {@code FreemarkerMixin} declares
 * {@link Rest#mergeResponseProcessorsIntoHost() @Rest(mergeResponseProcessorsIntoHost=true)} on its own class,
 * so the framework folds {@code FreemarkerMixin}'s {@link FreemarkerViewRenderer} into the host's own
 * response-processor chain automatically.
 *
 * <p>
 * This fold is per-mixin, opt-in, and response-processor-scoped: it happens only because
 * {@code FreemarkerMixin} declares the opt-in (a mixin that does not is still scoped to its own endpoints, the
 * standard {@link Rest#mixins() @Rest(mixins=...)} rule), and it folds only the renderer &mdash; never
 * {@code guards}, {@code serializers}, or any other list-shaped attribute.
 *
 * <p>
 * Two explicit wirings remain supported &mdash; useful when you want to be explicit, or to fold more than the
 * renderer:
 *
 * <ol class='spaced-list'>
 * 	<li><b>{@link Mixin#mergeIntoHost() @Mixin(mergeIntoHost=true)}</b> via the rich {@link Rest#mixinDefs()
 * 		mixinDefs} form &mdash; folds <b>all</b> of {@code FreemarkerMixin}'s list-shaped {@code @Rest}
 * 		attributes into the host's own chain, not just the renderer.
 * 	<li><b>List {@link FreemarkerViewRenderer FreemarkerViewRenderer.class} explicitly</b> in the host's own
 * 		{@code @Rest(responseProcessors=...)} &mdash; the fully manual equivalent.
 * </ol>
 *
 * <p>
 * The {@link org.apache.juneau.rest.server.processor.ResponseProcessorList} partition pass repositions
 * {@link FreemarkerViewRenderer} (a
 * {@link org.apache.juneau.rest.server.view.ViewRenderer ViewRenderer}) ahead of
 * {@code SerializedPojoProcessor} in every case, so the {@link FreemarkerView} bean is dispatched to the
 * FreeMarker engine rather than bean-serialized.
 *
 * <h5 class='figure'>Composition example (microservice):</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/app"</js>, mixins=FreemarkerMixin.<jk>class</jk>)
 * 	<jk>public class</jk> AppResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja> FreemarkerMixin freemarker() {
 * 			<jk>return</jk> FreemarkerMixin.<jsm>create</jsm>()
 * 				.basePath(<js>"/templates/"</js>)
 * 				.templateSuffix(<js>".ftlh"</js>)
 * 				.build();
 * 		}
 *
 * 		<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 		<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 			<jk>return</jk> FreemarkerView.<jsm>of</jsm>(<js>"hello"</js>).attr(<js>"name"</js>, <jv>name</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount {@code /freemarker/*} can be overridden via the SVL variable
 * {@code ${juneau.freemarker.path:freemarker}} &mdash; set via system property
 * ({@code -Djuneau.freemarker.path=views}), environment variable
 * ({@code JUNEAU_FREEMARKER_PATH=views}), or {@code Config} key
 * ({@code juneau.freemarker.path = views}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time (SVL resolution in {@code @RestOp(path)}).
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by
 * {@link RestGet @RestGet(path="/&#123;juneau.freemarker.path:freemarker&#125;/*")} on {@link #render};
 * a class-level {@code @Rest(paths=...)} declaration would be silently ignored under the mixin
 * pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='section'>Choosing a Configuration:</h5>
 *
 * <p>
 * The {@code juneau-rest-server-view-freemarker} module ships <b>only</b>
 * {@code org.freemarker:freemarker} in {@code provided} scope. <b>No configuration bean is
 * bundled.</b> The bridge resolves a {@link Configuration} at first use from three sources, in
 * order:
 *
 * <ul class='spaced-list'>
 * 	<li><b>User-supplied bean:</b> register a {@code @Bean freemarker.template.Configuration}
 * 		(Spring) or
 * 		{@code BasicBeanStore.put(freemarker.template.Configuration.class, cfg)} (microservice).
 * 		Spring Boot's {@code spring-boot-starter-freemarker} autoconfigures one out of the box;
 * 		the bridge picks it up via {@code BeanStore.getBean(Configuration.class)}.
 * 	<li><b>Bridge default:</b> when nothing is registered, the bridge constructs a default
 * 		{@link Configuration} anchored on a classpath resource root derived from
 * 		{@link #getBasePath() basePath} ({@code "/templates/"} becomes
 * 		{@code ClassLoaderTemplateResolver}-equivalent prefix {@code "/templates"}). The default
 * 		pins {@code IncompatibleImprovements} to the bridge-tested minor version
 * 		({@code Configuration.VERSION_2_3_34}), sets {@code DefaultEncoding} to {@code UTF-8},
 * 		uses {@code HTMLOutputFormat} so HTML escaping is the natural target, applies
 * 		{@code TemplateUpdateDelayMilliseconds} per the {@link #isCacheTemplates() cacheTemplates}
 * 		flag (production-safe by default; users opt into hot-reload via
 * 		{@link Builder#cacheTemplates(boolean) cacheTemplates(false)}), and sets an
 * 		{@code ObjectWrapper} with {@code exposeFields=true} (see below) unless overridden.
 * </ul>
 *
 * <p>
 * When no FreeMarker engine is on the classpath, the renderer surfaces
 * {@link FreemarkerViewRenderer#NO_ENGINE_DIAGNOSTIC} naming the missing dependency.
 *
 * <h5 class='section'>Public-field DTOs (getter-only default trap):</h5>
 *
 * <p>
 * FreeMarker's own version-default {@code ObjectWrapper} exposes only JavaBean getters &mdash; a
 * view-model bean written as a simple DTO with public fields and no getters resolves every
 * {@code ${bean.field}} reference to {@code null}/missing, <b>silently</b> (a template using
 * {@code !'default'} fallbacks renders the default with no error at all; a bare {@code ${bean.field}}
 * with no fallback throws at render time). This bridge avoids that trap by default: the bridge-built
 * {@link Configuration} sets a {@link DefaultObjectWrapper} with {@code exposeFields=true}
 * ({@link #DEFAULT_EXPOSE_FIELDS}), so public-field DTOs render their field values out of the
 * box &mdash; matching how Juneau's own marshalling is comfortable with public-field beans elsewhere.
 * Use {@link Builder#exposeFields(boolean) exposeFields(false)} to restore FreeMarker's getter-only
 * behavior, or {@link Builder#objectWrapper(ObjectWrapper) objectWrapper(...)} for full control
 * (e.g. a {@code BeansWrapper} with custom exposure rules). Both knobs only affect the
 * bridge-default {@link Configuration}; a user-supplied {@code @Bean Configuration} is used as-is.
 *
 * <h5 class='section'>Template suffix:</h5>
 *
 * <p>
 * FreeMarker's API does not expose a configuration-level resolver suffix &mdash; the literal
 * template name is what {@link Configuration#getTemplate(String) cfg.getTemplate(...)} sees. The
 * {@link Builder#templateSuffix(String) templateSuffix(...)} builder knob fills the gap: when
 * non-blank, the bridge appends the configured suffix to template names that don't already end
 * with it (idempotent), so callers can write {@code FreemarkerView.of("hello")} or request
 * {@code /freemarker/hello} and have it resolve to {@code hello.ftlh} on the classpath.
 *
 * <h5 class='section'>{@code .ftl} vs {@code .ftlh}:</h5>
 *
 * <p>
 * FreeMarker auto-selects HTML escaping by file extension: {@code .ftlh} templates emit
 * HTML-escaped output; {@code .ftl} templates emit raw output. For HTML responses, prefer
 * {@code .ftlh} so a future attribute-binding change can't introduce an XSS regression.
 *
 * <h5 class='section'>Multiple base paths:</h5>
 *
 * <p>
 * A host with {@code /templates/} and {@code /admin/templates/} template trees registers two
 * {@code FreemarkerMixin} beans, each mounted via a subclass declaring its own op-level
 * {@code @RestGet(path=...)} override and its own {@code basePath}. See the
 * {@code FreemarkerMixin_MockRest_Test} test in {@code juneau-integration-tests} for the canonical
 * pattern.
 *
 * <h5 class='section'>OpenAPI surface:</h5>
 *
 * <p>
 * The greedy {@code /*} handler is not API-meaningful and is excluded from generated
 * Swagger / OpenAPI specs via {@link OpSwagger#ignore() @OpSwagger(ignore=true)}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FreemarkerView}
 * 	<li class='jc'>{@link FreemarkerViewRenderer}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/FreemarkerViewSupport">FreeMarker View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Mixins and Multi-Mount Paths</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest(
	responseProcessors={FreemarkerViewRenderer.class},
	mergeResponseProcessorsIntoHost=true
)
public class FreemarkerMixin {

	/** Default base path applied when no {@link Builder#basePath(String)} call has been made. */
	public static final String DEFAULT_BASE_PATH = FreemarkerDispatcher.DEFAULT_BASE_PATH;

	/** Default template suffix &mdash; empty (literal template names, no implicit suffix). */
	public static final String DEFAULT_TEMPLATE_SUFFIX = FreemarkerDispatcher.DEFAULT_TEMPLATE_SUFFIX;

	/** Default template-cache flag &mdash; {@code true} (production-safe). */
	public static final boolean DEFAULT_CACHE_TEMPLATES = FreemarkerDispatcher.DEFAULT_CACHE_TEMPLATES;

	/** Default field-exposure flag &mdash; {@code true} (public-field DTOs render out of the box). */
	public static final boolean DEFAULT_EXPOSE_FIELDS = FreemarkerDispatcher.DEFAULT_EXPOSE_FIELDS;

	private final FreemarkerDispatcher worker;

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * No-arg constructor &mdash; uses {@link #DEFAULT_BASE_PATH} as the base path,
	 * {@link #DEFAULT_TEMPLATE_SUFFIX} as the template suffix, and
	 * {@link #DEFAULT_CACHE_TEMPLATES} as the cache flag.
	 *
	 * <p>
	 * The mixin walk falls back to this constructor when the
	 * {@link org.apache.juneau.commons.inject.BeanStore BeanStore} does not have a registered
	 * {@code FreemarkerMixin} bean.
	 */
	public FreemarkerMixin() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 */
	protected FreemarkerMixin(Builder builder) {
		worker = builder.worker.build();
	}

	/**
	 * Returns the base path under which template resources are resolved.
	 *
	 * @return The base path. Never {@code null}.
	 */
	public String getBasePath() {
		return worker.getBasePath();
	}

	/**
	 * Returns the configured template-name suffix.
	 *
	 * @return The template suffix. Never {@code null}.
	 */
	public String getTemplateSuffix() {
		return worker.getTemplateSuffix();
	}

	/**
	 * Returns the template-cache flag.
	 *
	 * @return The cache flag.
	 */
	public boolean isCacheTemplates() {
		return worker.isCacheTemplates();
	}

	/**
	 * Returns the field-exposure flag applied to the bridge-default object wrapper.
	 *
	 * @return The field-exposure flag. Ignored once {@link #getObjectWrapper()} returns non-{@code null}.
	 */
	public boolean isExposeFields() {
		return worker.isExposeFields();
	}

	/**
	 * Returns the user-supplied {@link ObjectWrapper} override, if any.
	 *
	 * @return The object wrapper override, or {@code null} if the bridge builds its own default.
	 */
	public ObjectWrapper getObjectWrapper() {
		return worker.getObjectWrapper();
	}

	/**
	 * Appends {@link #getTemplateSuffix()} to {@code name} if not already present (idempotent).
	 *
	 * @param name The template name. Must not be {@code null}.
	 * @return The template name with the configured suffix appended (if applicable).
	 */
	public String applyTemplateSuffix(String name) {
		return worker.applyTemplateSuffix(name);
	}

	/**
	 * Resolves the active {@link Configuration} via the shared {@link FreemarkerDispatcher} worker.
	 *
	 * @param req The current REST request.
	 * @return The active FreeMarker configuration. Never {@code null}.
	 */
	public Configuration resolveConfiguration(RestRequest req) {
		return worker.resolveConfiguration(req);
	}

	/**
	 * [GET /freemarker/*] &mdash; render a raw template through the FreeMarker engine.
	 *
	 * <p>
	 * The {@code @Path("/*") String path} captures the multi-segment trailing remainder (e.g. a
	 * request for {@code /freemarker/about.ftlh} matches the mount with
	 * {@code path = "about.ftlh"}). Delegates to the shared {@link FreemarkerDispatcher} worker
	 * (path-traversal hardening, suffix application, configuration resolution, and render).
	 *
	 * <p>
	 * Missing template surfaces as the engine's own {@link TemplateNotFoundException}
	 * (HTTP 500 from the renderer's catch-all); missing engine surfaces as
	 * {@link FreemarkerViewRenderer#NO_ENGINE_DIAGNOSTIC}.
	 *
	 * @param path The trailing path segment after {@code /freemarker/} (the template name
	 * 	relative to the configured {@link #getBasePath() base path}; configured suffix appended
	 * 	if missing).
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws IOException If the underlying response writer fails.
	 * @throws BasicHttpException On boundary violation (403), missing engine (500), or render
	 * 	failure (500).
	 */
	@RestGet(
		path="/${juneau.freemarker.path:freemarker}/*",
		summary="FreeMarker view",
		description="Render a raw FreeMarker template under the configured base path.",
		swagger=@OpSwagger(ignore=true)
	)
	public void render(@Path("/*") String path, RestRequest req, RestResponse res)
			throws IOException, BasicHttpException {
		worker.render(path, req, res);
	}

	/**
	 * Builder for {@link FreemarkerMixin}.
	 *
	 * <p>
	 * Mirrors {@link FreemarkerDispatcher.Builder}'s configuration methods on its own surface and
	 * forwards each call into a held {@link FreemarkerDispatcher.Builder} (§2.3.1 worker-bean
	 * composition). Implements {@link ViewMixinBuilder} so {@link #basePath(String)} and
	 * {@link #cacheTemplates(boolean)} are guaranteed to transfer to the sibling JSP / Thymeleaf /
	 * Mustache bridge builders.
	 */
	public static class Builder implements ViewMixinBuilder<Builder> {

		private final FreemarkerDispatcher.Builder worker = FreemarkerDispatcher.create();

		/** Constructor &mdash; package access for {@link FreemarkerMixin#create()}. */
		protected Builder() {}

		/**
		 * Sets the classpath base path under which template resources are resolved.
		 *
		 * <p>
		 * Defaults to {@link FreemarkerMixin#DEFAULT_BASE_PATH "/"}. A typical configured
		 * value is {@code "/templates/"} &mdash; the same Spring-Boot-compatible layout
		 * convention the sibling JSP / Thymeleaf / Mustache bridges use.
		 *
		 * @param value The base path. {@code null} or blank values reset to the default
		 * 	{@link FreemarkerMixin#DEFAULT_BASE_PATH}.
		 * @return This object.
		 */
		@Override /* ViewMixinBuilder */
		public Builder basePath(String value) {
			worker.basePath(value);
			return this;
		}

		/**
		 * Sets the suffix to append to template names that don't already end with it
		 * (idempotent).
		 *
		 * <p>
		 * Defaults to {@link FreemarkerMixin#DEFAULT_TEMPLATE_SUFFIX ""} (no implicit
		 * suffix &mdash; literal template names). Typical configured values are {@code ".ftlh"}
		 * (HTML auto-escape) or {@code ".ftl"} (no auto-escape).
		 *
		 * @param value The new suffix. {@code null} resets to
		 * 	{@link FreemarkerMixin#DEFAULT_TEMPLATE_SUFFIX}.
		 * @return This object.
		 */
		public Builder templateSuffix(String value) {
			worker.templateSuffix(value);
			return this;
		}

		/**
		 * Sets the template-cache flag for the bridge-default configuration.
		 *
		 * <p>
		 * Defaults to {@link FreemarkerMixin#DEFAULT_CACHE_TEMPLATES true} (cache
		 * forever &mdash; production-safe). Set {@code false} to enable dev hot-reload (FreeMarker
		 * re-checks the underlying resource on every render).
		 *
		 * <p>
		 * This flag only affects the bridge-default {@link Configuration}; user-supplied
		 * {@code @Bean Configuration}s are not modified.
		 *
		 * @param value The cache flag.
		 * @return This object.
		 */
		@Override /* ViewMixinBuilder */
		public Builder cacheTemplates(boolean value) {
			worker.cacheTemplates(value);
			return this;
		}

		/**
		 * Sets whether the bridge-default {@code ObjectWrapper} exposes public fields (not just
		 * JavaBean getters) to templates.
		 *
		 * <p>
		 * Defaults to {@link FreemarkerMixin#DEFAULT_EXPOSE_FIELDS true} so simple view-model
		 * DTOs written with public fields and no getters render their field values instead of
		 * silently resolving to {@code null}/missing (see the class-level
		 * "Public-field DTOs" section). Has no effect once {@link #objectWrapper(ObjectWrapper)}
		 * has been called with a non-{@code null} value &mdash; the explicit wrapper always wins.
		 *
		 * @param value The field-exposure flag.
		 * @return This object.
		 */
		public Builder exposeFields(boolean value) {
			worker.exposeFields(value);
			return this;
		}

		/**
		 * Sets a fully custom {@code ObjectWrapper} for the bridge-default configuration,
		 * overriding {@link #exposeFields(boolean)} entirely.
		 *
		 * <p>
		 * Escape hatch for consumers who need full control (e.g. a {@code BeansWrapper} with
		 * custom method/property exposure, or a third-party wrapper) without hand-rolling and
		 * registering a whole replacement {@code Configuration} bean.
		 *
		 * @param value The object wrapper. {@code null} reverts to the bridge-built
		 * 	{@link DefaultObjectWrapper} (per {@link #exposeFields(boolean)}).
		 * @return This object.
		 */
		public Builder objectWrapper(ObjectWrapper value) {
			worker.objectWrapper(value);
			return this;
		}

		/**
		 * Reads the current base path setting (test/inspection helper).
		 *
		 * @return The base path. Never {@code null}.
		 */
		public String getBasePath() {
			return worker.getBasePath();
		}

		/**
		 * Reads the current template-suffix setting (test/inspection helper).
		 *
		 * @return The template suffix. Never {@code null}.
		 */
		public String getTemplateSuffix() {
			return worker.getTemplateSuffix();
		}

		/**
		 * Reads the current cache-templates setting (test/inspection helper).
		 *
		 * @return The cache flag.
		 */
		public boolean isCacheTemplates() {
			return worker.isCacheTemplates();
		}

		/**
		 * Reads the current field-exposure setting (test/inspection helper).
		 *
		 * @return The field-exposure flag.
		 */
		public boolean isExposeFields() {
			return worker.isExposeFields();
		}

		/**
		 * Reads the current object-wrapper override (test/inspection helper).
		 *
		 * @return The object wrapper override, or {@code null} if none has been set.
		 */
		public ObjectWrapper getObjectWrapper() {
			return worker.getObjectWrapper();
		}

		/**
		 * Builds the {@link FreemarkerMixin}.
		 *
		 * @return A new {@link FreemarkerMixin} instance.
		 */
		public FreemarkerMixin build() {
			return new FreemarkerMixin(this);
		}
	}
}
