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
package org.apache.juneau.bean;

import static org.apache.juneau.common.utils.IOUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;

import java.sql.*;
import java.util.*;

import org.apache.juneau.common.collections.*;

/**
 * Transforms an SQL {@link ResultSet ResultSet} into a list of maps.
 * <p>
 * Loads the entire result set into an in-memory data structure, and then closes the result set object.
 *
 *
 * @serial exclude
 */
public class ResultSetList extends LinkedList<Map<String,Object>> {

	private static final long serialVersionUID = 1L;

	/**
	 * Reads the specified column from the current row in the result set.
	 *
	 * <p>
	 * Subclasses can override this method to handle specific data types in special ways.
	 *
	 * @param rs The result set to read from.
	 * @param col The column number (indexed by 1).
	 * @param dataType The {@link Types type} of the entry.
	 * @return The entry as an Object.
	 */
	static Object readEntry(ResultSet rs, int col, int dataType) {
		try {
			return switch (dataType) {
				case Types.BLOB -> {
					var b = rs.getBlob(col);
					yield "blob[" + b.length() + "]";
				}
				case Types.CLOB -> {
					var c = rs.getClob(col);
					yield "clob[" + c.length() + "]";
				}
				case Types.LONGVARBINARY -> "longvarbinary[" + count(rs.getBinaryStream(col)) + "]";
				case Types.LONGVARCHAR -> "longvarchar[" + count(rs.getAsciiStream(col)) + "]";
				case Types.LONGNVARCHAR -> "longnvarchar[" + count(rs.getCharacterStream(col)) + "]";
				case Types.TIMESTAMP -> rs.getTimestamp(col); // Oracle returns com.oracle.TIMESTAMP objects from getObject()
				default -> rs.getObject(col);
			};
		} catch (Exception e) {
			return lm(e);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param rs The result set to load into this DTO.
	 * @param pos The start position (zero-indexed).
	 * @param limit The maximum number of rows to retrieve.
	 * @param includeRowNums Make the first column be the row number.
	 * @throws SQLException Database error.
	 */
	public ResultSetList(ResultSet rs, int pos, int limit, boolean includeRowNums) throws SQLException {
		try {
			var rowNum = pos;

			// Get the column names.
			var rsmd = rs.getMetaData();
			var offset = (includeRowNums ? 1 : 0);
			var cc = rsmd.getColumnCount();
			var columns = new String[cc + offset];
			if (includeRowNums)
				columns[0] = "ROW";
			var colTypes = new int[cc];

			for (var i = 0; i < cc; i++) {
				columns[i + offset] = rsmd.getColumnName(i + 1);
				colTypes[i] = rsmd.getColumnType(i + 1);
			}

			while (--pos > 0 && rs.next()) { /* Skip to the specified position. */ }

			// Get the rows.
			while (limit-- > 0 && rs.next()) {
				var row = new Object[cc + offset];
				if (includeRowNums)
					row[0] = rowNum++;
				for (var i = 0; i < cc; i++) {
					var o = readEntry(rs, i + 1, colTypes[i]);
					row[i + offset] = o;
				}
				add(new SimpleMap<>(columns, row));
			}
		} finally {
			rs.close();
		}
	}
}