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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.bean.swagger.SwaggerCopyUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.marshall.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.http.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.objecttools.*;

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
@SuppressWarnings({
	"java:S1192" // Duplicated literals (argument/property names) read more clearly inline than as constants
})
public class Swagger extends SwaggerElement {

	private static interface MapOfStringLists extends Map<String,List<String>> {}

	/** Represents a null swagger */
	public static final Swagger NULL = new Swagger();

	private static final Comparator<String> PATH_COMPARATOR = (o1, o2) -> o1.replace('{', '@').compareTo(o2.replace('{', '@'));

	private String swagger = "2.0";
	private String host;
	private String basePath;
	private Info info;
	private ExternalDocumentation externalDocs;
	private Set<String> schemes = st();
	private Set<MediaType> consumes = st();
	private Set<MediaType> produces = st();
	private Set<Tag> tags = st();
	private List<Map<String,List<String>>> security = list();
	private Map<String,JsonMap> definitions = map();
	private Map<String,ParameterInfo> parameters = map();
	private Map<String,ResponseInfo> responses = map();
	private Map<String,SecurityScheme> securityDefinitions = map();

	private Map<String,OperationMap> paths = new TreeMap<>(PATH_COMPARATOR);

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
		this.consumes.addAll(copyOf(copyFrom.consumes));
		this.externalDocs = copyOf(copyFrom.externalDocs);
		this.host = copyFrom.host;
		this.info = copyOf(copyFrom.info);
		this.produces.addAll(copyOf(copyFrom.produces));
		this.schemes.addAll(copyOf(copyFrom.schemes));
		this.swagger = copyFrom.swagger;

		definitions.putAll(copyOf(copyFrom.definitions, JsonMap::new));

		copyFrom.paths.forEach((k, v) -> {
			var m = new OperationMap();
			v.forEach((k2, v2) -> m.put(k2, v2.copy()));
			paths.put(k, m);
		});

		parameters.putAll(copyOf(copyFrom.parameters, ParameterInfo::copy));
		responses.putAll(copyOf(copyFrom.responses, ResponseInfo::copy));
		securityDefinitions.putAll(copyOf(copyFrom.securityDefinitions, SecurityScheme::copy));

		copyFrom.security.forEach(x -> {
			Map<String,List<String>> m2 = map();
			x.forEach((k, v) -> m2.put(k, copyOf(v)));
			security.add(m2);
		});

		this.tags.addAll(copyOf(copyFrom.tags, Tag::copy));
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
		if (nn(values))
			consumes.addAll(values);
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
		addAllNn(consumes, values);
		return this;
	}

	/**
	 * Bean property appender:  <property>definitions</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>definitions</property> property.
	 *
	 * @param name A definition name.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param schema The schema that the name defines.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return This object.
	 */
	public Swagger addDefinition(String name, JsonMap schema) {
		reqnn("name", name);
		reqnn("schema", schema);
		definitions.put(name, schema);
		return this;
	}

	/**
	 * Bean property appender:  <property>parameters</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>parameter</property> property.
	 *
	 * @param name The parameter name.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param parameter The parameter definition.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return This object.
	 */
	public Swagger addParameter(String name, ParameterInfo parameter) {
		reqnn("name", name);
		reqnn("parameter", parameter);
		parameters.put(name, parameter);
		return this;
	}

	/**
	 * Bean property appender:  <property>paths</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>paths</property> property.
	 *
	 * @param path The path template.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param methodName The HTTP method name.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param operation The operation that describes the path.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return This object.
	 */
	public Swagger addPath(String path, String methodName, Operation operation) {
		reqnn("path", path);
		reqnn("methodName", methodName);
		reqnn("operation", operation);
		paths.computeIfAbsent(path, k -> new OperationMap()).put(methodName, operation);
		return this;
	}

	/**
	 * Bean property appender:  <property>paths</property>.
	 *
	 * <p>
	 * Adds a single operation for the specified path and HTTP method.
	 *
	 * <p>
	 * Functionally identical to {@link #addPath(String,String,Operation)}, but distinctly named so that this
	 * "add a single operation" call isn't confused with the OpenAPI 3 <c>OpenApi.addPath(String,PathItem)</c>
	 * method, which adds an entire <c>PathItem</c> (all methods for a path) at once.
	 *
	 * @param path The path template.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param method The HTTP method name.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param operation The operation that describes the path.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return This object.
	 */
	public Swagger addOperation(String path, String method, Operation operation) {
		return addPath(path, method, operation);
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
		if (nn(values))
			produces.addAll(values);
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
		addAllNn(produces, values);
		return this;
	}

	/**
	 * Bean property appender:  <property>responses</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>responses</property> property.
	 *
	 * @param name The response name.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param response The response definition.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return This object.
	 */
	public Swagger addResponse(String name, ResponseInfo response) {
		reqnn("name", name);
		reqnn("response", response);
		responses.put(name, response);
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
		if (nn(values))
			schemes.addAll(values);
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
		addAllNn(schemes, values);
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
		if (nn(values))
			security.addAll(values);
		return this;
	}

	/**
	 * Bean property appender:  <property>security</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>securityDefinitions</property> property.
	 *
	 * @param scheme The security scheme that applies to this operation  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param alternatives
	 * 	The list of values describes alternative security schemes that can be used (that is, there is a logical OR between the security requirements).
	 * @return This object.
	 */
	public Swagger addSecurity(String scheme, String...alternatives) {
		reqnn("scheme", scheme);
		Map<String,List<String>> m = map();
		m.put(scheme, l(alternatives));
		security.add(m);
		return this;
	}

	/**
	 * Bean property appender:  <property>securityDefinitions</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>securityDefinitions</property> property.
	 *
	 * @param name A security name.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param securityScheme A security schema.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return This object.
	 */
	public Swagger addSecurityDefinition(String name, SecurityScheme securityScheme) {
		reqnn("name", name);
		reqnn("securityScheme", securityScheme);
		securityDefinitions.put(name, securityScheme);
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
		if (nn(values))
			tags.addAll(values);
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
		addAllNn(tags, values);
		return this;
	}

	/**
	 * A synonym of {@link #toString()}.
	 * @return This object serialized as JSON.
	 */
	public String asJson() {
		return toString();
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Swagger copy() {
		return new Swagger(this);
	}

	/**
	 * Resolves a <js>"$ref"</js> tags to nodes in this swagger document.
	 *
	 * @param <T> The class to convert the reference to.
	 * @param ref The ref tag value.  Must not be <jk>null</jk> or blank, or an {@link IllegalArgumentException} is thrown.
	 * @param c The class to convert the reference to.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return The referenced node, or <jk>null</jk> if not found.
	 */
	public <T> T findRef(String ref, Class<T> c) {
		reqnb("ref", ref);
		reqnn("c", c);
		if (! ref.startsWith("#/"))
			throw rex("Unsupported reference:  '%s'", ref);
		try {
			return new PathTraversal(this).get(ref.substring(1), c);
		} catch (Exception e) {
			throw brex(e, c, "Reference '%s' could not be converted to type '%s'.", ref, cn(c));
		}
	}

	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		reqnn("property", property);
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

	/**
	 * Bean property getter:  <property>basePath</property>.
	 *
	 * <p>
	 * The base path on which the API is served, which is relative to the <c>host</c>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getBasePath() { return basePath; }

	/**
	 * Bean property getter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can consume.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<MediaType> getConsumes() { return nie(consumes); }

	/**
	 * Bean property getter:  <property>definitions</property>.
	 *
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,JsonMap> getDefinitions() { return nie(definitions); }

	/**
	 * Bean property getter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public ExternalDocumentation getExternalDocs() { return externalDocs; }

	/**
	 * Bean property getter:  <property>host</property>.
	 *
	 * <p>
	 * The host (name or IP) serving the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getHost() { return host; }

	/**
	 * Bean property getter:  <property>info</property>.
	 *
	 * <p>
	 * Provides metadata about the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Info getInfo() { return info; }

	/**
	 * Shortcut for calling <c>getPaths().get(path).get(operation);</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param operation The HTTP operation (e.g. <js>"get"</js>).  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return The operation for the specified path and operation id, or <jk>null</jk> if it doesn't exist.
	 */
	public Operation getOperation(String path, String operation) {
		reqnn("path", path);
		reqnn("operation", operation);
		return o(getPath(path)).map(x -> x.get(operation)).orElse(null);
	}

	/**
	 * Convenience method for calling <c>getPath(path).get(method).getParameter(in,name);</c>
	 *
	 * @param path The HTTP path.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param method The HTTP method.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param in The parameter type.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param name The parameter name.  Can be <jk>null</jk> for parameter type <c>body</c>.
	 * @return The parameter information or <jk>null</jk> if not found.
	 */
	public ParameterInfo getParameterInfo(String path, String method, String in, String name) {
		reqnn("path", path);
		reqnn("method", method);
		reqnn("in", in);
		return o(getPath(path)).map(x -> x.get(method)).map(x -> x.getParameter(in, name)).orElse(null);
	}

	/**
	 * Bean property getter:  <property>parameters</property>.
	 *
	 * <p>
	 * An object to hold parameters that can be used across operations.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,ParameterInfo> getParameters() { return nie(parameters); }

	/**
	 * Shortcut for calling <c>getPaths().get(path);</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return The operation map for the specified path, or <jk>null</jk> if it doesn't exist.
	 */
	public OperationMap getPath(String path) {
		reqnn("path", path);
		return o(getPaths()).map(x -> x.get(path)).orElse(null);
	}

	/**
	 * Bean property getter:  <property>paths</property>.
	 *
	 * <p>
	 * The available paths and operations for the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,OperationMap> getPaths() { return nie(paths); }

	/**
	 * Bean property getter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<MediaType> getProduces() { return nie(produces); }

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
	 * Shortcut for calling <c>getPaths().get(path).get(operation).getResponse(status);</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param operation The HTTP operation (e.g. <js>"get"</js>).  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @param status The HTTP response status (e.g. <js>"200"</js>).  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return The operation for the specified path and operation id, or <jk>null</jk> if it doesn't exist.
	 */
	public ResponseInfo getResponseInfo(String path, String operation, String status) {
		reqnn("path", path);
		reqnn("operation", operation);
		reqnn("status", status);
		return o(getPath(path)).map(x -> x.get(operation)).map(x -> x.getResponse(status)).orElse(null);
	}

	/**
	 * Bean property getter:  <property>responses</property>.
	 *
	 * <p>
	 * An object to hold responses that can be used across operations.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,ResponseInfo> getResponses() { return nie(responses); }

	/**
	 * Bean property getter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol of the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<String> getSchemes() { return nie(schemes); }

	/**
	 * Bean property getter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Map<String,List<String>>> getSecurity() { return nie(security); }

	/**
	 * Bean property getter:  <property>securityDefinitions</property>.
	 *
	 * <p>
	 * Security scheme definitions that can be used across the specification.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,SecurityScheme> getSecurityDefinitions() { return nie(securityDefinitions); }

	/**
	 * Bean property getter:  <property>swagger</property>.
	 *
	 * <p>
	 * Specifies the Swagger Specification version being used.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getSwagger() { return swagger; }

	/**
	 * Bean property getter:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<Tag> getTags() { return nie(tags); }

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = stb(String.class)
			.addIf(nn(basePath), "basePath")
			.addIf(ine(consumes), "consumes")
			.addIf(ine(definitions), "definitions")
			.addIf(nn(externalDocs), "externalDocs")
			.addIf(nn(host), "host")
			.addIf(nn(info), "info")
			.addIf(ine(parameters), "parameters")
			.addIf(ine(paths), "paths")
			.addIf(ine(produces), "produces")
			.addIf(ine(responses), "responses")
			.addIf(ine(schemes), "schemes")
			.addIf(ine(security), "security")
			.addIf(ine(securityDefinitions), "securityDefinitions")
			.addIf(nn(swagger), "swagger")
			.addIf(ine(tags), "tags")
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@SuppressWarnings({
		"rawtypes", // Raw types necessary for generic type handling
		"unchecked" // Type erasure requires unchecked casts in generic type conversions
	})
	@Override /* Overridden from SwaggerElement */
	public Swagger set(String property, Object value) {
		reqnn("property", property);
		return switch (property) {
			case "basePath" -> setBasePath(s(value));
			case "consumes" -> setConsumes(toListBuilder(value, MediaType.class).sparse().build());
			case "definitions" -> setDefinitions(toMapBuilder(value, String.class, JsonMap.class).sparse().build());
			case "externalDocs" -> setExternalDocs(toType(value, ExternalDocumentation.class));
			case "host" -> setHost(s(value));
			case "info" -> setInfo(toType(value, Info.class));
			case "parameters" -> setParameters(toMapBuilder(value, String.class, ParameterInfo.class).sparse().build());
			case "paths" -> setPaths(toMapBuilder(value, String.class, OperationMap.class).sparse().build());
			case "produces" -> setProduces(toListBuilder(value, MediaType.class).sparse().build());
			case "responses" -> setResponses(toMapBuilder(value, String.class, ResponseInfo.class).sparse().build());
			case "schemes" -> setSchemes(toListBuilder(value, String.class).sparse().build());
			case "security" -> setSecurity((List)toListBuilder(value, MapOfStringLists.class).sparse().build());
			case "securityDefinitions" -> setSecurityDefinitions(toMapBuilder(value, String.class, SecurityScheme.class).sparse().build());
			case "swagger" -> setSwagger(s(value));
			case "tags" -> setTags(toListBuilder(value, Tag.class).sparse().build());
			default -> {
				super.set(property, value);
				yield this;
			}
		};
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
		consumes.clear();
		if (nn(value))
			consumes.addAll(value);
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
		setConsumes(stb(MediaType.class).sparse().add(value).build());
		return this;
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
		definitions.clear();
		if (nn(value))
			definitions.putAll(value);
		return this;
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
		parameters.clear();
		if (nn(value))
			parameters.putAll(value);
		return this;
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
		paths.clear();
		if (nn(value))
			paths.putAll(value);
		return this;
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
		produces.clear();
		if (nn(value))
			produces.addAll(value);
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
		setProduces(stb(MediaType.class).sparse().add(value).build());
		return this;
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
		responses.clear();
		if (nn(value))
			responses.putAll(value);
		return this;
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
		schemes.clear();
		if (nn(value))
			schemes.addAll(value);
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
		setSchemes(stb(String.class).sparse().addJson(value).build());
		return this;
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
		security.clear();
		if (nn(value))
			security.addAll(value);
		return this;
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
		securityDefinitions.clear();
		if (nn(value))
			securityDefinitions.putAll(value);
		return this;
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
		tags.clear();
		if (nn(value))
			tags.addAll(value);
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
		setTags(stb(Tag.class).sparse().add(value).build());
		return this;
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

	@Override /* Overridden from Object */
	public String toString() {
		return Json.of(this);
	}
}
