/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.html;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.internal.*; 

/**
 * Metadata on classes specific to the HTML serializers and parsers pulled from the {@link Html @Html} annotation on the class.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class HtmlClassMeta extends ClassMetaExtended {

	private final Html html;
	private final boolean asXml, noTables, noTableHeaders, asPlainText;

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
		} else {
			asXml = false;
			noTables = false;
			noTableHeaders = false;
			asPlainText = false;
		}
	}

	/**
	 * Returns the {@link Html} annotation defined on the class.
	 *
	 * @return The value of the {@link Html} annotation, or <jk>null</jk> if not specified.
	 */
	protected Html getAnnotation() {
		return html;
	}

	/**
	 * Returns the {@link Html#asXml()} annotation defined on the class.
	 *
	 * @return The value of the {@link Html#asXml()} annotation.
	 */
	protected boolean isAsXml() {
		return asXml;
	}

	/**
	 * Returns the {@link Html#asPlainText()} annotation defined on the class.
	 *
	 * @return The value of the {@link Html#asPlainText()} annotation.
	 */
	protected boolean isAsPlainText() {
		return asPlainText;
	}

	/**
	 * Returns the {@link Html#noTables()} annotation defined on the class.
	 *
	 * @return The value of the {@link Html#noTables()} annotation.
	 */
	protected boolean isNoTables() {
		return noTables;
	}

	/**
	 * Returns the {@link Html#noTableHeaders()} annotation defined on the class.
	 *
	 * @return The value of the {@link Html#noTableHeaders()} annotation.
	 */
	public boolean isNoTableHeaders() {
		return noTableHeaders;
	}
}
