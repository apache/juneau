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
package org.apache.juneau.bean.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.sql.*;
import java.sql.Timestamp;

import org.apache.juneau.*;
import org.apache.juneau.bean.*;
import org.junit.jupiter.api.*;

class ResultSetList_Test extends TestBase {

	@Test void a01_basicUsage_noRowNums() throws Exception {
		var rs = mock(ResultSet.class);
		var md = mock(ResultSetMetaData.class);
		when(rs.getMetaData()).thenReturn(md);
		when(md.getColumnCount()).thenReturn(2);
		when(md.getColumnName(1)).thenReturn("id");
		when(md.getColumnName(2)).thenReturn("name");
		when(md.getColumnType(1)).thenReturn(Types.INTEGER);
		when(md.getColumnType(2)).thenReturn(Types.VARCHAR);

		var row = new int[]{0};
		when(rs.next()).thenAnswer(inv -> ++row[0] <= 3);
		when(rs.getObject(1)).thenReturn(1).thenReturn(2).thenReturn(3);
		when(rs.getObject(2)).thenReturn("Alice").thenReturn("Bob").thenReturn("Charlie");

		var result = new ResultSetList(rs, 0, Integer.MAX_VALUE, false);

		assertEquals(3, result.size());
		assertEquals(1, result.get(0).get("id"));
		assertEquals("Alice", result.get(0).get("name"));
		assertEquals(2, result.get(1).get("id"));
		assertEquals("Bob", result.get(1).get("name"));

		verify(rs).close();
	}

	@Test void a02_withRowNums() throws Exception {
		var rs = mock(ResultSet.class);
		var md = mock(ResultSetMetaData.class);
		when(rs.getMetaData()).thenReturn(md);
		when(md.getColumnCount()).thenReturn(1);
		when(md.getColumnName(1)).thenReturn("name");
		when(md.getColumnType(1)).thenReturn(Types.VARCHAR);

		var row = new int[]{0};
		when(rs.next()).thenAnswer(inv -> ++row[0] <= 2);
		when(rs.getObject(1)).thenReturn("Alice").thenReturn("Bob");

		var result = new ResultSetList(rs, 0, Integer.MAX_VALUE, true);

		assertEquals(2, result.size());
		assertEquals(0, result.get(0).get("ROW"));
		assertEquals("Alice", result.get(0).get("name"));
		assertEquals(1, result.get(1).get("ROW"));
		assertEquals("Bob", result.get(1).get("name"));
	}

	@Test void a03_withPositionOffset() throws Exception {
		var rs = mock(ResultSet.class);
		var md = mock(ResultSetMetaData.class);
		when(rs.getMetaData()).thenReturn(md);
		when(md.getColumnCount()).thenReturn(1);
		when(md.getColumnName(1)).thenReturn("name");
		when(md.getColumnType(1)).thenReturn(Types.VARCHAR);

		var row = new int[]{0};
		when(rs.next()).thenAnswer(inv -> ++row[0] <= 3);
		when(rs.getObject(1)).thenReturn("Bob").thenReturn("Charlie");

		// pos=2: skip loop runs once (skips 1 rs.next() call), then reads remaining 2 rows
		var result = new ResultSetList(rs, 2, Integer.MAX_VALUE, false);

		assertEquals(2, result.size());
		assertEquals("Bob", result.get(0).get("name"));
		assertEquals("Charlie", result.get(1).get("name"));
	}

	@Test void a04_withLimitZero() throws Exception {
		var rs = mock(ResultSet.class);
		var md = mock(ResultSetMetaData.class);
		when(rs.getMetaData()).thenReturn(md);
		when(md.getColumnCount()).thenReturn(1);
		when(md.getColumnName(1)).thenReturn("name");
		when(md.getColumnType(1)).thenReturn(Types.VARCHAR);
		when(rs.next()).thenReturn(true);

		var result = new ResultSetList(rs, 0, 0, false);
		assertEquals(0, result.size());
		verify(rs).close();
	}

	@Test void a05_readEntry_timestamp() throws Exception {
		var rs = mock(ResultSet.class);
		var md = mock(ResultSetMetaData.class);
		when(rs.getMetaData()).thenReturn(md);
		when(md.getColumnCount()).thenReturn(1);
		when(md.getColumnName(1)).thenReturn("ts");
		when(md.getColumnType(1)).thenReturn(Types.TIMESTAMP);

		var ts = new Timestamp(System.currentTimeMillis());
		var row = new int[]{0};
		when(rs.next()).thenAnswer(inv -> ++row[0] <= 1);
		when(rs.getTimestamp(1)).thenReturn(ts);

		var result = new ResultSetList(rs, 0, Integer.MAX_VALUE, false);
		assertEquals(1, result.size());
		assertEquals(ts, result.get(0).get("ts"));
	}

	@Test void a06_readEntry_blob() throws Exception {
		var rs = mock(ResultSet.class);
		var md = mock(ResultSetMetaData.class);
		when(rs.getMetaData()).thenReturn(md);
		when(md.getColumnCount()).thenReturn(1);
		when(md.getColumnName(1)).thenReturn("data");
		when(md.getColumnType(1)).thenReturn(Types.BLOB);

		var blob = mock(Blob.class);
		when(blob.length()).thenReturn(42L);
		var row = new int[]{0};
		when(rs.next()).thenAnswer(inv -> ++row[0] <= 1);
		when(rs.getBlob(1)).thenReturn(blob);

		var result = new ResultSetList(rs, 0, Integer.MAX_VALUE, false);
		assertEquals(1, result.size());
		assertEquals("blob[42]", result.get(0).get("data"));
	}

	@Test void a07_readEntry_clob() throws Exception {
		var rs = mock(ResultSet.class);
		var md = mock(ResultSetMetaData.class);
		when(rs.getMetaData()).thenReturn(md);
		when(md.getColumnCount()).thenReturn(1);
		when(md.getColumnName(1)).thenReturn("data");
		when(md.getColumnType(1)).thenReturn(Types.CLOB);

		var clob = mock(Clob.class);
		when(clob.length()).thenReturn(100L);
		var row = new int[]{0};
		when(rs.next()).thenAnswer(inv -> ++row[0] <= 1);
		when(rs.getClob(1)).thenReturn(clob);

		var result = new ResultSetList(rs, 0, Integer.MAX_VALUE, false);
		assertEquals(1, result.size());
		assertEquals("clob[100]", result.get(0).get("data"));
	}

	@Test void a08_readEntry_longVarbinary() throws Exception {
		var rs = mock(ResultSet.class);
		var md = mock(ResultSetMetaData.class);
		when(rs.getMetaData()).thenReturn(md);
		when(md.getColumnCount()).thenReturn(1);
		when(md.getColumnName(1)).thenReturn("data");
		when(md.getColumnType(1)).thenReturn(Types.LONGVARBINARY);

		byte[] bytes = {1, 2, 3, 4, 5};
		var row = new int[]{0};
		when(rs.next()).thenAnswer(inv -> ++row[0] <= 1);
		when(rs.getBinaryStream(1)).thenReturn(new ByteArrayInputStream(bytes));

		var result = new ResultSetList(rs, 0, Integer.MAX_VALUE, false);
		assertEquals(1, result.size());
		assertEquals("longvarbinary[5]", result.get(0).get("data"));
	}

	@Test void a09_readEntry_longVarchar() throws Exception {
		var rs = mock(ResultSet.class);
		var md = mock(ResultSetMetaData.class);
		when(rs.getMetaData()).thenReturn(md);
		when(md.getColumnCount()).thenReturn(1);
		when(md.getColumnName(1)).thenReturn("text");
		when(md.getColumnType(1)).thenReturn(Types.LONGVARCHAR);

		byte[] bytes = "hello".getBytes();
		var row = new int[]{0};
		when(rs.next()).thenAnswer(inv -> ++row[0] <= 1);
		when(rs.getAsciiStream(1)).thenReturn(new ByteArrayInputStream(bytes));

		var result = new ResultSetList(rs, 0, Integer.MAX_VALUE, false);
		assertEquals(1, result.size());
		assertEquals("longvarchar[5]", result.get(0).get("text"));
	}

	@Test void a10_readEntry_longNVarchar() throws Exception {
		var rs = mock(ResultSet.class);
		var md = mock(ResultSetMetaData.class);
		when(rs.getMetaData()).thenReturn(md);
		when(md.getColumnCount()).thenReturn(1);
		when(md.getColumnName(1)).thenReturn("text");
		when(md.getColumnType(1)).thenReturn(Types.LONGNVARCHAR);

		var row = new int[]{0};
		when(rs.next()).thenAnswer(inv -> ++row[0] <= 1);
		when(rs.getCharacterStream(1)).thenReturn(new java.io.StringReader("hello"));

		var result = new ResultSetList(rs, 0, Integer.MAX_VALUE, false);
		assertEquals(1, result.size());
		assertEquals("longnvarchar[5]", result.get(0).get("text"));
	}

	@Test void a10b_positionSkipExhaustsResultSet() throws Exception {
		var rs = mock(ResultSet.class);
		var md = mock(ResultSetMetaData.class);
		when(rs.getMetaData()).thenReturn(md);
		when(md.getColumnCount()).thenReturn(1);
		when(md.getColumnName(1)).thenReturn("name");
		when(md.getColumnType(1)).thenReturn(Types.VARCHAR);

		var row = new int[]{0};
		// Only 1 row total; pos=3 means skip loop tries to advance 2 times but rs.next() returns false
		when(rs.next()).thenAnswer(inv -> ++row[0] <= 1);

		// pos=3: skip loop runs, rs.next() becomes false before skip is complete → no rows returned
		var result = new ResultSetList(rs, 3, Integer.MAX_VALUE, false);
		assertEquals(0, result.size());
		verify(rs).close();
	}

	@Test void a11_readEntry_exceptionReturnsExceptionMap() throws Exception {
		var rs = mock(ResultSet.class);
		var md = mock(ResultSetMetaData.class);
		when(rs.getMetaData()).thenReturn(md);
		when(md.getColumnCount()).thenReturn(1);
		when(md.getColumnName(1)).thenReturn("col1");
		when(md.getColumnType(1)).thenReturn(Types.INTEGER);

		var row = new int[]{0};
		when(rs.next()).thenAnswer(inv -> ++row[0] <= 1);
		when(rs.getObject(1)).thenThrow(new SQLException("DB error"));

		var result = new ResultSetList(rs, 0, Integer.MAX_VALUE, false);
		assertEquals(1, result.size());
		// Exception is wrapped as a map via lm(e)
		assertNotNull(result.get(0).get("col1"));
	}
}
