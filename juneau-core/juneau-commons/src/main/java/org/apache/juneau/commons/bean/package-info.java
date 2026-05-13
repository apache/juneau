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

/**
 * Bean-modeling layer for Juneau.
 *
 * <p>
 * Contains the bean-modeling annotations, runtime types, and SPI seams that describe how Java types
 * map to bean property sets, independent of any marshalling concerns. The marshalling layer (in
 * {@code juneau-marshall}) layers serializer / parser / swap behavior on top of these primitives.
 *
 * <h5 class='topic'>Annotations</h5>
 * <ul>
 *   <li>{@link org.apache.juneau.commons.bean.BeanType @BeanType} — marks a class as a bean and configures
 *       bean-level options (interface class, stop class, property order, etc.).
 *   <li>{@link org.apache.juneau.commons.bean.BeanProp @BeanProp} — configures a bean property
 *       (name, getter, setter, etc.).
 *   <li>{@link org.apache.juneau.commons.bean.BeanCtor @BeanCtor} — marks a constructor as the bean
 *       constructor and names its arguments.
 *   <li>{@link org.apache.juneau.commons.bean.BeanIgnore @BeanIgnore} — marks a class, method, field, or
 *       constructor as not a bean / not a bean property.
 *   <li>{@link org.apache.juneau.commons.bean.BeanConfig @BeanConfig} — applies bean-modeling settings
 *       declaratively at the config-annotation level.
 *   <li>{@link org.apache.juneau.commons.bean.Name @Name} — alternative property name on a parameter.
 * </ul>
 *
 * <h5 class='topic'>Runtime types</h5>
 * <ul>
 *   <li>{@link org.apache.juneau.commons.bean.BeanMeta} — metadata describing a bean class
 *       (properties, constructor, type info).
 *   <li>{@link org.apache.juneau.commons.bean.BeanPropertyMeta} — metadata describing a single
 *       bean property.
 *   <li>{@link org.apache.juneau.commons.bean.BeanMap} — {@link java.util.Map} view of a bean
 *       backed by its {@link org.apache.juneau.commons.bean.BeanMeta}.
 *   <li>{@link org.apache.juneau.commons.bean.BeanMapEntry} — {@link java.util.Map.Entry} view of a
 *       single bean property.
 *   <li>{@link org.apache.juneau.commons.bean.BeanPropertyValue} — read-side property holder used by
 *       consumers that iterate bean properties.
 *   <li>{@link org.apache.juneau.commons.bean.BeanPropertyConsumer} — callback for iterating bean
 *       properties without materializing a {@link org.apache.juneau.commons.bean.BeanMap}.
 *   <li>{@link org.apache.juneau.commons.bean.BeanProxyInvocationHandler} — {@link java.lang.reflect.InvocationHandler}
 *       that backs interface-only bean proxies.
 *   <li>{@link org.apache.juneau.commons.bean.BeanInterceptor} — pre/post hooks fired around bean
 *       property reads / writes.
 * </ul>
 *
 * <h5 class='topic'>SPI seams</h5>
 * <ul>
 *   <li>{@link org.apache.juneau.commons.bean.BeanConfigContext} — immutable POJO carrying all
 *       bean-modeling configuration knobs plus SPI hooks. {@code MarshallingContext} (in
 *       {@code juneau-marshall}) installs the marshalling-side resolver / initializer / post-processor.
 *   <li>{@link org.apache.juneau.commons.bean.BeanTypeInfo} — abstract type-classification surface;
 *       {@code ClassMeta} extends it on the marshalling side.
 *   <li>{@link org.apache.juneau.commons.bean.BeanTypeResolver} — resolves {@link java.lang.reflect.Type}
 *       references to {@link org.apache.juneau.commons.bean.BeanTypeInfo} instances.
 *   <li>{@link org.apache.juneau.commons.bean.BeanSession} — per-operation session surface (type
 *       conversion, sub-bean construction, CharSequence parsing).
 *   <li>{@link org.apache.juneau.commons.bean.BeanFilter} — per-class filter surface;
 *       {@code MarshalledFilter} implements it.
 *   <li>{@link org.apache.juneau.commons.bean.BeanRegistryLookup} — bean-dictionary lookup surface;
 *       {@code BeanRegistry} implements it.
 *   <li>{@link org.apache.juneau.commons.bean.BeanMetaInitializer} — hook that lets marshalling-side
 *       code augment {@link org.apache.juneau.commons.bean.BeanMeta} construction (filter discovery,
 *       type-name resolution, bean-registry assembly).
 *   <li>{@link org.apache.juneau.commons.bean.BeanPropertyPostProcessor} — hook that lets marshalling-side
 *       code augment {@link org.apache.juneau.commons.bean.BeanPropertyMeta} construction
 *       (swap / surrogate / format annotation processing).
 *   <li>{@link org.apache.juneau.commons.bean.Delegate} — pluggable delegate-bean marker.
 *   <li>{@link org.apache.juneau.commons.bean.PropertyNamer} — pluggable Java-name to bean-property-name
 *       transformer.
 * </ul>
 */
package org.apache.juneau.commons.bean;
