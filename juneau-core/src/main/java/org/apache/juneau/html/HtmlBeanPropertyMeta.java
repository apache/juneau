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
 * Metadata on bean properties specific to the HTML serializers and parsers pulled from the {@link Html @Html} annotation on the bean property.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class HtmlBeanPropertyMeta extends BeanPropertyMetaExtended {

	private boolean asXml, noTables, noTableHeaders, asPlainText;

	/**
	 * Constructor.
	 *
	 * @param bpm The metadata of the bean property of this additional metadata.
	 */
	public HtmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		super(bpm);
		if (bpm.getField() != null)
			findHtmlInfo(bpm.getField().getAnnotation(Html.class));
		if (bpm.getGetter() != null)
			findHtmlInfo(bpm.getGetter().getAnnotation(Html.class));
		if (bpm.getSetter() != null)
			findHtmlInfo(bpm.getSetter().getAnnotation(Html.class));
	}

	private void findHtmlInfo(Html html) {
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
	 * @return <jk>true</jk> if the the {@link Html} annotation is specified, and {@link Html#asPlainText()} is <jk>true</jk>.
	 */
	protected boolean isAsPlainText() {
		return asPlainText;
	}

	/**
	 * Returns whether this bean property should not be serialized as an HTML table.
	 *
	 * @return <jk>true</jk> if the the {@link Html} annotation is specified, and {@link Html#noTables()} is <jk>true</jk>.
	 */
	protected boolean isNoTables() {
		return noTables;
	}

	/**
	 * Returns whether this bean property should not include table headers when serialized as an HTML table.
	 *
	 * @return <jk>true</jk> if the the {@link Html} annotation is specified, and {@link Html#noTableHeaders()} is <jk>true</jk>.
	 */
	public boolean isNoTableHeaders() {
		return noTableHeaders;
	}
}
