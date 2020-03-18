// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest;

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * TODO
 *
 */
@Bean(bpi="resource,methods")
@SuppressWarnings("javadoc")
public class StatusStats implements Comparable<StatusStats> {

	private final Class<?> resource;
	private final Map<java.lang.reflect.Method,StatusStats.Method> methods = new TreeMap<>();

	public StatusStats(Class<?> resource) {
		this.resource = resource;
	}

	public static StatusStats create(Class<?> resource) {
		return new StatusStats(resource);
	}

	public Class<?> getResource() {
		return resource;
	}

	public StatusStats.Method getMethod(java.lang.reflect.Method method) {
		StatusStats.Method m = methods.get(method);
		if (m == null) {
			m = new Method(method);
			methods.put(method, m);
		}
		return m;
	}

	public Set<StatusStats.Method> getMethods() {
		return new TreeSet<>(methods.values());
	}

	@Bean(bpi="method,codes")
	public static class Method implements Comparable<Method> {
		private java.lang.reflect.Method method;
		private Set<Status> codes = new TreeSet<>();

		private Method(java.lang.reflect.Method method) {
			this.method = method;
		}

		public Method status(int code, int count) {
			codes.add(new Status(code, count));
			return this;
		}

		@Override
		public int compareTo(Method o) {
			return method.getName().compareTo(o.method.getName());
		}
	}

	@Bean(bpi="code,count")
	public static class Status implements Comparable<Status> {
		private int code;
		private int count;

		public Status(int code, int count) {
			this.code = code;
			this.count = count;
		}

		@Override
		public int compareTo(Status o) {
			return Integer.compare(code, o.code);
		}
	}

	@Override
	public int compareTo(StatusStats o) {
		return resource.getName().compareTo(o.resource.getName());
	}
}
