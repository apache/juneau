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
package org.apache.juneau.rest.annotation;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;

/**
 * A concrete implementation of the {@link RestMethod} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
@SuppressWarnings("unchecked")
public class RestMethodAnnotation implements RestMethod {

	private String
		clientVersion = "",
		debug = "",
		defaultAccept = "",
		defaultCharset = "",
		defaultContentType = "",
		maxInput = "",
		name = "",
		method = "",
		path = "",
		rolesDeclared = "",
		roleGuard = "",
		summary = "";

	private String[]
		bpi = new String[0],
		bpx = new String[0],
		defaultFormData = new String[0],
		defaultQuery = new String[0],
		description = new String[0],
		flags = new String[0],
		paths = new String[0],
		reqAttrs = new String[0],
		reqHeaders = new String[0],
		produces = new String[0],
		consumes = new String[0];

	private Class<?>[]
		beanFilters = new Class<?>[0],
		encoders = new Class<?>[0],
		parsers = new Class<?>[0],
		serializers = new Class<?>[0];

	private Class<? extends RestConverter>[] converters = new Class[0];
	private Class<? extends RestGuard>[] guards = new Class[0];
	private Logging logging = new LoggingAnnotation();
	private Class<? extends RestMatcher>[] matchers = new Class[0];
	private int priority = 0;
	private Property[] properties = new Property[0];
	private MethodSwagger swagger = new MethodAnnotationSwagger();

	/**
	 * Constructor.
	 */
	public RestMethodAnnotation() {
	}

	@Override /* RestMethod */
	public Class<? extends Annotation> annotationType() {
		return RestMethod.class;
	}

	@Override /* RestMethod */
	public Class<?>[] beanFilters() {
		return beanFilters;
	}

	@Override /* RestMethod */
	public String[] bpi() {
		return bpi;
	}

	@Override /* RestMethod */
	public String[] bpx() {
		return bpx;
	}

	@Override /* RestMethod */
	public String clientVersion() {
		return clientVersion;
	}

	@Override /* RestMethod */
	public Class<? extends RestConverter>[] converters() {
		return converters;
	}

	@Override /* RestMethod */
	public String debug() {
		return debug;
	}

	@Override /* RestMethod */
	public String defaultAccept() {
		return defaultAccept;
	}

	@Override /* RestMethod */
	public String defaultCharset() {
		return defaultCharset;
	}

	@Override /* RestMethod */
	public String defaultContentType() {
		return defaultContentType;
	}

	@Override /* RestMethod */
	public String[] defaultFormData() {
		return defaultFormData;
	}

	@Override /* RestMethod */
	public String[] defaultQuery() {
		return defaultQuery;
	}

	@Override /* RestMethod */
	public String[] description() {
		return description;
	}

	@Override /* RestMethod */
	public Class<?>[] encoders() {
		return encoders;
	}

	@Override /* RestMethod */
	public String[] flags() {
		return flags;
	}

	@Override /* RestMethod */
	public Class<? extends RestGuard>[] guards() {
		return guards;
	}

	@Override /* RestMethod */
	public Logging logging() {
		return logging;
	}

	@Override /* RestMethod */
	public Class<? extends RestMatcher>[] matchers() {
		return matchers;
	}

	@Override /* RestMethod */
	public String maxInput() {
		return maxInput;
	}

	@Override /* RestMethod */
	public String name() {
		return name;
	}

	@Override /* RestMethod */
	public String method() {
		return method;
	}

	@Override /* RestMethod */
	public Class<?>[] parsers() {
		return parsers;
	}

	@Override /* RestMethod */
	public String path() {
		return path;
	}

	@Override /* RestMethod */
	public String[] paths() {
		return paths;
	}

	@Override /* RestMethod */
	public int priority() {
		return priority;
	}

	@Override /* RestMethod */
	public Property[] properties() {
		return properties;
	}

	@Override /* RestMethod */
	public String[] reqAttrs() {
		return reqAttrs;
	}

	@Override /* RestMethod */
	public String[] reqHeaders() {
		return reqHeaders;
	}

	@Override /* RestMethod */
	public String rolesDeclared() {
		return rolesDeclared;
	}

	@Override /* RestMethod */
	public String roleGuard() {
		return roleGuard;
	}

	@Override /* RestMethod */
	public Class<?>[] serializers() {
		return serializers;
	}

	@Override /* RestMethod */
	public String summary() {
		return summary;
	}

	@Override /* RestMethod */
	public String[] produces() {
		return produces;
	}

	@Override /* RestMethod */
	public String[] consumes() {
		return consumes;
	}

	@Override /* RestMethod */
	public MethodSwagger swagger() {
		return swagger;
	}
}
