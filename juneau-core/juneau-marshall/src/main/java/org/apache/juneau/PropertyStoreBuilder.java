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

import static org.apache.juneau.internal.ClassUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.PropertyStore.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;

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
			throw new RuntimeException("Property store mismatch!  This shouldn't happen.");
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
			this(Collections.EMPTY_MAP);
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
				case MAP_STRING:
				case MAP_INTEGER:
				case MAP_CLASS:
				case MAP_OBJECT: return new MutableMapProperty(name, pt, value);
				default: return new MutableSimpleProperty(name, PropertyType.STRING, value);
			}
		}

		abstract Property build();
		
		abstract boolean isEmpty();
		
		abstract void set(Object value);

		abstract void apply(Object value);
		
		abstract Object peek();

		void add(String arg, Object value) {
			throw new ConfigException("Cannot add value {0} ({1}) to property ''{2}'' ({3}).",
				JsonSerializer.DEFAULT_LAX.toString(value), value.getClass().getSimpleName(), name, type);
		}

		void remove(Object value) {
			throw new ConfigException("Cannot remove value {0} ({1}) from property ''{2}'' ({3}).",
				JsonSerializer.DEFAULT_LAX.toString(value), value.getClass().getSimpleName(), name, type);
		}

		Object convert(Object value) {
			return value == null ? null : type.converter.convert(value, this);
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
	
	@SuppressWarnings("unchecked")
	static class MutableSetProperty extends MutableProperty {
		private final Set<Object> value;

		MutableSetProperty(String name, PropertyType type, Object value) {
			super(name, type);
			this.value = new ConcurrentSkipListSet<>(type.comparator());
			set(value);
		}
		
		@Override /* MutableProperty */
		synchronized Property build() {
			return new Property(name, Collections.unmodifiableSet(new LinkedHashSet<>(value)), type);
		}

		@Override /* MutableProperty */
		synchronized void set(Object value) {
			this.value.clear();
			add(null, value);
		}
		
		@Override /* MutableProperty */
		synchronized void apply(Object values) {
			for (Object o : ((Set<?>)values)) 
				add(null, o);
		}
		
		@Override /* MutableProperty */
		synchronized void add(String arg, Object value) {
			if (arg != null)
				throw new ConfigException("Cannot use argument ''{0}'' on add command for property ''{1}'' ({2})", arg, name, type);
			if (value != null) {
				if (value.getClass().isArray()) {
					for (int i = 0; i < Array.getLength(value); i++)
						add(null, Array.get(value, i));
				} else if (value instanceof Collection) {
					for (Object o : (Collection<Object>)value)
						add(null, o);
				} else if (isObjectList(value)) {
					try {
						add(null, new ObjectList(value.toString()));
					} catch (Exception e) {
						throw new ConfigException(
							"Cannot add value {0} ({1}) to property ''{2}'' ({3}).",
							JsonSerializer.DEFAULT_LAX.toString(value), getReadableClassNameForObject(value), name, type
						);
					}
				} else {
					value = convert(value);
					this.value.add(value);
				}
			}
		}
		
		@Override /* MutableProperty */
		synchronized void remove(Object value) {
			if (value != null) {
				if (value.getClass().isArray()) {
					for (int i = 0; i < Array.getLength(value); i++)
						remove(Array.get(value, i));
				} else if (value instanceof Collection) {
					for (Object o : (Collection<Object>)value)
						remove(o);
				} else if (isObjectList(value)) {
					try {
						remove(new ObjectList(value.toString()));
					} catch (Exception e) {
						throw new ConfigException(
							"Cannot remove value {0} ({1}) from property ''{2}'' ({3}) because it''s not a valid JSON array.",
							JsonSerializer.DEFAULT_LAX.toString(value), getReadableClassNameForObject(value), name, type
						);
					}					
				} else {
					value = convert(value);
					this.value.remove(value);
				}
			}
		}
		
		@Override /* MutableProperty */
		synchronized boolean isEmpty() {
			return this.value.isEmpty();
		}

		@Override /* MutableProperty */
		synchronized Object peek() {
			return value;
		}
	}
	
	//-------------------------------------------------------------------------------------------------------------------
	// MutableListProperty
	//-------------------------------------------------------------------------------------------------------------------
	
	@SuppressWarnings("unchecked")
	static class MutableListProperty extends MutableProperty {
		private List<Object> value = new CopyOnWriteArrayList<>();

		MutableListProperty(String name, PropertyType type, Object value) {
			super(name, type);
			set(value);
		}
		
		@Override /* MutableProperty */
		synchronized Property build() {
			return new Property(name, Collections.unmodifiableList(CollectionUtils.reverse(new ArrayList<>(value))), type);
		}

		@Override /* MutableProperty */
		synchronized void set(Object value) {
			this.value.clear();
			add(null, value);
		}
		
		@Override /* MutableProperty */
		synchronized void apply(Object values) {
			add(null, values);
		}

		@Override /* MutableProperty */
		synchronized void add(String arg, Object value) {
			if (arg != null && ! StringUtils.isNumeric(arg))
				throw new ConfigException("Invalid argument ''{0}'' on add command for property ''{1}'' ({2})", arg, name, type);
			
			// Note that since lists are stored in reverse-order, the index should be the compliment.
			
			int index = arg == null ? this.value.size() : this.value.size() - Integer.parseInt(arg);
			if (index < 0)
				index = 0;
			else if (index > this.value.size())
				index = this.value.size();
			add(index, value);
		}
		
		private synchronized int add(int index, Object value) {
			if (value != null) {
				
				// Important!
				// Arrays and collections are inserted in REVERSE order.
				// So if you call addTo(X, "['a','b','c']").addTo(X, "['d','e','f']"), the list will end up
				// containing ['c','b','a','f','e','d'].
				// This ensures entries in the incoming value takes precedence in first-to-last order, but
				// subsequent calls to addTo() are added to the end.
				// What you get is a list ordered in least-to-most important.
				
				if (value.getClass().isArray()) {
					for (int i = Array.getLength(value) - 1; i >= 0; i--)
						index = add(index, Array.get(value, i));
				} else if (value instanceof Collection) {
					for (Object o : CollectionUtils.reverseIterable((Collection<Object>)value))
						index = add(index, o);
				} else if (isObjectList(value)) {
					try {
						index = add(index, new ObjectList(value.toString()));
					} catch (Exception e) {
						throw new ConfigException(
							"Cannot add value {0} ({1}) to property ''{2}'' ({3}).",
							JsonSerializer.DEFAULT_LAX.toString(value), getReadableClassNameForObject(value), name, type
						);
					}
				} else {
					value = convert(value);
					boolean replaced = this.value.remove(value);
					if (replaced)
						index--;
					this.value.add(index, value);
					index++;
				}
			}
			return index;
		}

		@Override /* MutableProperty */
		synchronized void remove(Object value) {
			if (value != null) {
				if (value.getClass().isArray()) {
					for (int i = 0; i < Array.getLength(value); i++)
						remove(Array.get(value, i));
				} else if (value instanceof Collection) {
					for (Object o : (Collection<Object>)value)
						remove(o);
				} else if (isObjectList(value)) {
					try {
						remove(new ObjectList(value.toString()));
					} catch (Exception e) {
						throw new ConfigException(
							"Cannot remove value {0} ({1}) from property ''{2}'' ({3}) because it''s not a valid JSON array.",
							JsonSerializer.DEFAULT_LAX.toString(value), getReadableClassNameForObject(value), name, type
						);
					}					
				} else {
					value = convert(value);
					this.value.remove(value);
				}
			}
		}

		@Override /* MutableProperty */
		synchronized boolean isEmpty() {
			return this.value.isEmpty();
		}

		@Override /* MutableProperty */
		synchronized Object peek() {
			return value;
		}
	}
	
	//-------------------------------------------------------------------------------------------------------------------
	// MutableMapProperty
	//-------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("rawtypes")
	static class MutableMapProperty extends MutableProperty {
		private Map<String,Object> value = new ConcurrentHashMap<>();
		
		MutableMapProperty(String name, PropertyType type, Object value) {
			super(name, type);
			set(value);
		}
		
		@Override /* MutableProperty */
		synchronized Property build() {
			return new Property(name, Collections.unmodifiableMap(new TreeMap<>(value)), type);
		}

		@Override /* MutableProperty */
		synchronized void set(Object value) {
			this.value.clear();
			add(null, value);
		}

		@SuppressWarnings("unchecked")
		@Override /* MutableProperty */
		synchronized void apply(Object values) {
			for (Map.Entry<String,Object> e : ((Map<String,Object>)values).entrySet()) 
				add(e.getKey(), e.getValue());
		}

		@Override /* MutableProperty */
		synchronized void add(String arg, Object value) {
			if (arg != null) {
				value = convert(value);
				if (value == null)
					this.value.remove(arg);
				else
					this.value.put(arg, value);
				
			} else if (value != null) {
				if (value instanceof Map) {
					Map m = (Map)value;
					for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
						if (e.getKey() != null)
							add(e.getKey().toString(), e.getValue());
				} else if (isObjectMap(value)) {
					try {
						add(null, new ObjectMap(value.toString()));
					} catch (Exception e) {
						throw new ConfigException(
							"Cannot add {0} ({1}) to property ''{2}'' ({3}) because it''s not a valid JSON object.",
							JsonSerializer.DEFAULT_LAX.toString(value), getReadableClassNameForObject(value), name, type
						);
					}
				} else {
					throw new ConfigException(
						"Cannot add {0} ({1}) to property ''{2}'' ({3}).",
						JsonSerializer.DEFAULT_LAX.toString(value), getReadableClassNameForObject(value), name, type
					);
				}
			}
		}

		@Override /* MutableProperty */
		synchronized boolean isEmpty() {
			return this.value.isEmpty();
		}

		@Override /* MutableProperty */
		synchronized Object peek() {
			return value;
		}
	}
	
	
	//-------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-------------------------------------------------------------------------------------------------------------------
	
	static boolean isObjectList(Object o) {
		if (o instanceof CharSequence) {
			String s = o.toString();
			return (s.startsWith("[") && s.endsWith("]") && BeanContext.DEFAULT != null);
		}
		return false;
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
}