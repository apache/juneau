/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.filter;

import java.util.*;

import com.ibm.juno.core.annotation.*;

/**
 * Bean filter constructed from a {@link Bean @Bean} annotation found on a class.
 * <p>
 * <b>*** Internal class - Not intended for external use ***</b>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The class type that this filter applies to.
 */
public final class AnnotationBeanFilter<T> extends BeanFilter<T> {

	/**
	 * Constructor.
	 *
	 * @param annotatedClass The class found to have a {@link Bean @Bean} annotation.
	 * @param annotations The {@link Bean @Bean} annotations found on the class and all parent classes in child-to-parent order.
	 */
	public AnnotationBeanFilter(Class<T> annotatedClass, List<Bean> annotations) {
		super(annotatedClass);

		ListIterator<Bean> li = annotations.listIterator(annotations.size());
		while (li.hasPrevious()) {
			Bean b = li.previous();

			if (b.properties().length > 0)
				setProperties(b.properties());

			if (b.excludeProperties().length > 0)
				setExcludeProperties(b.excludeProperties());

			setPropertyNamer(b.propertyNamer());

			if (b.interfaceClass() != Object.class)
				setInterfaceClass(b.interfaceClass());

			if (b.stopClass() != Object.class)
				setStopClass(b.stopClass());

			if (! b.subTypeProperty().isEmpty()) {
				setSubTypeProperty(b.subTypeProperty());

				LinkedHashMap<Class<?>,String> subTypes = new LinkedHashMap<Class<?>,String>();
				for (BeanSubType bst : b.subTypes())
					subTypes.put(bst.type(), bst.id());

				setSubTypes(subTypes);
			}
		}
	}
}
