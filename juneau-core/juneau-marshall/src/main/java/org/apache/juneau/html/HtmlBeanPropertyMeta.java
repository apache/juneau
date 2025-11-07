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
package org.apache.juneau.html;

import static org.apache.juneau.common.utils.Utils.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.html.annotation.*;

/**
 * Metadata on bean properties specific to the HTML serializers and parsers pulled from the {@link Html @Html}
 * annotation on the bean property.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>

 * </ul>
 */
@SuppressWarnings("rawtypes")
public class HtmlBeanPropertyMeta extends ExtendedBeanPropertyMeta {

	static class Builder {
		boolean noTables, noTableHeaders;
		HtmlFormat format = HtmlFormat.HTML;
		BeanCreator<HtmlRender> render = BeanCreator.of(HtmlRender.class);
		String link, anchorText, style;

		void findHtmlInfo(Html html) {
			if (html == null)
				return;
			format = html.format();
			if (html.noTables())
				noTables = html.noTables();
			if (html.noTableHeaders())
				noTableHeaders = html.noTableHeaders();
			if (html.render() != HtmlRender.class)
				render.type(html.render());
			if (! html.link().isEmpty())
				link = html.link();
			if (! html.anchorText().isEmpty())
				anchorText = html.anchorText();
			if (! html.style().isEmpty())
				style = html.style();
		}
	}

	/**
	 * Default instance.
	 */
	public static final HtmlBeanPropertyMeta DEFAULT = new HtmlBeanPropertyMeta();
	private final boolean noTables, noTableHeaders;
	private final HtmlFormat format;
	private final HtmlRender render;

	private final String link, anchorText, style;

	/**
	 * Constructor.
	 *
	 * @param bpm The metadata of the bean property of this additional metadata.
	 * @param mp HTML metadata provider (for finding information about other artifacts).
	 */
	public HtmlBeanPropertyMeta(BeanPropertyMeta bpm, HtmlMetaProvider mp) {
		super(bpm);

		Builder b = new Builder();
		if (nn(bpm.getInnerField()))
			mp.getAnnotationProvider().find(Html.class, bpm.getInnerField()).map(x -> x.inner()).filter(x -> true).forEach(x -> b.findHtmlInfo(x));
		if (nn(bpm.getGetter()))
			mp.getAnnotationProvider().find(Html.class, bpm.getGetter()).map(x -> x.inner()).filter(x -> true).forEach(x -> b.findHtmlInfo(x));
		if (nn(bpm.getSetter()))
			mp.getAnnotationProvider().find(Html.class, bpm.getSetter()).map(x -> x.inner()).filter(x -> true).forEach(x -> b.findHtmlInfo(x));

		this.format = b.format;
		this.noTables = b.noTables;
		this.noTableHeaders = b.noTableHeaders;
		this.render = b.render.orElse(null);
		this.link = b.link;
		this.anchorText = b.anchorText;
		this.style = b.style;
	}

	private HtmlBeanPropertyMeta() {
		super(null);
		this.format = HtmlFormat.HTML;
		this.noTables = false;
		this.noTableHeaders = false;
		this.render = null;
		this.link = null;
		this.anchorText = null;
		this.style = null;
	}

	/**
	 * Specifies the anchor text for this property.
	 *
	 * <p>
	 * This value is specified via the {@link Html#anchorText() @Html(anchorText)} annotation.
	 *
	 * @return The link string, or <jk>null</jk> if not specified.
	 */
	public String getAnchorText() { return anchorText; }

	/**
	 * Adds a hyperlink to this value in HTML.
	 *
	 * <p>
	 * This value is specified via the {@link Html#link() @Html(link)} annotation.
	 *
	 * @return The link string, or <jk>null</jk> if not specified.
	 */
	public String getLink() { return link; }

	/**
	 * Returns the render class for rendering the style and contents of this property value in HTML.
	 *
	 * <p>
	 * This value is specified via the {@link Html#render() @Html(render)} annotation.
	 *
	 * @return The render class, never <jk>null</jk>.
	 */
	public HtmlRender getRender() { return render; }

	/**
	 * Returns the CSS style string for this property.
	 *
	 * <p>
	 * This value is specified via the {@link Html#style() @Html(style)} annotation.
	 *
	 * @return The CSS style string, or <jk>null</jk> if not specified.
	 */
	public String getStyle() { return style; }

	/**
	 * Returns whether this bean property should not include table headers when serialized as an HTML table.
	 *
	 * @return
	 * 	<jk>true</jk> if the the {@link Html @Html} annotation is specified, and {@link Html#noTableHeaders() @Html(noTableHeaders)} is
	 * 	<jk>true</jk>.
	 */
	public boolean isNoTableHeaders() { return noTableHeaders; }

	/**
	 * Returns the format of this bean property
	 *
	 * @return The value of the {@link Html#format()} annotation.
	 */
	protected HtmlFormat getFormat() { return format; }

	/**
	 * Returns <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#HTML}.
	 *
	 * @return <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#HTML}.
	 */
	protected boolean isHtml() { return format == HtmlFormat.HTML; }

	/**
	 * Returns <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#HTML_CDC}.
	 *
	 * @return <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#HTML_CDC}.
	 */
	protected boolean isHtmlCdc() { return format == HtmlFormat.HTML_CDC; }

	/**
	 * Returns <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#HTML_SDC}.
	 *
	 * @return <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#HTML_SDC}.
	 */
	protected boolean isHtmlSdc() { return format == HtmlFormat.HTML_SDC; }

	/**
	 * Returns whether this bean property should not be serialized as an HTML table.
	 *
	 * @return
	 * 	<jk>true</jk> if the the {@link Html @Html} annotation is specified, and {@link Html#noTables() @Html(noTables)} is
	 * 	<jk>true</jk>.
	 */
	protected boolean isNoTables() { return noTables; }

	/**
	 * Returns <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#PLAIN_TEXT}.
	 *
	 * @return <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#PLAIN_TEXT}.
	 */
	protected boolean isPlainText() { return format == HtmlFormat.PLAIN_TEXT; }

	/**
	 * Returns <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#XML}.
	 *
	 * @return <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#XML}.
	 */
	protected boolean isXml() { return format == HtmlFormat.XML; }
}