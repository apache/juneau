/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.html.dto;

import static com.ibm.juno.core.xml.annotation.XmlFormat.*;

import com.ibm.juno.core.xml.annotation.*;

/**
 * Represents an HTML IMG element.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Xml(name="img")
public class Img extends HtmlElement {

	/** <code>src</code> attribute */
	@Xml(format=ATTR)
	public String src;

	/**
	 * Constructor
	 *
	 * @param src <code>src</code> attribute
	 */
	public Img(String src) {
		this.src = src;
	}
}
