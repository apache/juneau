/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.labels;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;

/**
 * Simple serializable bean description.
 * <p>
 * 	Given a particular class type, this serializes the class into
 * 	the fully-qualified class name and the properties associated with the class.
 * <p>
 * 	Useful for rendering simple information about a bean during REST OPTIONS requests.
 *
 * @param <T> The class type of the bean.
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Bean(properties={"type","properties"})
public final class BeanDescription<T> {

	/** The bean class type. */
	public String type;

	/** The bean properties. */
	public BeanPropertyDescription[] properties;

	/**
	 * Constructor
	 * @param c The bean class type.
	 */
	public BeanDescription(Class<T> c) {
		type = c.getName();
		BeanMeta<T> bm = BeanContext.DEFAULT.getBeanMeta(c);
		properties = new BeanPropertyDescription[bm.getPropertyMetas().size()];
		int i = 0;
		for (BeanPropertyMeta<T> pm : bm.getPropertyMetas())
			properties[i++] = new BeanPropertyDescription(pm.getName(), pm.getClassMeta());
	}

	/**
	 * Information about a bean property.
	 */
	public static class BeanPropertyDescription {

		/** The bean property name. */
		public String name;

		/** The bean property filtered class type. */
		public String type;

		/**
		 * Constructor.
		 *
		 * @param name The bean property name.
		 * @param type The bean property class type.
		 */
		public BeanPropertyDescription(String name, ClassMeta<?> type) {
			this.name = name;
			this.type = type.getFilteredClassMeta().toString();
		}
	}
}
