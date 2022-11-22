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

import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;

/**
 * Metadata on classes specific to the HTML serializers and parsers pulled from the {@link Html @Html} annotation on
 * the class.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public class HtmlClassMeta extends ExtendedClassMeta {

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

		Value<Boolean> noTables = Value.empty(), noTableHeaders = Value.empty();
		Value<HtmlFormat> format = Value.empty();
		Value<HtmlRender<?>> render = Value.empty();

		Consumer<Html> c = x -> {
			if (x.noTables())
				noTables.set(true);
			if (x.noTableHeaders())
				noTableHeaders.set(true);
			if (x.format() != HtmlFormat.HTML)
				format.set(x.format());
			if (x.render() != HtmlRender.class) {
				try {
					render.set(x.render().getDeclaredConstructor().newInstance());
				} catch (Exception e) {
					throw asRuntimeException(e);
				}
			}
		};
		cm.forEachAnnotation(Html.class, x -> true, c);

		this.noTables = noTables.orElse(false);
		this.noTableHeaders = noTableHeaders.orElse(false);
		this.render = render.orElse(null);
		this.format = format.orElse(HtmlFormat.HTML);
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
