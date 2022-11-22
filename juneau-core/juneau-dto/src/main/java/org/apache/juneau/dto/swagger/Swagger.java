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
package org.apache.juneau.dto.swagger;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.objecttools.*;

/**
 * This is the root document object for the API specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Overview &gt; juneau-rest-server &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="swagger,info,tags,externalDocs,basePath,schemes,consumes,produces,paths,definitions,parameters,responses,securityDefinitions,security,*")
@FluentSetters
public class Swagger extends SwaggerElement {

	/** Represents a null swagger */
	public static final Swagger NULL = new Swagger();

	private static final Comparator<String> PATH_COMPARATOR = new Comparator<String>() {
		@Override /* Comparator */
		public int compare(String o1, String o2) {
			return o1.replace('{', '@').compareTo(o2.replace('{', '@'));
		}
	};

	private String
		swagger = "2.0",
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
		if (copyFrom.definitions == null) {
			this.definitions = null;
		} else {
			this.definitions = map();
			copyFrom.definitions.forEach((k,v) -> this.definitions.put(k, new JsonMap(v)));
		}

		if (copyFrom.paths == null) {
			this.paths = null;
		} else {
			this.paths = map();
			copyFrom.paths.forEach((k,v) -> {
				OperationMap m = new OperationMap();
				v.forEach((k2,v2) -> m.put(k2, v2.copy()));
				this.paths.put(k, m);
			});
		}

		if (copyFrom.parameters == null) {
			this.parameters = null;
		} else {
			this.parameters = map();
			copyFrom.parameters.forEach((k,v) -> this.parameters.put(k, v.copy()));
		}

		if (copyFrom.responses == null) {
			this.responses = null;
		} else {
			this.responses = map();
			copyFrom.responses.forEach((k,v) -> this.responses.put(k, v.copy()));
		}

		if (copyFrom.security == null) {
			this.security = null;
		} else {
			this.security = list();
			copyFrom.security.forEach(x -> {
				Map<String,List<String>> m2 = map();
				x.forEach((k,v) -> m2.put(k, copyOf(v)));
				this.security.add(m2);
			});
		}

		if (copyFrom.securityDefinitions == null) {
			this.securityDefinitions = null;
		} else {
			this.securityDefinitions = map();
			copyFrom.securityDefinitions.forEach((k,v) -> this.securityDefinitions.put(k, v.copy()));
		}

		if (copyFrom.tags == null) {
			this.tags = null;
		} else {
			this.tags = CollectionUtils.set();
			copyFrom.tags.forEach(x -> this.tags.add(x.copy()));
		}
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
	 * @param name A definition name.
	 * @param schema The schema that the name defines.
	 * @return This object.
	 */
	public Swagger addDefinition(String name, JsonMap schema) {
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
	 * @param name The parameter name.
	 * @param parameter The parameter definition.
	 * @return This object.
	 */
	public Swagger addParameter(String name, ParameterInfo parameter) {
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
	 * @param path The path template.
	 * @param methodName The HTTP method name.
	 * @param operation The operation that describes the path.
	 * @return This object.
	 */
	public Swagger addPath(String path, String methodName, Operation operation) {
		if (paths == null)
			paths = new TreeMap<>(PATH_COMPARATOR);
		OperationMap p = paths.get(path);
		if (p == null) {
			p = new OperationMap();
			paths.put(path, p);
		}
		p.put(methodName, operation);
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
	 * @param name The response name.
	 * @param response The response definition.
	 * @return This object.
	 */
	public Swagger addResponse(String name, ResponseInfo response) {
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
	 * @param scheme The security scheme that applies to this operation
	 * @param alternatives
	 * 	The list of values describes alternative security schemes that can be used (that is, there is a logical OR between the security requirements).
	 * @return This object.
	 */
	public Swagger addSecurity(String scheme, String...alternatives) {
		Map<String,List<String>> m = map();
		m.put(scheme, alist(alternatives));
		security = listBuilder(security).sparse().addAll(Collections.singleton(m)).build();
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
	 * @param name A security name.
	 * @param securityScheme A security schema.
	 * @return This object.
	 */
	public Swagger addSecurityDefinition(String name, SecurityScheme securityScheme) {
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

	//-----------------------------------------------------------------------------------------------------------------
	// Convenience methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Shortcut for calling <c>getPaths().get(path);</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).
	 * @return The operation map for the specified path, or <jk>null</jk> if it doesn't exist.
	 */
	public OperationMap getPath(String path) {
		return getPaths().get(path);
	}

	/**
	 * Shortcut for calling <c>getPaths().get(path).get(operation);</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).
	 * @param operation The HTTP operation (e.g. <js>"get"</js>).
	 * @return The operation for the specified path and operation id, or <jk>null</jk> if it doesn't exist.
	 */
	public Operation getOperation(String path, String operation) {
		OperationMap om = getPath(path);
		if (om == null)
			return null;
		return om.get(operation);
	}

	/**
	 * Shortcut for calling <c>getPaths().get(path).get(operation).getResponse(status);</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).
	 * @param operation The HTTP operation (e.g. <js>"get"</js>).
	 * @param status The HTTP response status (e.g. <js>"200"</js>).
	 * @return The operation for the specified path and operation id, or <jk>null</jk> if it doesn't exist.
	 */
	public ResponseInfo getResponseInfo(String path, String operation, String status) {
		OperationMap om = getPath(path);
		if (om == null)
			return null;
		Operation op = om.get(operation);
		if (op == null)
			return null;
		return op.getResponse(status);
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
	 * @param path The HTTP path.
	 * @param method The HTTP method.
	 * @param in The parameter type.
	 * @param name The parameter name.
	 * @return The parameter information or <jk>null</jk> if not found.
	 */
	public ParameterInfo getParameterInfo(String path, String method, String in, String name) {
		OperationMap om = getPath(path);
		if (om != null) {
			Operation o = om.get(method);
			if (o != null) {
				return o.getParameter(in, name);
			}
		}
		return null;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "basePath": return toType(getBasePath(), type);
			case "consumes": return toType(getConsumes(), type);
			case "definitions": return toType(getDefinitions(), type);
			case "externalDocs": return toType(getExternalDocs(), type);
			case "host": return toType(getHost(), type);
			case "info": return toType(getInfo(), type);
			case "parameters": return toType(getParameters(), type);
			case "paths": return toType(getPaths(), type);
			case "produces": return toType(getProduces(), type);
			case "responses": return toType(getResponses(), type);
			case "schemes": return toType(getSchemes(), type);
			case "security": return toType(getSecurity(), type);
			case "securityDefinitions": return toType(getSecurityDefinitions(), type);
			case "swagger": return toType(getSwagger(), type);
			case "tags": return toType(getTags(), type);
			default: return super.get(property, type);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override /* SwaggerElement */
	public Swagger set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "basePath": return setBasePath(stringify(value));
			case "consumes": return setConsumes(listBuilder(MediaType.class).sparse().addAny(value).build());
			case "definitions": return setDefinitions(mapBuilder(String.class,JsonMap.class).sparse().addAny(value).build());
			case "externalDocs": return setExternalDocs(toType(value, ExternalDocumentation.class));
			case "host": return setHost(stringify(value));
			case "info": return setInfo(toType(value, Info.class));
			case "parameters": return setParameters(mapBuilder(String.class,ParameterInfo.class).sparse().addAny(value).build());
			case "paths": return setPaths(mapBuilder(String.class,OperationMap.class).sparse().addAny(value).build());
			case "produces": return setProduces(listBuilder(MediaType.class).sparse().addAny(value).build());
			case "responses": return setResponses(mapBuilder(String.class,ResponseInfo.class).sparse().addAny(value).build());
			case "schemes": return setSchemes(listBuilder(String.class).sparse().addAny(value).build());
			case "security": return setSecurity((List)listBuilder(Map.class,String.class,List.class,String.class).sparse().addAny(value).build());
			case "securityDefinitions": return setSecurityDefinitions(mapBuilder(String.class,SecurityScheme.class).sparse().addAny(value).build());
			case "swagger": return setSwagger(stringify(value));
			case "tags": return setTags(listBuilder(Tag.class).sparse().addAny(value).build());
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
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

	@Override /* Object */
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}

	/**
	 * Resolves a <js>"$ref"</js> tags to nodes in this swagger document.
	 *
	 * @param <T> The class to convert the reference to.
	 * @param ref The ref tag value.
	 * @param c The class to convert the reference to.
	 * @return The referenced node, or <jk>null</jk> if the ref was <jk>null</jk> or empty or not found.
	 */
	public <T> T findRef(String ref, Class<T> c) {
		if (isEmpty(ref))
			return null;
		if (! ref.startsWith("#/"))
			throw new BasicRuntimeException("Unsupported reference:  ''{0}''", ref);
		try {
			return new ObjectRest(this).get(ref.substring(1), c);
		} catch (Exception e) {
			throw new BeanRuntimeException(e, c, "Reference ''{0}'' could not be converted to type ''{1}''.", ref, className(c));
		}
	}
}
