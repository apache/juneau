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
package org.apache.juneau.beanquery.sql;

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.beanquery.*;

/**
 * Implementation of the built-in {@link SqlRowMapper#map()} and {@link SqlRowMapper#bean(Class)} mappers.
 *
 * <p>
 * Package-private on purpose: everything public is a static factory on {@link SqlRowMapper}.  Also hosts the
 * non-throwing {@link #tryBean(Class)} that {@link SqlBeanQueryContext} uses to pick a default row mapper.  Bean
 * properties come from {@link QueryModel}, so the mapper follows Juneau's bean rules.
 *
 * @since 10.0.0
 */
final class SqlRowMappers {

	private SqlRowMappers() {}

	/** The outcome of planning a bean mapper: a mapper or a reason, never both and never neither. */
	record PlanResult<T>(SqlRowMapper<T> mapper, String reason) {}

	/** The single shared {@code Map} mapper behind {@link SqlRowMapper#map()}. */
	static final SqlRowMapper<Map<String,Object>> MAP = (rs, columns) -> {
		var row = new LinkedHashMap<String,Object>();
		for (var i = 0; i < columns.size(); i++)
			row.put(columns.get(i), rs.getObject(i + 1));
		return row;
	};

	/**
	 * Plans {@code type} with {@link BeanConfigContext#DEFAULT} without throwing.
	 *
	 * @param <T> The bean type.
	 * @param type The bean type.
	 * @return A mapper, or the reason {@link SqlRowMapper#bean(Class)} would have rejected the type.
	 */
	static <T> PlanResult<T> tryBean(Class<T> type) {
		var reason = rejection(type, BeanConfigContext.DEFAULT.getPropertyNamer());
		return reason != null ? new PlanResult<>(null, reason) : new PlanResult<>(new BeanMapper<>(QueryModel.of(type)), null);
	}

	/**
	 * Plans existing bean metadata without throwing.
	 *
	 * @param <T> The bean type.
	 * @param beanMeta The bean metadata.
	 * @return A mapper, or the reason {@link SqlRowMapper#bean(BeanMeta)} would have rejected it.
	 */
	static <T> PlanResult<T> tryBean(BeanMeta<T> beanMeta) {
		var reason = rejection(beanMeta.getClassInfo().inner(), beanMeta.getConfig().getPropertyNamer());
		return reason != null ? new PlanResult<>(null, reason) : new PlanResult<>(new BeanMapper<>(QueryModel.of(beanMeta)), null);
	}

	private static String rejection(Class<?> type, PropertyNamer namer) {
		if (type.isRecord())
			return null;
		if (type.isInterface())
			return "an interface";
		if (type.isArray())
			return "an array type";
		if (type.isPrimitive())
			return "a primitive type";
		if (type.isEnum())  // Before the abstract check: an enum with constant bodies has the ABSTRACT modifier.
			return "an enum";
		if (Modifier.isAbstract(type.getModifiers()))
			return "an abstract class";
		try {
			if (! Modifier.isPublic(type.getDeclaredConstructor().getModifiers()))
				return "no public no-arg constructor";
		} catch (NoSuchMethodException e) {
			return "no public no-arg constructor";
		}
		return ambiguousSetter(type, namer);
	}

	// BeanMeta silently picks one of two same-named setters with unrelated parameter types, so reject the bean instead.
	// Bridges are skipped: generic and covariant overrides are one setter, not two.
	private static String ambiguousSetter(Class<?> type, PropertyNamer namer) {
		var seen = new HashMap<String,Class<?>>();
		for (var m : type.getMethods()) {
			var name = m.getName();
			if (Modifier.isStatic(m.getModifiers()) || m.isBridge() || m.getParameterCount() != 1 || name.length() <= 3 || ! name.startsWith("set"))
				continue;
			var prev = seen.put(name, m.getParameterTypes()[0]);
			if (prev != null && prev != m.getParameterTypes()[0])
				return format("ambiguous setter for '%s'", namer.getPropertyName(name.substring(3)));
		}
		return null;
	}

	/** Maps rows to a bean per a plan made once; stateless and thread-safe afterwards. */
	private static final class BeanMapper<T> implements SqlRowMapper<T> {

		private static final Map<Class<?>,Class<?>> BOXED = Map.of(
			boolean.class, Boolean.class, byte.class, Byte.class, short.class, Short.class, int.class, Integer.class,
			long.class, Long.class, float.class, Float.class, double.class, Double.class, char.class, Character.class);

		private final QueryModel<T> model;
		private final Map<String,Integer> argIndex;  // Constructor-arg (record) properties by name; empty for setter beans.
		private final List<Class<?>> argTypes;

		BeanMapper(QueryModel<T> model) {
			this.model = model;
			argTypes = model.getConstructorArgTypes();
			var args = model.getConstructorArgs();
			var m = new HashMap<String,Integer>();
			for (var i = 0; i < args.size(); i++)
				m.put(args.get(i), i);
			argIndex = Map.copyOf(m);
		}

		@Override /* SqlRowMapper */
		public T map(ResultSet rs, List<String> columns) throws SQLException {
			var args = new Object[argTypes.size()];
			for (var i = 0; i < columns.size(); i++) {
				var idx = argIndex.get(columns.get(i));
				if (idx != null)
					args[idx] = readColumn(rs, i + 1, argTypes.get(idx), columns.get(i));
			}
			var bean = model.newInstance(args);
			for (var i = 0; i < columns.size(); i++) {
				var col = columns.get(i);
				var p = argIndex.containsKey(col) ? null : model.getWritableProperties().get(col);
				if (p != null)
					write(bean, p, readColumn(rs, i + 1, p.getWriteType(), col), col);
			}
			return bean;
		}

		private void write(T bean, QueryProperty p, Object value, String col) {
			try {
				p.write(bean, value);  // A null for a primitive keeps the bean's default.
			} catch (BeanQueryExecutionException e) {
				throw failed(col, e.getCause());
			}
		}

		// Reads one column per the conversion table; every failure is wrapped with the column name (never SQL text).
		private Object readColumn(ResultSet rs, int index, Class<?> targetType, String column) {
			try {
				if (targetType == Instant.class) {
					var t = rs.getObject(index, OffsetDateTime.class);
					return t == null ? null : t.toInstant();
				}
				if (targetType == java.util.Date.class)
					return rs.getTimestamp(index);
				if (targetType.isEnum()) {
					var s = rs.getString(index);
					return s == null ? null : enumValue(targetType, s);
				}
				if (targetType == Object.class)
					return rs.getObject(index);
				return rs.getObject(index, BOXED.getOrDefault(targetType, targetType));
			} catch (SQLException | RuntimeException e) {
				throw failed(column, e);
			}
		}

		private BeanQueryExecutionException failed(String column, Throwable cause) {
			return new BeanQueryExecutionException(cause, "Failed to map column '%s' to %s.", column, model.getBeanClass().getSimpleName());
		}

		@SuppressWarnings({
			"rawtypes", // Enum.valueOf needs Class<T extends Enum<T>>; the target is only known as Class<?>.
			"unchecked" // Enum.valueOf needs Class<T extends Enum<T>>; the target is only known as Class<?>.
		})
		private static Object enumValue(Class<?> targetType, String name) {
			return Enum.valueOf((Class<? extends Enum>)targetType, name);
		}
	}
}
