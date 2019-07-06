// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import static java.util.Collections.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.PropertyStore.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.csv.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jso.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.json.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.msgpack.annotation.*;
import org.apache.juneau.oapi.annotation.*;
import org.apache.juneau.parser.annotation.*;
import org.apache.juneau.plaintext.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.soap.annotation.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.annotation.*;
import org.apache.juneau.urlencoding.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * A builder for {@link PropertyStore} objects.
 */
public class PropertyStoreBuilder {

	// Contains a cache of all created PropertyStore objects keyed by hashcode.
	// Used to minimize memory consumption by reusing identical PropertyStores.
	private static final Map<Integer,PropertyStore> CACHE = new ConcurrentHashMap<>();

	// Maps property suffixes (e.g. "lc") to PropertyType (e.g. LIST_CLASS)
	static final Map<String,PropertyType> SUFFIX_MAP = new ConcurrentHashMap<>();
	static {
		for (PropertyType pt : PropertyType.values())
			SUFFIX_MAP.put(pt.getSuffix(), pt);
	}

	private final Map<String,PropertyGroupBuilder> groups = new ConcurrentSkipListMap<>();

	// Previously-created property store.
	private volatile PropertyStore propertyStore;

	// Called by PropertyStore.builder()
	PropertyStoreBuilder(PropertyStore ps) {
		apply(ps);
	}

	// Called by PropertyStore.create()
	PropertyStoreBuilder() {}

	/**
	 * Creates a new {@link PropertyStore} based on the values in this builder.
	 *
	 * @return A new {@link PropertyStore} based on the values in this builder.
	 */
	public synchronized PropertyStore build() {

		// Reused the last one if we haven't change this builder.
		if (propertyStore == null)
			propertyStore = new PropertyStore(groups);

		PropertyStore ps = CACHE.get(propertyStore.hashCode());
		if (ps == null)
			CACHE.put(propertyStore.hashCode(), propertyStore);
		else if (! ps.equals(propertyStore))
			throw new RuntimeException("Property store mismatch!  This shouldn't happen.  hashCode=["+propertyStore.hashCode()+"]\n---PS#1---\n" + ps + "\n---PS#2---\n" + propertyStore);
		else
			propertyStore = ps;

		return propertyStore;
	}

	/**
	 * Copies all the values in the specified property store into this builder.
	 *
	 * @param copyFrom The property store to copy the values from.
	 * @return This object (for method chaining).
	 */
	public synchronized PropertyStoreBuilder apply(PropertyStore copyFrom) {
		propertyStore = null;

		if (copyFrom != null)
			for (Map.Entry<String,PropertyGroup> e : copyFrom.groups.entrySet()) {
				String gName = e.getKey();
				PropertyGroupBuilder g1 = this.groups.get(gName);
				PropertyGroup g2 = e.getValue();
				if (g1 == null)
					this.groups.put(gName, g2.builder());
				else
					g1.apply(g2);
			}
		return this;
	}

	/**
	 * Applies the settings in the specified annotations to this property store.
	 *
	 * @param al The list of annotations to apply.
	 * @param r The string resolver used to resolve any variables in the annotations.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public PropertyStoreBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		for (AnnotationInfo<?> ai : al) {
			try {
				ai.getConfigApply(r).apply((AnnotationInfo<Annotation>)ai, this);
			} catch (ConfigException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new ConfigException(ex, "Could not instantiate ConfigApply class {0}", ai);
			}
		}
		return this;
	}

	/**
	 * Applies any of the various <ja>@XConfig</ja> annotations on the specified class to this property store.
	 *
	 * <p>
	 * Applies any of the following annotations:
	 * <ul class='doctree'>
	 * 	<li class ='ja'>{@link BeanConfig}
	 * 	<li class ='ja'>{@link CsvConfig}
	 * 	<li class ='ja'>{@link HtmlConfig}
	 * 	<li class ='ja'>{@link HtmlDocConfig}
	 * 	<li class ='ja'>{@link JsoConfig}
	 * 	<li class ='ja'>{@link JsonConfig}
	 * 	<li class ='ja'>{@link JsonSchemaConfig}
	 * 	<li class ='ja'>{@link MsgPackConfig}
	 * 	<li class ='ja'>{@link OpenApiConfig}
	 * 	<li class ='ja'>{@link ParserConfig}
	 * 	<li class ='ja'>{@link PlainTextConfig}
	 * 	<li class ='ja'>{@link SerializerConfig}
	 * 	<li class ='ja'>{@link SoapXmlConfig}
	 * 	<li class ='ja'>{@link UonConfig}
	 * 	<li class ='ja'>{@link UrlEncodingConfig}
	 * 	<li class ='ja'>{@link XmlConfig}
	 * 	<li class ='ja'><c>RdfConfig</c>
	 * </ul>
	 *
	 * <p>
	 * Annotations are appended in the following order:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param fromClass The class on which the annotations are defined.
	 * @return This object (for method chaining).
	 */
	public PropertyStoreBuilder applyAnnotations(Class<?> fromClass) {
		applyAnnotations(ClassInfo.of(fromClass).getAnnotationListParentFirst(ConfigAnnotationFilter.INSTANCE), VarResolver.DEFAULT.createSession());
		return this;
	}

	/**
	 * Applies any of the various <ja>@XConfig</ja> annotations on the specified method to this property store.
	 *
	 * <p>
	 * Applies any of the following annotations:
	 * <ul class='doctree'>
	 * 	<li class ='ja'>{@link BeanConfig}
	 * 	<li class ='ja'>{@link CsvConfig}
	 * 	<li class ='ja'>{@link HtmlConfig}
	 * 	<li class ='ja'>{@link HtmlDocConfig}
	 * 	<li class ='ja'>{@link JsoConfig}
	 * 	<li class ='ja'>{@link JsonConfig}
	 * 	<li class ='ja'>{@link JsonSchemaConfig}
	 * 	<li class ='ja'>{@link MsgPackConfig}
	 * 	<li class ='ja'>{@link OpenApiConfig}
	 * 	<li class ='ja'>{@link ParserConfig}
	 * 	<li class ='ja'>{@link PlainTextConfig}
	 * 	<li class ='ja'>{@link SerializerConfig}
	 * 	<li class ='ja'>{@link SoapXmlConfig}
	 * 	<li class ='ja'>{@link UonConfig}
	 * 	<li class ='ja'>{@link UrlEncodingConfig}
	 * 	<li class ='ja'>{@link XmlConfig}
	 * 	<li class ='ja'><c>RdfConfig</c>
	 * </ul>
	 *
	 * <p>
	 * Annotations are appended in the following orders:
	 * <ol>
	 * 	<li>On the package of the method class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On the method class.
	 * 	<li>On this method and matching methods ordered parent-to-child.
	 * </ol>
	 *
	 * @param fromMethod The method on which the annotations are defined.
	 * @return This object (for method chaining).
	 */
	public PropertyStoreBuilder applyAnnotations(Method fromMethod) {
		applyAnnotations(MethodInfo.of(fromMethod).getAnnotationListParentFirst(ConfigAnnotationFilter.INSTANCE), VarResolver.DEFAULT.createSession());
		return this;
	}

	/**
	 * Sets a configuration property value on this object.
	 *
	 * @param key
	 * 	The configuration property key.
	 * 	<br>(e.g <js>"BeanContext.foo.ss/add.1"</js>)
	 * 	<br>If name ends with <l>/add</l>, then the specified value is added to the existing property value as an entry
	 * 	in a SET or LIST property.
	 * 	<br>If name ends with <l>/add.{key}</l>, then the specified value is added to the existing property value as a
	 * 	key/value pair in a MAP property.
	 * 	<br>If name ends with <l>/remove</l>, then the specified value is removed from the existing property property
	 * 	value in a SET or LIST property.
	 * @param value
	 * 	The new value.
	 * 	If <jk>null</jk>, the property value is deleted.
	 * 	In general, the value type can be anything.
	 * @return This object (for method chaining).
	 */
	public synchronized PropertyStoreBuilder set(String key, Object value) {
		propertyStore = null;

		String g = group(key);

		int i = key.indexOf('/');
		if (i != -1) {
			String command = key.substring(i+1), arg = null;
			String key2 = key.substring(0, i);
			int j = command.indexOf('.');
			if (j != -1) {
				arg = command.substring(j+1);
				command = command.substring(0, j);
			}

			if ("add".equals(command)) {
				return addTo(key2, arg, value);
			} else if ("remove".equals(command)) {
				if (arg != null)
					throw new ConfigException("Invalid key specified: ''{0}''", key);
				return removeFrom(key2, value);
			} else {
				throw new ConfigException("Invalid key specified: ''{0}''", key);
			}
		}

		String n = g.isEmpty() ? key : key.substring(g.length()+1);

		PropertyGroupBuilder gb = groups.get(g);
		if (gb == null) {
			gb = new PropertyGroupBuilder();
			groups.put(g, gb);
		}

		gb.set(n, value);

		if (gb.isEmpty())
			groups.remove(g);

		return this;
	}

	/**
	 * Removes the property with the specified key.
	 *
	 * <p>
	 * This is equivalent to calling <code>set(key, <jk>null</jk>);</code>
	 *
	 * @param key The property key.
	 * @return This object (for method chaining).
	 */
	public synchronized PropertyStoreBuilder remove(String key) {
		propertyStore = null;
		return set(key, null);
	}

	/**
	 * Convenience method for setting multiple properties in one call.
	 *
	 * <p>
	 * This replaces any previous configuration properties set on this store.
	 *
	 * @param newProperties The new properties to set.
	 * @return This object (for method chaining).
	 */
	public synchronized PropertyStoreBuilder set(Map<String,Object> newProperties) {
		propertyStore = null;
		clear();
		add(newProperties);
		return this;
	}

	/**
	 * Convenience method for setting multiple properties in one call.
	 *
	 * <p>
	 * This appends to any previous configuration properties set on this store.
	 *
	 * @param newProperties The new properties to set.
	 * @return This object (for method chaining).
	 */
	public synchronized PropertyStoreBuilder add(Map<String,Object> newProperties) {
		propertyStore = null;

		if (newProperties != null)
			for (Map.Entry<String,Object> e : newProperties.entrySet())
				set(e.getKey(), e.getValue());

		return this;
	}

	/**
	 * Adds one or more values to a SET, LIST, or MAP property.
	 *
	 * @param key The property key.
	 * @param arg
	 * 	The argument.
	 * 	<br>For SETs, this must always be <jk>null</jk>.
	 * 	<br>For LISTs, this can be <jk>null</jk> or a numeric index.
	 * 		Out-of-range indexes are simply 'adjusted' to the beginning or the end of the list.
	 * 		So, for example, a value of <js>"-100"</js> will always just cause the entry to be added to the beginning
	 * 		of the list.
	 * 	<br>For MAPs, this can be <jk>null</jk> if we're adding a map, or a string key if we're adding an entry.
	 * @param value
	 * 	The new value to add to the property.
	 * 	<br>For SETs and LISTs, this can be a single value, Collection, array, or JSON array string.
	 * 	<br>For MAPs, this can be a single value, Map, or JSON object string.
	 * 	<br><jk>null</jk> values have the effect of removing entries.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET/LIST/MAP property, or the argument is invalid.
	 */
	public synchronized PropertyStoreBuilder addTo(String key, String arg, Object value) {
		propertyStore = null;
		String g = group(key);
		String n = g.isEmpty() ? key : key.substring(g.length()+1);

		PropertyGroupBuilder gb = groups.get(g);
		if (gb == null) {
			gb = new PropertyGroupBuilder();
			groups.put(g, gb);
		}

		gb.addTo(n, arg, value);

		if (gb.isEmpty())
			groups.remove(g);

		return this;
	}

	/**
	 * Adds a value to a SET, LIST, or MAP property.
	 *
	 * <p>
	 * Shortcut for calling <code>addTo(key, <jk>null</jk>, value);</code>.
	 *
	 * @param key The property key.
	 * @param value
	 * 	The new value to add to the property.
	 * 	<br>For SETs and LISTs, this can be a single value, Collection, array, or JSON array string.
	 * 	<br>for MAPs, this can be a single value, Map, or JSON object string.
	 * 	<br><jk>null</jk> values have the effect of removing entries.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET/LIST/MAP property, or the argument is invalid.
	 */
	public synchronized PropertyStoreBuilder addTo(String key, Object value) {
		propertyStore = null;
		return addTo(key, null, value);
	}

	/**
	 * Removes a value from a SET or LIST property.
	 *
	 * @param key The property key.
	 * @param value The property value in the property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET or LIST property.
	 */
	public synchronized PropertyStoreBuilder removeFrom(String key, Object value) {
		propertyStore = null;
		String g = group(key);
		String n = g.isEmpty() ? key : key.substring(g.length()+1);

		PropertyGroupBuilder gb = groups.get(g);

		// Create property group anyway to generate a good error message.
		if (gb == null)
			gb = new PropertyGroupBuilder();

		gb.removeFrom(n, value);

		if (gb.isEmpty())
			groups.remove(g);

		return this;
	}

	/**
	 * Peeks at a property value.
	 *
	 * <p>
	 * Used for debugging purposes.
	 *
	 * @param key The property key.
	 * @return The property value, or <jk>null</jk> if it doesn't exist.
	 */
	public Object peek(String key) {
		String g = group(key);
		String n = g.isEmpty() ? key : key.substring(g.length()+1);

		PropertyGroupBuilder gb = groups.get(g);

		// Create property group anyway to generate a good error message.
		if (gb == null)
			return null;

		MutableProperty bp = gb.properties.get(n);
		if (bp == null)
			return null;

		return bp.peek();
	}

	/**
	 * Same as {@link #peek(String)} but converts the value to the specified type.
	 *
	 * @param <T> The type to convert to.
	 * @param c The type to convert to.
	 * @param key The property key.
	 * @return The property value, or <jk>null</jk> if it doesn't exist.
	 */
	public <T> T peek(Class<T> c, String key)  {
		Object o = peek(key);
		if (o == null)
			return null;
		return BeanContext.DEFAULT.createBeanSession().convertToType(o, c);
	}

	/**
	 * Clears all entries in this property store.
	 */
	public synchronized void clear() {
		propertyStore = null;
		this.groups.clear();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// PropertyGroupBuilder
	//-------------------------------------------------------------------------------------------------------------------

	static class PropertyGroupBuilder {
		final Map<String,MutableProperty> properties = new ConcurrentSkipListMap<>();

		PropertyGroupBuilder() {
			this(emptyMap());
		}

		synchronized void apply(PropertyGroup copyFrom) {
			for (Map.Entry<String,Property> e : copyFrom.properties.entrySet()) {
				String pName = e.getKey();
				MutableProperty p1 = properties.get(pName);
				Property p2 = e.getValue();
				if (p1 == null)
					properties.put(pName, p2.mutable());
				else
					p1.apply(p2.value);
			}
		}

		PropertyGroupBuilder(Map<String,Property> properties) {
			for (Map.Entry<String,Property> p : properties.entrySet())
				this.properties.put(p.getKey(), p.getValue().mutable());
		}

		synchronized void set(String key, Object value) {
			MutableProperty p = properties.get(key);
			if (p == null) {
				p = MutableProperty.create(key, value);
				if (! p.isEmpty())
					properties.put(key, p);
			} else {
				p.set(value);
				if (p.isEmpty())
					properties.remove(key);
				else
					properties.put(key, p);
			}
		}

		synchronized void addTo(String key, String arg, Object value) {
			MutableProperty p = properties.get(key);
			if (p == null) {
				p = MutableProperty.create(key, null);
				p.add(arg, value);
				if (! p.isEmpty())
					properties.put(key, p);
			} else {
				p.add(arg, value);
				if (p.isEmpty())
					properties.remove(key);
				else
					properties.put(key, p);
			}
		}

		synchronized void removeFrom(String key, Object value) {
			MutableProperty p = properties.get(key);
			if (p == null) {
				// Create property anyway to generate a good error message.
				p = MutableProperty.create(key, null);
				p.remove(value);
			} else {
				p.remove(value);
				if (p.isEmpty())
					properties.remove(key);
				else
					properties.put(key, p);
			}
		}

		synchronized boolean isEmpty() {
			return properties.isEmpty();
		}

		synchronized PropertyGroup build() {
			return new PropertyGroup(properties);
		}

		/**
		 * Used by the <c>toString()</c> method for debugging.
		 *
		 * @param bs The bean session.
		 * @return This object converted to a map.
		 */
		public Map<String,MutableProperty> swap(BeanSession bs) {
			return properties;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// MutableProperty
	//-------------------------------------------------------------------------------------------------------------------

	static abstract class MutableProperty {
		final String name;
		final PropertyType type;

		MutableProperty(String name, PropertyType type) {
			this.name = name;
			this.type = type;
		}

		static MutableProperty create(String name, Object value) {
			int i = name.lastIndexOf('.');
			String type = i == -1 ? "s" : name.substring(i+1);
			PropertyType pt = SUFFIX_MAP.get(type);

			if (pt == null)
				throw new ConfigException("Invalid type specified on property ''{0}''", name);

			switch (pt) {
				case STRING:
				case BOOLEAN:
				case INTEGER:
				case CLASS:
				case OBJECT:  return new MutableSimpleProperty(name, pt, value);
				case SET_STRING:
				case SET_INTEGER:
				case SET_CLASS: return new MutableSetProperty(name, pt, value);
				case LIST_STRING:
				case LIST_INTEGER:
				case LIST_CLASS:
				case LIST_OBJECT: return new MutableListProperty(name, pt, value);
				case SORTED_MAP_STRING:
				case SORTED_MAP_INTEGER:
				case SORTED_MAP_CLASS:
				case SORTED_MAP_OBJECT: return new MutableMapProperty(name, pt, value);
				case ORDERED_MAP_STRING:
				case ORDERED_MAP_INTEGER:
				case ORDERED_MAP_CLASS:
				case ORDERED_MAP_OBJECT: return new MutableLinkedMapProperty(name, pt, value);
				default: return new MutableSimpleProperty(name, PropertyType.STRING, value);
			}
		}

		abstract Property build();

		abstract boolean isEmpty();

		abstract void set(Object value);

		abstract void apply(Object value);

		abstract Object peek();

		void add(String arg, Object value) {
			throw new ConfigException("Cannot add value {0} ({1}) to property ''{2}'' ({3}).", string(value), className(value), name, type);
		}

		void remove(Object value) {
			throw new ConfigException("Cannot remove value {0} ({1}) from property ''{2}'' ({3}).", string(value), className(value), name, type);
		}

		Object convert(Object value) {
			return value == null ? null : type.converter.convert(value);
		}

		@Override /* Object */
		public String toString() {
			return StringUtils.stringify(peek());
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// MutableSimpleProperty
	//-------------------------------------------------------------------------------------------------------------------

	static class MutableSimpleProperty extends MutableProperty {
		private Object value;

		MutableSimpleProperty(String name, PropertyType type, Object value) {
			super(name, type);
			set(value);
		}

		@Override /* MutableProperty */
		synchronized Property build() {
			return new Property(name, value, type);
		}

		@Override /* MutableProperty */
		synchronized void set(Object value) {
			this.value = convert(value);
		}

		@Override /* MutableProperty */
		synchronized void apply(Object value) {
			this.value = convert(value);
		}

		@Override /* MutableProperty */
		synchronized boolean isEmpty() {
			return this.value == null;
		}

		@Override /* MutableProperty */
		synchronized Object peek() {
			return value;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// MutableSetProperty
	//-------------------------------------------------------------------------------------------------------------------

	static class MutableSetProperty extends MutableProperty {
		private final Set<Object> set;

		MutableSetProperty(String name, PropertyType type, Object value) {
			super(name, type);
			set = new ConcurrentSkipListSet<>(type.comparator());
			set(value);
		}

		@Override /* MutableProperty */
		synchronized Property build() {
			return new Property(name, unmodifiableSet(new LinkedHashSet<>(set)), type);
		}

		@Override /* MutableProperty */
		synchronized void set(Object value) {
			try {
				Set<Object> newSet = merge(set, type.converter, value);
				set.clear();
				set.addAll(newSet);
			} catch (Exception e) {
				throw new ConfigException(e, "Cannot set value {0} ({1}) on property ''{2}'' ({3}).", string(value), className(value), name, type);
			}
		}

		@Override /* MutableProperty */
		synchronized void apply(Object values) {
			set.addAll((Set<?>)values);
		}

		@Override /* MutableProperty */
		synchronized void add(String arg, Object o) {
			if (arg != null)
				throw new ConfigException("Cannot use argument ''{0}'' on add command for property ''{1}'' ({2})", arg, name, type);
			try {
				set.addAll(normalize(type.converter, o));
			} catch (Exception e) {
				throw new ConfigException(e, "Cannot add value {0} ({1}) to property ''{2}'' ({3}).", string(o), className(o), name, type);
			}
		}

		@Override /* MutableProperty */
		synchronized void remove(Object o) {
			try {
				set.removeAll(normalize(type.converter, o));
			} catch (Exception e) {
				throw new ConfigException(e, "Cannot remove value {0} ({1}) from property ''{2}'' ({3}).", string(o), className(o), name, type);
			}
		}

		@Override /* MutableProperty */
		synchronized boolean isEmpty() {
			return set.isEmpty();
		}

		@Override /* MutableProperty */
		synchronized Object peek() {
			return set;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// MutableListProperty
	//-------------------------------------------------------------------------------------------------------------------

	static class MutableListProperty extends MutableProperty {
		private final List<Object> list = synchronizedList(new LinkedList<>());

		MutableListProperty(String name, PropertyType type, Object value) {
			super(name, type);
			set(value);
		}

		@Override /* MutableProperty */
		synchronized Property build() {
			return new Property(name, unmodifiableList(new ArrayList<>(list)), type);
		}

		@Override /* MutableProperty */
		synchronized void set(Object value) {
			try {
				List<Object> newList = merge(list, type.converter, value);
				list.clear();
				list.addAll(newList);
			} catch (Exception e) {
				throw new ConfigException(e, "Cannot set value {0} ({1}) on property ''{2}'' ({3}).", string(value), className(value), name, type);
			}
		}

		@Override /* MutableProperty */
		synchronized void apply(Object values) {
			list.addAll((List<?>)values);
		}

		@Override /* MutableProperty */
		synchronized void add(String arg, Object o) {
			if (arg != null && ! StringUtils.isNumeric(arg))
				throw new ConfigException("Invalid argument ''{0}'' on add command for property ''{1}'' ({2})", arg, name, type);

			int index = arg == null ? 0 : Integer.parseInt(arg);
			if (index < 0)
				index = 0;
			else if (index > list.size())
				index = list.size();

			try {
				List<Object> l = normalize(type.converter, o);
				list.removeAll(l);
				list.addAll(index, l);
			} catch (Exception e) {
				throw new ConfigException(e, "Cannot add value {0} ({1}) to property ''{2}'' ({3}).", string(o), className(o), name, type);
			}
		}

		@Override /* MutableProperty */
		synchronized void remove(Object o) {
			try {
				list.removeAll(normalize(type.converter, o));
			} catch (Exception e) {
				throw new ConfigException(e, "Cannot remove value {0} ({1}) from property ''{2}'' ({3}).", string(o), className(o), name, type);
			}
		}

		@Override /* MutableProperty */
		synchronized boolean isEmpty() {
			return list.isEmpty();
		}

		@Override /* MutableProperty */
		synchronized Object peek() {
			return list;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// MutableMapProperty
	//-------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("rawtypes")
	static class MutableMapProperty extends MutableProperty {
		protected Map<String,Object> map;

		MutableMapProperty(String name, PropertyType type, Object value) {
			super(name, type);
			this.map = createMap();
			set(value);
		}

		protected Map<String,Object> createMap() {
			return new ConcurrentHashMap<>();
		}

		@Override /* MutableProperty */
		synchronized Property build() {
			return new Property(name, unmodifiableMap(new TreeMap<>(map)), type);
		}

		@Override /* MutableProperty */
		synchronized void set(Object value) {
			this.map.clear();
			add(null, value);
		}

		@SuppressWarnings("unchecked")
		@Override /* MutableProperty */
		synchronized void apply(Object values) {
			for (Map.Entry<String,Object> e : ((Map<String,Object>)values).entrySet())
				add(e.getKey(), e.getValue());
		}

		@Override /* MutableProperty */
		synchronized void add(String arg, Object o) {
			if (arg != null) {
				o = convert(o);
				if (o == null)
					this.map.remove(arg);
				else
					this.map.put(arg, o);

			} else if (o != null) {
				if (o instanceof Map) {
					Map m = (Map)o;
					for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
						if (e.getKey() != null)
							add(e.getKey().toString(), e.getValue());
				} else if (isObjectMap(o)) {
					try {
						add(null, new ObjectMap(o.toString()));
					} catch (Exception e) {
						throw new ConfigException(e, "Cannot add {0} ({1}) to property ''{2}'' ({3}).", string(o), className(o), name, type);
					}
				} else {
					throw new ConfigException("Cannot add {0} ({1}) to property ''{2}'' ({3}).", string(o), className(o), name, type);
				}
			}
		}

		@Override /* MutableProperty */
		synchronized boolean isEmpty() {
			return this.map.isEmpty();
		}

		@Override /* MutableProperty */
		synchronized Object peek() {
			return map;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// MutableLinkedMapProperty
	//-------------------------------------------------------------------------------------------------------------------

	static class MutableLinkedMapProperty extends MutableMapProperty {

		MutableLinkedMapProperty(String name, PropertyType type, Object value) {
			super(name, type, value);
			set(value);
		}

		@Override
		protected Map<String,Object> createMap() {
			return synchronizedMap(new LinkedHashMap<String,Object>());
		}

		@Override /* MutableProperty */
		synchronized Property build() {
			return new Property(name, unmodifiableMap(new LinkedHashMap<>(map)), type);
		}
	}


	//-------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-------------------------------------------------------------------------------------------------------------------

	static Set<Object> merge(Set<Object> oldSet, PropertyConverter<?> pc, Object o) throws Exception {
		return merge(oldSet, new LinkedHashSet<>(), normalize(pc, o));
	}

	private static Set<Object> merge(Set<Object> oldSet, Set<Object> newSet, List<Object> l) {
		for (Object o : l) {
			if (isNone(o))
				newSet.clear();
			else if (isInherit(o))
				newSet.addAll(oldSet);
			else
				newSet.add(o);
		}
		return newSet;
	}

	static List<Object> merge(List<Object> oldList, PropertyConverter<?> pc, Object o) throws Exception {
		return merge(oldList, new ArrayList<>(), normalize(pc, o));
	}

	private static List<Object> merge(List<Object> oldList, List<Object> newList, List<Object> l) {
		for (Object o : l) {
			if (isIndexed(o)) {
				Matcher lm = INDEXED_LINK_PATTERN.matcher(o.toString());
				lm.matches();
				String key = lm.group(1);
				int i2 = Math.min(newList.size(), Integer.parseInt(lm.group(2)));
				String remainder = lm.group(3);
				newList.add(i2, key.isEmpty() ? remainder : key + ":" + remainder);
			} else if (isNone(o)) {
				newList.clear();
			} else if (isInherit(o)) {
				if (oldList != null)
					for (Object o2 : oldList)
						newList.add(o2);
			} else {
				newList.remove(o);
				newList.add(o);
			}
		}

		return newList;
	}

	static List<Object> normalize(PropertyConverter<?> pc, Object o) throws Exception {
		return normalize(new ArrayList<>(), pc, o);
	}

	@SuppressWarnings("unchecked")
	static List<Object> normalize(List<Object> l, PropertyConverter<?> pc, Object o) throws Exception {
		if (o != null) {
			if (o.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(o); i++)
					normalize(l, pc, Array.get(o, i));
			} else if (o instanceof Collection) {
				for (Object o2 : (Collection<Object>)o)
					normalize(l, pc, o2);
			} else if (isObjectList(o)) {
				normalize(l, pc, new ObjectList(o.toString()));
			} else {
				l.add(pc == null ? o : pc.convert(o));
			}
		}
		return l;
	}

	static String string(Object value) {
		return SimpleJsonSerializer.DEFAULT.toString(value);
	}

	static String className(Object value) {
		return value.getClass().getSimpleName();
	}

	static boolean isObjectMap(Object o) {
		if (o instanceof CharSequence) {
			String s = o.toString();
			return (s.startsWith("{") && s.endsWith("}") && BeanContext.DEFAULT != null);
		}
		return false;
	}

	private static String group(String key) {
		if (key == null || key.indexOf('.') == -1)
			return "";
		return key.substring(0, key.indexOf('.'));
	}

	static boolean isObjectList(Object o) {
		if (o instanceof CharSequence) {
			String s = o.toString();
			return (s.startsWith("[") && s.endsWith("]") && BeanContext.DEFAULT != null);
		}
		return false;
	}

	private static boolean isNone(Object o) {
		if (o instanceof CharSequence) {
			String s = o.toString();
			return "NONE".equals(s);
		}
		return false;
	}

	private static boolean isIndexed(Object o) {
		if (o instanceof CharSequence) {
			String s = o.toString();
			return s.indexOf('[') != -1 && INDEXED_LINK_PATTERN.matcher(s).matches();
		}
		return false;
	}

	private static final Pattern INDEXED_LINK_PATTERN = Pattern.compile("(?s)(\\S*)\\[(\\d+)\\]\\:(.*)");

	private static boolean isInherit(Object o) {
		if (o instanceof CharSequence) {
			String s = o.toString();
			return "INHERIT".equals(s);
		}
		return false;
	}

	@Override /* Object */
	public String toString() {
		return SimpleJson.DEFAULT_READABLE.toString(groups);
	}
}