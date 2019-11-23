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
package org.apache.juneau.microservice.jetty;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.microservice.console.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;
import org.eclipse.jetty.server.*;

/**
 * Builder for {@link JettyMicroservice} class.
 *
 * <p>
 * Instances of this class are created using {@link JettyMicroservice#create()}.
 */
public class JettyMicroserviceBuilder extends MicroserviceBuilder {

	String jettyXml;
	int[] ports;
	Boolean jettyXmlResolveVars;
	Map<String,Servlet> servlets = new LinkedHashMap<>();
	Map<String,Object> servletAttributes = new LinkedHashMap<>();
	JettyMicroserviceListener listener;
	JettyServerFactory factory;

	/**
	 * Constructor.
	 */
	protected JettyMicroserviceBuilder() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The builder to copy settings from.
	 */
	protected JettyMicroserviceBuilder(JettyMicroserviceBuilder copyFrom) {
		super(copyFrom);
		this.jettyXml = copyFrom.jettyXml;
		this.ports = copyFrom.ports;
		this.jettyXmlResolveVars = copyFrom.jettyXmlResolveVars;
		this.servlets = new LinkedHashMap<>(copyFrom.servlets);
		this.servletAttributes = new LinkedHashMap<>(copyFrom.servletAttributes);
		this.listener = copyFrom.listener;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder copy() {
		return new JettyMicroserviceBuilder(this);
	}

	/**
	 * Specifies the contents or location of the <c>jetty.xml</c> file used by the Jetty server.
	 *
	 * <p>
	 * If you do not specify this value, it is pulled from the following in the specified order:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<c>Jetty/config</c> setting in the config file.
	 * 		<c>Jetty-Config</c> setting in the manifest file.
	 * </ul>
	 *
	 * <p>
	 * By default, we look for the <c>jetty.xml</c> file in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li><c>jetty.xml</c> in home directory.
	 * 	<li><c>files/jetty.xml</c> in home directory.
	 * 	<li><c>/jetty.xml</c> in classpath.
	 * 	<li><c>/files/jetty.xml</c> in classpath.
	 * </ul>
	 *
	 * @param jettyXml
	 * 	The contents or location of the file.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Relative path to file on file system or classpath.
	 * 		<li>{@link File} - File on file system.
	 * 		<li>{@link InputStream} - Raw contents as <c>UTF-8</c> encoded stream.
	 * 		<li>{@link Reader} - Raw contents.
	 * 	</ul>
	 *
	 * @param resolveVars
	 * 	If <jk>true</jk>, SVL variables in the file will automatically be resolved.
	 * @return This object (for method chaining).
	 * @throws IOException Thrown by underlying stream.
	 */
	public JettyMicroserviceBuilder jettyXml(Object jettyXml, boolean resolveVars) throws IOException {
		if (jettyXml instanceof String)
			this.jettyXml = IOUtils.read(resolveFile(jettyXml.toString()));
		else if (jettyXml instanceof File)
			this.jettyXml = IOUtils.read((File)jettyXml);
		else if (jettyXml instanceof InputStream)
			this.jettyXml = IOUtils.read((InputStream)jettyXml);
		else if (jettyXml instanceof Reader)
			this.jettyXml = IOUtils.read((Reader)jettyXml);
		else
			throw new FormattedRuntimeException("Invalid object type passed to jettyXml(Object)", jettyXml == null ? null : jettyXml.getClass().getName());
		this.jettyXmlResolveVars = resolveVars;
		return this;
	}

	/**
	 * Specifies the ports to use for the web server.
	 *
	 * <p>
	 * You can specify multiple ports.  The first available will be used.  <js>'0'</js> indicates to try a random port.
	 * The resulting available port gets set as the system property <js>"availablePort"</js> which can be referenced in the
	 * <c>jetty.xml</c> file as <js>"$S{availablePort}"</js> (assuming resolveVars is enabled).
	 *
	 * <p>
	 * If you do not specify this value, it is pulled from the following in the specified order:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<c>Jetty/port</c> setting in the config file.
	 * 	<li>
	 * 		<c>Jetty-Port</c> setting in the manifest file.
	 * 	<li>
	 * 		<c>8000</c>
	 * </ul>
	 *
	 * Jetty/port", mf.getWithDefault("Jetty-Port", new int[]{8000}
	 * @param ports The ports to use for the web server.
	 * @return This object (for method chaining).
	 */
	public JettyMicroserviceBuilder ports(int...ports) {
		this.ports = ports;
		return this;
	}

	/**
	 * Adds a servlet to the servlet container.
	 *
	 * <p>
	 * This method can only be used with servlets with no-arg constructors.
	 * <br>The path is pulled from the {@link Rest#path()} annotation.
	 *
	 * @param c The servlet to add to the servlet container.
	 * @return This object (for method chaining).
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public JettyMicroserviceBuilder servlet(Class<? extends RestServlet> c) throws ExecutableException {
		RestServlet rs;
		try {
			rs = c.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ExecutableException(e);
		}
		return servlet(rs, '/' + rs.getPath());
	}

	/**
	 * Adds a servlet to the servlet container.
	 *
	 * <p>
	 * This method can only be used with servlets with no-arg constructors.
	 *
	 * @param c The servlet to add to the servlet container.
	 * @param path The servlet path spec.
	 * @return This object (for method chaining).
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public JettyMicroserviceBuilder servlet(Class<? extends Servlet> c, String path) throws ExecutableException {
		try {
			return servlet(c.newInstance(), path);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ExecutableException(e);
		}
	}

	/**
	 * Adds a servlet instance to the servlet container.
	 *
	 * @param servlet The servlet to add to the servlet container.
	 * @param path The servlet path spec.
	 * @return This object (for method chaining).
	 */
	public JettyMicroserviceBuilder servlet(Servlet servlet, String path) {
		servlets.put(path, servlet);
		return this;
	}

	/**
	 * Adds a set of servlets to the servlet container.
	 *
	 * @param servlets
	 * 	A map of servlets to add to the servlet container.
	 * 	<br>Keys are path specs for the servlet.
	 * @return This object (for method chaining).
	 */
	public JettyMicroserviceBuilder servlets(Map<String,Servlet> servlets) {
		if (servlets != null)
			this.servlets.putAll(servlets);
		return this;
	}

	/**
	 * Adds a servlet attribute to the servlet container.
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return This object (for method chaining).
	 */
	public JettyMicroserviceBuilder servletAttribute(String name, Object value) {
		this.servletAttributes.put(name, value);
		return this;
	}

	/**
	 * Adds a set of servlet attributes to the servlet container.
	 *
	 * @param values The map of attributes.
	 * @return This object (for method chaining).
	 */
	public JettyMicroserviceBuilder servletAttribute(Map<String,Object> values) {
		if (values != null)
			this.servletAttributes.putAll(values);
		return this;
	}

	/**
	 * Specifies the factory to use for creating the Jetty {@link Server} instance.
	 *
	 * <p>
	 * If not specified, uses {@link BasicJettyServerFactory}.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public JettyMicroserviceBuilder jettyServerFactory(JettyServerFactory value) {
		this.factory = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inherited from MicroserviceBuilder
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* MicroserviceBuilder */
	public JettyMicroservice build() throws Exception {
		return new JettyMicroservice(this);
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder args(Args args) {
		super.args(args);
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder args(String...args) {
		super.args(args);
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder manifest(Object manifest) throws IOException {
		super.manifest(manifest);
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder logger(Logger logger) {
		super.logger(logger);
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder logConfig(LogConfig logConfig) {
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder config(Config config) {
		super.config(config);
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder configName(String configName) {
		super.configName(configName);
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder configStore(ConfigStore configStore) {
		super.configStore(configStore);
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder consoleEnabled(boolean consoleEnabled) {
		super.consoleEnabled(consoleEnabled);
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder consoleCommands(ConsoleCommand...consoleCommands) {
		super.consoleCommands(consoleCommands);
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder console(Scanner consoleReader, PrintWriter consoleWriter) {
		super.console(consoleReader, consoleWriter);
		return this;
	}

	@Override /* MicroserviceBuilder */
	@SuppressWarnings("unchecked")
	public JettyMicroserviceBuilder vars(Class<? extends Var>...vars) {
		super.vars(vars);
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder varContext(String name, Object object) {
		super.varContext(name, object);
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder workingDir(File path) {
		super.workingDir(path);
		return this;
	}

	@Override /* MicroserviceBuilder */
	public JettyMicroserviceBuilder workingDir(String path) {
		super.workingDir(path);
		return this;
	}

	/**
	 * Registers an event listener for this microservice.
	 *
	 * @param listener An event listener for this microservice.
	 * @return This object (for method chaining).
	 */
	public JettyMicroserviceBuilder listener(JettyMicroserviceListener listener) {
		super.listener(listener);
		this.listener = listener;
		return this;
	}
}
