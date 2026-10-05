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

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/**
 * Wraps a JDBC connection and records, in order: {@code "prepare <sql>"}, {@code "fetchSize <n>"},
 * {@code "queryTimeout <n>"}, {@code "rs.close"}, {@code "ps.close"} and {@code "conn.close"}.
 */
final class Recorder {

	final List<String> events = new ArrayList<>();

	/** When <jk>true</jk>, {@code conn.close()} and {@code rs.close()} throw instead of succeeding. */
	boolean failCloses;

	/** Events that start with the specified prefix. */
	List<String> events(String prefix) {
		return events.stream().filter(e -> e.startsWith(prefix)).toList();
	}

	Connection wrap(Connection c) {
		return proxy(Connection.class, c, (m, args, result) -> {
			if (m.getName().equals("close")) {
				events.add("conn.close");
				if (failCloses)
					throw new SQLException("close failed");
			}
			if (m.getName().equals("prepareStatement")) {
				events.add("prepare " + args[0]);
				return statement((PreparedStatement)result);
			}
			return result;
		});
	}

	private PreparedStatement statement(PreparedStatement ps) {
		return proxy(PreparedStatement.class, ps, (m, args, result) -> {
			switch (m.getName()) {
				case "setFetchSize":    events.add("fetchSize " + args[0]); break;
				case "setQueryTimeout": events.add("queryTimeout " + args[0]); break;
				case "close":           events.add("ps.close"); break;
				case "executeQuery":    return resultSet((ResultSet)result);
				default:                break;
			}
			return result;
		});
	}

	private ResultSet resultSet(ResultSet rs) {
		return proxy(ResultSet.class, rs, (m, args, result) -> {
			if (m.getName().equals("close")) {
				events.add("rs.close");
				if (failCloses)
					throw new SQLException("close failed");
			}
			return result;
		});
	}

	@FunctionalInterface
	private interface After {
		Object apply(Method m, Object[] args, Object result) throws SQLException;
	}

	@SuppressWarnings({
		"unchecked" // Proxy.newProxyInstance returns Object; the proxy implements exactly 'type'.
	})
	private static <I> I proxy(Class<I> type, I target, After after) {
		return (I)Proxy.newProxyInstance(Recorder.class.getClassLoader(), new Class<?>[] {type}, (p, m, args) -> {
			try {
				return after.apply(m, args, m.invoke(target, args));
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		});
	}
}
