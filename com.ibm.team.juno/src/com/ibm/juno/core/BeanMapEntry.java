/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

import java.util.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;

/**
 * Represents a single entry in a bean map.
 * <p>
 * 	This class can be used to get and set property values on a bean, or to get metadata on a property.
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Construct a new bean</jc>
 * 	Person p = <jk>new</jk> Person();
 *
 * 	<jc>// Wrap it in a bean map</jc>
 * 	BeanMap&lt;Person&gt; b = BeanContext.<jsf>DEFAULT</jsf>.forBean(p);
 *
 * 	<jc>// Get a reference to the birthDate property</jc>
 * 	BeanMapEntry birthDate = b.getProperty(<js>"birthDate"</js>);
 *
 * 	<jc>// Set the property value</jc>
 * 	birthDate.setValue(<jk>new</jk> Date(1, 2, 3, 4, 5, 6));
 *
 * 	<jc>// Or if the DateFilter.DEFAULT_ISO8601DT is registered with the bean context, set a filtered value</jc>
 * 	birthDate.setFilteredValue(<js>"'1901-03-03T04:05:06-5000'"</js>);
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 *
 * @param <T> The bean type.
 */
public class BeanMapEntry<T> implements Map.Entry<String,Object> {
	private final BeanMap<T> beanMap;
	private final BeanPropertyMeta<T> meta;

	/**
	 * Constructor.
	 *
	 * @param beanMap The bean map that this entry belongs to.
	 * @param property The bean property.
	 */
	protected BeanMapEntry(BeanMap<T> beanMap, BeanPropertyMeta<T> property) {
		this.beanMap = beanMap;
		this.meta = property;
	}

	@Override /* Map.Entry */
	public String getKey() {
		return meta.getName();
	}

	/**
	 * Returns the value of this property.
	 * <p>
	 * If there is a {@link PojoFilter} associated with this bean property or bean property type class, then
	 * 	this method will return the filtered value.
	 * For example, if the bean property type class is a {@link Date} and the bean property has the
	 * 	{@link com.ibm.juno.core.filters.DateFilter.ISO8601DT} filter associated with it through the
	 * 	{@link BeanProperty#filter() @BeanProperty.filter()} annotation, this method will return a String
	 * 	containing an ISO8601 date-time string value.
	 */
	@Override /* Map.Entry */
	public Object getValue() {
		return meta.get(this.beanMap);
	}

	/**
	 * Sets the value of this property.
	 * <p>
	 * If the property is an array of type {@code X}, then the value can be a {@code Collection<X>} or {@code X[]} or {@code Object[]}.
	 * <p>
	 * If the property is a bean type {@code X}, then the value can either be an {@code X} or a {@code Map}.
	 * <p>
	 * If there is a {@link PojoFilter} associated with this bean property or bean property type class, then
	 * 	you must pass in a filtered value.
	 * For example, if the bean property type class is a {@link Date} and the bean property has the
	 * 	{@link com.ibm.juno.core.filters.DateFilter.ISO8601DT} filter associated with it through the
	 * 	{@link BeanProperty#filter() @BeanProperty.filter()} annotation, the value being passed in must be
	 * 	a String containing an ISO8601 date-time string value.
	 *
	 * @return  The set value after it's been converted.
	 */
	@Override /* Map.Entry */
	public Object setValue(Object value) {
		return meta.set(this.beanMap, value);
	}

	/**
	 * Returns the bean map that contains this property.
	 *
	 * @return The bean map that contains this property.
	 */
	public BeanMap<T> getBeanMap() {
		return this.beanMap;
	}

	/**
	 * Returns the metadata about this bean property.
	 *
	 * @return Metadata about this bean property.
	 */
	public BeanPropertyMeta<T> getMeta() {
		return this.meta;
	}

	@Override /* Object */
	public String toString() {
		return this.getKey() + "=" + this.getValue();
	}
}