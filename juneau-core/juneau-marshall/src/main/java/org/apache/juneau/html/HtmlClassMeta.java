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

import static org.apache.juneau.internal.ClassUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;

/**
 * Metadata on classes specific to the HTML serializers and parsers pulled from the {@link Html @Html} annotation on
 * the class.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.HtmlDetails}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class HtmlClassMeta extends ExtendedClassMeta {

	private final List<Html> htmls;
	private final boolean noTables, noTableHeaders;
	private final HtmlFormat format;
	private final HtmlRender<?> render;

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.
	 * @param mp HTML metadata provider (for finding information about other artifacts).
	 */
	public HtmlClassMeta(ClassMeta<?> cm, HtmlMetaProvider mp) {
		super(cm);
		this.htmls = cm.getAnnotations(Html.class);

		boolean _noTables = false, _noTableHeaders = false;
		HtmlRender<?> _render = null;
		HtmlFormat _format = HtmlFormat.HTML;

		for (Html a : this.htmls) {
			_format = a.format();
			_noTables = a.noTables();
			_noTableHeaders = a.noTableHeaders();
			_render = castOrCreate(HtmlRender.class, a.render());
		}

		this.noTables = _noTables;
		this.noTableHeaders = _noTableHeaders;
		this.render = _render;
		this.format = _format;
	}

	/**
	 * Returns the {@link Html @Html} annotations defined on the class.
	 *
	 * @return An unmodifiable list of annotations ordered parent-to-child, or an empty list if not found.
	 */
	protected List<Html> getAnnotations() {
		return htmls;
	}

	/**
	 * Returns the {@link Html#format() @Html(format)} annotation defined on the class.
	 *
	 * @return The value of the annotation.
	 */
	protected HtmlFormat getFormat() {
		return format;
	}

	/**
	 * Returns <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#XML}.
	 *
	 * @return <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#XML}.
	 */
	protected boolean isXml() {
		return format == HtmlFormat.XML;
	}

	/**
	 * Returns <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#PLAIN_TEXT}.
	 *
	 * @return <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#PLAIN_TEXT}.
	 */
	protected boolean isPlainText() {
		return format == HtmlFormat.PLAIN_TEXT;
	}

	/**
	 * Returns <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#HTML}.
	 *
	 * @return <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#HTML}.
	 */
	protected boolean isHtml() {
		return format == HtmlFormat.HTML;
	}

	/**
	 * Returns <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#HTML_CDC}.
	 *
	 * @return <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#HTML_CDC}.
	 */
	protected boolean isHtmlCdc() {
		return format == HtmlFormat.HTML_CDC;
	}

	/**
	 * Returns <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#HTML_SDC}.
	 *
	 * @return <jk>true</jk> if {@link #getFormat()} returns {@link HtmlFormat#HTML_SDC}.
	 */
	protected boolean isHtmlSdc() {
		return format == HtmlFormat.HTML_SDC;
	}

	/**
	 * Returns the {@link Html#noTables() @Html(noTables)} annotation defined on the class.
	 *
	 * @return The value of the annotation.
	 */
	protected boolean isNoTables() {
		return noTables;
	}

	/**
	 * Returns the {@link Html#noTableHeaders() @Html(noTableHeaders)} annotation defined on the class.
	 *
	 * @return The value of the annotation.
	 */
	public boolean isNoTableHeaders() {
		return noTableHeaders;
	}

	/**
	 * Returns the {@link Html#render() @Html(render)} annotation defined on the class.
	 *
	 * @return The value of the annotation.
	 */
	public HtmlRender<?> getRender() {
		return render;
	}
}
