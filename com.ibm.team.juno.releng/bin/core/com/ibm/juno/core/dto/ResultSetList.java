/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.dto;

import java.sql.*;
import java.util.*;

import com.ibm.juno.core.utils.*;

/**
 * Transforms an SQL {@link ResultSet ResultSet} into a list of maps.
 * <p>
 * 	Loads the entire result set into an in-memory data structure, and then closes the result set object.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class ResultSetList extends LinkedList<Map<String,Object>> {

	private static final long serialVersionUID = 1L;

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
			int rowNum = pos;

			// Get the column names.
			ResultSetMetaData rsmd = rs.getMetaData();
			int offset = (includeRowNums ? 1 : 0);
			int cc = rsmd.getColumnCount();
			String[] columns = new String[cc + offset];
			if (includeRowNums)
				columns[0] = "ROW";
			int[] colTypes = new int[cc];

			for (int i = 0; i < cc; i++) {
				columns[i+offset] = rsmd.getColumnName(i+1);
				colTypes[i] = rsmd.getColumnType(i+1);
			}

			while (--pos > 0 && rs.next()) {}

			// Get the rows.
			while (limit-- > 0 && rs.next()) {
				Object[] row = new Object[cc + offset];
				if (includeRowNums)
					row[0] = rowNum++;
				for (int i = 0; i < cc; i++) {
					Object o = readEntry(rs, i+1, colTypes[i]);
					row[i+offset] = o;
				}
				add(new SimpleMap(columns, row));
			}
		} finally {
			try {
				rs.close();
			} catch (Exception e) {}
		}
	}

	/**
	 * Reads the specified column from the current row in the result set.
	 * Subclasses can override this method to handle specific data types in special ways.
	 *
	 * @param rs The result set to read from.
	 * @param col The column number (indexed by 1).
	 * @param dataType The {@link Types type} of the entry.
	 * @return The entry as an Object.
	 */
	protected Object readEntry(ResultSet rs, int col, int dataType) {
		try {
			switch (dataType) {
				case Types.BLOB:
					Blob b = rs.getBlob(col);
					return "blob["+b.length()+"]";
				case Types.CLOB:
					Clob c = rs.getClob(col);
					return "clob["+c.length()+"]";
				case Types.LONGVARBINARY:
					return "longvarbinary["+IOUtils.count(rs.getBinaryStream(col))+"]";
				case Types.LONGVARCHAR:
					return "longvarchar["+IOUtils.count(rs.getAsciiStream(col))+"]";
				case Types.LONGNVARCHAR:
					return "longnvarchar["+IOUtils.count(rs.getCharacterStream(col))+"]";
				case Types.TIMESTAMP:
					return rs.getTimestamp(col);  // Oracle returns com.oracle.TIMESTAMP objects from getObject() which isn't a Timestamp.
				default:
					return rs.getObject(col);
			}
		} catch (Exception e) {
			return e.getLocalizedMessage();
		}
	}
}
