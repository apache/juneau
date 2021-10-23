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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.utils.*;

/**
 * This is the root document object for the API specification.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc DtoSwagger}
 * </ul>
 */
@Bean(properties="swagger,info,tags,externalDocs,basePath,schemes,consumes,produces,paths,definitions,parameters,responses,securityDefinitions,security,*")
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
	private Map<String,OMap> definitions;
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
		this.consumes = newSet(copyFrom.consumes);
		this.externalDocs = copyFrom.externalDocs == null ? null : copyFrom.externalDocs.copy();
		this.host = copyFrom.host;
		this.info = copyFrom.info == null ? null : copyFrom.info.copy();
		this.produces = newSet(copyFrom.produces);
		this.schemes = newSet(copyFrom.schemes);
		this.swagger = copyFrom.swagger;

		// TODO - Definitions are not deep copied, so they should not contain references.
		if (copyFrom.definitions == null) {
			this.definitions = null;
		} else {
			this.definitions = new LinkedHashMap<>();
			for (Map.Entry<String,OMap> e : copyFrom.definitions.entrySet())
				this.definitions.put(e.getKey(), new OMap(e.getValue()));
		}

		if (copyFrom.paths == null) {
			this.paths = null;
		} else {
			this.paths = new LinkedHashMap<>();
			for (Map.Entry<String,OperationMap> e : copyFrom.paths.entrySet()) {
				OperationMap m = new OperationMap();
				for (Map.Entry<String,Operation> e2 : e.getValue().entrySet())
					m.put(e2.getKey(), e2.getValue().copy());
				this.paths.put(e.getKey(), m);
			}
		}

		if (copyFrom.parameters == null) {
			this.parameters = null;
		} else {
			this.parameters = new LinkedHashMap<>();
			for (Map.Entry<String,ParameterInfo> e : copyFrom.parameters.entrySet())
				this.parameters.put(e.getKey(), e.getValue().copy());
		}

		if (copyFrom.responses == null) {
			this.responses = null;
		} else {
			this.responses = new LinkedHashMap<>();
			for (Map.Entry<String,ResponseInfo> e : copyFrom.responses.entrySet())
				this.responses.put(e.getKey(), e.getValue().copy());
		}

		if (copyFrom.security == null) {
			this.security = null;
		} else {
			this.security = new ArrayList<>();
			for (Map<String,List<String>> m : copyFrom.security) {
				Map<String,List<String>> m2 = new LinkedHashMap<>();
				for (Map.Entry<String,List<String>> e : m.entrySet())
					m2.put(e.getKey(), newList(e.getValue()));
				this.security.add(m2);
			}
		}

		if (copyFrom.securityDefinitions == null) {
			this.securityDefinitions = null;
		} else {
			this.securityDefinitions = new LinkedHashMap<>();
			for (Map.Entry<String,SecurityScheme> e : copyFrom.securityDefinitions.entrySet())
				this.securityDefinitions.put(e.getKey(), e.getValue().copy());
		}

		if (copyFrom.tags == null) {
			this.tags = null;
		} else {
			this.tags = new LinkedHashSet<>();
			for (Tag t : copyFrom.tags)
				this.tags.add(t.copy());
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
	// basePath
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
	 * 	<br>The <c>basePath</c> does not support {@doc ExtSwaggerPathTemplating path templating}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setBasePath(String value) {
		basePath = value;
	}

	/**
	 * Bean property fluent getter:  <property>basePath</property>.
	 *
	 * <p>
	 * The base path on which the API is served, which is relative to the <c>host</c>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> basePath() {
		return Optional.ofNullable(getBasePath());
	}

	/**
	 * Bean property fluent setter:  <property>basePath</property>.
	 *
	 * <p>
	 * The base path on which the API is served, which is relative to the <c>host</c>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>If it is not included, the API is served directly under the <c>host</c>.
	 * 	<br>The value MUST start with a leading slash (/).
	 * 	<br>The <c>basePath</c> does not support {@doc ExtSwaggerPathTemplating path templating}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger basePath(String value) {
		setBasePath(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// consumes
	//-----------------------------------------------------------------------------------------------------------------

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
	 * 	<br>Value MUST be as described under {@doc ExtSwaggerMimeTypes}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setConsumes(Collection<MediaType> value) {
		consumes = newSet(value);
	}

	/**
	 * Bean property appender:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can consume.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Values MUST be as described under {@doc ExtSwaggerMimeTypes}.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addConsumes(Collection<MediaType> values) {
		consumes = setBuilder(consumes).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can consume.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Set<MediaType>> consumes() {
		return Optional.ofNullable(getConsumes());
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
	public Swagger consumes(Collection<MediaType> value) {
		setConsumes(value);
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
	public Swagger consumes(MediaType...value) {
		setConsumes(setBuilder(MediaType.class).sparse().add(value).build());
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
	 * 	<br>Strings can be JSON arrays.
	 * @return This object.
	 */
	public Swagger consumes(String...value) {
		setConsumes(setBuilder(MediaType.class).sparse().addJson(value).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// definitions
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>definitions</property>.
	 *
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,OMap> getDefinitions() {
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
	 */
	public void setDefinitions(Map<String,OMap> value) {
		definitions = newMap(value);
	}

	/**
	 * Bean property appender:  <property>definitions</property>.
	 *
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addDefinitions(Map<String,OMap> values) {
		definitions = mapBuilder(definitions).sparse().addAll(values).build();
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
	public Swagger definition(String name, OMap schema) {
		definitions = mapBuilder(definitions).sparse().add(name, schema).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>definitions</property>.
	 *
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Map<String,OMap>> definitions() {
		return Optional.ofNullable(getDefinitions());
	}

	/**
	 * Bean property fluent setter:  <property>definitions</property>.
	 *
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Swagger definitions(Map<String,OMap> value) {
		setDefinitions(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>definitions</property>.
	 *
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @param json
	 * 	The value to set on this property as JSON.
	 * @return This object.
	 */
	public Swagger definitions(String json) {
		setDefinitions(mapBuilder(String.class,OMap.class).sparse().addJson(json).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// externalDocs
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
	}

	/**
	 * Bean property fluent getter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<ExternalDocumentation> externalDocs() {
		return Optional.ofNullable(getExternalDocs());
	}

	/**
	 * Bean property fluent setter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Swagger externalDocs(ExternalDocumentation value) {
		setExternalDocs(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>externalDocs</property>.
	 *
	 * <p>
	 * Additional external documentation as raw JSON.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	externalDocs(<js>"{description:'description',url:'url'}"</js>);
	 * </p>
	 *
	 * @param json
	 * 	The new value for this property as JSON.
	 * @return This object.
	 */
	public Swagger externalDocs(String json) {
		setExternalDocs(toType(json, ExternalDocumentation.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// host
	//-----------------------------------------------------------------------------------------------------------------

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
	 * 	<br>The host does not support {@doc ExtSwaggerPathTemplating path templating}
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setHost(String value) {
		host = value;
	}

	/**
	 * Bean property fluent getter:  <property>host</property>.
	 *
	 * <p>
	 * The host (name or IP) serving the API.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> host() {
		return Optional.ofNullable(getHost());
	}

	/**
	 * Bean property fluent setter:  <property>host</property>.
	 *
	 * <p>
	 * The host (name or IP) serving the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>This MUST be the host only and does not include the scheme nor sub-paths.
	 * 	<br>It MAY include a port.
	 * 	<br>If the host is not included, the host serving the documentation is to be used (including the port).
	 * 	<br>The host does not support {@doc ExtSwaggerPathTemplating path templating}
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger host(String value) {
		setHost(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// info
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setInfo(Info value) {
		info = value;
	}

	/**
	 * Bean property fluent getter:  <property>info</property>.
	 *
	 * <p>
	 * Provides metadata about the API.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Info> info() {
		return Optional.ofNullable(getInfo());
	}

	/**
	 * Bean property fluent setter:  <property>info</property>.
	 *
	 * <p>
	 * Provides metadata about the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object.
	 */
	public Swagger info(Info value) {
		setInfo(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>info</property>.
	 *
	 * <p>
	 * Provides metadata about the API as raw JSON.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	info(<js>"{title:'title',description:'description',...}"</js>);
	 * </p>
	 *
	 * @param json
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object.
	 */
	public Swagger info(String json) {
		setInfo(toType(json, Info.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// parameters
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setParameters(Map<String,ParameterInfo> value) {
		parameters = newMap(value);
	}

	/**
	 * Bean property appender:  <property>parameters</property>.
	 *
	 * <p>
	 * An object to hold parameters that can be used across operations.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addParameters(Map<String,ParameterInfo> values) {
		parameters = mapBuilder(parameters).sparse().addAll(values).build();
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
	public Swagger parameter(String name, ParameterInfo parameter) {
		parameters = mapBuilder(parameters).sparse().add(name, parameter).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>parameters</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Map<String,ParameterInfo>> parameters() {
		return Optional.ofNullable(getParameters());
	}

	/**
	 * Bean property fluent setter:  <property>parameters</property>.
	 *
	 * <p>
	 * An object to hold parameters that can be used across operations.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Swagger parameters(Map<String,ParameterInfo> value) {
		setParameters(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>parameters</property>.
	 *
	 * <p>
	 * An object to hold parameters that can be used across operations.
	 *
	 * @param json
	 * 	The value to set on this property as JSON.
	 * @return This object.
	 */
	public Swagger parameters(String json) {
		setParameters(mapBuilder(String.class,ParameterInfo.class).sparse().addJson(json).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// paths
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setPaths(Map<String,OperationMap> value) {
		paths = mapBuilder(String.class,OperationMap.class).sparse().sorted(PATH_COMPARATOR).addAll(value).build();
	}

	/**
	 * Bean property appender:  <property>paths</property>.
	 *
	 * <p>
	 * The available paths and operations for the API.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addPaths(Map<String,OperationMap> values) {
		paths = mapBuilder(paths).sparse().sorted(PATH_COMPARATOR).addAll(values).build();
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
	public Swagger path(String path, String methodName, Operation operation) {
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
	 * Bean property fluent getter:  <property>paths</property>.
	 *
	 * <p>
	 * The available paths and operations for the API.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Map<String,OperationMap>> paths() {
		return Optional.ofNullable(getPaths());
	}

	/**
	 * Bean property fluent setter:  <property>paths</property>.
	 *
	 * <p>
	 * The available paths and operations for the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Swagger paths(Map<String,OperationMap> value) {
		setPaths(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>paths</property>.
	 *
	 * <p>
	 * The available paths and operations for the API.
	 *
	 * @param json
	 * 	The values to set on this property as JSON.
	 * @return This object.
	 */
	public Swagger paths(String json) {
		setPaths(mapBuilder(String.class,OperationMap.class).sparse().sorted(PATH_COMPARATOR).addJson(json).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// produces
	//-----------------------------------------------------------------------------------------------------------------

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
	 * 	<br>Value MUST be as described under {@doc ExtSwaggerMimeTypes}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setProduces(Collection<MediaType> value) {
		produces = newSet(value);
	}

	/**
	 * Adds one or more values to the <property>produces</property> property.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Value MUST be as described under {@doc ExtSwaggerMimeTypes}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger addProduces(Collection<MediaType> values) {
		produces = setBuilder(produces).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Set<MediaType>> produces() {
		return Optional.ofNullable(getProduces());
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
	public Swagger produces(Collection<MediaType> value) {
		setProduces(value);
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
	public Swagger produces(MediaType...value) {
		setProduces(setBuilder(MediaType.class).sparse().add(value).build());
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
	 * 	<br>Strings can be JSON arrays.
	 * @return This object.
	 */
	public Swagger produces(String...value) {
		setProduces(setBuilder(MediaType.class).sparse().addJson(value).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// responses
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setResponses(Map<String,ResponseInfo> value) {
		responses = newMap(value);
	}

	/**
	 * Bean property appender:  <property>responses</property>.
	 *
	 * <p>
	 * Adds one or more values to the <property>responses</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addResponses(Map<String,ResponseInfo> values) {
		responses = mapBuilder(responses).sparse().addAll(values).build();
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
	public Swagger response(String name, ResponseInfo response) {
		responses = mapBuilder(responses).sparse().add(name, response).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>responses</property>.
	 *
	 * <p>
	 * An object to hold responses that can be used across operations.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Map<String,ResponseInfo>> responses() {
		return Optional.ofNullable(getResponses());
	}

	/**
	 * Bean property fluent setter:  <property>responses</property>.
	 *
	 * <p>
	 * An object to hold responses that can be used across operations.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Swagger responses(Map<String,ResponseInfo> value) {
		setResponses(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>responses</property>.
	 *
	 * <p>
	 * An object to hold responses that can be used across operations.
	 *
	 * @param json
	 * 	The values to set on this property as JSON.
	 * @return This object.
	 */
	public Swagger responses(String json) {
		setResponses(mapBuilder(String.class,ResponseInfo.class).sparse().addJson(json).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// schemes
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setSchemes(Collection<String> value) {
		schemes = newSet(value);
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
	 * Bean property fluent getter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol of the API.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Set<String>> schemes() {
		return Optional.ofNullable(getSchemes());
	}

	/**
	 * Bean property fluent setter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol of the API.
	 *
	 * @param value
	 * 	The values to set on this property.
	 * @return This object.
	 */
	public Swagger schemes(Collection<String> value) {
		setSchemes(value);
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
	public Swagger schemes(String...value) {
		setSchemes(setBuilder(String.class).sparse().addJson(value).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// security
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setSecurity(Collection<Map<String,List<String>>> value) {
		security = newList(value);
	}

	/**
	 * Bean property appender:  <property>security</property>.
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
	public Swagger security(String scheme, String...alternatives) {
		Map<String,List<String>> m = new LinkedHashMap<>();
		m.put(scheme, Arrays.asList(alternatives));
		return addSecurity(Collections.singleton(m));
	}

	/**
	 * Bean property fluent getter:  <property>securities</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<List<Map<String,List<String>>>> security() {
		return Optional.ofNullable(getSecurity());
	}

	/**
	 * Bean property fluent setter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Swagger security(Collection<Map<String,List<String>>> value) {
		setSecurity(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 *
	 * @param json
	 * 	The value to set on this property as JSON.
	 * @return This object.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Swagger security(String json) {
		setSecurity((List)listBuilder(Map.class,String.class,List.class,String.class).sparse().addJson(json).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// securityDefinitions
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setSecurityDefinitions(Map<String,SecurityScheme> value) {
		securityDefinitions = newMap(value);
	}

	/**
	 * Bean property appender:  <property>securityDefinitions</property>.
	 *
	 * <p>
	 * Security scheme definitions that can be used across the specification.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addSecurityDefinitions(Map<String,SecurityScheme> values) {
		securityDefinitions = mapBuilder(securityDefinitions).sparse().addAll(values).build();
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
	public Swagger securityDefinition(String name, SecurityScheme securityScheme) {
		securityDefinitions = mapBuilder(securityDefinitions).sparse().add(name, securityScheme).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>securityDefinitions</property>.
	 *
	 * <p>
	 * Security scheme definitions that can be used across the specification.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Map<String,SecurityScheme>> securityDefinitions() {
		return Optional.ofNullable(getSecurityDefinitions());
	}

	/**
	 * Bean property fluent setter:  <property>securityDefinitions</property>.
	 *
	 * <p>
	 * Security scheme definitions that can be used across the specification.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Swagger securityDefinitions(Map<String,SecurityScheme> value) {
		setSecurityDefinitions(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>securityDefinitions</property>.
	 *
	 * <p>
	 * Security scheme definitions that can be used across the specification.
	 *
	 * @param json
	 * 	The value to set on this property as JSON.
	 * @return This object.
	 */
	public Swagger securityDefinitions(String json) {
		setSecurityDefinitions(mapBuilder(String.class,SecurityScheme.class).sparse().addJson(json).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// swagger
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setSwagger(String value) {
		swagger = value;
	}

	/**
	 * Bean property fluent getter:  <property>swagger</property>.
	 *
	 * <p>
	 * Specifies the Swagger Specification version being used.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> swagger() {
		return Optional.ofNullable(getSwagger());
	}

	/**
	 * Bean property fluent setter:  <property>swagger</property>.
	 *
	 * <p>
	 * Specifies the Swagger Specification version being used.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Swagger swagger(String value) {
		setSwagger(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// tags
	//-----------------------------------------------------------------------------------------------------------------

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
	 * 	<br>Not all tags that are used by the {@doc ExtSwaggerOperationObject Operation Object} must be declared.
	 * 	<br>The tags that are not declared may be organized randomly or based on the tools' logic.
	 * 	<br>Each tag name in the list MUST be unique.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setTags(Collection<Tag> value) {
		tags = newSet(value);
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
	 * 	<br>Not all tags that are used by the {@doc ExtSwaggerOperationObject Operation Object} must be declared.
	 * 	<br>The tags that are not declared may be organized randomly or based on the tools' logic.
	 * 	<br>Each tag name in the list MUST be unique.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public Swagger addTags(Collection<Tag> values) {
		tags = setBuilder(tags).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Set<Tag>> tags() {
		return Optional.ofNullable(getTags());
	}

	/**
	 * Bean property fluent setter:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public Swagger tags(Collection<Tag> value) {
		setTags(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>tags</property>.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @param json
	 * 	The new value for this property as JSON.
	 * @return This object.
	 */
	public Swagger tags(String json) {
		setTags(setBuilder(Tag.class).sparse().addJson(json).build());
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
	 * Shortcut for calling <c>Optional.of(getPaths().get(path));</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).
	 * @return The operation map for the specified path as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<OperationMap> path(String path) {
		return Optional.ofNullable(getPath(path));
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
	 * Shortcut for calling <c>Optional.of(getPaths().get(path).get(operation));</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).
	 * @param operation The HTTP operation (e.g. <js>"get"</js>).
	 * @return The operation for the specified path and operation id as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Operation> operation(String path, String operation) {
		return Optional.ofNullable(getOperation(path, operation));
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
	 * Shortcut for calling <c>Optional.of(getPaths().get(path).get(operation).getResponse(status));</c>
	 *
	 * @param path The path (e.g. <js>"/foo"</js>).
	 * @param operation The HTTP operation (e.g. <js>"get"</js>).
	 * @param status The HTTP response status (e.g. <js>"200"</js>).
	 * @return The operation for the specified path and operation id as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<ResponseInfo> responseInfo(String path, String operation, String status) {
		return Optional.ofNullable(getResponseInfo(path, operation, status));
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

	/**
	 * Convenience method for calling <c>getPath(path).get(method).getParameter(in,name);</c>
	 *
	 * @param path The HTTP path.
	 * @param method The HTTP method.
	 * @param in The parameter type.
	 * @param name The parameter name.
	 * @return The parameter information as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<ParameterInfo> parameterInfo(String path, String method, String in, String name) {
		return Optional.of(getParameterInfo(path, method, in, name));
	}


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
			case "basePath": return basePath(stringify(value));
			case "consumes": return consumes(listBuilder(MediaType.class).sparse().addAny(value).build());
			case "definitions": return definitions(mapBuilder(String.class,OMap.class).sparse().addAny(value).build());
			case "externalDocs": return externalDocs(toType(value, ExternalDocumentation.class));
			case "host": return host(stringify(value));
			case "info": return info(toType(value, Info.class));
			case "parameters": return parameters(mapBuilder(String.class,ParameterInfo.class).sparse().addAny(value).build());
			case "paths": return paths(mapBuilder(String.class,OperationMap.class).sparse().addAny(value).build());
			case "produces": return produces(listBuilder(MediaType.class).sparse().addAny(value).build());
			case "responses": return responses(mapBuilder(String.class,ResponseInfo.class).sparse().addAny(value).build());
			case "schemes": return schemes(listBuilder(String.class).sparse().addAny(value).build());
			case "security": return security((List)listBuilder(Map.class,String.class,List.class,String.class).sparse().addAny(value).build());
			case "securityDefinitions": return securityDefinitions(mapBuilder(String.class,SecurityScheme.class).sparse().addAny(value).build());
			case "swagger": return swagger(stringify(value));
			case "tags": return tags(listBuilder(Tag.class).sparse().addAny(value).build());
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = ASet.<String>of()
			.appendIf(basePath != null, "basePath")
			.appendIf(consumes != null, "consumes")
			.appendIf(definitions != null, "definitions")
			.appendIf(externalDocs != null, "externalDocs")
			.appendIf(host != null, "host")
			.appendIf(info != null, "info")
			.appendIf(parameters != null, "parameters")
			.appendIf(paths != null, "paths")
			.appendIf(produces != null, "produces")
			.appendIf(responses != null, "responses")
			.appendIf(schemes != null, "schemes")
			.appendIf(security != null, "security")
			.appendIf(securityDefinitions != null, "securityDefinitions")
			.appendIf(swagger != null, "swagger")
			.appendIf(tags != null, "tags");
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Object */
	public String toString() {
		return JsonSerializer.DEFAULT.toString(this);
	}

	/**
	 * Resolves a <js>"$ref"</js> tags to nodes in this swagger document.
	 *
	 * @param ref The ref tag value.
	 * @param c The class to convert the reference to.
	 * @return The referenced node, or <jk>null</jk> if the ref was <jk>null</jk> or empty or not found.
	 */
	public <T> T findRef(String ref, Class<T> c) {
		if (isEmpty(ref))
			return null;
		if (! ref.startsWith("#/"))
			throw runtimeException("Unsupported reference:  ''{0}''", ref);
		try {
			return new PojoRest(this).get(ref.substring(1), c);
		} catch (Exception e) {
			throw new BeanRuntimeException(e, c, "Reference ''{0}'' could not be converted to type ''{1}''.", ref, className(c));
		}
	}
}
