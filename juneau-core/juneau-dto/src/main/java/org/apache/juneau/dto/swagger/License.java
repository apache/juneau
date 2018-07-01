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

import static org.apache.juneau.internal.BeanPropertyUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.net.*;
import java.net.URI;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

/**
 * License information for the exposed API.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	License x = <jsm>license</jsm>(<js>"Apache 2.0"</js>, <js>"http://www.apache.org/licenses/LICENSE-2.0.html"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.toString(x);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String json = x.toString();
 * </p>
 * <p class='bcode'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"name"</js>: <js>"Apache 2.0"</js>,
 * 		<js>"url"</js>: <js>"http://www.apache.org/licenses/LICENSE-2.0.html"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-dto.Swagger'>Overview &gt; juneau-dto &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="name,url,*")
public class License extends SwaggerElement {

	private String name;
	private URI url;

	/**
	 * Default constructor.
	 */
	public License() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public License(License copyFrom) {
		super(copyFrom);

		this.name = copyFrom.name;
		this.url = copyFrom.url;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public License copy() {
		return new License(this);
	}

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The license name used for the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * <p>
	 * The license name used for the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public License setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Same as {@link #setName(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public License name(Object value) {
		return setName(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>url</property>.
	 *
	 * <p>
	 * A URL to the license used for the API.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public URI getUrl() {
		return url;
	}

	/**
	 * Bean property setter:  <property>url</property>.
	 *
	 * <p>
	 * A URL to the license used for the API.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>URIs defined by {@link UriResolver} can be used for values.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public License setUrl(URI value) {
		url = value;
		return this;
	}

	/**
	 * Same as {@link #setUrl(URI)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-URI values will be converted to URI using <code><jk>new</jk> URI(value.toString())</code>.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 			<br>Converted to a URI using <code>URI.<jsm>create</jsm>(value.toString())</code>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public License url(Object value) {
		return setUrl(StringUtils.toURI(value));
	}

	/**
	 * Returns <jk>true</jk> if the name property is not null or empty.
	 *
	 * @return <jk>true</jk> if the name property is not null or empty.
	 */
	public boolean hasName() {
		return isNotEmpty(name);
	}

	/**
	 * Returns <jk>true</jk> if the url property is not null.
	 *
	 * @return <jk>true</jk> if the url property is not null.
	 */
	public boolean hasUrl() {
		return url != null;
	}

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "name": return toType(getName(), type);
			case "url": return toType(getUrl(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public License set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "name": return name(value);
			case "url": return url(value);
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = new ASet<String>()
			.appendIf(name != null, "name")
			.appendIf(url != null, "url");
		return new MultiSet<>(s, super.keySet());
	}
}
