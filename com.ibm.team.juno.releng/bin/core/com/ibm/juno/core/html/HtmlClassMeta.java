/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html;

import com.ibm.juno.core.html.annotation.*;
import com.ibm.juno.core.utils.*;

/**
 * Metadata on classes specific to the HTML serializers and parsers pulled from the {@link Html @Html} annotation on the class.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class HtmlClassMeta {

	private final Html html;
	private final boolean asXml, noTables, noTableHeaders, asPlainText;

	/**
	 * Constructor.
	 *
	 * @param c The class that this annotation is defined on.
	 */
	public HtmlClassMeta(Class<?> c) {
		this.html = ReflectionUtils.getAnnotation(Html.class, c);
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
