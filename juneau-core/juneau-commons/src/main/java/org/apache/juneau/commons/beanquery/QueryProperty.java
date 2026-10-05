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
package org.apache.juneau.commons.beanquery;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.lang.reflect.*;
import java.time.temporal.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;

/**
 * One bean property as BeanQuery sees it: its column name, value type and inferred {@link SearchType}, and how to read
 * and write it.
 *
 * <p>
 * Instances come from {@link QueryModel}.  The name is the Juneau bean property name, after {@link BeanProp @BeanProp}
 * and the configured {@link PropertyNamer}, so it is the name serializers emit.  Reads use the getter (or record
 * accessor), else the field.  Writes use the setter, else a non-final field.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	QueryProperty <jv>age</jv> = QueryModel.<jsm>of</jsm>(Person.<jk>class</jk>).getReadableProperty(<js>"age"</js>);
 * 	SearchType <jv>type</jv> = <jv>age</jv>.getSearchType();  <jc>// NUMERIC</jc>
 * 	Object <jv>value</jv> = <jv>age</jv>.read(<jv>person</jv>);
 * </p>
 *
 * @since 10.0.0
 */
public final class QueryProperty {

	private final String name;
	private final Method getter;
	private final Method setter;
	private final Field field;
	private final Class<?> type;
	private final Class<?> writeType;
	private final SearchType searchType;
	private final boolean writeOnly;  // @BeanProp(wo) / writeOnlyProperties.

	QueryProperty(BeanPropertyMeta p) {
		name = p.getName();
		writeOnly = p.isWriteOnly();
		getter = p.getGetter() == null ? null : p.getGetter().<Method>inner();
		setter = p.getSetter() == null ? null : p.getSetter().<Method>inner();
		field = p.getField() == null ? null : p.getField().<Field>inner();
		if (nn(getter))
			type = getter.getReturnType();
		else if (nn(field))
			type = field.getType();
		else if (nn(setter))
			type = setter.getParameterTypes()[0];
		else
			throw isex("Property '%s' has no getter, setter or field.", name);
		writeType = nn(setter) ? setter.getParameterTypes()[0] : type;
		searchType = searchTypeOf(type);
	}

	/**
	 * Infers a column's {@link SearchType} from a property's declared type.
	 *
	 * <p>
	 * Booleans are {@link SearchType#BOOLEAN}; characters are {@link SearchType#TEXT}; primitives and {@link Number}s
	 * are {@link SearchType#NUMERIC}; enums are {@link SearchType#ENUM}; dates, calendars and temporals are
	 * {@link SearchType#TIMESTAMP}; everything else is {@link SearchType#TEXT}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	QueryProperty.<jsm>searchTypeOf</jsm>(<jk>int</jk>.<jk>class</jk>);        <jc>// NUMERIC</jc>
	 * 	QueryProperty.<jsm>searchTypeOf</jsm>(Instant.<jk>class</jk>);     <jc>// TIMESTAMP</jc>
	 * 	QueryProperty.<jsm>searchTypeOf</jsm>(String.<jk>class</jk>);      <jc>// TEXT</jc>
	 * </p>
	 *
	 * @param t The property type.  Must not be <jk>null</jk>.
	 * @return The inferred type (never <jk>null</jk>).
	 */
	public static SearchType searchTypeOf(Class<?> t) {
		if (t == boolean.class || t == Boolean.class)
			return SearchType.BOOLEAN;
		if (t == char.class || t == Character.class)
			return SearchType.TEXT;
		if (t.isPrimitive() || Number.class.isAssignableFrom(t))
			return SearchType.NUMERIC;
		if (t.isEnum())
			return SearchType.ENUM;
		if (Date.class.isAssignableFrom(t) || Calendar.class.isAssignableFrom(t) || TemporalAccessor.class.isAssignableFrom(t))
			return SearchType.TIMESTAMP;
		return SearchType.TEXT;
	}

	/**
	 * Returns the property (column) name.
	 *
	 * @return The name.  Never <jk>null</jk>.
	 */
	public String getName() { return name; }

	/**
	 * Returns the declared read type: the getter's return type, else the field type, else the setter's parameter type.
	 *
	 * @return The type.  Never <jk>null</jk>.
	 */
	public Class<?> getType() { return type; }

	/**
	 * Returns the type {@link #write(Object, Object)} accepts: the setter's parameter type, else {@link #getType()}.
	 *
	 * @return The type.  Never <jk>null</jk>.
	 */
	public Class<?> getWriteType() { return writeType; }

	/**
	 * Returns the search type inferred from {@link #getType()}.
	 *
	 * @return The search type.  Never <jk>null</jk>.
	 */
	public SearchType getSearchType() { return searchType; }

	/**
	 * Returns <jk>true</jk> if the property has a getter or a field and isn't write-only.
	 *
	 * @return <jk>true</jk> if {@link #read(Object)} is supported.
	 */
	public boolean canRead() { return ! writeOnly && (nn(getter) || nn(field)); }

	/**
	 * Returns <jk>true</jk> if the property has a setter or a non-final field.
	 *
	 * <p>
	 * {@link BeanProp#ro() @BeanProp(ro)} is deliberately ignored: in Juneau it means "parsers must not set this
	 * property", and loading a database row is not parsing.
	 *
	 * @return <jk>true</jk> if {@link #write(Object, Object)} is supported.
	 */
	public boolean canWrite() { return (nn(setter) || (nn(field) && ! Modifier.isFinal(field.getModifiers()))); }

	/**
	 * Reads the property from a bean.
	 *
	 * @param bean The bean.  Must not be <jk>null</jk>.
	 * @return The value.  Can be <jk>null</jk>.
	 * @throws BeanQueryExecutionException If the getter throws or the value can't be read.  The cause is the getter's
	 * 	own exception.
	 * @throws IllegalArgumentException If <c>bean</c> is <jk>null</jk>.
	 * @throws IllegalStateException If the property is not readable.
	 */
	public Object read(Object bean) {
		reqnn("bean", bean);
		if (! canRead())
			throw isex("Property '%s' is not readable.", name);
		try {
			return nn(getter) ? getter.invoke(bean) : field.get(bean);
		} catch (InvocationTargetException e) {
			throw new BeanQueryExecutionException(e.getCause(), "Failed to read column '%s'.", name);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new BeanQueryExecutionException(e, "Failed to read column '%s'.", name);
		}
	}

	/**
	 * Writes the property on a bean.  A <jk>null</jk> for a primitive property is ignored, so the bean keeps its
	 * default.
	 *
	 * @param bean The bean.  Must not be <jk>null</jk>.
	 * @param value The value, assignable to {@link #getWriteType()}.  Can be <jk>null</jk>.
	 * @throws BeanQueryExecutionException If the setter throws or the value can't be written.  The cause is the
	 * 	setter's own exception.
	 * @throws IllegalArgumentException If <c>bean</c> is <jk>null</jk>.
	 * @throws IllegalStateException If the property is not writable.
	 */
	public void write(Object bean, Object value) {
		reqnn("bean", bean);
		if (! canWrite())
			throw isex("Property '%s' is not writable.", name);
		if (value == null && writeType.isPrimitive())
			return;
		try {
			if (nn(setter))
				setter.invoke(bean, value);
			else
				field.set(bean, value);
		} catch (InvocationTargetException e) {
			throw new BeanQueryExecutionException(e.getCause(), "Failed to write column '%s'.", name);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new BeanQueryExecutionException(e, "Failed to write column '%s'.", name);
		}
	}

	@Override /* Object */
	public String toString() {
		return name + ":" + searchType;
	}
}
