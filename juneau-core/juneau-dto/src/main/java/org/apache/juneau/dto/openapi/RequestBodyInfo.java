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
package org.apache.juneau.dto.openapi;

import org.apache.juneau.UriResolver;
import org.apache.juneau.annotation.Bean;
import org.apache.juneau.internal.MultiSet;
import org.apache.juneau.utils.ASet;

import java.net.URI;
import java.net.URL;
import java.util.*;

import static org.apache.juneau.internal.BeanPropertyUtils.*;

@Bean(properties="description,content,required,*")
public class RequestBodyInfo extends OpenApiElement{

    private String description;
    private Map<String,MediaType> content;
    private Boolean required;

    /**
     * Default constructor.
     */
    public RequestBodyInfo() { }

    /**
     * Copy constructor.
     *
     * @param copyFrom The object to copy.
     */
    public RequestBodyInfo(RequestBodyInfo copyFrom) {
        super(copyFrom);

        this.description = copyFrom.description;
        this.required = copyFrom.required;
        if (copyFrom.content == null) {
            this.content = null;
        } else {
            this.content = new LinkedHashMap<>();
            for (Map.Entry<String,MediaType> e : copyFrom.content.entrySet())
                this.content.put(e.getKey(),	e.getValue().copy());
        }
    }

    /**
     * Make a deep copy of this object.
     *
     * @return A deep copy of this object.
     */
    public RequestBodyInfo copy() {
        return new RequestBodyInfo(this);
    }

    @Override /* OpenApiElement */
    protected RequestBodyInfo strict() {
        super.strict();
        return this;
    }

    /**
     * Bean property getter:  <property>contentType</property>.
     *
     * <p>
     * The URL pointing to the contact information.
     *
     * @return The property value, or <jk>null</jk> if it is not set.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Bean property setter:  <property>url</property>.
     *
     * <p>
     * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
     * <br>Strings must be valid URIs.
     *
     * <p>
     * URIs defined by {@link UriResolver} can be used for values.
     *
     * @param value
     * 	The new value for this property.
     * 	<br>Can be <jk>null</jk> to unset the property.
     * @return This object (for method chaining).
     */
    public RequestBodyInfo setDescription(String value) {
        description = value;
        return this;
    }

    /**
     * Same as {@link #setDescription(String)}.
     *
     * @param value
     * 	The new value for this property.
     * 	<br>Non-URI values will be converted to URI using <code><jk>new</jk> URI(value.toString())</code>.
     * 	<br>Can be <jk>null</jk> to unset the property.
     * @return This object (for method chaining).
     */
    public RequestBodyInfo description(Object value) {
        return setDescription(toStringVal(value));
    }

    /**
     * Bean property getter:  <property>content</property>.
     */
    public Map<String, MediaType> getContent() {
        return content;
    }

    /**
     * Bean property setter:  <property>content</property>.
     *
     * @param value
     * 	The new value for this property.
     * @return This object (for method chaining).
     */
    public RequestBodyInfo setContent(Map<String, MediaType> value) {
        content = newMap(value);
        return this;
    }

    /**
     * Adds one or more values to the <property>content</property> property.
     *
     * @param value
     * 	The values to add to this property.
     * 	<br>Ignored if <jk>null</jk>.
     * @return This object (for method chaining).
     */
    public RequestBodyInfo addContent(Map<String, MediaType> value) {
        content = addToMap(content,value);
        return this;
    }

    /**
     * Adds one or more values to the <property>content</property> property.
     *
     * @param value
     * 	The values to add to this property.
     * 	<br>Ignored if <jk>null</jk>.
     * @return This object (for method chaining).
     */
    public RequestBodyInfo addContent(String keyval, MediaType value) {
        content = addToMap(content,keyval,value);
        return this;
    }

    /**
     * Adds a single value to the <property>content</property> property.
     *
     * @param name variable name.
     * @param value The server variable instance.
     * @return This object (for method chaining).
     */
    public RequestBodyInfo content(String name, MediaType value) {
        addContent(Collections.singletonMap(name, value));
        return this;
    }

    /**
     * Adds one or more values to the <property>content</property> property.
     *
     * @param values
     * 	The values to add to this property.
     * 	<br>Ignored if <jk>null</jk>.
     * @return This object (for method chaining).
     */
    public RequestBodyInfo content(Object...values) {
        content = addToMap(content, values, String.class, MediaType.class);
        return this;
    }

    public RequestBodyInfo content(Object value) {
        return setContent((HashMap<String,MediaType>)value);
    }


    /**
     * Bean property getter:  <property>required</property>.
     *
     * <p>
     * The type of the object.
     *
     * @return The property value, or <jk>null</jk> if it is not set.
     */
    public Boolean getRequired() {
        return required;
    }

    /**
     * Bean property setter:  <property>explode</property>.
     *
     * <p>
     * The type of the object.
     *
     * <h5 class='section'>See Also:</h5>
     * <ul class='doctree'>
     * 	<li class='extlink'>{@doc SwaggerDataTypes}
     * </ul>
     *
     * @param value
     * 	The new value for this property.
     * 	<br>Property value is required.
     * 	</ul>
     * @return This object (for method chaining).
     */
    public RequestBodyInfo setRequired(Boolean value) {
        required = value;
        return this;
    }

    /**
     * Same as {@link #setRequired(Boolean)}
     *
     * @param value
     * 	The new value for this property.
     * 	<br>Non-String values will be converted to String using <code>toBoolean()</code>.
     * 	<br>Can be <jk>null</jk> to unset the property.
     * @return This object (for method chaining).
     */
    public RequestBodyInfo required(Object value) {
        return setRequired(toBoolean(value));
    }

    @Override /* OpenApiElement */
    public <T> T get(String property, Class<T> type) {
        if (property == null)
            return null;
        switch (property) {
            case "description": return toType(getDescription(), type);
            case "content": return toType(getContent(), type);
            case "required": return toType(getRequired(), type);
            default: return super.get(property, type);
        }
    }

    @Override /* OpenApiElement */
    public RequestBodyInfo set(String property, Object value) {
        if (property == null)
            return this;
        switch (property) {
            case "description": return description(value);
            case "content": return content(value);
            case "required": return required(value);
            default:
                super.set(property, value);
                return this;
        }
    }

    @Override /* OpenApiElement */
    public Set<String> keySet() {
        ASet<String> s = new ASet<String>()
                .appendIf(description != null, "description")
                .appendIf(content != null, "content")
                .appendIf(required != null, "required");
        return new MultiSet<>(s, super.keySet());
    }
}
