/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.labels;

import com.ibm.juno.core.annotation.*;

/**
 * Simple bean with {@code name} and {@code description} properties.
 * <p>
 * 	Primarily used for constructing tables with name/description columns on REST OPTIONS requests.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Bean(properties={"name","description"})
public class NameDescription {

	private Object name;
	private Object description;

	/** No-arg constructor.  Used for JUnit testing of OPTIONS pages. */
	public NameDescription() {}

	/**
	 * Constructor.
	 * @param name A name.
	 * @param description A description.
	 */
	public NameDescription(Object name, Object description) {
		this.name = name;
		this.description = description;
	}

	/**
	 * Returns the name field on this label.
	 * @return The name.
	 */
	public Object getName() {
		return name;
	}

	/**
	 * Sets the name field on this label to a new value.
	 * @param name The new name.
	 */
	public void setName(Object name) {
		this.name = name;
	}

	/**
	 * Returns the description field on this label.
	 * @return The description.
	 */
	public Object getDescription() {
		return description;
	}

	/**
	 * Sets the description field on this label to a new value.
	 * @param description The new description.
	 */
	public void setDescription(Object description) {
		this.description = description;
	}
}
