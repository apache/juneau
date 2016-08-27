/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Java bean wrapper class.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	A wrapper that wraps Java bean instances inside of a {@link Map} interface that allows
 * 	properties on the wrapped object can be accessed using the {@link Map#get(Object) get()} and {@link Map#put(Object,Object) put()} methods.
 * <p>
 * 	Use the {@link BeanContext} class to create instances of this class.
 *
 *
 * <h6 class='topic'>Bean property order</h6>
 * <p>
 * 	The order of the properties returned by the {@link Map#keySet() keySet()} and {@link Map#entrySet() entrySet()} methods are as follows:
 * 	<ul class='spaced-list'>
 * 		<li>If {@link Bean @Bean} annotation is specified on class, then the order is the same as the list of properties in the annotation.
 * 		<li>If {@link Bean @Bean} annotation is not specified on the class, then the order is the same as that returned
 * 			by the {@link java.beans.BeanInfo} class (i.e. ordered by definition in the class).
 * 	</ul>
 * 	<br>
 * 	The order can also be overridden through the use of a {@link BeanFilter}.
 *
 *
 * <h6 class='topic'>POJO swaps</h6>
 * <p>
 * 	If {@link PojoSwap PojoSwaps} are defined on the class types of the properties of this bean or the bean properties themselves, the
 * 	{@link #get(Object)} and {@link #put(String, Object)} methods will automatically
 * 	transform the property value to and from the serialized form.
 *
 * @author Barry M. Caceres
 * @author James Bognar (james.bognar@salesforce.com)
 * @param <T> Specifies the type of object that this map encapsulates.
 */
public class BeanMap<T> extends AbstractMap<String,Object> implements Delegate<T> {

	/** The wrapped object. */
	protected T bean;

	/** Temporary holding cache for beans with read-only properties.  Normally null. */
	protected Map<String,Object> propertyCache;

	/** Temporary holding cache for bean properties of array types when the add() method is being used. */
	protected Map<String,List<?>> arrayPropertyCache;

	/** The BeanMeta associated with the class of the object. */
	protected BeanMeta<T> meta;

	/**
	 * Instance of this class are instantiated through the BeanContext class.
	 *
	 * @param bean The bean to wrap inside this map.
	 * @param meta The metadata associated with the bean class.
	 */
	protected BeanMap(T bean, BeanMeta<T> meta) {
		this.bean = bean;
		this.meta = meta;
		if (meta.constructorArgs.length > 0)
			propertyCache = new TreeMap<String,Object>();
	}

	/**
	 * Returns the metadata associated with this bean map.
	 *
	 * @return The metadata associated with this bean map.
	 */
	public BeanMeta<T> getMeta() {
		return meta;
	}

	/**
	 * Returns the wrapped bean object.
	 * Triggers bean creation if bean has read-only properties set through a constructor
	 * 	defined by the {@link BeanConstructor} annotation.
	 *
	 * @return The inner bean object.
	 */
	public T getBean() {
		T b = getBean(true);

		// If we have any arrays that need to be constructed, do it now.
		if (arrayPropertyCache != null) {
			for (Map.Entry<String,List<?>> e : arrayPropertyCache.entrySet()) {
				String key = e.getKey();
				List<?> value = e.getValue();
				BeanPropertyMeta bpm = getPropertyMeta(key);
				try {
					bpm.setArray(b, value);
				} catch (Exception e1) {
					throw new RuntimeException(e1);
				}
			}
			arrayPropertyCache = null;
		}
		return b;
	}

	/**
	 * Returns the wrapped bean object.
	 * <p>
	 * If <code>create</code> is <jk>false</jk>, then this method may return <jk>null</jk>
	 * 	if the bean has read-only properties set through a constructor
	 * 	defined by the {@link BeanConstructor} annotation.
	 * <p>
	 * This method does NOT always return the bean in it's final state.
	 * 	Array properties temporary stored as ArrayLists are not finalized
	 * 	until the {@link #getBean()} method is called.
	 *
	 * @param create If bean hasn't been instantiated yet, then instantiate it.
	 * @return The inner bean object.
	 */
	public T getBean(boolean create) {
		/** If this is a read-only bean, then we need to create it. */
		if (bean == null && create && meta.constructorArgs.length > 0) {
			String[] props = meta.constructorArgs;
			Constructor<T> c = meta.constructor;
			Object[] args = new Object[props.length];
			for (int i = 0; i < props.length; i++)
				args[i] = propertyCache.remove(props[i]);
			try {
				bean = c.newInstance(args);
				for (Map.Entry<String,Object> e : propertyCache.entrySet())
					put(e.getKey(), e.getValue());
				propertyCache = null;
			} catch (Exception e) {
				throw new BeanRuntimeException(e);
			}
		}
		return bean;
	}

	/**
	 * Returns the value of the property identified as the URI property (annotated with {@link BeanProperty#beanUri()} as <jk>true</jk>).
	 *
	 * @return The URI value, or <jk>null</jk> if no URI property exists on this bean.
	 */
	public Object getBeanUri() {
		BeanMeta<T> bm = getMeta();
		return bm.hasBeanUriProperty() ? bm.getBeanUriProperty().get(this) : null;
	}

	/**
	 * Sets the bean URI property if the bean has a URI property.
	 * Ignored otherwise.
	 *
	 * @param o The bean URI object.
	 * @return If the bean context setting {@code beanMapPutReturnsOldValue} is <jk>true</jk>, then the old value of the property is returned.
	 * 		Otherwise, this method always returns <jk>null</jk>.
	 */
	public Object putBeanUri(Object o) {
		BeanMeta<T> bm = getMeta();
		return bm.hasBeanUriProperty() ? bm.getBeanUriProperty().set(this, o) : null;
	}

	/**
	 * Sets a property on the bean.
	 * <p>
	 * If there is a {@link PojoSwap} associated with this bean property or bean property type class, then
	 * 	you must pass in a transformed value.
	 * For example, if the bean property type class is a {@link Date} and the bean property has the
	 * 	{@link org.apache.juneau.transforms.DateSwap.ISO8601DT} transform associated with it through the
	 * 	{@link BeanProperty#transform() @BeanProperty.transform()} annotation, the value being passed in must be
	 * 	a String containing an ISO8601 date-time string value.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	<jc>// Construct a bean with a 'birthDate' Date field</jc>
	 * 	Person p = <jk>new</jk> Person();
	 *
	 * 	<jc>// Create a bean context and add the ISO8601 date-time transform</jc>
	 * 	BeanContext beanContext = <jk>new</jk> BeanContext().addTransform(DateSwap.ISO8601DT.<jk>class</jk>);
	 *
	 * 	<jc>// Wrap our bean in a bean map</jc>
	 * 	BeanMap&lt;Person&gt; b = beanContext.forBean(p);
	 *
	 * 	<jc>// Set the field</jc>
	 * 	myBeanMap.put(<js>"birthDate"</js>, <js>"'1901-03-03T04:05:06-5000'"</js>);
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param property The name of the property to set.
	 * @param value The value to set the property to.
	 * @return If the bean context setting {@code beanMapPutReturnsOldValue} is <jk>true</jk>, then the old value of the property is returned.
	 * 		Otherwise, this method always returns <jk>null</jk>.
	 * @throws RuntimeException if any of the following occur.
	 * 	<ul class='spaced-list'>
	 * 		<li>BeanMapEntry does not exist on the underlying object.
	 * 		<li>Security settings prevent access to the underlying object setter method.
	 * 		<li>An exception occurred inside the setter method.
	 * 	</ul>
	 */
	@Override /* Map */
	public Object put(String property, Object value) {
		BeanPropertyMeta p = meta.properties.get(property);
		if (p == null) {
			if (meta.ctx.ignoreUnknownBeanProperties)
				return null;
			if (property.equals("<uri>") && meta.uriProperty != null)
				return meta.uriProperty.set(this, value);

			// If this bean has subtypes, and we haven't set the subtype yet,
			// store the property in a temporary cache until the bean can be instantiated.
			// This eliminates the need for requiring that the sub type attribute be provided first.
			if (meta.subTypeIdProperty != null) {
				if (propertyCache == null)
					propertyCache = new TreeMap<String,Object>();
				return propertyCache.put(property, value);
			}

			throw new BeanRuntimeException(meta.c, "Bean property ''{0}'' not found.", property);
		}
		if (meta.transform != null)
			if (meta.transform.writeProperty(this.bean, property, value))
				return null;
		return p.set(this, value);
	}

	/**
	 * Add a value to a collection or array property.
	 * <p>
	 * 	As a general rule, adding to arrays is not recommended since the array must be recreate each time
	 * 	this method is called.
	 *
	 * @param property Property name or child-element name (if {@link Xml#childName()} is specified).
	 * @param value The value to add to the collection or array.
	 */
	public void add(String property, Object value) {
		BeanPropertyMeta p = meta.properties.get(property);
		if (p == null) {
			if (meta.ctx.ignoreUnknownBeanProperties)
				return;
			throw new BeanRuntimeException(meta.c, "Bean property ''{0}'' not found.", property);
		}
		p.add(this, value);
	}


	/**
	 * Gets a property on the bean.
	 * <p>
	 * If there is a {@link PojoSwap} associated with this bean property or bean property type class, then
	 * 	this method will return the transformed value.
	 * For example, if the bean property type class is a {@link Date} and the bean property has the
	 * 	{@link org.apache.juneau.transforms.DateSwap.ISO8601DT} transform associated with it through the
	 * 	{@link BeanProperty#transform() @BeanProperty.transform()} annotation, this method will return a String
	 * 	containing an ISO8601 date-time string value.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	<jc>// Construct a bean with a 'birthDate' Date field</jc>
	 * 	Person p = <jk>new</jk> Person();
	 * 	p.setBirthDate(<jk>new</jk> Date(1, 2, 3, 4, 5, 6));
	 *
	 * 	<jc>// Create a bean context and add the ISO8601 date-time transform</jc>
	 * 	BeanContext beanContext = <jk>new</jk> BeanContext().addTransform(DateSwap.ISO8601DT.<jk>class</jk>);
	 *
	 * 	<jc>// Wrap our bean in a bean map</jc>
	 * 	BeanMap&lt;Person&gt; b = beanContext.forBean(p);
	 *
	 * 	<jc>// Get the field as a string (i.e. "'1901-03-03T04:05:06-5000'")</jc>
	 * 	String s = myBeanMap.get(<js>"birthDate"</js>);
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param property The name of the property to get.
	 * @throws RuntimeException if any of the following occur.
	 * 	<ol>
	 * 		<li>BeanMapEntry does not exist on the underlying object.
	 * 		<li>Security settings prevent access to the underlying object getter method.
	 * 		<li>An exception occurred inside the getter method.
	 * 	</ol>
	 */
	@Override /* Map */
	public Object get(Object property) {
		BeanPropertyMeta p = meta.properties.get(property);
		if (p == null)
			return null;
		if (meta.transform != null && property != null)
			return meta.transform.readProperty(this.bean, property.toString(), p.get(this));
		return p.get(this);
	}

	/**
	 * Convenience method for setting multiple property values by passing in JSON (or other) text.
	 * <p>
	 * 	Typically the input is going to be JSON, although the actual data type
	 * 	depends on the default parser specified by the {@link BeanContext#BEAN_defaultParser} property
	 * 	value on the config that created the context that created this map.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	aPersonBean.load(<js>"{name:'John Smith',age:21}"</js>)
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param input The text that will get parsed into a map and then added to this map.
	 * @return This object (for method chaining).
	 * @throws ParseException If the input contains a syntax error or is malformed.
	 */
	public BeanMap<T> load(String input) throws ParseException {
		putAll(new ObjectMap(input, this.meta.ctx.defaultParser));
		return this;
	}

	/**
	 * Convenience method for setting multiple property values by passing in a reader.
	 *
	 * @param r The text that will get parsed into a map and then added to this map.
	 * @param p The parser to use to parse the text.
	 * @return This object (for method chaining).
	 * @throws ParseException If the input contains a syntax error or is malformed.
	 * @throws IOException Thrown by <code>Reader</code>.
	 */
	public BeanMap<T> load(Reader r, ReaderParser p) throws ParseException, IOException {
		putAll(new ObjectMap(r, p));
		return this;
	}

	/**
	 * Convenience method for loading this map with the contents of the specified map.
	 * <p>
	 * Identical to {@link #putAll(Map)} except as a fluent-style method.
	 *
	 * @param entries The map containing the entries to add to this map.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public BeanMap<T> load(Map entries) {
		putAll(entries);
		return this;
	}

	/**
	 * Returns the names of all properties associated with the bean.
	 * <p>
	 * 	The returned set is unmodifiable.
	 */
	@Override /* Map */
	public Set<String> keySet() {
		return meta.properties.keySet();
	}

	/**
	 * Returns the specified property on this bean map.
	 * <p>
	 * 	Allows you to get and set an individual property on a bean without having a
	 * 	handle to the bean itself by using the {@link BeanMapEntry#getValue()}
	 * 	and {@link BeanMapEntry#setValue(Object)} methods.
	 * <p>
	 * 	This method can also be used to get metadata on a property by
	 * 	calling the {@link BeanMapEntry#getMeta()} method.
	 *
	 * @param propertyName The name of the property to look up.
	 * @return The bean property, or null if the bean has no such property.
	 */
	public BeanMapEntry getProperty(String propertyName) {
		BeanPropertyMeta p = meta.properties.get(propertyName);
		if (p == null)
			return null;
		return new BeanMapEntry(this, p);
	}

	/**
	 * Returns the metadata on the specified property.
	 *
	 * @param propertyName The name of the bean property.
	 * @return Metadata on the specified property, or <jk>null</jk> if that property does not exist.
	 */
	public BeanPropertyMeta getPropertyMeta(String propertyName) {
		return meta.properties.get(propertyName);
	}

	/**
	 * Returns the {@link ClassMeta} of the wrapped bean.
	 *
	 * @return The class type of the wrapped bean.
	 */
	@Override /* Delagate */
	public ClassMeta<T> getClassMeta() {
		return this.meta.getClassMeta();
	}

	/**
	 * Invokes all the getters on this bean and return the values as a list of {@link BeanPropertyValue} objects.
	 * <p>
	 * This allows a snapshot of all values to be grabbed from a bean in one call.
	 *
	 * @param addClassAttr Add a <jk>"_class"</jk> bean property to the returned list.
	 * @param ignoreNulls Don't return properties whose values are null.
	 * @return The list of all bean property values.
	 */
	public List<BeanPropertyValue> getValues(final boolean addClassAttr, final boolean ignoreNulls) {
		Collection<BeanPropertyMeta> properties = getProperties();
		int capacity = (ignoreNulls && properties.size() > 10) ? 10 : properties.size() + (addClassAttr ? 1 : 0);
		List<BeanPropertyValue> l = new ArrayList<BeanPropertyValue>(capacity);
		if (addClassAttr)
			l.add(new BeanPropertyValue(meta.getClassProperty(), meta.c.getName(), null));
		for (BeanPropertyMeta bpm : properties) {
			try {
				Object val = bpm.get(this);
				if (val != null || ! ignoreNulls)
					l.add(new BeanPropertyValue(bpm, val, null));
			} catch (Error e) {
				// Errors should always be uncaught.
				throw e;
			} catch (Throwable t) {
				l.add(new BeanPropertyValue(bpm, null, t));
			}
		}
		return l;
	}

	/**
	 * Returns a simple collection of properties for this bean map.
	 * @return A simple collection of properties for this bean map.
	 */
	protected Collection<BeanPropertyMeta> getProperties() {
		return meta.properties.values();
	}

	/**
	 * Returns all the properties associated with the bean.
	 * @return A new set.
	 */
	@Override
	public Set<Entry<String,Object>> entrySet() {

		// Construct our own anonymous set to implement this function.
		Set<Entry<String,Object>> s = new AbstractSet<Entry<String,Object>>() {

			// Get the list of properties from the meta object.
			// Note that the HashMap.values() method caches results, so this collection
			// will really only be constructed once per bean type since the underlying
			// map never changes.
			final Collection<BeanPropertyMeta> pSet = getProperties();

			@Override /* Set */
			public Iterator<java.util.Map.Entry<String, Object>> iterator() {

				// Construct our own anonymous iterator that uses iterators against the meta.properties
				// map to maintain position.  This prevents us from having to construct any of our own
				// collection objects.
				return new Iterator<Entry<String,Object>>() {

					final Iterator<BeanPropertyMeta> pIterator = pSet.iterator();

					@Override /* Iterator */
					public boolean hasNext() {
						return pIterator.hasNext();
					}

					@Override /* Iterator */
					public Map.Entry<String, Object> next() {
						return new BeanMapEntry(BeanMap.this, pIterator.next());
					}

					@Override /* Iterator */
					public void remove() {
						throw new UnsupportedOperationException("Cannot remove item from iterator.");
					}
				};
			}

			@Override /* Set */
			public int size() {
				return pSet.size();
			}
		};

		return s;
	}

	@SuppressWarnings("unchecked")
	void setBean(Object bean) {
		this.bean = (T)bean;
	}
}