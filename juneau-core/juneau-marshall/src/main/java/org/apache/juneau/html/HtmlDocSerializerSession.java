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

import java.io.IOException;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Context object that lives for the duration of a single serialization of {@link HtmlSerializer} and its subclasses.
 *
 * <p>
 * See {@link Serializer} for details.
 *
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public class HtmlDocSerializerSession extends HtmlStrippedDocSerializerSession {

	private static final VarResolver DEFAULT_VR = VarResolver.create().defaultVars().vars(HtmlWidgetVar.class).build();

	private final HtmlDocSerializer ctx;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 */
	protected HtmlDocSerializerSession(HtmlDocSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
		this.ctx = ctx;
		addVarBean(HtmlWidgetMap.class, ctx.getWidgets());
	}

	@Override /* SerializerSession */
	protected VarResolverSession createDefaultVarResolverSession() {
		return DEFAULT_VR.createSession();
	}

	/**
	 * Returns the {@link HtmlDocSerializerBuilder#navlinks(String...)} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializerBuilder#navlinks(String...)} setting value in this context.
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
	 * Configuration property:  Aside section contents.
	 *
	 * @see HtmlDocSerializerBuilder#aside(String...)
	 * @return
	 * 	The overridden contents of the aside section on the HTML page.
	 */
	protected final String[] getAside() {
		return ctx.aside;
	}

	/**
	 * Configuration property:  Aside section contents float.
	 *
	 * @see HtmlDocSerializerBuilder#asideFloat(AsideFloat)
	 * @return
	 * 	The location of where to place the aside section.
	 */
	protected final AsideFloat getAsideFloat() {
		return ctx.asideFloat;
	}

	/**
	 * Configuration property:  Footer section contents.
	 *
	 * @see HtmlDocSerializerBuilder#footer(String...)
	 * @return
	 * 	The overridden contents of the footer section on the HTML page.
	 */
	protected final String[] getFooter() {
		return ctx.footer;
	}

	/**
	 * Configuration property:  Additional head section content.
	 *
	 * @see HtmlDocSerializerBuilder#head(String...)
	 * @return
	 * 	HTML content to add to the head section of the HTML page.
	 */
	protected final String[] getHead() {
		return ctx.head;
	}

	/**
	 * Configuration property:  Header section contents.
	 *
	 * @see HtmlDocSerializerBuilder#header(String...)
	 * @return
	 * 	The overridden contents of the header section on the HTML page.
	 */
	protected final String[] getHeader() {
		return ctx.header;
	}

	/**
	 * Configuration property:  Nav section contents.
	 *
	 * @see HtmlDocSerializerBuilder#nav(String...)
	 * @return
	 * 	The overridden contents of the nav section on the HTML page.
	 */
	protected final String[] getNav() {
		return ctx.nav;
	}

	/**
	 * Configuration property:  Page navigation links.
	 *
	 * @see HtmlDocSerializerBuilder#navlinks(String...)
	 * @return
	 * 	Navigation links to add to the HTML page.
	 */
	protected final String[] getNavlinks() {
		return ctx.navlinks;
	}

	/**
	 * Configuration property:  No-results message.
	 *
	 * @see HtmlDocSerializerBuilder#noResultsMessage(String)
	 * @return
	 * 	The message used when serializing an empty array or empty list.
	 */
	protected final String getNoResultsMessage() {
		return ctx.getNoResultsMessage();
	}

	/**
	 * Configuration property:  Prevent word wrap on page.
	 *
	 * @see HtmlDocSerializerBuilder#nowrap()
	 * @return
	 * 	<jk>true</jk> if <js>"* {white-space:nowrap}"</js> should be added to the CSS instructions on the page to prevent word wrapping.
	 */
	protected final boolean isNowrap() {
		return ctx.nowrap;
	}

	/**
	 * Configuration property:  Javascript code.
	 *
	 * @see HtmlDocSerializerBuilder#script(String...)
	 * @return
	 * 	Arbitrary Javascript to add to the HTML page.
	 */
	protected final String[] getScript() {
		return ctx.script;
	}

	/**
	 * Configuration property:  CSS style code.
	 *
	 * @see HtmlDocSerializerBuilder#style(String...)
	 * @return
	 * 	The CSS instructions to add to the HTML page.
	 */
	protected final String[] getStyle() {
		return ctx.style;
	}

	/**
	 * Configuration property:  Stylesheet import URLs.
	 *
	 * @see HtmlDocSerializerBuilder#stylesheet(String...)
	 * @return
	 * 	The link to the stylesheet of the HTML page.
	 */
	protected final String[] getStylesheet() {
		return ctx.stylesheet;
	}

	/**
	 * Configuration property:  HTML document template.
	 *
	 * @see HtmlDocSerializerBuilder#template(Class)
	 * @return
	 * 	The template to use for serializing the page.
	 */
	protected final HtmlDocTemplate getTemplate() {
		return ctx.getTemplate();
	}

	/**
	 * Configuration property:  Page widgets.
	 *
	 * @see HtmlDocSerializerBuilder#widgets(Class...)
	 * @return
	 * 	Navigation links to add to the HTML page.
	 */
	protected final Collection<HtmlWidget> getWidgets() {
		return ctx.getWidgets().values();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* ContextSession */
	public OMap toMap() {
		return super.toMap()
			.a(
				"HtmlDocSerializerSession",
				OMap
					.create()
					.filtered()
					.a("ctx", ctx)
					.a("varResolver", getVarResolver())
			);
	}
}
