/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.labels;

import com.ibm.juno.core.annotation.*;

/**
 * Simple bean for describing GET parameters.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Bean(properties={"name","dataType","description"})
public final class ParamDescription {
	private String name;
	private String dataType;
	private String description;

	/** No-arg constructor.  Used for JUnit testing of OPTIONS pages. */
	public ParamDescription() {}

	/**
	 * Constructor.
	 *
	 * @param name A name.
	 * @param dataType Typically a fully-qualified class name.
	 * @param description A description.
	 */
	public ParamDescription(String name, String dataType, String description) {
		this.name = name;
		this.dataType = dataType;
		this.description = description;
	}

	/**
	 * Returns the name field on this label.
	 *
	 * @return The name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name field on this label to a new value.
	 *
	 * @param name The new name.
	 * @return This object (for method chaining).
	 */
	public ParamDescription setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Returns the dataType field on this label.
	 *
	 * @return The dataType.
	 */
	public String getDataType() {
		return dataType;
	}

	/**
	 * Sets the dataType field on this label to a new value.
	 *
	 * @param dataType The new data type.
	 * @return This object (for method chaining).
	 */
	public ParamDescription setDataType(String dataType) {
		this.dataType = dataType;
		return this;
	}

	/**
	 * Returns the description field on this label.
	 *
	 * @return The description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description field on this label to a new value.
	 *
	 * @param description The new description.
	 * @return This object (for method chaining).
	 */
	public ParamDescription setDescription(String description) {
		this.description = description;
		return this;
	}
}
