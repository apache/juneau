/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.dto;

import java.text.*;

import com.ibm.juno.core.html.*;
import com.ibm.juno.core.urlencoding.*;
import com.ibm.juno.core.utils.*;

/**
 * Simple bean that implements a hyperlink for the HTML serializer.
 * <p>
 * 	The name and url properties correspond to the following parts of a hyperlink in an HTML document...
 * <p class='bcode'>
 * 	<xt>&lt;a</xt> <xa>href</xa>=<xs>'href'</xs><xt>&gt;</xt>name<xt>&lt;/a&gt;</xt>
 * <p>
 * 	When encountered by the {@link HtmlSerializer} class, this object gets converted to a hyperlink.<br>
 * 	All other serializers simply convert it to a simple bean.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@HtmlLink(nameProperty = "name", hrefProperty = "href")
public class Link implements Comparable<Link> {
	private String name, href;

	/** No-arg constructor. */
	public Link() {}

	/**
	 * Constructor.
	 *
	 * @param name Corresponds to the text inside of the <xt>&lt;A&gt;</xt> element.
	 * @param href Corresponds to the value of the <xa>href</xa> attribute of the <xt>&lt;A&gt;</xt> element.
	 * @param hrefArgs Optional arguments for {@link MessageFormat} style arguments in the href.
	 */
	public Link(String name, String href, Object...hrefArgs) {
		setName(name);
		setHref(href, hrefArgs);
	}

	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>name</property>.
	 * Corresponds to the text inside of the <xt>&lt;A&gt;</xt> element.
	 *
	 * @return The value of the <property>name</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Link setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Bean property getter:  <property>href</property>.
	 * Corresponds to the value of the <xa>href</xa> attribute of the <xt>&lt;A&gt;</xt> element.
	 *
	 * @return The value of the <property>href</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getHref() {
		return href;
	}

	/**
	 * Bean property setter:  <property>href</property>.
	 *
	 * @param href The new value for the <property>href</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Link setHref(String href) {
		setHref(href, new Object[0]);
		return this;
	}

	/**
	 * Bean property setter:  <property>href</property>.
	 * Same as {@link #setHref(String)} except allows for {@link MessageFormat} style arguments.
	 *
	 * @param href The new href.
	 * @param args Optional message format arguments.
	 * @return This object (for method chaining).
	 */
	public Link setHref(String href, Object...args) {
		for (int i = 0; i < args.length; i++)
			args[i] = UrlEncodingSerializer.DEFAULT.serializeUrlPart(args[i]);
		this.href = (args.length > 0 ? MessageFormat.format(href, args) : href);
		return this;
	}

	/**
	 * Returns the name so that the {@link PojoQuery} class can search against it.
	 */
	@Override /* Object */
	public String toString() {
		return name;
	}

	@Override /* Comparable */
	public int compareTo(Link o) {
		return name.compareTo(o.name);
	}

	@Override /* Object */
	public boolean equals(Object o) {
		if (! (o instanceof Link))
			return false;
		return (compareTo((Link)o) == 0);
	}

	@Override /* Object */
	public int hashCode() {
		return super.hashCode();
	}
}
