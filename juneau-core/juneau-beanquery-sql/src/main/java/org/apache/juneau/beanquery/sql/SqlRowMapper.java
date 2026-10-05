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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.sql.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.beanquery.*;

/**
 * Turns the current {@link ResultSet} row into a bean, for {@link SqlBeanQuerySession#find(BeanQuery)} and
 * {@link SqlBeanQuerySession#stream(BeanQuery)}.
 *
 * <p>
 * The result set is positioned on the row; do not call {@link ResultSet#next()}.  {@code columns} lists the selected
 * (logical) column names in {@code SELECT} order (the query's view, or every column), so column {@code i} of the
 * result set is {@code columns.get(i - 1)} and a mapper can skip columns the query did not select.
 *
 * <p>
 * Two built-in mappers need no dependency beyond this module:
 * <ul>
 * 	<li>{@link #map()} &mdash; each row as a {@link LinkedHashMap} keyed by column name in view order.
 * 	<li>{@link #bean(Class)} &mdash; each row as a {@code record} (canonical constructor) or as a class with a public
 * 		no-arg constructor and public setters ({@code setX}/{@code withX}, void or fluent) or public non-final fields.
 * 		Columns are matched to Juneau bean property names by exact name; columns with no property are ignored, and
 * 		properties with no column keep their defaults.
 * 	<li>{@link #bean(BeanMeta)} &mdash; the same, but using existing bean metadata, so custom property namers apply and
 * 		column names match the property names a serializer built from that metadata emits.
 * </ul>
 *
 * <p>
 * When no mapper is set on the {@link SqlBeanQueryContext.Builder}, one is chosen from the row type: a {@code Map}
 * type gets {@link #map()}, and any other type that {@link #bean(Class)} can plan gets {@link #bean(Class)}.
 * {@code Object}, {@code Map} types a {@link LinkedHashMap} is not assignable to (such as {@code TreeMap}) and types
 * that cannot be planned get none, and {@code find}/{@code stream} then throw {@link IllegalStateException}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Custom mapper.</jc>
 * 	SqlRowMapper&lt;Person&gt; <jv>mapper</jv> = (<jv>rs</jv>, <jv>columns</jv>) -&gt; <jk>new</jk> Person(
 * 		<jv>columns</jv>.contains(<js>"name"</js>) ? <jv>rs</jv>.getString(<js>"full_name"</js>) : <jk>null</jk>,
 * 		<jv>columns</jv>.contains(<js>"age"</js>) ? <jv>rs</jv>.getInt(<js>"age"</js>) : 0);
 *
 * 	<jc>// Rows as maps, or as a record / setter bean.</jc>
 * 	SqlRowMapper&lt;Map&lt;String,Object&gt;&gt; <jv>maps</jv> = SqlRowMapper.<jsm>map</jsm>();
 * 	SqlRowMapper&lt;Person&gt; <jv>beans</jv> = SqlRowMapper.<jsm>bean</jsm>(Person.<jk>class</jk>);
 *
 * 	<jc>// Default selection: no rowMapper(...) call, so Person (a record) gets bean(Person.class).</jc>
 * 	SqlBeanQueryContext&lt;Person&gt; <jv>context</jv> = SqlBeanQueryContext.<jsm>create</jsm>(Person.<jk>class</jk>)
 * 		<jc>// ...dialect, table, columns, connection source...</jc>
 * 		.build();
 * </p>
 *
 * @param <T> The row (bean) type.
 * @since 10.0.0
 */
@FunctionalInterface
public interface SqlRowMapper<T> {

	/**
	 * Maps the current result-set row to a bean.
	 *
	 * @param rs The result set, positioned on the row to map.  Must not be advanced.
	 * @param columns The selected column names in {@code SELECT} order.
	 * @return The mapped bean.
	 * @throws SQLException If a column read fails.
	 */
	T map(ResultSet rs, List<String> columns) throws SQLException;

	/**
	 * A built-in row mapper that maps each row to a new {@link LinkedHashMap}, keyed by the logical column name in
	 * {@code SELECT} order, with each value read via {@link ResultSet#getObject(int)} (the driver's natural type;
	 * <jk>null</jk> for SQL {@code NULL}).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	SqlRowMapper&lt;Map&lt;String,Object&gt;&gt; <jv>mapper</jv> = SqlRowMapper.<jsm>map</jsm>();
	 * </p>
	 *
	 * @return A shared, stateless row mapper.  Always the same instance.
	 * @since 10.0.0
	 */
	static SqlRowMapper<Map<String,Object>> map() {
		return SqlRowMappers.MAP;
	}

	/**
	 * A built-in row mapper that maps each row to a new instance of {@code type}: a {@code record} (via its
	 * canonical constructor) or a concrete class with a public no-arg constructor and public setters
	 * ({@code setX}/{@code withX} returning {@code void} or the bean type, so fluent setters work) or public non-final
	 * fields.
	 *
	 * <p>
	 * Planning happens here, once; mapping never re-inspects {@code type}.  Columns match Juneau bean property names
	 * exactly (setters, public fields and record components, with {@code @BeanIgnore}, {@code @BeanProp} and the
	 * default property namer applied; see {@link QueryModel}).  A column with no
	 * property is ignored; a record component with no column, or a SQL {@code NULL} for a primitive, gets the type's
	 * default.  Values are read with {@code ResultSet.getObject(i, type)}, except {@link Instant} (via
	 * {@link OffsetDateTime}), {@link java.util.Date} (via {@code getTimestamp}) and enums (by constant name).
	 * A failure reading or setting a column throws a {@link BeanQueryExecutionException} naming the column.  The
	 * returned mapper is thread-safe.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>record</jk> Task(String name, <jk>int</jk> age) {}
	 * 	SqlRowMapper&lt;Task&gt; <jv>mapper</jv> = SqlRowMapper.<jsm>bean</jsm>(Task.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The bean type.
	 * @param type The bean type.  Must not be <jk>null</jk>.
	 * @return A row mapper for {@code type}.
	 * @throws IllegalArgumentException If {@code type} cannot be planned: an interface, abstract class, enum, array or
	 * 	primitive type, a class with no public no-arg constructor, or a setter bean with an ambiguous (overloaded) setter.
	 * @since 10.0.0
	 */
	static <T> SqlRowMapper<T> bean(Class<T> type) {
		reqnn("type", type);
		var plan = SqlRowMappers.tryBean(type);
		if (plan.reason() != null)
			throw new IllegalArgumentException(format("Cannot map rows to '%s': %s.", type.getSimpleName(), plan.reason()));
		return plan.mapper();
	}

	/**
	 * Same as {@link #bean(Class)}, but follows the bean rules of existing bean metadata, for example the metadata of
	 * the serializer that renders the rows, so column names match the property names it emits.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BeanConfigContext <jv>config</jv> = BeanConfigContext.<jsm>create</jsm>().propertyNamer(PropertyNamerDLC.<jsf>INSTANCE</jsf>).build();
	 * 	SqlRowMapper&lt;Task&gt; <jv>mapper</jv> = SqlRowMapper.<jsm>bean</jsm>(BeanMeta.<jsm>of</jsm>(Task.<jk>class</jk>, <jv>config</jv>));
	 * </p>
	 *
	 * @param <T> The bean type.
	 * @param beanMeta The bean metadata.  Must not be <jk>null</jk>.
	 * @return A row mapper for the bean type.
	 * @throws IllegalArgumentException For the same reasons as {@link #bean(Class)}.
	 * @since 10.0.0
	 */
	static <T> SqlRowMapper<T> bean(BeanMeta<T> beanMeta) {
		reqnn("beanMeta", beanMeta);
		var plan = SqlRowMappers.tryBean(beanMeta);
		if (plan.reason() != null)
			throw new IllegalArgumentException(format("Cannot map rows to '%s': %s.", beanMeta.getClassInfo().inner().getSimpleName(), plan.reason()));
		return plan.mapper();
	}
}
