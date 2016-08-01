/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.filter;

import com.ibm.juno.core.*;


/**
 * Simple bean filter that simply identifies a class to be used as an interface
 * 	class for all child classes.
 * <p>
 * 	These objects are created when you pass in non-<code>Filter</code> classes to {@link BeanContextFactory#addFilters(Class...)},
 * 		and are equivalent to adding a <code><ja>@Bean</ja>(interfaceClass=Foo.<jk>class</jk>)</code> annotation on the <code>Foo</code> class.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The class type that this filter applies to.
 */
public class InterfaceBeanFilter<T> extends BeanFilter<T> {

	/**
	 * Constructor.
	 *
	 * @param interfaceClass The class to use as an interface on all child classes.
	 */
	public InterfaceBeanFilter(Class<T> interfaceClass) {
		super(interfaceClass);
		setInterfaceClass(interfaceClass);
	}
}
