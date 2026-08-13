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
package org.apache.juneau.rest.server.converter;

/**
 * The output of running the query engine over a response POJO: the filtered/paged data plus the counts a
 * {@link QueryProtocol} needs to build a response envelope.
 *
 * <ul class='spaced-list'>
 * 	<li>{@link #getData() data} &mdash; the final search/sort/view/page-filtered value (typically a {@code List}).
 * 	<li>{@link #getRecordsTotal() recordsTotal} &mdash; the size of the <i>original</i> data set before any filtering,
 * 		or <c>-1</c> when the response was not a countable collection/array.
 * 	<li>{@link #getRecordsFiltered() recordsFiltered} &mdash; the size after search filtering but <i>before</i> paging,
 * 		or <c>-1</c> when the response was not a countable collection/array.
 * </ul>
 *
 * <p>
 * The native protocol ignores the counts and simply returns {@link #getData() data}; envelope protocols such as
 * DataTables echo the counts.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link QueryProtocol}
 * 	<li class='jc'>{@link QueryArgs}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class QueryResult {

	private final Object data;
	private final int recordsTotal;
	private final int recordsFiltered;

	/**
	 * Constructor.
	 *
	 * @param data The filtered/paged data.  Can be <jk>null</jk>.
	 * @param recordsTotal The original record count, or <c>-1</c> if not countable.
	 * @param recordsFiltered The post-filter/pre-page record count, or <c>-1</c> if not countable.
	 */
	public QueryResult(Object data, int recordsTotal, int recordsFiltered) {
		this.data = data;
		this.recordsTotal = recordsTotal;
		this.recordsFiltered = recordsFiltered;
	}

	/**
	 * The filtered/paged data.
	 *
	 * @return The data.  Can be <jk>null</jk>.
	 */
	public Object getData() {
		return data;
	}

	/**
	 * The size of the original data set before any filtering.
	 *
	 * @return The original record count, or <c>-1</c> if the response was not a countable collection/array.
	 */
	public int getRecordsTotal() {
		return recordsTotal;
	}

	/**
	 * The size after search filtering but before paging.
	 *
	 * @return The post-filter/pre-page record count, or <c>-1</c> if the response was not a countable collection/array.
	 */
	public int getRecordsFiltered() {
		return recordsFiltered;
	}
}
