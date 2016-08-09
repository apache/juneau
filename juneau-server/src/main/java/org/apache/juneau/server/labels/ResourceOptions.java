/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server.labels;

import static javax.servlet.http.HttpServletResponse.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.server.*;
import org.apache.juneau.server.annotation.*;

/**
 * Default POJO bean used for generating OPTIONS page results.
 * <p>
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Bean(properties={"label","description","className","methods","children","consumes","produces","guards","transforms","converters"})
public class ResourceOptions {
	private String label, description;
	private String className;
	private Collection<MethodDescription> methods;
	private ChildResourceDescriptions children;
	private Collection<String> consumes, produces;
	private Collection<String> guards;
	private Collection<String> transforms;
	private Collection<String> converters;

	/**
	 * Constructor.
	 * @param servlet The servlet that this bean describes.
	 * @param req The HTTP servlet request.
	 */
	public ResourceOptions(RestServlet servlet, RestRequest req) {
		try {
			setClassName(servlet.getClass().getName());
			setLabel(servlet.getLabel(req));
			setDescription(servlet.getDescription(req));
			setMethods(servlet.getMethodDescriptions(req));
			setConsumes(servlet.getSupportedAcceptTypes());
			setProduces(servlet.getSupportedContentTypes());
			setChildren(new ChildResourceDescriptions(servlet, req));
			setGuards(servlet.getGuards());
			setTransforms(servlet.getTransforms());
			setConverters(servlet.getConverters());
		} catch (RestServletException e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Bean constructor.
	 */
	public ResourceOptions() {}

	/**
	 * Returns the label of the REST resource.
	 * @return The current bean property value.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the label of the REST resource.
	 * @param label The new bean property value.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setLabel(String label) {
		this.label = label;
		return this;
	}

	/**
	 * Returns the description of the REST resource.
	 * @return The current bean property value.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description of the REST resource.
	 * @param description The new bean property value.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Returns the class name of the REST resource.
	 * @return The current bean property value.
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Sets the class name of the REST resource.
	 * @param className The new bean property value.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setClassName(String className) {
		this.className = className;
		return this;
	}

	/**
	 * Returns the methods provided on this REST resource.
	 * @return The current bean property value.
	 */
	public Collection<MethodDescription> getMethods() {
		return methods;
	}

	/**
	 * Sets the methods provided on this REST resource.
	 * @param methods The new bean property value.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setMethods(Collection<MethodDescription> methods) {
		List<MethodDescription> l = new ArrayList<MethodDescription>(methods);
		Collections.sort(l);
		this.methods = l;
		return this;
	}

	/**
	 * Returns the list of allowable <code>Accept</code> header values on requests.
	 * @return The current bean property value.
	 */
	public Collection<String> getConsumes() {
		return consumes;
	}

	/**
	 * Sets the list of allowable <code>Accept</code> header values on requests.
	 * @param consumes The new bean property value.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setConsumes(Collection<String> consumes) {
		this.consumes = consumes;
		return this;
	}

	/**
	 * Returns the list of allowable <code>Content-Type</code> header values on requests.
	 * @return The current bean property value.
	 */
	public Collection<String> getProduces() {
		return produces;
	}

	/**
	 * Sets the list of allowable <code>Content-Type</code> header values on requests.
	 * @param produces The new bean property value.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setProduces(Collection<String> produces) {
		this.produces = produces;
		return this;
	}

	/**
	 * Returns the description of child resources with this resource (typically through {@link RestResource#children()} annotation).
	 * @return The description of child resources of this resource.
	 */
	public ChildResourceDescriptions getChildren() {
		return children;
	}

	/**
	 * Sets the child resource descriptions associated with this resource.
	 * @param children The child resource descriptions.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setChildren(ChildResourceDescriptions children) {
		this.children = children;
		return this;
	}


	/**
	 * Returns the list of class-wide guards associated with this resource (typically through {@link RestResource#guards()} annotation).
	 * @return The simple class names of the guards.
	 */
	public Collection<String> getGuards() {
		return guards;
	}

	/**
	 * Sets the simple class names of the guards associated with this resource.
	 * @param guards The simple class names of the guards.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setGuards(Collection<String> guards) {
		this.guards = guards;
		return this;
	}

	/**
	 * Shortcut for calling {@link #setGuards(Collection)} from {@link RestGuard} instances.
	 * @param guards Instances of guards associated with this resource.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setGuards(RestGuard[] guards) {
		Collection<String> l = new ArrayList<String>(guards.length);
		for (RestGuard g : guards)
			l.add(g.getClass().getSimpleName());
		return setGuards(l);
	}

	/**
	 * Returns the list of class-wide transforms associated with this resource (typically through {@link RestResource#transforms()} annotation).
	 * @return The simple class names of the transforms.
	 */
	public Collection<String> getTransforms() {
		return transforms;
	}

	/**
	 * Sets the simple class names of the transforms associated with this resource.
	 * @param transforms The simple class names of the transforms.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setTransforms(Collection<String> transforms) {
		this.transforms = transforms;
		return this;
	}

	/**
	 * Shortcut for calling {@link #setTransforms(Collection)} from {@link Class} instances.
	 * @param transforms Transform classes associated with this resource.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setTransforms(Class<?>[] transforms) {
		Collection<String> l = new ArrayList<String>(transforms.length);
		for (Class<?> c : transforms)
			l.add(c.getSimpleName());
		return setTransforms(l);
	}

	/**
	 * Returns the list of class-wide converters associated with this resource (typically through {@link RestResource#converters()} annotation).
	 * @return The simple class names of the converters.
	 */
	public Collection<String> getConverters() {
		return converters;
	}

	/**
	 * Sets the simple class names of the converters associated with this resource.
	 * @param converters The simple class names of the converters.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setConverters(Collection<String> converters) {
		this.converters = converters;
		return this;
	}

	/**
	 * Shortcut for calling {@link #setConverters(Collection)} from {@link RestConverter} instances.
	 * @param converters Converter classes associated with this resource.
	 * @return This object (for method chaining).
	 */
	public ResourceOptions setConverters(RestConverter[] converters) {
		Collection<String> l = new ArrayList<String>(converters.length);
		for (RestConverter c : converters)
			l.add(c.getClass().getSimpleName());
		return setConverters(l);
	}
}
