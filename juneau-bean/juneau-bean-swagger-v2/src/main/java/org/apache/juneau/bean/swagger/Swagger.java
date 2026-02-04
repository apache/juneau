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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.collections.*;
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

	private static interface MapOfStringLists extends Map<String,List<String>> {}

	/** Represents a null swagger */
	public static final Swagger NULL = new Swagger();

	private static final Comparator<String> PATH_COMPARATOR = (o1, o2) -> o1.replace('{', '@').compareTo(o2.replace('{', '@'));

	// Property name constants
	private static final String PROP_BASE_PATH = "basePath";
	private static final String PROP_CONSUMES = "consumes";
	private static final String PROP_DEFINITIONS = "definitions";
	private static final String PROP_EXTERNAL_DOCS = "externalDocs";
	private static final String PROP_HOST = "host";
	private static final String PROP_INFO = "info";
	private static final String PROP_PARAMETERS = "parameters";
	private static final String PROP_PATHS = "paths";
	private static final String PROP_PRODUCES = "produces";
	private static final String PROP_RESPONSES = "responses";
	private static final String PROP_SCHEMES = "schemes";
	private static final String PROP_SECURITY = "security";
	private static final String PROP_SECURITY_DEFINITIONS = "securityDefinitions";
	private static final String PROP_SWAGGER = "swagger";
	private static final String PROP_TAGS = "tags";

	private String swagger = "2.0",  // NOSONAR - Intentional naming.
		host, basePath;
	private Info info;
	private ExternalDocumentation externalDocs;
	private Set<String> schemes = new LinkedHashSet<>();
	private Set<MediaType> consumes = new LinkedHashSet<>(), produces = new LinkedHashSet<>();
	private Set<Tag> tags = new LinkedHashSet<>();
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
		if (nn(copyFrom.consumes))
			this.consumes.addAll(copyOf(copyFrom.consumes));
		this.externalDocs = copyFrom.externalDocs == null ? null : copyFrom.externalDocs.copy();
		this.host = copyFrom.host;
		this.info = copyFrom.info == null ? null : copyFrom.info.copy();
		if (nn(copyFrom.produces))
			this.produces.addAll(copyOf(copyFrom.produces));
		if (nn(copyFrom.schemes))
			this.schemes.addAll(copyOf(copyFrom.schemes));
		this.swagger = copyFrom.swagger;

		// TODO - Definitions are not deep copied, so they should not contain references.
		if (nn(copyFrom.definitions))
			definitions.putAll(copyOf(copyFrom.definitions, JsonMap::new));

		if (nn(copyFrom.paths))
			copyFrom.paths.forEach((k, v) -> {
				var m = new OperationMap();
				v.forEach((k2, v2) -> m.put(k2, v2.copy()));
				paths.put(k, m);
			});

		if (nn(copyFrom.parameters))
			parameters.putAll(copyOf(copyFrom.parameters, ParameterInfo::copy));
		if (nn(copyFrom.responses))
			responses.putAll(copyOf(copyFrom.responses, ResponseInfo::copy));
		if (nn(copyFrom.securityDefinitions))
			securityDefinitions.putAll(copyOf(copyFrom.securityDefinitions, SecurityScheme::copy));

		if (nn(copyFrom.security))
			copyFrom.security.forEach(x -> {
				Map<String,List<String>> m2 = map();
				x.forEach((k, v) -> m2.put(k, copyOf(v)));
				security.add(m2);
			});

		if (nn(copyFrom.tags))
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
		if (nn(values))
			for (var v : values)
				if (nn(v))
					consumes.add(v);
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
		definitions.put(name, schema);
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
		parameters.put(name, parameter);
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
		paths.computeIfAbsent(path, k -> new OperationMap()).put(methodName, operation);
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
		if (nn(values))
			for (var v : values)
				if (nn(v))
					produces.add(v);
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
		if (nn(values))
			for (var v : values)
				if (nn(v))
					schemes.add(v);
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
	 * @param scheme The security scheme that applies to this operation  Must not be <jk>null</jk>.
	 * @param alternatives
	 * 	The list of values describes alternative security schemes that can be used (that is, there is a logical OR between the security requirements).
	 * @return This object.
	 */
	public Swagger addSecurity(String scheme, String...alternatives) {
		assertArgNotNull("scheme", scheme);
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
	 * @param name A security name.  Must not be <jk>null</jk>.
	 * @param securityScheme A security schema.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addSecurityDefinition(String name, SecurityScheme securityScheme) {
		assertArgNotNull("name", name);
		assertArgNotNull("securityScheme", securityScheme);
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
		if (nn(values))
			for (var v : values)
				if (nn(v))
					tags.add(v);
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
	 * @param ref The ref tag value.  Must not be <jk>null</jk> or blank.
	 * @param c The class to convert the reference to.  Must not be <jk>null</jk>.
	 * @return The referenced node, or <jk>null</jk> if not found.
	 */
	public <T> T findRef(String ref, Class<T> c) {
		assertArgNotNullOrBlank("ref", ref);
		assertArgNotNull("c", c);
		if (! ref.startsWith("#/"))
			throw rex("Unsupported reference:  ''{0}''", ref);
		try {
			return new ObjectRest(this).get(ref.substring(1), c);
		} catch (Exception e) {
			throw bex(e, c, "Reference ''{0}'' could not be converted to type ''{1}''.", ref, cn(c));
		}
	}

	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case PROP_BASE_PATH -> toType(getBasePath(), type);
			case PROP_CONSUMES -> toType(getConsumes(), type);
			case PROP_DEFINITIONS -> toType(getDefinitions(), type);
			case PROP_EXTERNAL_DOCS -> toType(getExternalDocs(), type);
			case PROP_HOST -> toType(getHost(), type);
			case PROP_INFO -> toType(getInfo(), type);
			case PROP_PARAMETERS -> toType(getParameters(), type);
			case PROP_PATHS -> toType(getPaths(), type);
			case PROP_PRODUCES -> toType(getProduces(), type);
			case PROP_RESPONSES -> toType(getResponses(), type);
			case PROP_SCHEMES -> toType(getSchemes(), type);
			case PROP_SECURITY -> toType(getSecurity(), type);
			case PROP_SECURITY_DEFINITIONS -> toType(getSecurityDefinitions(), type);
			case PROP_SWAGGER -> toType(getSwagger(), type);
			case PROP_TAGS -> toType(getTags(), type);
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
	public Set<MediaType> getConsumes() { return nullIfEmpty(consumes); }

	/**
	 * Bean property getter:  <property>definitions</property>.
	 *
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,JsonMap> getDefinitions() { return nullIfEmpty(definitions); }

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

	/**
	 * Bean property getter:  <property>parameters</property>.
	 *
	 * <p>
	 * An object to hold parameters that can be used across operations.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,ParameterInfo> getParameters() { return nullIfEmpty(parameters); }

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
	 * Bean property getter:  <property>paths</property>.
	 *
	 * <p>
	 * The available paths and operations for the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,OperationMap> getPaths() { return nullIfEmpty(paths); }

	/**
	 * Bean property getter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<MediaType> getProduces() { return nullIfEmpty(produces); }

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
	 * Bean property getter:  <property>responses</property>.
	 *
	 * <p>
	 * An object to hold responses that can be used across operations.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,ResponseInfo> getResponses() { return nullIfEmpty(responses); }

	/**
	 * Bean property getter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol of the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<String> getSchemes() { return nullIfEmpty(schemes); }

	/**
	 * Bean property getter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Map<String,List<String>>> getSecurity() { return nullIfEmpty(security); }

	/**
	 * Bean property getter:  <property>securityDefinitions</property>.
	 *
	 * <p>
	 * Security scheme definitions that can be used across the specification.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,SecurityScheme> getSecurityDefinitions() { return nullIfEmpty(securityDefinitions); }

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
	public Set<Tag> getTags() { return nullIfEmpty(tags); }

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(nn(basePath), PROP_BASE_PATH)
			.addIf(ne(consumes), PROP_CONSUMES)
			.addIf(ne(definitions), PROP_DEFINITIONS)
			.addIf(nn(externalDocs), PROP_EXTERNAL_DOCS)
			.addIf(nn(host), PROP_HOST)
			.addIf(nn(info), PROP_INFO)
			.addIf(ne(parameters), PROP_PARAMETERS)
			.addIf(ne(paths), PROP_PATHS)
			.addIf(ne(produces), PROP_PRODUCES)
			.addIf(ne(responses), PROP_RESPONSES)
			.addIf(ne(schemes), PROP_SCHEMES)
			.addIf(ne(security), PROP_SECURITY)
			.addIf(ne(securityDefinitions), PROP_SECURITY_DEFINITIONS)
			.addIf(nn(swagger), PROP_SWAGGER)
			.addIf(ne(tags), PROP_TAGS)
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override /* Overridden from SwaggerElement */
	public Swagger set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case PROP_BASE_PATH -> setBasePath(s(value));
			case PROP_CONSUMES -> setConsumes(toListBuilder(value, MediaType.class).sparse().build());
			case PROP_DEFINITIONS -> setDefinitions(toMapBuilder(value, String.class, JsonMap.class).sparse().build());
			case PROP_EXTERNAL_DOCS -> setExternalDocs(toType(value, ExternalDocumentation.class));
			case PROP_HOST -> setHost(s(value));
			case PROP_INFO -> setInfo(toType(value, Info.class));
			case PROP_PARAMETERS -> setParameters(toMapBuilder(value, String.class, ParameterInfo.class).sparse().build());
			case PROP_PATHS -> setPaths(toMapBuilder(value, String.class, OperationMap.class).sparse().build());
			case PROP_PRODUCES -> setProduces(toListBuilder(value, MediaType.class).sparse().build());
			case PROP_RESPONSES -> setResponses(toMapBuilder(value, String.class, ResponseInfo.class).sparse().build());
			case PROP_SCHEMES -> setSchemes(toListBuilder(value, String.class).sparse().build());
			case PROP_SECURITY -> setSecurity((List)toListBuilder(value, MapOfStringLists.class).sparse().build());
			case PROP_SECURITY_DEFINITIONS -> setSecurityDefinitions(toMapBuilder(value, String.class, SecurityScheme.class).sparse().build());
			case PROP_SWAGGER -> setSwagger(s(value));
			case PROP_TAGS -> setTags(toListBuilder(value, Tag.class).sparse().build());
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
		setConsumes(setb(MediaType.class).sparse().add(value).build());
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
		setProduces(setb(MediaType.class).sparse().add(value).build());
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
		setSchemes(setb(String.class).sparse().addJson(value).build());
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
		setTags(setb(Tag.class).sparse().add(value).build());
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
		return JsonSerializer.DEFAULT.toString(this);
	}
}