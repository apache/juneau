/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.bean;

import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.reflect.*;

/**
 * Java bean wrapper class.
 *
 * <h5 class='topic'>Description</h5>
 *
 * A wrapper that wraps Java bean instances inside of a {@link Map} interface that allows properties on the wrapped
 * object can be accessed using the {@link Map#get(Object) get()} and {@link Map#put(Object,Object) put()} methods.
 *
 * <p>
 * Create instances through the bean API.  For the bean-modeling-only path use {@link BeanMap#of(Object)} or
 * {@link BeanMap#of(Object, BeanMeta)}; the marshalling layer's {@code MarshallingSession#toBeanMap(Object)}
 * additionally wires a {@link BeanSession} into the resulting map for session-aware behavior.
 *
 * <h5 class='topic'>Bean property order</h5>
 *
 * The order of the properties returned by the {@link Map#keySet() keySet()} and {@link Map#entrySet() entrySet()}
 * methods is as follows:
 * <ul class='spaced-list'>
 * 	<li>
 * 		If the {@link BeanType @BeanType} annotation specifies an explicit property list via
 * 		{@link BeanType#properties() properties()} (or its synonym {@link BeanType#p() p()}),
 * 		the order matches the annotation.
 * 	<li>
 * 		Otherwise, if {@link BeanType#unsorted() @BeanType(unsorted=true)} is set on the class, or
 * 		{@link BeanConfigContext#isUnsortedProperties()} is enabled on the session, properties are returned
 * 		in their natural reflection order.
 * 	<li>
 * 		Otherwise, properties are returned in alphabetical order.
 * </ul>
 *
 * <h5 class='topic'>POJO swaps</h5>
 *
 * If {@code ObjectSwap} transforms are defined on the class types of the properties of this bean or the bean properties
 * themselves, the {@link #get(Object)} and {@link #put(String, Object)} methods will automatically transform the
 * property value to and from the serialized form.
 *
 * <h5 class='topic'>Thread safety</h5>
 *
 * Instances are not thread-safe.
 * This type is mutable (wrapped bean reference, property caches, and optional session pointer) and does not perform
 * internal synchronization.
 *
 *
 * @param <T> Specifies the type of object that this map encapsulates.
 */
@SuppressWarnings({
	"java:S3776" // Cognitive complexity acceptable for bean property iteration and filtering
})
public class BeanMap<T> extends AbstractMap<String,Object> implements Delegate<T> {

	/**
	 * Convenience method for wrapping a bean inside a {@link BeanMap}.
	 *
	 * @param <T> The bean type.
	 * @param bean The bean being wrapped.
	 * @return A new {@link BeanMap} instance wrapping the bean.
	 */
	@SuppressWarnings("unchecked")
	public static <T> BeanMap<T> of(T bean) {
		return new BeanMap<>(bean, BeanMeta.of((Class<T>) bean.getClass()));
	}

	/**
	 * Convenience method for wrapping a bean inside a {@link BeanMap} using a pre-built {@link BeanMeta}.
	 *
	 * <p>
	 * This is the bean-modeling entry point — it builds a {@link BeanMap} without going through a
	 * marshalling session, paired with a {@link BeanMeta} typically produced by
	 * {@link BeanMeta#of(Class, BeanConfigContext)}.  No marshalling session is attached.
	 *
	 * @param <T> The bean type.
	 * @param bean The bean being wrapped.
	 * @param meta The bean metadata.  Must not be <jk>null</jk>.
	 * @return A new {@link BeanMap} instance wrapping the bean.
	 */
	public static <T> BeanMap<T> of(T bean, BeanMeta<T> meta) {
		return new BeanMap<>(bean, meta);
	}

	/** The wrapped object. */
	protected T bean;

	/** Temporary holding cache for beans with read-only properties.  Normally null. */
	protected Map<String,Object> propertyCache;

	/** Temporary holding cache for bean properties of array types when the add() method is being used. */
	protected Map<String,List<?>> arrayPropertyCache;

	/** The BeanMeta associated with the class of the object. */
	protected BeanMeta<T> meta;
	private BeanSession session;

	private final String typePropertyName;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Bean-modeling-only constructor. Does not carry a {@link BeanSession} reference.
	 * The marshalling layer wires the session in via {@link #setBeanSession(BeanSession)}
	 * immediately after construction.
	 *
	 * @param bean The bean to wrap inside this map.
	 * @param meta The metadata associated with the bean class.
	 */
	public BeanMap(T bean, BeanMeta<T> meta) {
		this.bean = bean;
		this.meta = meta;
		if (ne(meta.getConstructorArgs()))
			propertyCache = new TreeMap<>();
		this.typePropertyName = meta.getTypePropertyName();
	}

	/**
	 * Wires this bean map to a {@link BeanSession}.
	 *
	 * <p>
	 * Called by marshalling-side construction paths (for example,
	 * {@code MarshallingSession#toBeanMap(Object)}) to wire in the session used by session-aware
	 * operations (type conversion, child collection construction, and parser/serializer-backed
	 * conversions) after bean-modeling construction.
	 *
	 * @param value The bean session that produced this bean map.  Typically a marshalling-session implementation.
	 */
	public void setBeanSession(BeanSession value) {
		session = value;
	}

	/**
	 * Add a value to a collection or array property.
	 *
	 * <p>
	 * As a general rule, adding to arrays is not recommended since the array must be recreate each time this method is
	 * called.
	 *
	 * @param property Property name or child-element name (if {@code @Xml(childName)} is specified).
	 * @param value The value to add to the collection or array.
	 */
	public void add(String property, Object value) {
		var p = getPropertyMeta(property);
		if (p == null) {
			if (meta.getConfig().isIgnoreUnknownBeanProperties())
				return;
			throw bex(meta.getClassInfo(), "Bean property ''{0}'' not found.", property);
		}
		p.add(this, property, value);
	}

	@Override /* Overridden from Map */
	public boolean containsKey(Object property) {
		// JUNEAU-248: Match the behavior of keySet() - only check properties map, not hiddenProperties
		var key = emptyIfNull(property);
		if (meta.getProperties().containsKey(key) && ! "*".equals(key))
			return true;
		if (nn(meta.getDynaProperty())) {
			try {
				return meta.getDynaProperty().getDynaMap(bean).containsKey(key);
			} catch (Exception e) {
				throw bex(e);
			}
		}
		return false;
	}

	/**
	 * Returns all the properties associated with the bean.
	 *
	 * @return A new set.
	 */
	@Override
	public Set<Entry<String,Object>> entrySet() {

		// If this bean has a dyna-property, then we need to construct the entire set before returning.
		// Otherwise, we can create an iterator without a new data structure.
		if (nn(meta.getDynaProperty())) {
			Set<Entry<String,Object>> s = set();
			forEachProperty(x -> true, x -> {
				if (x.isDyna()) {
					try {
						x.getDynaMap(bean).entrySet().forEach(y -> s.add(new BeanMapEntry(this, x, y.getKey())));
					} catch (Exception e) {
						throw bex(e);
					}
				} else {
					s.add(new BeanMapEntry(this, x, x.getName()));
				}
			});
			return s;
		}

		// Construct our own anonymous set to implement this function.
		return new AbstractSet<>() {

			// Get the list of properties from the meta object.
			// Note that the HashMap.values() method caches results, so this collection
			// will really only be constructed once per bean type since the underlying
			// map never changes.
			final Collection<BeanPropertyMeta> pSet = getProperties();

			@Override /* Overridden from Set */
			public Iterator<java.util.Map.Entry<String,Object>> iterator() {

				// Construct our own anonymous iterator that uses iterators against the meta.getProperties()
				// map to maintain position.  This prevents us from having to construct any of our own
				// collection objects.
				return new Iterator<>() {

					final Iterator<BeanPropertyMeta> pIterator = pSet.iterator();

					@Override /* Overridden from Iterator */
					public boolean hasNext() {
						return pIterator.hasNext();
					}

					@Override /* Overridden from Iterator */
					public Map.Entry<String,Object> next() {
						return new BeanMapEntry(BeanMap.this, pIterator.next(), null);
					}

					@Override /* Overridden from Iterator */
					public void remove() {
						throw unsupportedOp("Cannot remove item from iterator.");
					}
				};
			}

			@Override /* Overridden from Set */
			public int size() {
				return pSet.size();
			}
		};
	}

	/**
	 * Performs an action on each property in this bean map.
	 *
	 * @param filter The filter to apply to properties.
	 * @param action The action.
	 * @return This object.
	 */
	public BeanMap<T> forEachProperty(Predicate<BeanPropertyMeta> filter, Consumer<BeanPropertyMeta> action) {
		meta.getProperties().values().stream().filter(filter).forEach(action);
		return this;
	}

	/**
	 * Invokes all the getters on this bean and consumes the results.
	 *
	 * @param valueFilter Filter to apply to value before applying action.
	 * @param action The action to perform.
	 * @return The list of all bean property values.
	 */
	@SuppressWarnings("java:S3776") // Cognitive complexity acceptable for bean property filtering with predicate
	public BeanMap<T> forEachValue(Predicate<Object> valueFilter, BeanPropertyConsumer action) {

		// Normal bean.
		if (meta.getDynaProperty() == null) {
			forEachProperty(BeanPropertyMeta::canRead, bpm -> {
				try {
					var val = bpm.get(this, null);
					if (valueFilter.test(val))
						action.apply(bpm, bpm.getName(), val, null);
				} catch (Exception t) {
					action.apply(bpm, bpm.getName(), null, t);
				}
			});

			// Bean with dyna properties.
		} else {
			Map<String,BeanPropertyValue> actions = (meta.isUnsortedProperties() ? map() : sortedMap());

			forEachProperty(x -> ! x.isDyna(), bpm -> {
				try {
					actions.put(bpm.getName(), new BeanPropertyValue(bpm, bpm.getName(), bpm.get(this, null), null));
				} catch (Exception t) {
					actions.put(bpm.getName(), new BeanPropertyValue(bpm, bpm.getName(), null, t));
				}
			});

			forEachProperty(BeanPropertyMeta::isDyna, bpm -> {
				Map<String,Object> dynaMap;
				try {
					dynaMap = bpm.getDynaMap(bean);
				} catch (Exception e) {
					actions.put(bpm.getName(), new BeanPropertyValue(bpm, bpm.getName(), null, e));
					return;
				}
				if (nn(dynaMap)) {
					dynaMap.forEach((k, v) -> {
						try {
							var val = bpm.get(this, k);
							actions.put(k, new BeanPropertyValue(bpm, k, val, null));
						} catch (Exception e) {
							actions.put(k, new BeanPropertyValue(bpm, k, null, e));
						}
					});
				}
			});

			actions.forEach((k, v) -> {
				if (v.getThrown() != null || valueFilter.test(v.getValue()))
					action.apply(v.getMeta(), v.getName(), v.getValue(), v.getThrown());
			});
		}

		return this;
	}

	/**
	 * Gets a property on the bean.
	 *
	 * <p>
	 * If an {@code ObjectSwap}-style transform is associated with this bean property or bean property type class, then this method
	 * will return the transformed value.
	 * For example, if the bean property type class is a {@link Date} and the bean property has the
	 * {@code TemporalDateSwap.IsoInstant} swap associated with it through the
	 * {@code @Swap(value)} annotation, this method will return a String containing an
	 * ISO8601 date-time string value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Construct a bean with a 'birthDate' Date field</jc>
	 * 	Person <jv>person</jv> = <jk>new</jk> Person();
	 * 	<jv>person</jv>.setBirthDate(<jk>new</jk> Date(1, 2, 3, 4, 5, 6));
	 *
	 * 	<jc>// Create a marshalling context and add the ISO8601 date-time swap</jc>
	 * 	MarshallingContext <jv>marshallingContext</jv> = MarshallingContext.<jsm>create</jsm>().swaps(TemporalDateSwap.IsoInstant.<jk>class</jk>).build();
	 *
	 * 	<jc>// Wrap our bean in a bean map</jc>
	 * 	BeanMap&lt;Person&gt; <jv>beanMap</jv> = <jv>marshallingContext</jv>.toBeanMap(<jv>person</jv>);
	 *
	 * 	<jc>// Get the field as a string (i.e. "'1901-03-03T04:05:06-5000'")</jc>
	 * 	String <jv>birthDate</jv> = <jv>beanMap</jv>.get(<js>"birthDate"</js>);
	 * </p>
	 *
	 * @param property The name of the property to get.
	 * @return The property value.
	 * @throws RuntimeException if any of the following occur.
	 * 	<ol>
	 * 		<li>BeanMapEntry does not exist on the underlying object.
	 * 		<li>Security settings prevent access to the underlying object getter method.
	 * 		<li>An exception occurred inside the getter method.
	 * 	</ol>
	 */
	@Override /* Overridden from Map */
	public Object get(Object property) {
		var pName = s(property);
		var p = getPropertyMeta(pName);
		if (p == null)
			return meta.onReadProperty(this.bean, pName, null);
		return p.get(this, pName);
	}

	/**
	 * Same as {@link #get(Object)} but casts the value to the specific type.
	 *
	 * @param <T2> The type to cast to.
	 * @param property The name of the property to get.
	 * @param c The type to cast to.
	 * @return The property value.
	 * @throws RuntimeException if any of the following occur.
	 * 	<ol>
	 * 		<li>BeanMapEntry does not exist on the underlying object.
	 * 		<li>Security settings prevent access to the underlying object getter method.
	 * 		<li>An exception occurred inside the getter method.
	 * 	</ol>
	 * @throws ClassCastException if property is not the specified type.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast to T2 for property retrieval
	})
	public <T2> T2 get(String property, Class<T2> c) {
		var pName = s(property);
		var p = getPropertyMeta(pName);
		if (p == null)
			return (T2)meta.onReadProperty(this.bean, pName, null);
		return (T2)p.get(this, pName);
	}

	/**
	 * Returns the wrapped bean object.
	 *
	 * <p>
	 * Triggers bean creation if bean has read-only properties set through a constructor defined by the
	 * {@link BeanCtor @BeanCtor} annotation.
	 *
	 * <p>
	 * The post-creation Optional&lt;X&gt; initialization step (which seeds null {@link Optional} properties with
	 * {@link BeanInfo#getOptionalDefault()}) is skipped for properties built via the bean-modeling-only path
	 * ({@link BeanMeta#of(Class, BeanConfigContext)}) because per-property type metadata is unavailable;
	 * those properties are left untouched and any {@link Optional}-typed field stays at its constructor-assigned
	 * value.
	 *
	 * @return The inner bean object.
	 */
	public T getBean() {
		T b = getBean(true);

		// If we have any arrays that need to be constructed, do it now.
		if (nn(arrayPropertyCache)) {
			arrayPropertyCache.forEach((k, v) -> {
				try {
					getPropertyMeta(k).setArray(b, v);
				} catch (Exception e1) {
					throw toRex(e1);
				}
			});
			arrayPropertyCache = null;
		}

		// Initialize any null Optional<X> fields.  Skip properties whose ClassMeta is unavailable
		// (bean-modeling-only path — Optional handling is a marshalling concern that requires type metadata).
		meta.getProperties().forEach((k,v) -> {
			var cm = v.getBeanInfo();
			if (nn(cm) && cm.isOptional() && v.get(this, k) == null)
				v.set(this, k, cm.getOptionalDefault());
		});

		// Do the same for hidden fields.
		meta.getHiddenProperties().forEach((k, v) -> {
			var cm = v.getBeanInfo();
			if (nn(cm) && cm.isOptional() && v.get(this, k) == null)
				v.set(this, k, cm.getOptionalDefault());
		});

		return b;
	}

	/**
	 * Returns the wrapped bean object.
	 *
	 * <p>
	 * If <c>create</c> is <jk>false</jk>, then this method may return <jk>null</jk> if the bean has read-only
	 * properties set through a constructor defined by the {@link BeanCtor @BeanCtor} annotation.
	 *
	 * <p>
	 * This method does NOT always return the bean in it's final state.
	 * Array properties temporary stored as ArrayLists are not finalized until the {@link #getBean()} method is called.
	 *
	 * @param create If bean hasn't been instantiated yet, then instantiate it.
	 * @return The inner bean object.
	 */
	public T getBean(boolean create) {
		/** If this is a read-only bean, then we need to create it. */
		if (bean == null && create && ne(meta.getConstructorArgs())) {
			var props = meta.getConstructorArgs();
			var c = meta.getConstructor();
			var args = new Object[props.size()];
			for (var i = 0; i < props.size(); i++) {
				var propName = props.get(i);
				var rawVal = propertyCache.remove(propName);
				// Convert value to expected property type if it's not already the correct type.
				// Uses the same logic as MarshallingSession.convertToMemberType's early-return check:
				// collections/maps with specific element/value types always need conversion.
				if (rawVal != null) {
					var pm = getPropertyMeta(propName);
					if (pm != null) {
						var cm = pm.getBeanInfo();
						var needsConversion = !cm.inner().isInstance(rawVal)
							|| (cm.isCollection() && !cm.getElementType().isObject())
							|| (cm.isMap() && !cm.getValueType().is(Object.class));
						if (needsConversion)
							rawVal = session.convertToType(rawVal, cm);
					}
				}
				args[i] = rawVal;
			}
			try {
				bean = c.<T>newInstance(args);
				propertyCache.forEach(this::put);
				propertyCache = null;
			} catch (IllegalArgumentException e) {
				throw bex(e, meta.getBeanInfo().inner(), "IllegalArgumentException occurred on call to class constructor ''{0}'' with argument types ''{1}''", c.getNameSimple(),
					Arrays.toString(getClasses(args)));
			} catch (Exception e) {
				throw bex(e);
			}
		}
		return bean;
	}

	/**
	 * Returns the bean session that created this bean map.
	 *
	 * <p>
	 * The returned value is the bean-modeling SPI seam.  Marshalling-side callers needing the concrete
	 * marshalling-session implementation can cast — every session wired in via
	 * {@link #setBeanSession(BeanSession)} on the marshalling-side path is currently a
	 * {@code MarshallingSession}.
	 *
	 * @return The bean session that created this bean map.
	 */
	public final BeanSession getBeanSession() { return session; }

	/**
	 * Returns the {@link BeanInfo} of the wrapped bean.
	 *
	 * @return The class type of the wrapped bean.
	 */
	@Override /* Overridden from Delegate */
	public BeanInfo<T> getBeanInfo() { return this.meta.getBeanInfo(); }

	/**
	 * Returns the metadata associated with this bean map.
	 *
	 * @return The metadata associated with this bean map.
	 */
	public BeanMeta<T> getMeta() { return meta; }

	/**
	 * Extracts the specified field values from this bean and returns it as a simple Map.
	 *
	 * <p>
	 * The returned map is a <i>live</i> view over this bean map: each entry's {@code getValue} reads through to
	 * {@code BeanMap.get} and {@code setValue} writes through to {@code BeanMap.put}.  Unknown {@code fields}
	 * (keys not present in this bean map) are silently skipped.
	 *
	 * @param fields The fields to extract.
	 * @return
	 * 	A new map with fields as key-value pairs.
	 * 	<br>Note that modifying the values in this map will also modify the underlying bean.
	 */
	public Map<String,Object> getProperties(String...fields) {
		Map<String,Object> thisMap = this;
		var entries = new ArrayList<Map.Entry<String,Object>>(fields.length);
		for (var k : fields) {
			if (thisMap.containsKey(k)) {
				entries.add(new Map.Entry<>() {
					@Override public String getKey() { return k; }
					@Override public Object getValue() { return thisMap.get(k); }
					@Override public Object setValue(Object v) { return thisMap.put(k, v); }
				});
			}
		}
		return new AbstractMap<>() {
			@Override public Set<Map.Entry<String,Object>> entrySet() {
				return new AbstractSet<>() {
					@Override public Iterator<Map.Entry<String,Object>> iterator() { return entries.iterator(); }
					@Override public int size() { return entries.size(); }
				};
			}
		};
	}

	/**
	 * Returns the specified property on this bean map.
	 *
	 * <p>
	 * Allows you to get and set an individual property on a bean without having a handle to the bean itself by using
	 * the {@link BeanMapEntry#getValue()} and {@link BeanMapEntry#setValue(Object)} methods.
	 *
	 * <p>
	 * This method can also be used to get metadata on a property by calling the {@link BeanMapEntry#getMeta()} method.
	 *
	 * @param propertyName The name of the property to look up.
	 * @return The bean property, or null if the bean has no such property.
	 */
	public BeanMapEntry getProperty(String propertyName) {
		var p = getPropertyMeta(propertyName);
		if (p == null)
			return null;
		return new BeanMapEntry(this, p, propertyName);
	}

	/**
	 * Returns the metadata on the specified property.
	 *
	 * @param propertyName The name of the bean property.
	 * @return Metadata on the specified property, or <jk>null</jk> if that property does not exist.
	 */
	public BeanPropertyMeta getPropertyMeta(String propertyName) {
		return meta.getPropertyMeta(propertyName);
	}

	/**
	 * Same as {@link #get(Object)} except bypasses any {@code ObjectSwap} associated with the bean property or
	 * {@link BeanFilter} associated with the bean class.
	 *
	 * @param property The name of the property to get.
	 * @return The raw property value.
	 */
	public Object getRaw(Object property) {
		var pName = s(property);
		var p = getPropertyMeta(pName);
		if (p == null)
			return null;
		return p.getRaw(this, pName);
	}

	/**
	 * Returns the names of all properties associated with the bean.
	 *
	 * <p>
	 * The returned set is unmodifiable.
	 */
	@Override /* Overridden from Map */
	public Set<String> keySet() {
		if (meta.getDynaProperty() == null)
			return meta.getProperties().keySet();
		Set<String> l = set();
		meta.getProperties().forEach((k, v) -> {
			if (! "*".equals(k))
				l.add(k);
		});
		try {
			l.addAll(meta.getDynaProperty().getDynaMap(bean).keySet());
		} catch (Exception e) {
			throw new BeanRuntimeException(e);
		}
		return l;
	}

	/**
	 * Convenience method for loading this map with the contents of the specified map.
	 *
	 * <p>
	 * Identical to {@link #putAll(Map)} except as a fluent-style method.
	 *
	 * @param entries The map containing the entries to add to this map.
	 * @return This object.
	 */
	@SuppressWarnings({
		"unchecked", // Type erasure requires unchecked casts for dynamic bean property access
		"rawtypes", // Raw types necessary for generic type handling
	})
	public BeanMap<T> load(Map entries) {
		putAll(entries);
		return this;
	}

	/**
	 * Sets a property on the bean.
	 *
	 * <p>
	 * If there is a {@code ObjectSwap} associated with this bean property or bean property type class, then you must pass in a transformed value.
	 * For example, if the bean property type class is a {@link Date} and the bean property has the
	 * {@code TemporalDateSwap.IsoInstant} swap associated with it through the
	 * {@code @Swap(value)} annotation, the value being passed in must be
	 * a String containing an ISO8601 date-time string value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Construct a bean with a 'birthDate' Date field</jc>
	 * 	Person <jv>person</jv> = <jk>new</jk> Person();
	 *
	 * 	<jc>// Create a marshalling context and add the ISO8601 date-time swap</jc>
	 * 	MarshallingContext <jv>marshallingContext</jv> = MarshallingContext.<jsm>create</jsm>().swaps(TemporalDateSwap.IsoInstant.<jk>class</jk>).build();
	 *
	 * 	<jc>// Wrap our bean in a bean map</jc>
	 * 	BeanMap&lt;Person&gt; <jv>beanMap</jv> = <jv>marshallingContext</jv>.toBeanMap(<jv>person</jv>);
	 *
	 * 	<jc>// Set the field</jc>
	 * 	<jv>beanMap</jv>.put(<js>"birthDate"</js>, <js>"'1901-03-03T04:05:06-5000'"</js>);
	 * </p>
	 *
	 * @param property The name of the property to set.
	 * @param value The value to set the property to.
	 * @return
	 * 	If the {@code beanMapPutReturnsOldValue} setting is <jk>true</jk> on the session's bean configuration,
	 * 	then the old value of the property is returned.
	 * 	Otherwise, this method always returns <jk>null</jk>.
	 * @throws
	 * 	RuntimeException if any of the following occur.
	 * 	<ul>
	 * 		<li>BeanMapEntry does not exist on the underlying object.
	 * 		<li>Security settings prevent access to the underlying object setter method.
	 * 		<li>An exception occurred inside the setter method.
	 * 	</ul>
	 */
	@Override /* Overridden from Map */
	public Object put(String property, Object value) {
		var p = getPropertyMeta(property);
		if (p == null) {
			if (meta.getConfig().isIgnoreUnknownBeanProperties() || property.equals(typePropertyName))
				return meta.onWriteProperty(bean, property, null);

			p = getPropertyMeta("*");
			if (p == null)
				throw bex(meta.getClassInfo(), "Bean property ''{0}'' not found.", property);
		}
		return p.set(this, property, value);
	}

	/**
	 * Given a string containing variables of the form <c>"{property}"</c>, replaces those variables with property
	 * values in this bean.
	 *
	 * @param s The string containing variables.
	 * @return A new string with variables replaced, or the same string if no variables were found.
	 */
	public String resolveVars(String s) {
		return formatNamed(s, this);
	}

	/**
	 * Returns a simple collection of properties for this bean map.
	 *
	 * @return A simple collection of properties for this bean map.
	 */
	protected Collection<BeanPropertyMeta> getProperties() { return meta.getProperties().values(); }

	@SuppressWarnings({
		"unchecked" // Type erasure requires cast to T for bean assignment
	})
	void setBean(Object bean) { this.bean = (T)bean; }

	/**
	 * Compares the specified object with this map for equality.
	 *
	 * <p>
	 * Returns <jk>true</jk> if the given object is also a map and the two maps represent the same
	 * mappings. More formally, two maps <c>m1</c> and <c>m2</c> represent the same mappings if
	 * <c>m1.entrySet().equals(m2.entrySet())</c>.
	 *
	 * <p>
	 * This implementation compares the entry sets of the two maps.
	 *
	 * @param o Object to be compared for equality with this map.
	 * @return <jk>true</jk> if the specified object is equal to this map.
	 */
	@Override
	public boolean equals(Object o) {
		return o instanceof Map<?,?> o2 && eq(this, o2, (x, y) -> x.entrySet().equals(y.entrySet()));
	}

	/**
	 * Returns the hash code value for this map.
	 *
	 * <p>
	 * The hash code of a map is defined to be the sum of the hash codes of each entry in the map's
	 * <c>entrySet()</c> view. This ensures that <c>m1.equals(m2)</c> implies that
	 * <c>m1.hashCode()==m2.hashCode()</c> for any two maps <c>m1</c> and <c>m2</c>, as required
	 * by the general contract of {@link Object#hashCode()}.
	 *
	 * <p>
	 * This implementation computes the hash code from the entry set.
	 *
	 * @return The hash code value for this map.
	 */
	@Override
	public int hashCode() {
		return entrySet().hashCode();
	}
}