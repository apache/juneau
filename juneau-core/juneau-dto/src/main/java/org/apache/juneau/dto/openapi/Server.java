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
import org.apache.juneau.dto.swagger.Contact;
import org.apache.juneau.dto.swagger.HeaderInfo;
import org.apache.juneau.internal.StringUtils;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.juneau.internal.BeanPropertyUtils.*;

public class Server extends OpenApiElement{
    private URI url;
    private String description;
    private Map<String,ServerVariable> variables;

    /**
     * Default constructor.
     */
    public Server() { }

    /**
     * Copy constructor.
     *
     * @param copyFrom The object to copy.
     */
    public Server(Server copyFrom) {
        super(copyFrom);

        this.url = copyFrom.url;
        this.description = copyFrom.description;
        if (copyFrom.variables == null) {
            this.variables = null;
        } else {
            this.variables = new LinkedHashMap<>();
            for (Map.Entry<String,ServerVariable> e : copyFrom.variables.entrySet())
                this.variables.put(e.getKey(),	e.getValue().copy());
        }
    }

    /**
     * Make a deep copy of this object.
     *
     * @return A deep copy of this object.
     */
    public Server copy() {
        return new Server(this);
    }

    @Override /* OpenApiElement */
    protected Server strict() {
        super.strict();
        return this;
    }

    /**
     * Bean property getter:  <property>url</property>.
     *
     * <p>
     * The URL pointing to the contact information.
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
    public Server setUrl(URI value) {
        url = value;
        return this;
    }

    /**
     * Same as {@link #setUrl(URI)}.
     *
     * @param value
     * 	The new value for this property.
     * 	<br>Non-URI values will be converted to URI using <code><jk>new</jk> URI(value.toString())</code>.
     * 	<br>Can be <jk>null</jk> to unset the property.
     * @return This object (for method chaining).
     */
    public Server url(Object value) {
        return setUrl(StringUtils.toURI(value));
    }

    /**
     * Bean property getter:  <property>description</property>.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Bean property setter:  <property>description</property>.
     *
     * @param value
     * 	The new value for this property.
     * @return This object (for method chaining).
     */
    public Server setDescription(String value) {
        description = value;
        return this;
    }

    /**
     * Same as {@link #setDescription(String)}.
     *
     * @param value
     * 	The new value for this property.
     * @return This object (for method chaining).
     */
    public Server description(Object value) {
        return setDescription(toStringVal(value));
    }

    /**
     * Bean property getter:  <property>variables</property>.
     */
    public Map<String, ServerVariable> getVariables() {
        return variables;
    }

    /**
     * Bean property setter:  <property>variables</property>.
     *
     * @param value
     * 	The new value for this property.
     * @return This object (for method chaining).
     */
    public Server setVariables(Map<String, ServerVariable> value) {
        variables = newMap(value);
        return this;
    }

    /**
     * Adds one or more values to the <property>variables</property> property.
     *
     * @param value
     * 	The values to add to this property.
     * 	<br>Ignored if <jk>null</jk>.
     * @return This object (for method chaining).
     */
    public Server addVariables(Map<String, ServerVariable> value) {
        variables = addToMap(variables,value);
        return this;
    }

    /**
     * Adds one or more values to the <property>variables</property> property.
     *
     * @param value
     * 	The values to add to this property.
     * 	<br>Ignored if <jk>null</jk>.
     * @return This object (for method chaining).
     */
    public Server addVariables(String keyval, ServerVariable value) {
        variables = addToMap(variables,keyval,value);
        return this;
    }

    /**
     * Adds a single value to the <property>headers</property> property.
     *
     * @param name variable name.
     * @param value The server variable instance.
     * @return This object (for method chaining).
     */
    public Server variable(String name, ServerVariable value) {
        addVariables(Collections.singletonMap(name, value));
        return this;
    }

    /**
     * Adds one or more values to the <property>variables</property> property.
     *
     * @param values
     * 	The values to add to this property.
     * 	<br>Ignored if <jk>null</jk>.
     * @return This object (for method chaining).
     */
    public Server headers(Object...values) {
        variables = addToMap(variables, values, String.class, ServerVariable.class);
        return this;
    }


    public Server variables(Object value) {
        return setVariables((HashMap<String,ServerVariable>)value);
    }
}
