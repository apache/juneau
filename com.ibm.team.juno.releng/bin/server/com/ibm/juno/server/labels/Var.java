/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.labels;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.utils.*;

/**
 * A request or response variable.
 */
@Bean(properties={"category","name","description"})
public class Var implements Comparable<Var> {

	/** Variable category (e.g. <js>"header"</js>, <js>"content"</js>) */
	public String category;

	/** Variable name (e.g. <js>"Content-Type"</js>) */
	public String name;

	/** Variable description */
	public String description;

	/** Bean constructor */
	public Var() {}

	/**
	 *
	 * Constructor.
	 * @param category Variable category (e.g. "ATTR", "PARAM").
	 * @param name Variable name.
	 */
	public Var(String category, String name) {
		this.category = category;
		this.name = name;
	}

	@SuppressWarnings("hiding")
	boolean matches(String category, String name) {
		if (this.category.equals(category))
			if (name == null || name.equals(this.name))
				return true;
		return false;
	}

	/**
	 * Sets the description for this variable.
	 *
	 * @param description The new description.
	 * @return This object (for method chaining).
	 */
	public Var setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override /* Comparable */
	public int compareTo(Var o) {
		int i = category.compareTo(o.category);
		if (i == 0)
			i = (name == null ? -1 : name.compareTo(o.name));
		return i;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		if (o instanceof Var) {
			Var v = (Var)o;
			return (v.category.equals(category) && StringUtils.isEquals(name, v.name));
		}
		return false;
	}

	@Override /* Object */
	public int hashCode() {
		return category.hashCode() + (name == null ? 0 : name.hashCode());
	}
}