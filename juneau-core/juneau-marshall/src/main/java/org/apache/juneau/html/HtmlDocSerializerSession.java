// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.html;

import static org.apache.juneau.collections.JsonMap.*;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

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
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public class HtmlDocSerializerSession extends HtmlStrippedDocSerializerSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final VarResolver DEFAULT_VR = VarResolver.create().defaultVars().vars(HtmlWidgetVar.class).build();

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(HtmlDocSerializer ctx) {
		return new Builder(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends HtmlStrippedDocSerializerSession.Builder {

		HtmlDocSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(HtmlDocSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public HtmlDocSerializerSession build() {
			return new HtmlDocSerializerSession(this);
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder resolver(VarResolverSession value) {
			super.resolver(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder useWhitespace(Boolean value) {
			super.useWhitespace(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected HtmlDocSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		addVarBean(HtmlWidgetMap.class, ctx.getWidgets());
	}

	private final HtmlDocSerializer ctx;

	@Override /* SerializerSession */
	protected VarResolverSession createDefaultVarResolverSession() {
		return DEFAULT_VR.createSession();
	}

	/**
	 * Returns the {@link HtmlDocSerializer.Builder#navlinks(String...)} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializer.Builder#navlinks(String...)} setting value in this context.
	 * 	<jk>null</jk> if not specified.
	 * 	Never an empty map.
	 */
	public final String[] getNavLinks() {
		return ctx.navlinks;
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {

		try (HtmlWriter w = getHtmlWriter(out)) {
			try {
				getTemplate().writeTo(this, w, o);
			} catch (Exception e) {
				throw new SerializeException(e);
			}
		}
	}

	/**
	 * Calls the parent {@link #doSerialize(SerializerPipe, Object)} method which invokes just the HTML serializer.
	 *
	 * @param out
	 * 	Where to send the output from the serializer.
	 * @param o The object being serialized.
	 * @throws Exception Error occurred during serialization.
	 */
	public void parentSerialize(Object out, Object o) throws Exception {
		try (SerializerPipe pipe = createPipe(out)) {
			super.doSerialize(pipe, o);
		}
	}
	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Aside section contents.
	 *
	 * @see HtmlDocSerializer.Builder#aside(String...)
	 * @return
	 * 	The overridden contents of the aside section on the HTML page.
	 */
	protected final String[] getAside() {
		return ctx.aside;
	}

	/**
	 * Aside section contents float.
	 *
	 * @see HtmlDocSerializer.Builder#asideFloat(AsideFloat)
	 * @return
	 * 	The location of where to place the aside section.
	 */
	protected final AsideFloat getAsideFloat() {
		return ctx.asideFloat;
	}

	/**
	 * Footer section contents.
	 *
	 * @see HtmlDocSerializer.Builder#footer(String...)
	 * @return
	 * 	The overridden contents of the footer section on the HTML page.
	 */
	protected final String[] getFooter() {
		return ctx.footer;
	}

	/**
	 * Additional head section content.
	 *
	 * @see HtmlDocSerializer.Builder#head(String...)
	 * @return
	 * 	HTML content to add to the head section of the HTML page.
	 */
	protected final String[] getHead() {
		return ctx.head;
	}

	/**
	 * Header section contents.
	 *
	 * @see HtmlDocSerializer.Builder#header(String...)
	 * @return
	 * 	The overridden contents of the header section on the HTML page.
	 */
	protected final String[] getHeader() {
		return ctx.header;
	}

	/**
	 * Nav section contents.
	 *
	 * @see HtmlDocSerializer.Builder#nav(String...)
	 * @return
	 * 	The overridden contents of the nav section on the HTML page.
	 */
	protected final String[] getNav() {
		return ctx.nav;
	}

	/**
	 * Page navigation links.
	 *
	 * @see HtmlDocSerializer.Builder#navlinks(String...)
	 * @return
	 * 	Navigation links to add to the HTML page.
	 */
	protected final String[] getNavlinks() {
		return ctx.navlinks;
	}

	/**
	 * No-results message.
	 *
	 * @see HtmlDocSerializer.Builder#noResultsMessage(String)
	 * @return
	 * 	The message used when serializing an empty array or empty list.
	 */
	protected final String getNoResultsMessage() {
		return ctx.getNoResultsMessage();
	}

	/**
	 * Prevent word wrap on page.
	 *
	 * @see HtmlDocSerializer.Builder#nowrap()
	 * @return
	 * 	<jk>true</jk> if <js>"* {white-space:nowrap}"</js> should be added to the CSS instructions on the page to prevent word wrapping.
	 */
	protected final boolean isNowrap() {
		return ctx.nowrap;
	}

	/**
	 * Javascript code.
	 *
	 * @see HtmlDocSerializer.Builder#script(String...)
	 * @return
	 * 	Arbitrary Javascript to add to the HTML page.
	 */
	protected final String[] getScript() {
		return ctx.script;
	}

	/**
	 * CSS style code.
	 *
	 * @see HtmlDocSerializer.Builder#style(String...)
	 * @return
	 * 	The CSS instructions to add to the HTML page.
	 */
	protected final String[] getStyle() {
		return ctx.style;
	}

	/**
	 * Stylesheet import URLs.
	 *
	 * @see HtmlDocSerializer.Builder#stylesheet(String...)
	 * @return
	 * 	The link to the stylesheet of the HTML page.
	 */
	protected final String[] getStylesheet() {
		return ctx.stylesheet;
	}

	/**
	 * HTML document template.
	 *
	 * @see HtmlDocSerializer.Builder#template(Class)
	 * @return
	 * 	The template to use for serializing the page.
	 */
	protected final HtmlDocTemplate getTemplate() {
		return ctx.getTemplate();
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

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* ContextSession */
	protected JsonMap properties() {
		return filteredMap("ctx", ctx, "varResolver", getVarResolver());
	}
}
