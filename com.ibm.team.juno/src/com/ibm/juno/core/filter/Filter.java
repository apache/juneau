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
 * Parent class for all bean and POJO filters.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Filters are used to alter how POJOs are handled by bean contexts (and subsequently serializers and parsers).
 * 	The are a very powerful feature of the Juno framework that allows virtually any POJO to be serialized and parsed.
 * 	For example, they can be used to...
 * <ul>
 * 	<li>Convert a non-serializable POJO into a serializable POJO during serialization (and optionally vis-versa during parsing).
 * 	<li>Control various aspects of beans, such as what properties are visible, bean subclasses, etc...
 * </ul>
 * <p>
 * 	There are 2 subclasses of filters:
 * <ul>
 * 	<li>{@link PojoFilter} - Non-bean filters for converting POJOs into serializable equivalents.
 * 	<li>{@link BeanFilter} - Bean filters for configuring how beans are handled.
 * </ul>
 * <p>
 * 	Filters are associated with bean contexts (and serializers/parsers) through the {@link BeanContextFactory#addFilters(Class[])}
 * 		and {@link CoreApi#addFilters(Class[])} methods.
 *
 *
 * <h6 class='topic'>Additional information</h6>
 * 	See {@link com.ibm.juno.core.filter} for more information.
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class Filter {

	/** Represents no filter. */
	public static class NULL extends Filter {}

	/** The filter subtype */
	public static enum FilterType {
		/** PojoFilter */
		POJO,
		/** BeanFilter */
		BEAN
	}

	/** The class that this filter applies to. */
	protected Class<?> forClass;

	/** The bean context that this filter instance belongs to. */
	protected BeanContext beanContext;

	/** Whether this is a BeanFilter or PojoFilter. */
	protected FilterType type = FilterType.POJO;

	Filter() {}

	Filter(Class<?> forClass) {
		this.forClass = forClass;
	}


	/**
	 * Returns the class that this filter applies to.
	 *
	 * @return The class that this filter applies to.
	 */
	public Class<?> forClass() {
		return forClass;
	}

	/**
	 * Returns the implementation class.
	 * Useful for debugging when calling {@link BeanContext#toString()}.
	 *
	 * @return The implementation class of this filter.
	 */
	public Class<?> getImplClass() {
		return this.getClass();
	}

	/**
	 * Returns whether this is an instance of {@link PojoFilter} or {@link BeanFilter}.
	 *
	 * @return The filter type.
	 */
	public FilterType getType() {
		return type;
	}

	/**
	 * Returns the {@link BeanContext} that created this filter.
	 *
	 * @return The bean context that created this filter.
	 */
	protected BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Sets the bean context that this filter instance was created by.
	 *
	 * @param beanContext The bean context that created this filter.
	 * @return This object (for method chaining).
	 */
	public Filter setBeanContext(BeanContext beanContext) {
		this.beanContext = beanContext;
		return this;
	}

	@Override /* Object */
	public int hashCode() {
		return getClass().getName().hashCode() + forClass().getName().hashCode();
	}

	/**
	 * Checks if the specified filter class is the same as this one.
	 *
	 * @param f The filter to check.
	 * @return <jk>true</jk> if the specified filter is equivalent to this one.
	 */
	public boolean isSameAs(Filter f) {
		return f.getClass().equals(getClass()) && f.forClass().equals(forClass());
	}
}
