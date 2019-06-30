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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.utils.*;

/**
 * This is the root document object for the API specification.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-dto.Swagger}
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
	private List<String> schemes;
	private List<MediaType>
		consumes,
		produces;
	private List<Tag> tags;
	private List<Map<String,List<String>>> security;
	private Map<String,ObjectMap> definitions;
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

		this.swagger = copyFrom.swagger;
		this.host = copyFrom.host;
		this.basePath = copyFrom.basePath;
		this.info = copyFrom.info == null ? null : copyFrom.info.copy();
		this.externalDocs = copyFrom.externalDocs == null ? null : copyFrom.externalDocs.copy();
		this.schemes = newList(copyFrom.schemes);
		this.consumes = newList(copyFrom.consumes);
		this.produces = newList(copyFrom.produces);

		if (copyFrom.tags == null) {
			this.tags = null;
		} else {
			this.tags = new ArrayList<>();
			for (Tag t : copyFrom.tags)
				this.tags.add(t.copy());
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

		// TODO - Definitions are not deep copied, so they should not contain references.
		if (copyFrom.definitions == null) {
			this.definitions = null;
		} else {
			this.definitions = new LinkedHashMap<>();
			for (Map.Entry<String,ObjectMap> e : copyFrom.definitions.entrySet())
				this.definitions.put(e.getKey(), new ObjectMap(e.getValue()));
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

		if (copyFrom.securityDefinitions == null) {
			this.securityDefinitions = null;
		} else {
			this.securityDefinitions = new LinkedHashMap<>();
			for (Map.Entry<String,SecurityScheme> e : copyFrom.securityDefinitions.entrySet())
				this.securityDefinitions.put(e.getKey(), e.getValue().copy());
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
	 * Bean property getter:  <property>swagger</property>.
	 *
	 * <p>
	 * Specifies the Swagger Specification version being used.
	 *
	 * <p>
	 * It can be used by the Swagger UI and other clients to interpret the API listing.
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
	 * <p>
	 * It can be used by the Swagger UI and other clients to interpret the API listing.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public Swagger setSwagger(String value) {
		swagger = value;
		return this;
	}

	/**
	 * Same as {@link #setSwagger(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger swagger(Object value) {
		return setSwagger(stringify(value));
	}

	/**
	 * Bean property getter:  <property>info</property>.
	 *
	 * <p>
	 * Provides metadata about the API.
	 *
	 * <p>
	 * The metadata can be used by the clients if needed.
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
	 * <p>
	 * The metadata can be used by the clients if needed.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public Swagger setInfo(Info value) {
		info = value;
		return this;
	}

	/**
	 * Same as {@link #setInfo(Info)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link Info}
	 * 		<li><c>String</c> - JSON object representation of {@link Info}
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	info(<js>"{title:'title',description:'description',...}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public Swagger info(Object value) {
		return setInfo(toType(value, Info.class));
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
	 * 	<br>The host does not support {@doc SwaggerPathTemplating path templating}
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger setHost(String value) {
		host = value;
		return this;
	}

	/**
	 * Same as {@link #setHost(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>This MUST be the host only and does not include the scheme nor sub-paths.
	 * 	<br>It MAY include a port.
	 * 	<br>If the host is not included, the host serving the documentation is to be used (including the port).
	 * 	<br>The host does not support {@doc SwaggerPathTemplating path templating}
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger host(Object value) {
		return setHost(stringify(value));
	}

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
	 * 	<br>The <c>basePath</c> does not support {@doc SwaggerPathTemplating path templating}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger setBasePath(String value) {
		basePath = value;
		return this;
	}

	/**
	 * Same as {@link #setBasePath(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <c>toString()</c>.
	 * 	<br>If it is not included, the API is served directly under the <c>host</c>.
	 * 	<br>The value MUST start with a leading slash (/).
	 * 	<br>The <c>basePath</c> does not support {@doc SwaggerPathTemplating path templating}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger basePath(Object value) {
		return setBasePath(stringify(value));
	}

	/**
	 * Bean property getter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol of the API.
	 *
	 * <p>
	 * If the <c>schemes</c> is not included, the default scheme to be used is the one used to access the Swagger
	 * definition itself.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<String> getSchemes() {
		return schemes;
	}

	/**
	 * Bean property setter:  <property>schemes</property>.
	 *
	 * <p>
	 * The transfer protocol of the API.
	 *
	 * <p>
	 * If the <c>schemes</c> is not included, the default scheme to be used is the one used to access the Swagger
	 * definition itself.
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
	 * @return This object (for method chaining).
	 */
	public Swagger setSchemes(Collection<String> value) {
		schemes = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>schemes</property> property.
	 *
	 * <p>
	 * The transfer protocol of the API.
	 *
	 * <p>
	 * Values MUST be from the list:  <js>"http"</js>, <js>"https"</js>, <js>"ws"</js>, <js>"wss"</js>.
	 * If the <c>schemes</c> is not included, the default scheme to be used is the one used to access the Swagger
	 * definition itself.
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
	 * @return This object (for method chaining).
	 */
	public Swagger addSchemes(Collection<String> values) {
		schemes = addToList(schemes, values);
		return this;
	}

	/**
	 * Same as {@link #addSchemes(Collection)}.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><c>Collection&lt;String&gt;</c>
	 * 		<li><c>String</c> - JSON array representation of <c>Collection&lt;String&gt;</c>
	 * 		<h5 class='figure'>Example:</h5>
	 * 		<p class='bcode w800'>
	 * 	schemes(<js>"['scheme1','scheme2']"</js>);
	 * 		</p>
	 * 		<li><c>String</c> - Individual values
	 * 		<h5 class='figure'>Example:</h5>
	 * 		<p class='bcode w800'>
	 * 	schemes(<js>"scheme1"</js>, <js>"scheme2"</js>);
	 * 		</p>
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public Swagger schemes(Object...values) {
		schemes = addToList(schemes, values, String.class);
		return this;
	}

	/**
	 * Bean property getter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can consume.
	 *
	 * <p>
	 * This is global to all APIs but can be overridden on specific API calls.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<MediaType> getConsumes() {
		return consumes;
	}

	/**
	 * Bean property setter:  <property>consumes</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can consume.
	 *
	 * <p>
	 * This is global to all APIs but can be overridden on specific API calls.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Value MUST be as described under {@doc SwaggerMimeTypes}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger setConsumes(Collection<MediaType> value) {
		consumes = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>consumes</property> property.
	 *
	 * <p>
	 * A list of MIME types the operation can consume.
	 * This overrides the <c>consumes</c> definition at the Swagger Object.
	 * An empty value MAY be used to clear the global definition.
	 * Value MUST be as described under {@doc SwaggerMimeTypes}.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Values MUST be as described under {@doc SwaggerMimeTypes}.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger addConsumes(Collection<MediaType> values) {
		consumes = addToList(consumes, values);
		return this;
	}

	/**
	 * Adds one or more values to the <property>consumes</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link MediaType}
	 * 		<li><c>Collection&lt;{@link MediaType}|String&gt;</c>
	 * 		<li><c>String</c> - JSON array representation of <c>Collection&lt;{@link MediaType}&gt;</c>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	consumes(<js>"['text/json']"</js>);
	 * 			</p>
	 * 		<li><c>String</c> - Individual values
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	consumes(<js>"text/json"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger consumes(Object...values) {
		consumes = addToList(consumes, values, MediaType.class);
		return this;
	}

	/**
	 * Bean property getter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * <p>
	 * This is global to all APIs but can be overridden on specific API calls.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<MediaType> getProduces() {
		return produces;
	}

	/**
	 * Bean property setter:  <property>produces</property>.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * <p>
	 * This is global to all APIs but can be overridden on specific API calls.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Value MUST be as described under {@doc SwaggerMimeTypes}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger setProduces(Collection<MediaType> value) {
		produces = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>produces</property> property.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * <p>
	 * This is global to all APIs but can be overridden on specific API calls.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Value MUST be as described under {@doc SwaggerMimeTypes}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger addProduces(Collection<MediaType> values) {
		produces = addToList(produces, values);
		return this;
	}

	/**
	 * Adds one or more values to the <property>produces</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link MediaType}
	 * 		<li><c>Collection&lt;{@link MediaType}|String&gt;</c>
	 * 		<li><c>String</c> - JSON array representation of <c>Collection&lt;{@link MediaType}&gt;</c>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	consumes(<js>"['text/json']"</js>);
	 * 			</p>
	 * 		<li><c>String</c> - Individual values
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	consumes(<js>"text/json"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger produces(Object...values) {
		produces = addToList(produces, values, MediaType.class);
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
	public ResponseInfo getResponseInfo(String path, String operation, Object status) {
		OperationMap om = getPath(path);
		if (om == null)
			return null;
		Operation op = om.get(operation);
		if (op == null)
			return null;
		return op.getResponse(status);
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
	 * @return This object (for method chaining).
	 */
	public Swagger setPaths(Map<String,OperationMap> value) {
		paths = newSortedMap(value, PATH_COMPARATOR);
		return this;
	}

	/**
	 * Adds one or more values to the <property>produces</property> property.
	 *
	 * <p>
	 * A list of MIME types the APIs can produce.
	 *
	 * <p>
	 * This is global to all APIs but can be overridden on specific API calls.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger addPaths(Map<String,OperationMap> values) {
		paths = addToSortedMap(paths, values, PATH_COMPARATOR);
		return this;
	}

	/**
	 * Adds a single value to the <property>paths</property> property.
	 *
	 * @param path The path template.
	 * @param methodName The HTTP method name.
	 * @param operation The operation that describes the path.
	 * @return This object (for method chaining).
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
	 * Adds one or more values to the <property>paths</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><c>Map&lt;String,Map&lt;String,{@link Operation}&gt;|String&gt;</c>
	 * 		<li><c>String</c> - JSON object representation of <c>Map&lt;String,Map&lt;String,{@link Operation}&gt;&gt;</c>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	paths(<js>"{'foo':{'get':{operationId:'operationId',...}}}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Swagger paths(Object...values) {
		if (paths == null)
			paths = new TreeMap<>(PATH_COMPARATOR);
		paths = addToMap((Map)paths, values, String.class, Map.class, String.class, Operation.class);
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
	public Map<String,ObjectMap> getDefinitions() {
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
	 * @return This object (for method chaining).
	 */
	public Swagger setDefinitions(Map<String,ObjectMap> value) {
		definitions = newMap(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>definitions</property> property.
	 *
	 * <p>
	 * An object to hold data types produced and consumed by operations.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger addDefinitions(Map<String,ObjectMap> values) {
		definitions = addToMap(definitions, values);
		return this;
	}

	/**
	 * Adds a single value to the <property>definitions</property> property.
	 *
	 * @param name A definition name.
	 * @param schema The schema that the name defines.
	 * @return This object (for method chaining).
	 */
	public Swagger definition(String name, ObjectMap schema) {
		definitions = addToMap(definitions, name, schema);
		return this;
	}

	/**
	 * Adds one or more values to the <property>definitions</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><c>Map&lt;String,Map&lt;String,{@link SchemaInfo}&gt;|String&gt;</c>
	 * 		<li><c>String</c> - JSON object representation of <c>Map&lt;String,Map&lt;String,{@link SchemaInfo}&gt;&gt;</c>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	definitions(<js>"{'foo':{'bar':{format:'format',...}}}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger definitions(Object...values) {
		definitions = addToMap(definitions, values, String.class, ObjectMap.class);
		return this;
	}

	/**
	 * Convenience method for testing whether this Swagger has one or more definitions defined.
	 *
	 * @return <jk>true</jk> if this Swagger has one or more definitions defined.
	 */
	public boolean hasDefinitions() {
		return definitions != null && ! definitions.isEmpty();
	}

	/**
	 * Bean property getter:  <property>parameters</property>.
	 *
	 * <p>
	 * An object to hold parameters that can be used across operations.
	 *
	 * <p>
	 * This property does not define global parameters for all operations.
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
	 * <p>
	 * This property does not define global parameters for all operations.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger setParameters(Map<String,ParameterInfo> value) {
		parameters = newMap(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>parameters</property> property.
	 *
	 * <p>
	 * An object to hold parameters that can be used across operations.
	 *
	 * <p>
	 * This property does not define global parameters for all operations.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger addParameters(Map<String,ParameterInfo> values) {
		parameters = addToMap(parameters, values);
		return this;
	}

	/**
	 * Adds a single value to the <property>parameter</property> property.
	 *
	 * @param name The parameter name.
	 * @param parameter The parameter definition.
	 * @return This object (for method chaining).
	 */
	public Swagger parameter(String name, ParameterInfo parameter) {
		parameters = addToMap(parameters, name, parameter);
		return this;
	}

	/**
	 * Adds one or more values to the <property>properties</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><c>Map&lt;String,{@link ParameterInfo}|String&gt;</c>
	 * 		<li><c>String</c> - JSON object representation of <c>Map&lt;String,{@link ParameterInfo}&gt;</c>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	parameters(<js>"{'foo':{name:'name',...}}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger parameters(Object...values) {
		parameters = addToMap(parameters, values, String.class, ParameterInfo.class);
		return this;
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
	 * Bean property getter:  <property>responses</property>.
	 *
	 * <p>
	 * An object to hold responses that can be used across operations.
	 *
	 * <p>
	 * This property does not define global responses for all operations.
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
	 * <p>
	 * This property does not define global responses for all operations.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger setResponses(Map<String,ResponseInfo> value) {
		responses = newMap(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>responses</property> property.
	 *
	 * <p>
	 * An object to hold responses that can be used across operations.
	 *
	 * <p>
	 * This property does not define global responses for all operations.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger addResponses(Map<String,ResponseInfo> values) {
		responses = addToMap(responses, values);
		return this;
	}

	/**
	 * Adds a single value to the <property>responses</property> property.
	 *
	 * @param name The response name.
	 * @param response The response definition.
	 * @return This object (for method chaining).
	 */
	public Swagger response(String name, ResponseInfo response) {
		responses = addToMap(responses, name, response);
		return this;
	}

	/**
	 * Adds one or more values to the <property>properties</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><c>Map&lt;String,{@link ResponseInfo}|String&gt;</c>
	 * 		<li><c>String</c> - JSON object representation of <c>Map&lt;String,{@link ResponseInfo}&gt;</c>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	responses(<js>"{'foo':{description:'description',...}}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger responses(Object...values) {
		responses = addToMap(responses, values, String.class, ResponseInfo.class);
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
	 * @return This object (for method chaining).
	 */
	public Swagger setSecurityDefinitions(Map<String,SecurityScheme> value) {
		securityDefinitions = newMap(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>securityDefinitions</property> property.
	 *
	 * <p>
	 * Security scheme definitions that can be used across the specification.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger addSecurityDefinitions(Map<String,SecurityScheme> values) {
		securityDefinitions = addToMap(securityDefinitions, values);
		return this;
	}

	/**
	 * Adds a single value to the <property>securityDefinitions</property> property.
	 *
	 * @param name A security name.
	 * @param securityScheme A security schema.
	 * @return This object (for method chaining).
	 */
	public Swagger securityDefinition(String name, SecurityScheme securityScheme) {
		securityDefinitions = addToMap(securityDefinitions, name, securityScheme);
		return this;
	}

	/**
	 * Adds one or more values to the <property>securityDefinitions</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><c>Map&lt;String,{@link SecurityScheme}|String&gt;</c>
	 * 		<li><c>String</c> - JSON object representation of <c>Map&lt;String,{@link SecurityScheme}&gt;</c>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	securityDefinitions(<js>"{'foo':{name:'name',...}}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger securityDefinitions(Object...values) {
		securityDefinitions = addToMap(securityDefinitions, values, String.class, SecurityScheme.class);
		return this;
	}

	/**
	 * Bean property getter:  <property>security</property>.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 *
	 * <p>
	 * The list of values describes alternative security schemes that can be used (that is, there is a logical OR
	 * between the security requirements).
	 * <br>Individual operations can override this definition.
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
	 * <p>
	 * The list of values describes alternative security schemes that can be used (that is, there is a logical OR
	 * between the security requirements).
	 * <br>Individual operations can override this definition.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger setSecurity(Collection<Map<String,List<String>>> value) {
		security = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>security</property> property.
	 *
	 * <p>
	 * A declaration of which security schemes are applied for the API as a whole.
	 *
	 * <p>
	 * The list of values describes alternative security schemes that can be used (that is, there is a logical OR
	 * between the security requirements).
	 * <br>Individual operations can override this definition.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger addSecurity(Collection<Map<String,List<String>>> values) {
		security = addToList(security, values);
		return this;
	}


	/**
	 * Adds a single value to the <property>securityDefinitions</property> property.
	 *
	 * @param scheme The security scheme that applies to this operation
	 * @param alternatives
	 * 	The list of values describes alternative security schemes that can be used (that is, there is a logical OR between the security requirements).
	 * @return This object (for method chaining).
	 */
	public Swagger security(String scheme, String...alternatives) {
		Map<String,List<String>> m = new LinkedHashMap<>();
		m.put(scheme, Arrays.asList(alternatives));
		return addSecurity(Collections.singleton(m));
	}

	/**
	 * Adds one or more values to the <property>securityDefinitions</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><c>Collection&lt;Map&lt;String,List&lt;String&gt;&gt;&gt;</c>
	 * 		<li><c>String</c> - JSON array representation of <c>Collection&lt;Map&lt;String,List&lt;String&gt;&gt;&gt;</c>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	securities(<js>"[{...}]"</js>);
	 * 			</p>
	 * 		<li><c>String</c> - JSON object representation of <c>Map&lt;String,List&lt;String&gt;&gt;</c>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	securities(<js>"{...}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Swagger securities(Object...values) {
		security = addToList((List)security, values, Map.class, String.class, List.class, String.class);
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
	public List<Tag> getTags() {
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
	 * 	<br>Not all tags that are used by the {@doc SwaggerOperationObject Operation Object} must be declared.
	 * 	<br>The tags that are not declared may be organized randomly or based on the tools' logic.
	 * 	<br>Each tag name in the list MUST be unique.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger setTags(Collection<Tag> value) {
		tags = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>security</property> property.
	 *
	 * <p>
	 * A list of tags used by the specification with additional metadata.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>The order of the tags can be used to reflect on their order by the parsing tools.
	 * 	<br>Not all tags that are used by the {@doc SwaggerOperationObject Operation Object} must be declared.
	 * 	<br>The tags that are not declared may be organized randomly or based on the tools' logic.
	 * 	<br>Each tag name in the list MUST be unique.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger addTags(Collection<Tag> values) {
		tags = addToList(tags, values);
		return this;
	}


	/**
	 * Adds one or more values to the <property>tags</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link Tag}
	 * 		<li><c>Collection&lt;{@link Tag}|String&gt;</c>
	 * 		<li><c>{@link Tag}[]</c>
	 * 		<li><c>String</c> - JSON array representation of <c>Collection&lt;{@link Tag}&gt;</c>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	tags(<js>"[{name:'name',description:'description',...}]"</js>);
	 * 			</p>
	 * 		<li><c>String</c> - JSON object representation of <c>{@link Tag}</c>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	tags(<js>"{name:'name',description:'description',...}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Swagger tags(Object...values) {
		tags = addToList(tags, values, Tag.class);
		return this;
	}

	/**
	 * Convenience method for testing whether this Swagger has one or more tags defined.
	 *
	 * @return <jk>true</jk> if this Swagger has one or more tags defined.
	 */
	public boolean hasTags() {
		return tags != null && ! tags.isEmpty();
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
	 * @return This object (for method chaining).
	 */
	public Swagger setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
		return this;
	}

	/**
	 * Same as {@link #setExternalDocs(ExternalDocumentation)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link ExternalDocumentation}
	 * 		<li><c>String</c> - JSON object representation of {@link ExternalDocumentation}
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	externalDocs(<js>"{description:'description',url:'url'}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Swagger externalDocs(Object value) {
		return setExternalDocs(toType(value, ExternalDocumentation.class));
	}

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "swagger": return toType(getSwagger(), type);
			case "info": return toType(getInfo(), type);
			case "host": return toType(getHost(), type);
			case "basePath": return toType(getBasePath(), type);
			case "schemes": return toType(getSchemes(), type);
			case "consumes": return toType(getConsumes(), type);
			case "produces": return toType(getProduces(), type);
			case "paths": return toType(getPaths(), type);
			case "definitions": return toType(getDefinitions(), type);
			case "parameters": return toType(getParameters(), type);
			case "responses": return toType(getResponses(), type);
			case "securityDefinitions": return toType(getSecurityDefinitions(), type);
			case "security": return toType(getSecurity(), type);
			case "tags": return toType(getTags(), type);
			case "externalDocs": return toType(getExternalDocs(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public Swagger set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "swagger": return swagger(value);
			case "info": return info(value);
			case "host": return host(value);
			case "basePath": return basePath(value);
			case "schemes": return setSchemes(null).schemes(value);
			case "consumes": return setConsumes(null).consumes(value);
			case "produces": return setProduces(null).produces(value);
			case "paths": return setPaths(null).paths(value);
			case "definitions": return setDefinitions(null).definitions(value);
			case "parameters": return setParameters(null).parameters(value);
			case "responses": return setResponses(null).responses(value);
			case "securityDefinitions": return setSecurityDefinitions(null).securityDefinitions(value);
			case "security": return setSecurity(null).securities(value);
			case "tags": return setTags(null).tags(value);
			case "externalDocs": return externalDocs(value);
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = new ASet<String>()
			.appendIf(swagger != null, "swagger")
			.appendIf(info != null, "info")
			.appendIf(host != null, "host")
			.appendIf(basePath != null, "basePath")
			.appendIf(schemes != null, "schemes")
			.appendIf(consumes != null, "consumes")
			.appendIf(produces != null, "produces")
			.appendIf(paths != null, "paths")
			.appendIf(definitions != null, "definitions")
			.appendIf(parameters != null, "parameters")
			.appendIf(responses != null, "responses")
			.appendIf(securityDefinitions != null, "securityDefinitions")
			.appendIf(security != null, "security")
			.appendIf(tags != null, "tags")
			.appendIf(externalDocs != null, "externalDocs");
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
			throw new RuntimeException("Unsupported reference:  '" + ref + '"');
		try {
			return new PojoRest(this).get(ref.substring(1), c);
		} catch (Exception e) {
			throw new BeanRuntimeException(e, c, "Reference ''{0}'' could not be converted to type ''{1}''.", ref, c.getName());
		}
	}
}
