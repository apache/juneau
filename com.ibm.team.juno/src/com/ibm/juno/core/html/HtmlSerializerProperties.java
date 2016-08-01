/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.xml.*;

/**
 * Configurable properties on the {@link HtmlSerializer} class.
 * <p>
 * 	Use the {@link HtmlSerializer#setProperty(String, Object)} method to set property values.
 * <p>
 * 	In addition to these properties, the following properties are also applicable for {@link HtmlSerializer}.
 * <ul>
 * 	<li>{@link XmlSerializerProperties}
 * 	<li>{@link SerializerProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class HtmlSerializerProperties implements Cloneable {

	/**
	 * Anchor text source ({@link String}, default={@link #TO_STRING}).
	 * <p>
	 * When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs><xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>)
	 * 	in HTML, this setting defines what to set the inner text to.
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li>{@link #TO_STRING} / <js>"toString"</js> - Set to whatever is returned by {@link #toString()} on the object.
	 * 	<li>{@link #URI} / <js>"uri"</js> - Set to the URI value.
	 * 	<li>{@link #LAST_TOKEN} / <js>"lastToken"</js> - Set to the last token of the URI value.
	 * 	<li>{@link #PROPERTY_NAME} / <js>"propertyName"</js> - Set to the bean property name.
	 * </ul>
	 */
	public static final String HTML_uriAnchorText = "HtmlSerializer.uriAnchorText";

	/** Constant for {@link HtmlSerializerProperties#HTML_uriAnchorText} property. */
	public static final String PROPERTY_NAME = "propertyName";
	/** Constant for {@link HtmlSerializerProperties#HTML_uriAnchorText} property. */
	public static final String TO_STRING = "toString";
	/** Constant for {@link HtmlSerializerProperties#HTML_uriAnchorText} property. */
	public static final String URI = "uri";
	/** Constant for {@link HtmlSerializerProperties#HTML_uriAnchorText} property. */
	public static final String LAST_TOKEN = "lastToken";

	String uriAnchorText = TO_STRING, title, description, cssUrl;
	String[] cssImports;
	ObjectMap links;
	boolean nowrap;

	/**
	 * Sets the specified property value.
	 *
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 */
	public boolean setProperty(String property, Object value) {
		if (property.equals(HTML_uriAnchorText))
			uriAnchorText = (value == null ? null : value.toString());
		else if (property.equals(HTMLDOC_title))
			title = (value == null ? null : value.toString());
		else if (property.equals(HTMLDOC_description))
			description = (value == null ? null : value.toString());
		else if (property.equals(HTMLDOC_nowrap))
			nowrap = Boolean.valueOf(value.toString());
		else if (property.equals(HTMLDOC_links))
			try {
				links = new ObjectMap(value.toString());
			} catch (ParseException e) {
				e.printStackTrace();
			}
		else if (property.equals(HTMLDOC_addLinks))
			try {
				if (links == null)
					links = new ObjectMap(value.toString());
				else
					links.putAll(new ObjectMap(value.toString()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		else if (property.equals(HTMLDOC_cssUrl))
			cssUrl = (value == null ? null : value.toString());
		else if (property.equals(HTMLDOC_cssImports))
			cssImports = StringUtils.split(value == null ? null : value.toString(), ',');
		else
			return false;
		return true;
	}

	@Override /* Cloneable */
	public HtmlSerializerProperties clone() {
		try {
			return (HtmlSerializerProperties)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
