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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.CollectionUtils.map;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.objecttools.*;

/**
 * This is the root document object for the Swagger 2.0 API specification.
 *
 * <p>
 * The Swagger Object is the root document that describes an entire API. It contains metadata about the API,
 * available paths and operations, parameters, responses, security definitions, and other information. This is
 * the Swagger 2.0 specification (predecessor to OpenAPI 3.0).
 *
 * <h5 class='section'>Swagger Specification:</h5>
 * <p>
 * The Swagger Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>swagger</c> (string, REQUIRED) - The Swagger Specification version (must be <js>"2.0"</js>)
 * 	<li><c>info</c> ({@link Info}, REQUIRED) - Provides metadata about the API
 * 	<li><c>host</c> (string) - The host (name or IP) serving the API
 * 	<li><c>basePath</c> (string) - The base path on which the API is served (relative to host)
 * 	<li><c>schemes</c> (array of string) - The transfer protocols of the API (e.g., <js>"http"</js>, <js>"https"</js>)
 * 	<li><c>consumes</c> (array of string) - A list of MIME types the APIs can consume
 * 	<li><c>produces</c> (array of string) - A list of MIME types the APIs can produce
 * 	<li><c>paths</c> (map of {@link OperationMap}, REQUIRED) - The available paths and operations for the API
 * 	<li><c>definitions</c> (map of {@link SchemaInfo}) - Schema definitions that can be referenced
 * 	<li><c>parameters</c> (map of {@link ParameterInfo}) - Parameters definitions that can be referenced
 * 	<li><c>responses</c> (map of {@link ResponseInfo}) - Response definitions that can be referenced
 * 	<li><c>securityDefinitions</c> (map of {@link SecurityScheme}) - Security scheme definitions
 * 	<li><c>security</c> (array of map) - Security requirements applied to all operations
 * 	<li><c>tags</c> (array of {@link Tag}) - A list of tags used by the specification with additional metadata
 * 	<li><c>externalDocs</c> ({@link ExternalDocumentation}) - Additional external documentation
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a Swagger document</jc>
 * 	Swagger <jv>doc</jv> = <jk>new</jk> Swagger()
 * 		.setSwagger(<js>"2.0"</js>)
 * 		.setInfo(
 * 			<jk>new</jk> Info()
 * 				.setTitle(<js>"Pet Store API"</js>)
 * 				.setVersion(<js>"1.0.0"</js>)
 * 		)
 * 		.setHost(<js>"petstore.swagger.io"</js>)
 * 		.setBasePath(<js>"/v2"</js>)
 * 		.setSchemes(<js>"https"</js>)
 * 		.addPath(<js>"/pets"</js>, <js>"get"</js>,
 * 			<jk>new</jk> Operation()
 * 				.setSummary(<js>"List all pets"</js>)
 * 				.addResponse(<js>"200"</js>, <jk>new</jk> ResponseInfo(<js>"Success"</js>))
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/">Swagger 2.0 Specification</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/2-0/basic-structure/">Swagger Basic Structure</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
public class Swagger extends SwaggerElement {

	/** Represents a null swagger */
	public static final Swagger NULL = new Swagger();

	private static final Comparator<String> PATH_COMPARATOR = (o1, o2) -> o1.replace('{', '@').compareTo(o2.replace('{', '@'));

	private String
		swagger = "2.0",  // NOSONAR - Intentional naming.
		host,
		basePath;
	private Info info;
	private ExternalDocumentation externalDocs;
	private Set<String> schemes;
	private Set<MediaType>
		consumes,
		produces;
	private Set<Tag> tags;
	private List<Map<String,List<String>>> security;
	private Map<String,JsonMap> definitions;
	private Map<String,ParameterInfo> parameters;
	private Map<String,ResponseInfo> responses;
	private Map<String,SecurityScheme> securityDefinitions;
	private Map<String,OperationMap> paths;

	/**
	 * Default constructor.
	 */
	public Swagger() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Swagger(Swagger copyFrom) {
		super(copyFrom);

		this.basePath = copyFrom.basePath;
		this.consumes = copyOf(copyFrom.consumes);
		this.externalDocs = copyFrom.externalDocs == null ? null : copyFrom.externalDocs.copy();
		this.host = copyFrom.host;
		this.info = copyFrom.info == null ? null : copyFrom.info.copy();
		this.produces = copyOf(copyFrom.produces);
		this.schemes = copyOf(copyFrom.schemes);
		this.swagger = copyFrom.swagger;

		// TODO - Definitions are not deep copied, so they should not contain references.
		this.definitions = copyOf(copyFrom.definitions, JsonMap::new);

		this.paths = copyOf(copyFrom.paths, v -> {
			var m = new OperationMap();
			v.forEach((k2,v2) -> m.put(k2, v2.copy()));
			return m;
		});

		this.parameters = copyOf(copyFrom.parameters, ParameterInfo::copy);
		this.responses = copyOf(copyFrom.responses, ResponseInfo::copy);
		this.securityDefinitions = copyOf(copyFrom.securityDefinitions, SecurityScheme::copy);

		this.security = copyOf(copyFrom.security, x -> {
			Map<String,List<String>> m2 = map();
			x.forEach((k,v) -> m2.put(k, copyOf(v)));
			return m2;
		});

		this.tags = copyOf(copyFrom.tags, x -> x.copy());
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Swagger copy() {
		return new Swagger(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>basePath</property>.
	 *
	 * <p>
	 * The base path on which the API is served, which is relative to the <c>host</c>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Bean property setter:  <property>basePath</property>.
	 *
	 * <p>
	 * The base path on which the API is served, which is relative to the <c>host</c>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>If it is not included, the API is served directly under the <c>host</c>.
	 * 	<br>The value MUST start with a leading slash (/).
	 * 	<br>The <c>basePath</c> does not support <a class="doclink" href="https://swagger.io/specification/v2#pathTemplating">path templating</a>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setBasePath(String value) {
		basePath = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can consume.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<MediaType> getConsumes() {
		return consumes;
	}

	/**
	 * Bean property setter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can consume.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Value MUST be as described under <a class="doclink" href="https://swagger.io/specification#mimeTypes">Swagger Mime Types</a>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setConsumes(Collection<MediaType> value) {
		consumes = setFrom(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can consume.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Values MUST be as described under <a class="doclink" href="https://swagger.io/specification#mimeTypes">Swagger Mime Types</a>.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addConsumes(MediaType...values) {
		consumes = setBuilder(consumes).sparse().add(values).build();
		return this;
	}

	/**
	 * Bean property appender:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can consume.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addConsumes(Collection<MediaType> values) {
		consumes = setBuilder(consumes).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can consume.
	 *
	 * @param value
	 * 	The values to set on this property.
	 * @return This object.
	 */
	public Swagger setConsumes(MediaType...value) {
		setConsumes(setBuilder(MediaType.class).sparse().add(value).build());
		return this;
	}

	/**
	 * Bean property getter:  <property>definitions</property>.
	 *
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,JsonMap> getDefinitions() {
		return definitions;
	}

	/**
	 * Bean property setter:  <property>definitions</property>.
	 *
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setDefinitions(Map<String,JsonMap> value) {
		definitions = copyOf(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>definitions</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>definitions</property> property.
	 *
	 * @param name A definition name.  Must not be <jk>null</jk>.
	 * @param schema The schema that the name defines.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addDefinition(String name, JsonMap schema) {
		assertArgNotNull("name", name);
		assertArgNotNull("schema", schema);
		definitions = mapBuilder(definitions).sparse().add(name, schema).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public ExternalDocumentation getExternalDocs() {
		return externalDocs;
	}

	/**
	 * Bean property setter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>host</property>.
	 *
	 * <p>
	 * The host (name or IP) serving the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Bean property setter:  <property>host</property>.
	 *
	 * <p>
	 * The host (name or IP) serving the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>This MUST be the host only and does not include the scheme nor sub-paths.
	 * 	<br>It MAY include a port.
	 * 	<br>If the host is not included, the host serving the documentation is to be used (including the port).
	 * 	<br>The host does not support <a class="doclink" href="https://swagger.io/specification/v2#pathTemplating">path templating</a>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setHost(String value) {
		host = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>info</property>.
	 *
	 * <p>
	 * Provides metadata about the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Info getInfo() {
		return info;
	}

	/**
	 * Bean property setter:  <property>info</property>.
	 *
	 * <p>
	 * Provides metadata about the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setInfo(Info value) {
		info = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>parameters</property>.
	 *
	 * <p>
	 * An object to hold parameters that can be used across operations.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,ParameterInfo> getParameters() {
		return parameters;
	}

	/**
	 * Bean property setter:  <property>parameters</property>.
	 *
	 * <p>
	 * An object to hold parameters that can be used across operations.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setParameters(Map<String,ParameterInfo> value) {
		parameters = copyOf(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>parameters</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>parameter</property> property.
	 *
	 * @param name The parameter name.  Must not be <jk>null</jk>.
	 * @param parameter The parameter definition.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addParameter(String name, ParameterInfo parameter) {
		assertArgNotNull("name", name);
		assertArgNotNull("parameter", parameter);
		parameters = mapBuilder(parameters).sparse().add(name, parameter).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>paths</property>.
	 *
	 * <p>
	 * The available paths and operations for the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,OperationMap> getPaths() {
		return paths;
	}

	/**
	 * Bean property setter:  <property>paths</property>.
	 *
	 * <p>
	 * The available paths and operations for the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setPaths(Map<String,OperationMap> value) {
		paths = mapBuilder(String.class,OperationMap.class).sparse().sorted(PATH_COMPARATOR).addAll(value).build();
		return this;
	}

	/**
	 * Bean property appender:  <property>paths</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>paths</property> property.
	 *
	 * @param path The path template.  Must not be <jk>null</jk>.
	 * @param methodName The HTTP method name.  Must not be <jk>null</jk>.
	 * @param operation The operation that describes the path.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addPath(String path, String methodName, Operation operation) {
		assertArgNotNull("path", path);
		assertArgNotNull("methodName", methodName);
		assertArgNotNull("operation", operation);
		if (paths == null)
			paths = new TreeMap<>(PATH_COMPARATOR);
		getPaths().computeIfAbsent(path, k -> new OperationMap()).put(methodName, operation);
		return this;
	}

	/**
	 * Bean property getter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<MediaType> getProduces() {
		return produces;
	}

	/**
	 * Bean property setter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Value MUST be as described under <a class="doclink" href="https://swagger.io/specification#mimeTypes">Swagger Mime Types</a>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setProduces(Collection<MediaType> value) {
		produces = setFrom(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>produces</property> property.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Value MUST be as described under <a class="doclink" href="https://swagger.io/specification#mimeTypes">Swagger Mime Types</a>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger addProduces(MediaType...values) {
		produces = setBuilder(produces).sparse().add(values).build();
		return this;
	}

	/**
	 * Bean property appender:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Value MUST be as described under <a class="doclink" href="https://swagger.io/specification#mimeTypes">Swagger Mime Types</a>.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addProduces(Collection<MediaType> values) {
		produces = setBuilder(produces).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Swagger setProduces(MediaType...value) {
		setProduces(setBuilder(MediaType.class).sparse().add(value).build());
		return this;
	}

	/**
	 * Bean property getter:  <property>responses</property>.
	 *
	 * <p>
	 * An object to hold responses that can be used across operations.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,ResponseInfo> getResponses() {
		return responses;
	}

	/**
	 * Bean property setter:  <property>responses</property>.
	 *
	 * <p>
	 * An object to hold responses that can be used across operations.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setResponses(Map<String,ResponseInfo> value) {
		responses = copyOf(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>responses</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>responses</property> property.
	 *
	 * @param name The response name.  Must not be <jk>null</jk>.
	 * @param response The response definition.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addResponse(String name, ResponseInfo response) {
		assertArgNotNull("name", name);
		assertArgNotNull("response", response);
		responses = mapBuilder(responses).sparse().add(name, response).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol of the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<String> getSchemes() {
		return schemes;
	}

	/**
	 * Bean property setter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol of the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"http"</js>
	 * 		<li><js>"https"</js>
	 * 		<li><js>"ws"</js>
	 * 		<li><js>"wss"</js>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setSchemes(Collection<String> value) {
		schemes = setFrom(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol of the API.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"http"</js>
	 * 		<li><js>"https"</js>
	 * 		<li><js>"ws"</js>
	 * 		<li><js>"wss"</js>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addSchemes(String...values) {
		schemes = setBuilder(schemes).sparse().add(values).build();
		return this;
	}

	/**
	 * Bean property appender:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol of the API.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"http"</js>
	 * 		<li><js>"https"</js>
	 * 		<li><js>"ws"</js>
	 * 		<li><js>"wss"</js>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addSchemes(Collection<String> values) {
		schemes = setBuilder(schemes).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol of the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Strings can be JSON arrays.
	 * @return This object.
	 */
	public Swagger setSchemes(String...value) {
		setSchemes(setBuilder(String.class).sparse().addJson(value).build());
		return this;
	}

	/**
	 * Bean property getter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Map<String,List<String>>> getSecurity() {
		return security;
	}

	/**
	 * Bean property setter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setSecurity(Collection<Map<String,List<String>>> value) {
		security = listFrom(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>security</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>securityDefinitions</property> property.
	 *
	 * @param scheme The security scheme that applies to this operation  Must not be <jk>null</jk>.
	 * @param alternatives
	 * 	The list of values describes alternative security schemes that can be used (that is, there is a logical OR between the security requirements).
	 * @return This object.
	 */
	public Swagger addSecurity(String scheme, String...alternatives) {
		assertArgNotNull("scheme", scheme);
		Map<String,List<String>> m = map();
		m.put(scheme, alist(alternatives));
		security = listBuilder(security).sparse().addAll(Collections.singleton(m)).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addSecurity(Collection<Map<String,List<String>>> values) {
		security = listBuilder(security).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>securityDefinitions</property>.
	 *
	 * <p>
	 * Security scheme definitions that can be used across the specification.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,SecurityScheme> getSecurityDefinitions() {
		return securityDefinitions;
	}

	/**
	 * Bean property setter:  <property>securityDefinitions</property>.
	 *
	 * <p>
	 * Security scheme definitions that can be used across the specification.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setSecurityDefinitions(Map<String,SecurityScheme> value) {
		securityDefinitions = copyOf(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>securityDefinitions</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>securityDefinitions</property> property.
	 *
	 * @param name A security name.  Must not be <jk>null</jk>.
	 * @param securityScheme A security schema.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addSecurityDefinition(String name, SecurityScheme securityScheme) {
		assertArgNotNull("name", name);
		assertArgNotNull("securityScheme", securityScheme);
		securityDefinitions = mapBuilder(securityDefinitions).sparse().add(name, securityScheme).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>swagger</property>.
	 *
	 * <p>
	 * Specifies the Swagger Specification version being used.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getSwagger() {
		return swagger;
	}

	/**
	 * Bean property setter:  <property>swagger</property>.
	 *
	 * <p>
	 * Specifies the Swagger Specification version being used.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setSwagger(String value) {
		swagger = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<Tag> getTags() {
		return tags;
	}

	/**
	 * Bean property setter:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The order of the tags can be used to reflect on their order by the parsing tools.
	 * 	<br>Not all tags that are used by the <a class="doclink" href="https://swagger.io/specification/v2#operationObject">Operation Object</a> must be declared.
	 * 	<br>The tags that are not declared may be organized randomly or based on the tools' logic.
	 * 	<br>Each tag name in the list MUST be unique.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger setTags(Collection<Tag> value) {
		tags = setFrom(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The order of the tags can be used to reflect on their order by the parsing tools.
	 * 	<br>Not all tags that are used by the <a class="doclink" href="https://swagger.io/specification/v2#operationObject">Operation Object</a> must be declared.
	 * 	<br>The tags that are not declared may be organized randomly or based on the tools' logic.
	 * 	<br>Each tag name in the list MUST be unique.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger setTags(Tag...value) {
		setTags(setBuilder(Tag.class).sparse().add(value).build());
		return this;
	}

	/**
	 * Bean property appender:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>The order of the tags can be used to reflect on their order by the parsing tools.
	 * 	<br>Not all tags that are used by the <a class="doclink" href="https://swagger.io/specification/v2#operationObject">Operation Object</a> must be declared.
	 * 	<br>The tags that are not declared may be organized randomly or based on the tools' logic.
	 * 	<br>Each tag name in the list MUST be unique.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addTags(Tag...values) {
		tags = setBuilder(tags).sparse().add(values).build();
		return this;
	}

	/**
	 * Bean property appender:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>The order of the tags can be used to reflect on their order by the parsing tools.
	 * 	<br>Not all tags that are used by the <a class="doclink" href="https://swagger.io/specification/v2#operationObject">Operation Object</a> must be declared.
	 * 	<br>The tags that are not declared may be organized randomly or based on the tools' logic.
	 * 	<br>Each tag name in the list MUST be unique.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addTags(Collection<Tag> values) {
		tags = setBuilder(tags).sparse().addAll(values).build();
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Convenience methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Shortcut for calling <c>getPaths().get(path);</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).  Must not be <jk>null</jk>.
	 * @return The operation map for the specified path, or <jk>null</jk> if it doesn't exist.
	 */
	public OperationMap getPath(String path) {
		assertArgNotNull("path", path);
		return opt(getPaths()).map(x -> x.get(path)).orElse(null);
	}

	/**
	 * Shortcut for calling <c>getPaths().get(path).get(operation);</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).  Must not be <jk>null</jk>.
	 * @param operation The HTTP operation (e.g. <js>"get"</js>).  Must not be <jk>null</jk>.
	 * @return The operation for the specified path and operation id, or <jk>null</jk> if it doesn't exist.
	 */
	public Operation getOperation(String path, String operation) {
		assertArgNotNull("path", path);
		assertArgNotNull("operation", operation);
		return opt(getPath(path)).map(x -> x.get(operation)).orElse(null);
	}

	/**
	 * Shortcut for calling <c>getPaths().get(path).get(operation).getResponse(status);</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).  Must not be <jk>null</jk>.
	 * @param operation The HTTP operation (e.g. <js>"get"</js>).  Must not be <jk>null</jk>.
	 * @param status The HTTP response status (e.g. <js>"200"</js>).  Must not be <jk>null</jk>.
	 * @return The operation for the specified path and operation id, or <jk>null</jk> if it doesn't exist.
	 */
	public ResponseInfo getResponseInfo(String path, String operation, String status) {
		assertArgNotNull("path", path);
		assertArgNotNull("operation", operation);
		assertArgNotNull("status", status);
		return opt(getPath(path)).map(x -> x.get(operation)).map(x -> x.getResponse(status)).orElse(null);
	}

	/**
	 * Shortcut for calling <c>getPaths().get(path).get(operation).getResponse(status);</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).
	 * @param operation The HTTP operation (e.g. <js>"get"</js>).
	 * @param status The HTTP response status (e.g. <js>"200"</js>).
	 * @return The operation for the specified path and operation id, or <jk>null</jk> if it doesn't exist.
	 */
	public ResponseInfo getResponseInfo(String path, String operation, int status) {
		return getResponseInfo(path, operation, String.valueOf(status));
	}

	/**
	 * Convenience method for calling <c>getPath(path).get(method).getParameter(in,name);</c>
	 *
	 * @param path The HTTP path.  Must not be <jk>null</jk>.
	 * @param method The HTTP method.  Must not be <jk>null</jk>.
	 * @param in The parameter type.  Must not be <jk>null</jk>.
	 * @param name The parameter name.  Can be <jk>null</jk> for parameter type <c>body</c>.
	 * @return The parameter information or <jk>null</jk> if not found.
	 */
	public ParameterInfo getParameterInfo(String path, String method, String in, String name) {
		assertArgNotNull("path", path);
		assertArgNotNull("method", method);
		assertArgNotNull("in", in);
		return opt(getPath(path)).map(x -> x.get(method)).map(x -> x.getParameter(in, name)).orElse(null);
	}

	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "basePath" -> toType(getBasePath(), type);
			case "consumes" -> toType(getConsumes(), type);
			case "definitions" -> toType(getDefinitions(), type);
			case "externalDocs" -> toType(getExternalDocs(), type);
			case "host" -> toType(getHost(), type);
			case "info" -> toType(getInfo(), type);
			case "parameters" -> toType(getParameters(), type);
			case "paths" -> toType(getPaths(), type);
			case "produces" -> toType(getProduces(), type);
			case "responses" -> toType(getResponses(), type);
			case "schemes" -> toType(getSchemes(), type);
			case "security" -> toType(getSecurity(), type);
			case "securityDefinitions" -> toType(getSecurityDefinitions(), type);
			case "swagger" -> toType(getSwagger(), type);
			case "tags" -> toType(getTags(), type);
			default -> super.get(property, type);
		};
	}

	@SuppressWarnings("rawtypes")
	@Override /* Overridden from SwaggerElement */
	public Swagger set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "basePath" -> setBasePath(Utils.s(value));
			case "consumes" -> setConsumes(listBuilder(MediaType.class).sparse().addAny(value).build());
			case "definitions" -> setDefinitions(mapBuilder(String.class,JsonMap.class).sparse().addAny(value).build());
			case "externalDocs" -> setExternalDocs(toType(value, ExternalDocumentation.class));
			case "host" -> setHost(Utils.s(value));
			case "info" -> setInfo(toType(value, Info.class));
			case "parameters" -> setParameters(mapBuilder(String.class,ParameterInfo.class).sparse().addAny(value).build());
			case "paths" -> setPaths(mapBuilder(String.class,OperationMap.class).sparse().addAny(value).build());
			case "produces" -> setProduces(listBuilder(MediaType.class).sparse().addAny(value).build());
			case "responses" -> setResponses(mapBuilder(String.class,ResponseInfo.class).sparse().addAny(value).build());
			case "schemes" -> setSchemes(listBuilder(String.class).sparse().addAny(value).build());
			case "security" -> setSecurity((List)listBuilder(Map.class,String.class,List.class,String.class).sparse().addAny(value).build());
			case "securityDefinitions" -> setSecurityDefinitions(mapBuilder(String.class,SecurityScheme.class).sparse().addAny(value).build());
			case "swagger" -> setSwagger(Utils.s(value));
			case "tags" -> setTags(listBuilder(Tag.class).sparse().addAny(value).build());
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(basePath != null, "basePath")
			.addIf(consumes != null, "consumes")
			.addIf(definitions != null, "definitions")
			.addIf(externalDocs != null, "externalDocs")
			.addIf(host != null, "host")
			.addIf(info != null, "info")
			.addIf(parameters != null, "parameters")
			.addIf(paths != null, "paths")
			.addIf(produces != null, "produces")
			.addIf(responses != null, "responses")
			.addIf(schemes != null, "schemes")
			.addIf(security != null, "security")
			.addIf(securityDefinitions != null, "securityDefinitions")
			.addIf(swagger != null, "swagger")
			.addIf(tags != null, "tags")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	/**
	 * A synonym of {@link #toString()}.
	 * @return This object serialized as JSON.
	 */
	public String asJson() {
		return toString();
	}

	@Override /* Overridden from Object */
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}

	/**
	 * Resolves a <js>"$ref"</js> tags to nodes in this swagger document.
	 *
	 * @param <T> The class to convert the reference to.
	 * @param ref The ref tag value.  Must not be <jk>null</jk> or blank.
	 * @param c The class to convert the reference to.  Must not be <jk>null</jk>.
	 * @return The referenced node, or <jk>null</jk> if not found.
	 */
	public <T> T findRef(String ref, Class<T> c) {
		assertArgNotNullOrBlank("ref", ref);
		assertArgNotNull("c", c);
		if (! ref.startsWith("#/"))
			throw new BasicRuntimeException("Unsupported reference:  ''{0}''", ref);
		try {
			return new ObjectRest(this).get(ref.substring(1), c);
		} catch (Exception e) {
			throw new BeanRuntimeException(e, c, "Reference ''{0}'' could not be converted to type ''{1}''.", ref, className(c));
		}
	}

	/**
	 * Sets strict mode on this bean.
	 *
	 * @return This object.
	 */
	@Override
	public Swagger strict() {
		super.strict();
		return this;
	}

	/**
	 * Sets strict mode on this bean.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> (interpreted as <jk>false</jk>).
	 * @return This object.
	 */
	@Override
	public Swagger strict(Object value) {
		super.strict(value);
		return this;
	}
}