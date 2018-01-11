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
import org.apache.juneau.internal.*;

/**
 * Metadata on classes specific to the HTML serializers and parsers pulled from the {@link Html @Html} annotation on
 * the class.
 */
public class HtmlClassMeta extends ClassMetaExtended {

	private final Html html;
	private final boolean asXml, noTables, noTableHeaders, asPlainText;
	private final HtmlRender<?> render;

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.
	 */
	public HtmlClassMeta(ClassMeta<?> cm) {
		super(cm);
		this.html = ReflectionUtils.getAnnotation(Html.class, getInnerClass());
		if (html != null) {
			asXml = html.asXml();
			noTables = html.noTables();
			noTableHeaders = html.noTableHeaders();
			asPlainText = html.asPlainText();
			render = cm.getBeanContext().newInstance(HtmlRender.class, html.render());
		} else {
			asXml = false;
			noTables = false;
			noTableHeaders = false;
			asPlainText = false;
			render = null;
		}
	}

	/**
	 * Returns the {@link Html @Html} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected Html getAnnotation() {
		return html;
	}

	/**
	 * Returns the {@link Html#asXml() @Html.asXml()} annotation defined on the class.
	 *
	 * @return The value of the annotation.
	 */
	protected boolean isAsXml() {
		return asXml;
	}

	/**
	 * Returns the {@link Html#asPlainText() @Html.asPlainText()} annotation defined on the class.
	 *
	 * @return The value of the annotation.
	 */
	protected boolean isAsPlainText() {
		return asPlainText;
	}

	/**
	 * Returns the {@link Html#noTables() @Html.noTables()} annotation defined on the class.
	 *
	 * @return The value of the annotation.
	 */
	protected boolean isNoTables() {
		return noTables;
	}

	/**
	 * Returns the {@link Html#noTableHeaders() @Html.noTableHeaders()} annotation defined on the class.
	 *
	 * @return The value of the annotation.
	 */
	public boolean isNoTableHeaders() {
		return noTableHeaders;
	}

	/**
	 * Returns the {@link Html#render() @Html.render()} annotation defined on the class.
	 *
	 * @return The value of the annotation.
	 */
	public HtmlRender<?> getRender() {
		return render;
	}
}
