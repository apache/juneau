/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html;

import com.ibm.juno.core.*;
import com.ibm.juno.core.html.annotation.*;

/**
 * Metadata on bean properties specific to the HTML serializers and parsers pulled from the {@link Html @Html} annotation on the bean property.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The bean class.
 */
public class HtmlBeanPropertyMeta<T> {

	private boolean asXml, noTables, noTableHeaders, asPlainText;

	/**
	 * Constructor.
	 *
	 * @param beanPropertyMeta The metadata of the bean property of this additional metadata.
	 */
	public HtmlBeanPropertyMeta(BeanPropertyMeta<T> beanPropertyMeta) {
		if (beanPropertyMeta.getField() != null)
			findHtmlInfo(beanPropertyMeta.getField().getAnnotation(Html.class));
		if (beanPropertyMeta.getGetter() != null)
			findHtmlInfo(beanPropertyMeta.getGetter().getAnnotation(Html.class));
		if (beanPropertyMeta.getSetter() != null)
			findHtmlInfo(beanPropertyMeta.getSetter().getAnnotation(Html.class));
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
