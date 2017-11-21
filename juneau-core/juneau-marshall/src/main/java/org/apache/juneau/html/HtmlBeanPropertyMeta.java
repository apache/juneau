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

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;

/**
 * Metadata on bean properties specific to the HTML serializers and parsers pulled from the {@link Html @Html}
 * annotation on the bean property.
 */
@SuppressWarnings("rawtypes")
public final class HtmlBeanPropertyMeta extends BeanPropertyMetaExtended {

	private final boolean asXml, noTables, noTableHeaders, asPlainText;
	private final HtmlRender render;
	private final String link, anchorText;

	/**
	 * Constructor.
	 *
	 * @param bpm The metadata of the bean property of this additional metadata.
	 * @throws Exception If render class could not be instantiated.
	 */
	public HtmlBeanPropertyMeta(BeanPropertyMeta bpm) throws Exception {
		super(bpm);
		Builder b = new Builder();
		if (bpm.getField() != null)
			b.findHtmlInfo(bpm.getField().getAnnotation(Html.class));
		if (bpm.getGetter() != null)
			b.findHtmlInfo(bpm.getGetter().getAnnotation(Html.class));
		if (bpm.getSetter() != null)
			b.findHtmlInfo(bpm.getSetter().getAnnotation(Html.class));

		this.asXml = b.asXml;
		this.noTables = b.noTables;
		this.noTableHeaders = b.noTableHeaders;
		this.asPlainText = b.asPlainText;
		this.render = bpm.getBeanMeta().getClassMeta().getBeanContext().newInstance(HtmlRender.class, b.render);
		this.link = b.link;
		this.anchorText = b.anchorText;
	}

	static final class Builder {
		boolean asXml, noTables, noTableHeaders, asPlainText;
		Class<? extends HtmlRender> render = HtmlRender.class;
		String link, anchorText;

		void findHtmlInfo(Html html) {
			if (html == null)
				return;
			if (html.asXml())
				asXml = html.asXml();
			if (html.noTables())
				noTables = html.noTables();
			if (html.noTableHeaders())
				noTableHeaders = html.noTableHeaders();
			if (html.asPlainText())
				asPlainText = html.asPlainText();
			if (html.render() != HtmlRender.class)
				render = html.render();
			if (! html.link().isEmpty())
				link = html.link();
			if (! html.anchorText().isEmpty())
				anchorText = html.anchorText();
		}
	}

	/**
	 * Returns whether this bean property should be serialized as XML instead of HTML.
	 *
	 * @return <jk>true</jk> if the the {@link Html} annotation is specified, and {@link Html#asXml()} is <jk>true</jk>.
	 */
	protected boolean isAsXml() {
		return asXml;
	}

	/**
	 * Returns whether this bean property should be serialized as plain text instead of HTML.
	 *
	 * @return
	 * 	<jk>true</jk> if the the {@link Html} annotation is specified, and {@link Html#asPlainText()} is
	 * 	<jk>true</jk>.
	 */
	protected boolean isAsPlainText() {
		return asPlainText;
	}

	/**
	 * Returns whether this bean property should not be serialized as an HTML table.
	 *
	 * @return
	 * 	<jk>true</jk> if the the {@link Html} annotation is specified, and {@link Html#noTables()} is
	 * 	<jk>true</jk>.
	 */
	protected boolean isNoTables() {
		return noTables;
	}

	/**
	 * Returns whether this bean property should not include table headers when serialized as an HTML table.
	 *
	 * @return
	 * 	<jk>true</jk> if the the {@link Html} annotation is specified, and {@link Html#noTableHeaders()} is
	 * 	<jk>true</jk>.
	 */
	public boolean isNoTableHeaders() {
		return noTableHeaders;
	}

	/**
	 * Returns the render class for rendering the style and contents of this property value in HTML.
	 *
	 * <p>
	 * This value is specified via the {@link Html#render()} annotation.
	 *
	 * @return The render class, never <jk>null</jk>.
	 */
	public HtmlRender getRender() {
		return render;
	}

	/**
	 * Adds a hyperlink to this value in HTML.
	 *
	 * <p>
	 * This value is specified via the {@link Html#link()} annotation.
	 *
	 * @return The link string, or <jk>null</jk> if not specified.
	 */
	public String getLink() {
		return link;
	}

	/**
	 * Specifies the anchor text for this property.
	 *
	 * <p>
	 * This value is specified via the {@link Html#anchorText()} annotation.
	 *
	 * @return The link string, or <jk>null</jk> if not specified.
	 */
	public String getAnchorText() {
		return anchorText;
	}
}
