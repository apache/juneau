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
package org.apache.juneau.server.labels;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.server.*;

/**
 * Simple bean for describing REST methods.
 * <p>
 * 	Primarily used for constructing tables with name/path/description/... columns on REST OPTIONS requests.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Bean(properties={"httpMethod","path","javaMethod","description","input","responses","consumes","produces","matchers","guards","converters"})
public final class MethodDescription implements Comparable<MethodDescription> {
	private String javaMethod, httpMethod, path, description;
	private String[] guards, converters, matchers;
	private Set<Var> requestVars = new TreeSet<Var>();
	private Map<Integer,Response> responses = new TreeMap<Integer,Response>();
	private Collection<String> consumes, produces;
	private int httpMethodOrder;

	/**
	 * A possible response status.
	 */
	public static class Response {

		/** HTTP status code */
		public int status;

		/** Response description */
		public String description;

		/** Response headers set */
		public Set<Var> output = new TreeSet<Var>();

		/** Bean constructor */
		public Response() {}

		/**
		 * Constructor.
		 *
		 * @param status HTTP status code.
		 */
		public Response(int status) {
			this.status = status;
			this.description = RestUtils.getHttpResponseText(status);
		}

		/**
		 * Add a response variable to this response.
		 *
		 * @param category The response variable category (typically only <js>"header"</js>).
		 * @param name The response variable name.
		 * @return The new variable object whose fields can be updated.
		 */
		public Var addResponseVar(String category, String name) {
			for (Var v : output)
				if (v.matches(category, name))
					return v;
			Var v = new Var(category, name);
			output.add(v);
			return v;
		}

		/**
		 * Sets the description for this response.
		 *
		 * @param description The new description.
		 * @return This object (for method chaining).
		 */
		public Response setDescription(String description) {
			this.description = description;
			return this;
		}
	}

	/** Constructor. */
	public MethodDescription() {}

	/**
	 * Returns the javaMethod field on this label.
	 *
	 * @return The name.
	 */
	public String getJavaMethod() {
		return javaMethod;
	}

	/**
	 * Sets the javaMethod field on this label to a new value.
	 *
	 * @param javaMethod The new name.
	 * @return This object (for method chaining).
	 */
	public MethodDescription setJavaMethod(String javaMethod) {
		this.javaMethod = javaMethod;
		return this;
	}

	/**
	 * Returns the httpMethod field on this label.
	 *
	 * @return The name.
	 */
	public String getHttpMethod() {
		return httpMethod;
	}

	/**
	 * Sets the httpMethod field on this label to a new value.
	 *
	 * @param httpMethod The new name.
	 * @return This object (for method chaining).
	 */
	public MethodDescription setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
		this.httpMethodOrder = (httpMethodOrders.containsKey(httpMethod) ? httpMethodOrders.get(httpMethod) : 10);
		return this;
	}

	/**
	 * Returns the path field on this label.
	 *
	 * @return The path.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Sets the path field on this label to a new value.
	 *
	 * @param path The new path.
	 * @return This object (for method chaining).
	 */
	public MethodDescription setPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * Returns the description field on this label.
	 *
	 * @return The description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description field on this label to a new value.
	 *
	 * @param description The new description.
	 * @return This object (for method chaining).
	 */
	public MethodDescription setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Returns the vars field on this label.
	 *
	 * @return The vars.
	 */
	public Collection<Var> getInput() {
		return requestVars;
	}

	/**
	 * Returns the possible response codes returned by this method.
	 *
	 * @return The possible response codes returned by this method.
	 */
	public Collection<Response> getResponses() {
		return responses.values();
	}

	/**
	 * Sets the list of <code>Accept</code> header values that this method accepts if it's different
	 * 	from the servlet.
	 *
	 * @param consumes The list of <code>Accept</code> header values.
	 * @return This object (for method chaining).
	 */
	public MethodDescription setConsumes(Collection<String> consumes) {
		this.consumes = consumes;
		return this;
	}

	/**
	 * Returns the list of <code>Accept</code> header values that this method accepts if it's different
	 * 	from the servlet.
	 *
	 * @return The list of <code>Accept</code> header values.
	 */
	public Collection<String> getConsumes() {
		return consumes;
	}

	/**
	 * Sets the list of <code>Content-Type</code> header values that this method accepts if it's different
	 * 	from the servlet.
	 *
	 * @param produces The list of <code>Content-Type</code> header values.
	 * @return This object (for method chaining).
	 */
	public MethodDescription setProduces(Collection<String> produces) {
		this.produces = produces;
		return this;
	}

	/**
	 * Returns the list of <code>Content-Type</code> header values that this method accepts if it's different
	 * 	from the servlet.
	 *
	 * @return The list of <code>Content-Type</code> header values.
	 */
	public Collection<String> getProduces() {
		return produces;
	}
	/**
	 * Sets the guards field on this label to a new value.
	 *
	 * @param guards The guards associated with this method.
	 * @return This object (for method chaining).
	 */
	public MethodDescription setGuards(Class<?>...guards) {
		this.guards = new String[guards.length];
		for (int i = 0; i < guards.length; i++)
			this.guards[i] = guards[i].getSimpleName();
		return this;
	}

	/**
	 * Returns the guards field on this label.
	 *
	 * @return The guards.
	 */
	public String[] getGuards() {
		return guards;
	}

	/**
	 * Sets the matchers field on this label to a new value.
	 *
	 * @param matchers The matchers associated with this method.
	 * @return This object (for method chaining).
	 */
	public MethodDescription setMatchers(Class<?>...matchers) {
		this.matchers = new String[matchers.length];
		for (int i = 0; i < matchers.length; i++)
			this.matchers[i] = matchers[i].getSimpleName();
		return this;
	}

	/**
	 * Returns the matchers field on this label.
	 *
	 * @return The matchers.
	 */
	public String[] getMatchers() {
		return matchers;
	}

	/**
	 * Sets the converters field on this label to a new value.
	 *
	 * @param converters The converters associated with this method.
	 * @return This object (for method chaining).
	 */
	public MethodDescription setConverters(Class<?>...converters) {
		this.converters = new String[converters.length];
		for (int i = 0; i < converters.length; i++)
			this.converters[i] = converters[i].getSimpleName();
		return this;
	}

	/**
	 * Returns the converters field on this label.
	 *
	 * @return The converters.
	 */
	public String[] getConverters() {
		return converters;
	}

	/**
	 * Add a request variable to this method description.
	 *
	 * @param category The variable category (e.g. <js>"attr"</js>, <js>"attr"</js>, <js>"header"</js>, <js>"content"</js>).
	 * @param name The variable name.
	 * 	Can be <jk>null</jk> in the case of <js>"content"</js> category.
	 * @return The new variable whose fields can be modified.
	 */
	public Var addRequestVar(String category, String name) {
		for (Var v : requestVars)
			if (v.matches(category, name))
				return v;
		Var v = new Var(category, name);
		requestVars.add(v);
		return v;
	}

	/**
	 * Add a possible HTTP response code from this method.
	 *
	 * @param httpStatus The HTTP status code.
	 * @return The new response object whose fields can be modified.
	 */
	public Response addResponse(int httpStatus) {
		if (! responses.containsKey(httpStatus))
			responses.put(httpStatus, new Response(httpStatus));
		return responses.get(httpStatus);
	}

	@Override
	public int compareTo(MethodDescription md) {
		int i = Utils.compare(httpMethodOrder, md.httpMethodOrder);
		if (i == 0)
			i = path.compareTo(md.path);
		if (i == 0)
			i = javaMethod.compareTo(md.javaMethod);
		return i;
	}

	@SuppressWarnings("serial")
	private static final Map<String,Integer> httpMethodOrders = new HashMap<String,Integer>() {{
		put("GET", 1);
		put("PUT", 2);
		put("POST", 3);
		put("DELETE", 4);
		put("OPTIONS", 5);
	}};
}
