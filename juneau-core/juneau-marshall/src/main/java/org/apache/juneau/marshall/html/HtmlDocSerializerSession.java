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
package org.apache.juneau.marshall.html;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * Context object that lives for the duration of a single serialization of {@link HtmlSerializer} and its subclasses.
 *
 * <p>
 * See {@link Serializer} for details.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlSupport">HTML Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class HtmlDocSerializerSession extends HtmlStrippedDocSerializerSession {

	// Property name constants
	private static final String PROP_ctx = "ctx";
	private static final String PROP_varResolver = "varResolver";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends HtmlStrippedDocSerializerSession.Builder<SELF> {

		private HtmlDocSerializer ctx;
		private String nonce;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(HtmlDocSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		/**
		 * Sets the per-response Content-Security-Policy nonce stamped onto inline {@code <script>}/{@code <style>}
		 * tags emitted during this render.
		 *
		 * <p>
		 * This is a per-render (session-scoped) value — it intentionally does NOT participate in the serializer
		 * cache key, since a per-request nonce would otherwise force a cache miss on every request.
		 *
		 * @param value The nonce token. May be <jk>null</jk> to disable nonce stamping.
		 * @return This object.
		 */
		public SELF nonce(String value) {
			nonce = value;
			return self();
		}

		@Override
		public HtmlDocSerializerSession build() {
			return new HtmlDocSerializerSession(this);
		}

	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@code create()} path (CRTP terminal).
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder(HtmlDocSerializer ctx) {
			super(ctx);
		}
	}

	private static final VarResolver DEFAULT_VR = VarResolver.create().defaultVars().vars(HtmlWidgetVar.class).build();

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S1452" // Builder<?> wildcard return intentional; callers use it to construct session instances polymorphically
	})
	public static Builder<?> create(HtmlDocSerializer ctx) {
		return new DefaultBuilder(ctx);
	}

	private final HtmlDocSerializer ctx;
	private final String nonce;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected HtmlDocSerializerSession(Builder<?> builder) {
		super(builder);
		ctx = builder.ctx;
		nonce = builder.nonce;
		addVarBean(HtmlWidgetMap.class, ctx.getWidgets());
	}

	/**
	 * The per-response Content-Security-Policy nonce for this render.
	 *
	 * <p>
	 * When non-<jk>null</jk>, inline {@code <script>}/{@code <style>} tags emitted by the HtmlDoc template are
	 * stamped with a matching {@code nonce="..."} attribute so they satisfy a nonce-based CSP.
	 *
	 * @see HtmlDocSerializerSession.Builder#nonce(String)
	 * @return The nonce token, or <jk>null</jk> if nonce stamping is disabled.
	 */
	public final String getNonce() { return nonce; }

	/**
	 * Returns the {@link HtmlDocSerializer.Builder#navlinks(String...)} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializer.Builder#navlinks(String...)} setting value in this context.
	 * 	<jk>null</jk> if not specified.
	 * 	Never an empty map.
	 */
	public final String[] getNavLinks() { return ctx.navlinks; }

	/**
	 * Calls the parent {@link #doWrite(SerializerPipe, Object)} method which invokes just the HTML serializer.
	 *
	 * @param out
	 * 	Where to send the output from the serializer.
	 * @param o The object being serialized.
	 * @throws Exception Error occurred during serialization.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - callback/lifecycle method for parent serializer
	})
	public void parentSerialize(Object out, Object o) throws Exception {
		try (var pipe = createPipe(out)) {
			super.doWrite(pipe, o);
		}
	}

	@Override /* Overridden from SerializerSession */
	protected VarResolverSession createDefaultVarResolverSession() {
		return DEFAULT_VR.createSession();
	}

	@Override /* Overridden from Serializer */
	protected void doWrite(SerializerPipe out, Object o) throws IOException, SerializeException {

		try (var w = getHtmlWriter(out)) {
			try {
				getTemplate().writeTo(this, w, o);
			} catch (Exception e) {
				throw new SerializeException(e);
			}
		}
	}

	/**
	 * Performs an action on all widgets defined in his session.
	 *
	 * @param action The action to perform.
	 * @see HtmlDocSerializer.Builder#widgets(Class...)
	 * @return This object.
	 */
	protected final HtmlDocSerializerSession forEachWidget(Consumer<HtmlWidget> action) {
		ctx.forEachWidget(action);
		return this;
	}

	/**
	 * Aside section contents.
	 *
	 * @see HtmlDocSerializer.Builder#aside(String...)
	 * @return
	 * 	The overridden contents of the aside section on the HTML page.
	 */
	protected final String[] getAside() { return ctx.aside; }

	/**
	 * Aside section contents float.
	 *
	 * @see HtmlDocSerializer.Builder#asideFloat(AsideFloat)
	 * @return
	 * 	The location of where to place the aside section.
	 */
	protected final AsideFloat getAsideFloat() { return ctx.asideFloat; }

	/**
	 * Footer section contents.
	 *
	 * @see HtmlDocSerializer.Builder#footer(String...)
	 * @return
	 * 	The overridden contents of the footer section on the HTML page.
	 */
	protected final String[] getFooter() { return ctx.footer; }

	/**
	 * Additional head section content.
	 *
	 * @see HtmlDocSerializer.Builder#head(String...)
	 * @return
	 * 	HTML content to add to the head section of the HTML page.
	 */
	protected final String[] getHead() { return ctx.head; }

	/**
	 * Header section contents.
	 *
	 * @see HtmlDocSerializer.Builder#header(String...)
	 * @return
	 * 	The overridden contents of the header section on the HTML page.
	 */
	protected final String[] getHeader() { return ctx.header; }

	/**
	 * Nav section contents.
	 *
	 * @see HtmlDocSerializer.Builder#nav(String...)
	 * @return
	 * 	The overridden contents of the nav section on the HTML page.
	 */
	protected final String[] getNav() { return ctx.nav; }

	/**
	 * Page navigation links.
	 *
	 * @see HtmlDocSerializer.Builder#navlinks(String...)
	 * @return
	 * 	Navigation links to add to the HTML page.
	 */
	@SuppressWarnings({
		"java:S1845" // Method name intentionally differs only by case from getNavLinks for backward compatibility
	})
	protected final String[] getNavlinks() { return ctx.navlinks; }

	/**
	 * No-results message.
	 *
	 * @see HtmlDocSerializer.Builder#noResultsMessage(String)
	 * @return
	 * 	The message used when serializing an empty array or empty list.
	 */
	protected final String getNoResultsMessage() { return ctx.getNoResultsMessage(); }

	/**
	 * Javascript code.
	 *
	 * @see HtmlDocSerializer.Builder#script(String...)
	 * @return
	 * 	Arbitrary Javascript to add to the HTML page.
	 */
	protected final String[] getScript() { return ctx.script; }

	/**
	 * CSS style code.
	 *
	 * @see HtmlDocSerializer.Builder#style(String...)
	 * @return
	 * 	The CSS instructions to add to the HTML page.
	 */
	protected final String[] getStyle() { return ctx.style; }

	/**
	 * Stylesheet import URLs.
	 *
	 * @see HtmlDocSerializer.Builder#stylesheet(String...)
	 * @return
	 * 	The link to the stylesheet of the HTML page.
	 */
	protected final String[] getStylesheet() { return ctx.stylesheet; }

	/**
	 * HTML document template.
	 *
	 * @see HtmlDocSerializer.Builder#template(Class)
	 * @return
	 * 	The template to use for serializing the page.
	 */
	protected final HtmlDocTemplate getTemplate() { return ctx.getTemplate(); }

	/**
	 * Prevent word wrap on page.
	 *
	 * @see HtmlDocSerializer.Builder#nowrap()
	 * @return
	 * 	<jk>true</jk> if <js>"* {white-space:nowrap}"</js> should be added to the CSS instructions on the page to prevent word wrapping.
	 */
	protected final boolean isNowrap() { return ctx.nowrap; }

	/**
	 * Resolve $ variables in serialized POJO.
	 *
	 * @see HtmlDocSerializer.Builder#resolveBodyVars()
	 * @return
	 * 	<jk>true</jk> if $ variables in serialized POJO should be resolved.
	 */
	protected final boolean isResolveBodyVars() { return ctx.resolveBodyVars; }

	@Override /* Overridden from HtmlStrippedDocSerializerSession */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_ctx, ctx)
			.a(PROP_varResolver, getVarResolver());
	}
}