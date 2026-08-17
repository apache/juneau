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

import java.util.*;

/**
 * The DataTables server-side-processing response envelope.
 *
 * <p>
 * Serializes (via the normal negotiated serializer) to the JSON shape DataTables expects from a server-side source:
 * <p class='bjson'>
 * 	{
 * 		<jf>draw</jf>: 1,
 * 		<jf>recordsTotal</jf>: 57,
 * 		<jf>recordsFiltered</jf>: 12,
 * 		<jf>data</jf>: [ ... ]
 * 	}
 * </p>
 *
 * <ul class='spaced-list'>
 * 	<li>{@link #getDraw() draw} &mdash; the draw counter echoed back from the request (cast to a number by DataTables).
 * 	<li>{@link #getRecordsTotal() recordsTotal} &mdash; total records before filtering.
 * 	<li>{@link #getRecordsFiltered() recordsFiltered} &mdash; records after filtering (before paging).
 * 	<li>{@link #getData() data} &mdash; the page of rows.
 * 	<li>{@link #getError() error} &mdash; optional error message (omitted from the response when <jk>null</jk>).
 * </ul>
 *
 * @param <T> The row type.
 *
 * @since 10.0.0
 */
public class DataTablesResults<T> {

	private int draw;
	private int recordsTotal;
	private int recordsFiltered;
	private List<T> data;
	private String error;

	/**
	 * Constructor.
	 */
	public DataTablesResults() { /* All fields are populated via the fluent setters below. */ }

	/**
	 * Static creator.
	 *
	 * @param <T> The row type.
	 * @return A new empty {@link DataTablesResults}.
	 */
	public static <T> DataTablesResults<T> create() {
		return new DataTablesResults<>();
	}

	/**
	 * Returns the draw counter.
	 *
	 * @return The draw counter.
	 */
	public int getDraw() {
		return draw;
	}

	/**
	 * Sets the draw counter.
	 *
	 * @param value The value.
	 * @return This object.
	 */
	public DataTablesResults<T> setDraw(int value) {
		this.draw = value;
		return this;
	}

	/**
	 * Returns the total record count before filtering.
	 *
	 * @return The total record count.
	 */
	public int getRecordsTotal() {
		return recordsTotal;
	}

	/**
	 * Sets the total record count before filtering.
	 *
	 * @param value The value.
	 * @return This object.
	 */
	public DataTablesResults<T> setRecordsTotal(int value) {
		this.recordsTotal = value;
		return this;
	}

	/**
	 * Returns the record count after filtering (before paging).
	 *
	 * @return The filtered record count.
	 */
	public int getRecordsFiltered() {
		return recordsFiltered;
	}

	/**
	 * Sets the record count after filtering (before paging).
	 *
	 * @param value The value.
	 * @return This object.
	 */
	public DataTablesResults<T> setRecordsFiltered(int value) {
		this.recordsFiltered = value;
		return this;
	}

	/**
	 * Returns the page of rows.
	 *
	 * @return The page of rows.  Can be <jk>null</jk>.
	 */
	public List<T> getData() {
		return data;
	}

	/**
	 * Sets the page of rows.
	 *
	 * @param value The value.
	 * @return This object.
	 */
	public DataTablesResults<T> setData(List<T> value) {
		this.data = value;
		return this;
	}

	/**
	 * Returns the optional error message.
	 *
	 * @return The error message, or <jk>null</jk> if none.
	 */
	public String getError() {
		return error;
	}

	/**
	 * Sets the optional error message.
	 *
	 * @param value The value.
	 * @return This object.
	 */
	public DataTablesResults<T> setError(String value) {
		this.error = value;
		return this;
	}
}
