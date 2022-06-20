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
import org.apache.juneau.annotation.BeanProperty;
import org.apache.juneau.internal.MultiSet;
import org.apache.juneau.internal.StringUtils;
import org.apache.juneau.utils.ASet;

import java.net.URI;
import java.net.URL;
import java.util.*;

import static org.apache.juneau.internal.BeanPropertyUtils.*;

@Bean(properties="schema,example,examples,encoding,*")
public class MediaType extends OpenApiElement{
    private SchemaInfo schema;
    private Object example;
    private Map<String,Example> examples;
    private Map<String,Encoding> encoding;

    /**
     * Default constructor.
     */
    public MediaType() { }

    /**
     * Copy constructor.
     *
     * @param copyFrom The object to copy.
     */
    public MediaType(MediaType copyFrom) {
        super(copyFrom);

        this.schema = copyFrom.schema;
        this.example = copyFrom.example;
        if (copyFrom.examples == null)
            this.examples = null;
        else
            this.examples = new LinkedHashMap<>();
        for (Map.Entry<String,Example> e : copyFrom.examples.entrySet())
            this.examples.put(e.getKey(),	e.getValue().copy());

        if (copyFrom.encoding == null)
            this.encoding = null;
        else
            this.encoding = new LinkedHashMap<>();
        for (Map.Entry<String,Encoding> e : copyFrom.encoding.entrySet())
            this.encoding.put(e.getKey(),	e.getValue().copy());
    }

    /**
     * Make a deep copy of this object.
     *
     * @return A deep copy of this object.
     */
    public MediaType copy() {
        return new MediaType(this);
    }

    @Override /* OpenApiElement */
    protected MediaType strict() {
        super.strict();
        return this;
    }

    /**
     * Bean property getter:  <property>schema</property>.
     *
     * @return The property value, or <jk>null</jk> if it is not set.
     */
    public SchemaInfo getSchema() {
        return schema;
    }

    /**
     * Bean property setter:  <property>schema</property>.
     *
     * @param value
     * 	The new value for this property.
     * 	<br>Can be <jk>null</jk> to unset the property.
     * @return This object (for method chaining).
     */
    public MediaType setSchema(SchemaInfo value) {
        schema = value;
        return this;
    }

    /**
     * Same as {@link #setSchema(SchemaInfo)}.
     *
     * @param value
     * 	The new value for this property.
     * 	<br>Valid types:
     * 	<br>Can be <jk>null</jk> to unset the property.
     * @return This object (for method chaining).
     */
    public MediaType schema(Object value) {
        return setSchema(toType(value, SchemaInfo.class));
    }


    /**
     * Bean property getter:  <property>x-example</property>.
     *
     * @return The property value, or <jk>null</jk> if it is not set.
     */
    @BeanProperty("x-example")
    public Object getExample() {
        return example;
    }

    /**
     * Bean property setter:  <property>examples</property>.
     *
     * @param value
     * 	The new value for this property.
     * 	<br>Can be <jk>null</jk> to unset the property.
     * @return This object (for method chaining).
     */
    @BeanProperty("x-example")
    public MediaType setExample(Object value) {
        example = value;
        return this;
    }

    /**
     * Bean property setter:  <property>examples</property>.
     *
     * @param value
     * 	The new value for this property.
     * 	<br>Can be <jk>null</jk> to unset the property.
     * @return This object (for method chaining).
     */
    public MediaType example(Object value) {
        example = value;
        return this;
    }

    /**
     * Bean property getter:  <property>variables</property>.
     */
    public Map<String, Encoding> getEncoding() {
        return encoding;
    }

    /**
     * Bean property setter:  <property>variables</property>.
     *
     * @param value
     * 	The new value for this property.
     * @return This object (for method chaining).
     */
    public MediaType setEncoding(Map<String, Encoding> value) {
        encoding = newMap(value);
        return this;
    }

    /**
     * Adds one or more values to the <property>encoding</property> property.
     *
     * @param value
     * 	The values to add to this property.
     * 	<br>Ignored if <jk>null</jk>.
     * @return This object (for method chaining).
     */
    public MediaType addEncoding(Map<String, Encoding> value) {
        encoding = addToMap(encoding,value);
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
    public MediaType addEncoding(String keyval, Encoding value) {
        encoding = addToMap(encoding,keyval,value);
        return this;
    }

    /**
     * Adds a single value to the <property>headers</property> property.
     *
     * @param name variable name.
     * @param value The server variable instance.
     * @return This object (for method chaining).
     */
    public MediaType encoding(String name, Encoding value) {
        addEncoding(Collections.singletonMap(name, value));
        return this;
    }

    public MediaType encoding(Object value) {
        return setEncoding((HashMap<String,Encoding>)value);
    }


    /**
     * Bean property getter:  <property>examples</property>.
     *
     * <p>
     * The list of possible responses as they are returned from executing this operation.
     *
     * @return The property value, or <jk>null</jk> if it is not set.
     */
    public Map<String,Example> getExamples() {
        return examples;
    }

    /**
     * Bean property setter:  <property>headers</property>.
     *
     * <p>
     * A list of examples that are sent with the response.
     *
     * @param value
     * 	The new value for this property.
     * 	<br>Can be <jk>null</jk> to unset the property.
     * @return This object (for method chaining).
     */
    public MediaType setExamples(Map<String,Example> value) {
        examples = newMap(value);
        return this;
    }

    /**
     * Adds one or more values to the <property>headers</property> property.
     *
     * @param values
     * 	The values to add to this property.
     * 	<br>Ignored if <jk>null</jk>.
     * @return This object (for method chaining).
     */
    public MediaType addExamples(Map<String,Example> values) {
        examples = addToMap(examples, values);
        return this;
    }

    /**
     * Adds a single value to the <property>examples</property> property.
     *
     * @param name The example name.
     * @param example The example.
     * @return This object (for method chaining).
     */
    public MediaType example(String name, Example example) {
        addExamples(Collections.singletonMap(name, example));
        return this;
    }
    /**
     * Adds one or more values to the <property>examples</property> property.
     *
     * @param values
     * 	The values to add to this property.
     * 	<br>Valid types:
     * 	<ul>
     * 		<li><code>Map&lt;String,{@link org.apache.juneau.dto.swagger.HeaderInfo}|String&gt;</code>
     * 		<li><code>String</code> - JSON object representation of <code>Map&lt;String,{@link org.apache.juneau.dto.swagger.HeaderInfo}&gt;</code>
     * 			<h5 class='figure'>Example:</h5>
     * 			<p class='bcode w800'>
     * 	headers(<js>"{headerName:{description:'description',...}}"</js>);
     * 			</p>
     * 	</ul>
     * 	<br>Ignored if <jk>null</jk>.
     * @return This object (for method chaining).
     */
    public MediaType examples(Object...values) {
        examples = addToMap(examples,values, String.class, Example.class);
        return this;
    }

    @Override /* OpenApiElement */
    public <T> T get(String property, Class<T> type) {
        if (property == null)
            return null;
        switch (property) {
            case "schema": return toType(getSchema(), type);
            case "example": return toType(getExample(), type);
            case "examples": return toType(getExamples(), type);
            case "encoding": return toType(getEncoding(), type);
            default: return super.get(property, type);
        }
    }

    @Override /* OpenApiElement */
    public MediaType set(String property, Object value) {
        if (property == null)
            return this;
        switch (property) {
            case "schema": return schema(value);
            case "example": return example(value);
            case "examples": return examples(value);
            case "encoding": return encoding(value);
            default:
                super.set(property, value);
                return this;
        }
    }

    @Override /* OpenApiElement */
    public Set<String> keySet() {
        ASet<String> s = new ASet<String>()
                .appendIf(schema != null, "schema")
                .appendIf(example != null, "example")
                .appendIf(encoding != null, "encoding")
                .appendIf(examples != null, "examples");
        return new MultiSet<>(s, super.keySet());
    }
}
