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
package org.apache.juneau.rest.server.datatables;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.Schema;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;

/**
 * Generates a DataTables <a class="doclink" href="https://datatables.net/reference/option/columns">{@code columns}</a>
 * array from the bean properties of a row type, so the browser-side column list doesn't have to be hand-duplicated.
 *
 * <p>
 * Each generated column descriptor carries the four fields DataTables reads for a server-side-processing column:
 * <ul class='spaced-list'>
 * 	<li><c>data</c> &mdash; the bean property name (also the JSON key DataTables reads from each row).
 * 	<li><c>title</c> &mdash; the human-readable header.  Sourced from {@link Schema#title() @Schema(title)} on the
 * 		property (field or getter) when present, otherwise a humanized form of the property name
 * 		(e.g. {@code "releaseDate"} &rarr; {@code "Release Date"}).
 * 	<li><c>orderable</c> / <c>searchable</c> &mdash; both default to <jk>true</jk> (DataTables' own defaults), emitted
 * 		explicitly so the array is self-describing.
 * </ul>
 *
 * <p>
 * Columns are emitted in Juneau bean-property order (the same order the negotiated serializer uses for the row data),
 * so a generated {@code columns} array lines up positionally with the {@code data} rows in a
 * {@link DataTablesResults} envelope.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Bean whose properties drive both the server response and the browser column list.</jc>
 * 	<jk>public class</jk> Release {
 * 		<jk>public</jk> String getName() {...}
 * 		<ja>@Schema</ja>(title=<js>"Ship date"</js>)
 * 		<jk>public</jk> Date getReleaseDate() {...}
 * 	}
 *
 * 	<jc>// [{data:'name',title:'Name',orderable:true,searchable:true},</jc>
 * 	<jc>//  {data:'releaseDate',title:'Ship date',orderable:true,searchable:true}]</jc>
 * 	List&lt;Map&lt;String,Object&gt;&gt; <jv>columns</jv> = DataTablesColumns.<jsm>of</jsm>(Release.<jk>class</jk>);
 * </p>
 *
 * <p>
 * The result is an ordinary {@code List<Map<String,Object>>}; serialize it with any Juneau serializer (or embed it in a
 * page) to produce the JSON DataTables' {@code columns} option expects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link DataTablesQueryProtocol}
 * 	<li class='jc'>{@link DataTablesTable}
 * 	<li class='link'><a class="doclink" href="https://datatables.net/reference/option/columns">DataTables columns option</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class DataTablesColumns {

	private DataTablesColumns() {}

	/**
	 * Generates the DataTables {@code columns} array for the specified bean type using the default marshalling context.
	 *
	 * @param beanClass The row bean type.  Must not be <jk>null</jk> and must be a bean.
	 * @return A new mutable list of column descriptors, in bean-property order.
	 * @throws IllegalArgumentException If the class is not a bean (no readable bean properties could be resolved).
	 */
	public static List<Map<String,Object>> of(Class<?> beanClass) {
		return of(MarshallingContext.DEFAULT, beanClass);
	}

	/**
	 * Generates the DataTables {@code columns} array for the specified bean type using the specified marshalling context.
	 *
	 * @param ctx The marshalling context supplying the bean metadata (e.g. property naming/ordering).  Must not be <jk>null</jk>.
	 * @param beanClass The row bean type.  Must not be <jk>null</jk> and must be a bean.
	 * @return A new mutable list of column descriptors, in bean-property order.
	 * @throws IllegalArgumentException If the class is not a bean (no readable bean properties could be resolved).
	 */
	public static List<Map<String,Object>> of(MarshallingContext ctx, Class<?> beanClass) {
		var bm = ctx.getBeanMeta(beanClass);
		if (bm == null)
			throw iaex("Class '%s' is not a bean.", beanClass.getName());

		var out = new ArrayList<Map<String,Object>>();
		for (var pm : bm.getProperties().values()) {
			// Skip write-only (setter-only) properties - getProperties() surfaces them, but a column whose data key
			// can't be read back from a row is useless to DataTables (and DataTablesTable would fail reading it).
			if (! pm.canRead())
				continue;
			var col = new LinkedHashMap<String,Object>();
			col.put("data", pm.getName());
			col.put("title", title(pm));
			col.put("orderable", Boolean.TRUE);
			col.put("searchable", Boolean.TRUE);
			out.add(col);
		}
		return out;
	}

	/** Resolves a column title: {@code @Schema(title)} on the property (field then getter), else the humanized name. */
	private static String title(BeanPropertyMeta pm) {
		var fromField = schemaTitle(pm.getField());
		if (fromField != null)
			return fromField;
		var fromGetter = pm.getGetter() == null ? null : schemaTitleOf(pm.getGetter().inner());
		if (fromGetter != null)
			return fromGetter;
		return humanize(pm.getName());
	}

	private static String schemaTitle(FieldInfo f) {
		return f == null ? null : schemaTitleOf(f.inner());
	}

	private static String schemaTitleOf(AnnotatedElement el) {
		var s = el.getAnnotation(Schema.class);
		if (s == null || s.title().isEmpty())
			return null;
		return s.title();
	}

	/**
	 * Converts a bean property name into a human-readable title by splitting camel-case boundaries and capitalizing the
	 * first letter (e.g. {@code "releaseDate"} &rarr; {@code "Release Date"}, {@code "name"} &rarr; {@code "Name"}).
	 */
	static String humanize(String name) {
		var sb = new StringBuilder(name.length() + 4);
		for (var i = 0; i < name.length(); i++) {
			var ch = name.charAt(i);
			if (i == 0) {
				sb.append(Character.toUpperCase(ch));
			} else {
				if (Character.isUpperCase(ch) && Character.isLowerCase(name.charAt(i - 1)))
					sb.append(' ');
				sb.append(ch);
			}
		}
		return sb.toString();
	}
}
