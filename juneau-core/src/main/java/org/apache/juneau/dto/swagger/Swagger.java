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

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * This is the root document object for the API specification.
 *
 * @author james.bognar
 */
@Bean(properties="swagger,info,tags,externalDocs,basePath,schemes,consumes,produces,paths,definitions,parameters,responses,securityDefinitions,security")
public class Swagger {

	/** Represents a null swagger */
	public static final Swagger NULL = new Swagger();

	private String swagger = "2.0";
	private Info info;
	private String host, basePath;
	private List<String> schemes;
	private List<String> consumes;
	private List<String> produces;
	private Map<String,Map<String,Operation>> paths;
	private Map<String,Schema> definitions;
	private Map<String,Parameter> parameters;
	private Map<String,Response> responses;
	private Map<String,SecurityScheme> securityDefinitions;
	private List<Map<String,List<String>>> security;
	private List<Tag> tags;
	private ExternalDocumentation externalDocs;

	/**
	 * Convenience method for creating a new Swagger object.
	 *
	 * @param info Required. Provides metadata about the API.
	 * 	The metadata can be used by the clients if needed.
	 * @return A new Swagger object.
	 */
	public static Swagger create(Info info) {
		return new Swagger().setInfo(info);
	}

	/**
	 * Bean property getter:  <property>swagger</property>.
	 * <p>
	 * Required. Specifies the Swagger Specification version being used.
	 * It can be used by the Swagger UI and other clients to interpret the API listing.
	 * The value MUST be <js>"2.0"</js>.
	 *
	 * @return The value of the <property>swagger</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getSwagger() {
		return swagger;
	}

	/**
	 * Bean property setter:  <property>swagger</property>.
	 * <p>
	 * Required. Specifies the Swagger Specification version being used.
	 * It can be used by the Swagger UI and other clients to interpret the API listing.
	 * The value MUST be <js>"2.0"</js>.
	 *
	 * @param swagger The new value for the <property>swagger</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setSwagger(String swagger) {
		this.swagger = swagger;
		return this;
	}

	/**
	 * Bean property getter:  <property>info</property>.
	 * <p>
	 * Required. Provides metadata about the API.
	 * The metadata can be used by the clients if needed.
	 *
	 * @return The value of the <property>info</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Info getInfo() {
		return info;
	}

	/**
	 * Bean property setter:  <property>info</property>.
	 * <p>
	 * Required. Provides metadata about the API.
	 * The metadata can be used by the clients if needed.
	 *
	 * @param info The new value for the <property>info</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setInfo(Info info) {
		this.info = info;
		return this;
	}

	/**
	 * Bean property getter:  <property>host</property>.
	 * <p>
	 * The host (name or ip) serving the API.
	 * This MUST be the host only and does not include the scheme nor sub-paths.
	 * It MAY include a port.
	 * If the host is not included, the host serving the documentation is to be used (including the port).
	 * The host does not support <a href='http://swagger.io/specification/#pathTemplating'>path templating</a>.
	 *
	 * @return The value of the <property>host</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Bean property setter:  <property>host</property>.
	 * <p>
	 * The host (name or ip) serving the API.
	 * This MUST be the host only and does not include the scheme nor sub-paths.
	 * It MAY include a port.
	 * If the host is not included, the host serving the documentation is to be used (including the port).
	 * The host does not support <a href='http://swagger.io/specification/#pathTemplating'>path templating</a>.
	 *
	 * @param host The new value for the <property>host</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setHost(String host) {
		this.host = host;
		return this;
	}

	/**
	 * Bean property getter:  <property>basePath</property>.
	 * <p>
	 * The base path on which the API is served, which is relative to the <code>host</code>.
	 * If it is not included, the API is served directly under the <code>host</code>.
	 * The value MUST start with a leading slash (/).
	 * The <code>basePath</code> does not support <a href='http://swagger.io/specification/#pathTemplating'>path templating</a>.
	 *
	 * @return The value of the <property>basePath</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Bean property setter:  <property>basePath</property>.
	 * <p>
	 * The base path on which the API is served, which is relative to the <code>host</code>.
	 * If it is not included, the API is served directly under the <code>host</code>.
	 * The value MUST start with a leading slash (/).
	 * The <code>basePath</code> does not support <a href='http://swagger.io/specification/#pathTemplating'>path templating</a>.
	 *
	 * @param basePath The new value for the <property>basePath</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setBasePath(String basePath) {
		this.basePath = basePath;
		return this;
	}

	/**
	 * Bean property getter:  <property>schemes</property>.
	 * <p>
	 * The transfer protocol of the API.
	 * Values MUST be from the list:  <js>"http"</js>, <js>"https"</js>, <js>"ws"</js>, <js>"wss"</js>.
	 * If the <code>schemes</code> is not included, the default scheme to be used is the one used to access the Swagger definition itself.
	 *
	 * @return The value of the <property>schemes</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<String> getSchemes() {
		return schemes;
	}

	/**
	 * Bean property setter:  <property>schemes</property>.
	 * <p>
	 * The transfer protocol of the API.
	 * Values MUST be from the list:  <js>"http"</js>, <js>"https"</js>, <js>"ws"</js>, <js>"wss"</js>.
	 * If the <code>schemes</code> is not included, the default scheme to be used is the one used to access the Swagger definition itself.
	 *
	 * @param schemes The new value for the <property>schemes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setSchemes(List<String> schemes) {
		this.schemes = schemes;
		return this;
	}

	/**
	 * Bean property adder:  <property>schemes</property>.
	 * <p>
	 * The transfer protocol of the API.
	 * Values MUST be from the list:  <js>"http"</js>, <js>"https"</js>, <js>"ws"</js>, <js>"wss"</js>.
	 * If the <code>schemes</code> is not included, the default scheme to be used is the one used to access the Swagger definition itself.
	 *
	 * @param schemes The values to add for the <property>schemes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Swagger addSchemes(String...schemes) {
		return addSchemes(Arrays.asList(schemes));
	}

	/**
	 * Bean property adder:  <property>schemes</property>.
	 * <p>
	 * The transfer protocol of the API.
	 * Values MUST be from the list:  <js>"http"</js>, <js>"https"</js>, <js>"ws"</js>, <js>"wss"</js>.
	 * If the <code>schemes</code> is not included, the default scheme to be used is the one used to access the Swagger definition itself.
	 *
	 * @param schemes The values to add for the <property>schemes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Swagger addSchemes(Collection<String> schemes) {
		if (this.schemes == null)
			this.schemes = new LinkedList<String>();
		this.schemes.addAll(schemes);
		return this;
	}

	/**
	 * Bean property getter:  <property>consumes</property>.
	 * <p>
	 * A list of MIME types the APIs can consume.
	 * This is global to all APIs but can be overridden on specific API calls.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @return The value of the <property>consumes</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<String> getConsumes() {
		return consumes;
	}

	/**
	 * Bean property setter:  <property>consumes</property>.
	 * <p>
	 * A list of MIME types the APIs can consume.
	 * This is global to all APIs but can be overridden on specific API calls.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @param consumes The new value for the <property>consumes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setConsumes(List<String> consumes) {
		this.consumes = consumes;
		return this;
	}

	/**
	 * Bean property adder:  <property>consumes</property>.
	 * <p>
	 * A list of MIME types the APIs can consume.
	 * This is global to all APIs but can be overridden on specific API calls.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @param consumes The values to add for the <property>consumes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Swagger addConsumes(String...consumes) {
		return addConsumes(Arrays.asList(consumes));
	}

	/**
	 * Bean property adder:  <property>consumes</property>.
	 * <p>
	 * A list of MIME types the APIs can consume.
	 * This is global to all APIs but can be overridden on specific API calls.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @param consumes The values to add for the <property>consumes</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Swagger addConsumes(Collection<String> consumes) {
		if (this.consumes == null)
			this.consumes = new LinkedList<String>();
		this.consumes.addAll(consumes);
		return this;
	}

	/**
	 * Bean property getter:  <property>produces</property>.
	 * <p>
	 * A list of MIME types the APIs can produce.
	 * This is global to all APIs but can be overridden on specific API calls.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @return The value of the <property>produces</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<String> getProduces() {
		return produces;
	}

	/**
	 * Bean property setter:  <property>produces</property>.
	 * <p>
	 * A list of MIME types the APIs can produce.
	 * This is global to all APIs but can be overridden on specific API calls.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @param produces The new value for the <property>produces</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setProduces(List<String> produces) {
		this.produces = produces;
		return this;
	}

	/**
	 * Bean property adder:  <property>produces</property>.
	 * <p>
	 * A list of MIME types the APIs can produce.
	 * This is global to all APIs but can be overridden on specific API calls.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @param produces The values to add for the <property>produces</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Swagger addProduces(String...produces) {
		return addProduces(Arrays.asList(produces));
	}

	/**
	 * Bean property adder:  <property>produces</property>.
	 * <p>
	 * A list of MIME types the APIs can produce.
	 * This is global to all APIs but can be overridden on specific API calls.
	 * Value MUST be as described under <a href='http://swagger.io/specification/#mimeTypes'>Mime Types</a>.
	 *
	 * @param produces The values to add for the <property>produces</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Swagger addProduces(Collection<String> produces) {
		if (this.produces == null)
			this.produces = new LinkedList<String>();
		this.produces.addAll(produces);
		return this;
	}

	/**
	 * Bean property getter:  <property>paths</property>.
	 * <p>
	 * Required. The available paths and operations for the API.
	 *
	 * @return The value of the <property>paths</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Map<String,Operation>> getPaths() {
		return paths;
	}

	/**
	 * Bean property setter:  <property>paths</property>.
	 * <p>
	 * Required. The available paths and operations for the API.
	 *
	 * @param paths The new value for the <property>paths</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setPaths(Map<String,Map<String,Operation>> paths) {
		this.paths = paths;
		return this;
	}

	/**
	 * Bean property adder:  <property>paths</property>.
	 * <p>
	 * Required. The available paths and operations for the API.
	 *
	 * @param path The path template.
	 * @param methodName The HTTP method name.
	 * @param operation The operation that describes the path.
	 * @return This object (for method chaining).
	 */
	public Swagger addPath(String path, String methodName, Operation operation) {
		if (paths == null)
			paths = new TreeMap<String,Map<String,Operation>>();
		Map<String,Operation> p = paths.get(path);
		if (p == null) {
			p = new TreeMap<String,Operation>();
			paths.put(path, p);
		}
		p.put(methodName, operation);
		return this;
	}

	/**
	 * Bean property getter:  <property>definitions</property>.
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @return The value of the <property>definitions</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Schema> getDefinitions() {
		return definitions;
	}

	/**
	 * Bean property setter:  <property>definitions</property>.
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @param definitions The new value for the <property>definitions</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setDefinitions(Map<String,Schema> definitions) {
		this.definitions = definitions;
		return this;
	}

	/**
	 * Bean property adder:  <property>definitions</property>.
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @param name A definition name.
	 * @param schema The schema that the name defines.
	 * @return This object (for method chaining).
	 */
	public Swagger addDefinition(String name, Schema schema) {
		if (definitions == null)
			definitions = new TreeMap<String,Schema>();
		definitions.put(name, schema);
		return this;
	}

	/**
	 * Bean property getter:  <property>parameters</property>.
	 * <p>
	 * An object to hold parameters that can be used across operations.
	 * This property does not define global parameters for all operations.
	 *
	 * @return The value of the <property>parameters</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Parameter> getParameters() {
		return parameters;
	}

	/**
	 * Bean property setter:  <property>parameters</property>.
	 * <p>
	 * An object to hold parameters that can be used across operations.
	 * This property does not define global parameters for all operations.
	 *
	 * @param parameters The new value for the <property>parameters</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setParameters(Map<String,Parameter> parameters) {
		this.parameters = parameters;
		return this;
	}

	/**
	 * Bean property adder:  <property>parameters</property>.
	 * <p>
	 * An object to hold parameters that can be used across operations.
	 * This property does not define global parameters for all operations.
	 *
	 * @param name The parameter name.
	 * @param parameter The parameter definition.
	 * @return This object (for method chaining).
	 */
	public Swagger addParameter(String name, Parameter parameter) {
		if (parameters == null)
			parameters = new TreeMap<String,Parameter>();
		parameters.put(name, parameter);
		return this;
	}

	/**
	 * Bean property getter:  <property>responses</property>.
	 * <p>
	 * An object to hold responses that can be used across operations.
	 * This property does not define global responses for all operations.
	 *
	 * @return The value of the <property>responses</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Response> getResponses() {
		return responses;
	}

	/**
	 * Bean property setter:  <property>responses</property>.
	 * <p>
	 * An object to hold responses that can be used across operations.
	 * This property does not define global responses for all operations.
	 *
	 * @param responses The new value for the <property>responses</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setResponses(Map<String,Response> responses) {
		this.responses = responses;
		return this;
	}

	/**
	 * Bean property adder:  <property>responses</property>.
	 * <p>
	 * An object to hold responses that can be used across operations.
	 * This property does not define global responses for all operations.
	 *
	 * @param name The response name.
	 * @param response The response definition.
	 * @return This object (for method chaining).
	 */
	public Swagger addResponse(String name, Response response) {
		if (responses == null)
			responses = new TreeMap<String,Response>();
		responses.put(name, response);
		return this;
	}

	/**
	 * Bean property getter:  <property>securityDefinitions</property>.
	 * <p>
	 * Security scheme definitions that can be used across the specification.
	 *
	 * @return The value of the <property>securityDefinitions</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,SecurityScheme> getSecurityDefinitions() {
		return securityDefinitions;
	}

	/**
	 * Bean property setter:  <property>securityDefinitions</property>.
	 * <p>
	 * Security scheme definitions that can be used across the specification.
	 *
	 * @param securityDefinitions The new value for the <property>securityDefinitions</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setSecurityDefinitions(Map<String,SecurityScheme> securityDefinitions) {
		this.securityDefinitions = securityDefinitions;
		return this;
	}

	/**
	 * Bean property adder:  <property>securityDefinitions</property>.
	 * <p>
	 * Security scheme definitions that can be used across the specification.
	 *
	 * @param name A security name.
	 * @param securityScheme A security schema.
	 * @return This object (for method chaining).
	 */
	public Swagger addSecurityDefinition(String name, SecurityScheme securityScheme) {
		if (securityDefinitions == null)
			securityDefinitions = new TreeMap<String,SecurityScheme>();
		securityDefinitions.put(name, securityScheme);
		return this;
	}

	/**
	 * Bean property getter:  <property>security</property>.
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 * The list of values describes alternative security schemes that can be used (that is, there is a logical OR between the security requirements).
	 * Individual operations can override this definition.
	 *
	 * @return The value of the <property>security</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Map<String,List<String>>> getSecurity() {
		return security;
	}

	/**
	 * Bean property setter:  <property>security</property>.
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 * The list of values describes alternative security schemes that can be used (that is, there is a logical OR between the security requirements).
	 * Individual operations can override this definition.
	 *
	 * @param security The new value for the <property>security</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setSecurity(List<Map<String,List<String>>> security) {
		this.security = security;
		return this;
	}

	/**
	 * Bean property adder:  <property>security</property>.
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 * The list of values describes alternative security schemes that can be used (that is, there is a logical OR between the security requirements).
	 * Individual operations can override this definition.
	 *
	 * @param security The value to add for the <property>security</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Swagger addSecurity(Map<String,List<String>> security) {
		if (this.security == null)
			this.security = new LinkedList<Map<String,List<String>>>();
		this.security.add(security);
		return this;
	}

	/**
	 * Bean property getter:  <property>tags</property>.
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 * The order of the tags can be used to reflect on their order by the parsing tools.
	 * Not all tags that are used by the <a href='http://swagger.io/specification/#operationObject'>Operation Object</a> must be declared.
	 * The tags that are not declared may be organized randomly or based on the tools' logic.
	 * Each tag name in the list MUST be unique.
	 *
	 * @return The value of the <property>tags</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Tag> getTags() {
		return tags;
	}

	/**
	 * Bean property setter:  <property>tags</property>.
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 * The order of the tags can be used to reflect on their order by the parsing tools.
	 * Not all tags that are used by the <a href='http://swagger.io/specification/#operationObject'>Operation Object</a> must be declared.
	 * The tags that are not declared may be organized randomly or based on the tools' logic.
	 * Each tag name in the list MUST be unique.
	 *
	 * @param tags The new value for the <property>tags</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setTags(List<Tag> tags) {
		this.tags = tags;
		return this;
	}

	/**
	 * Bean property adder:  <property>tags</property>.
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 * The order of the tags can be used to reflect on their order by the parsing tools.
	 * Not all tags that are used by the <a href='http://swagger.io/specification/#operationObject'>Operation Object</a> must be declared.
	 * The tags that are not declared may be organized randomly or based on the tools' logic.
	 * Each tag name in the list MUST be unique.
	 *
	 * @param tags The values to add for the <property>tags</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public Swagger addTags(Tag...tags) {
		if (this.tags == null)
			this.tags = new LinkedList<Tag>();
		this.tags.addAll(Arrays.asList(tags));
		return this;
	}

	/**
	 * Bean property getter:  <property>externalDocs</property>.
	 * <p>
	 * Additional external documentation.
	 *
	 * @return The value of the <property>externalDocs</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public ExternalDocumentation getExternalDocs() {
		return externalDocs;
	}

	/**
	 * Bean property setter:  <property>externalDocs</property>.
	 * <p>
	 * Additional external documentation.
	 *
	 * @param externalDocs The new value for the <property>externalDocs</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Swagger setExternalDocs(ExternalDocumentation externalDocs) {
		this.externalDocs = externalDocs;
		return this;
	}
}
