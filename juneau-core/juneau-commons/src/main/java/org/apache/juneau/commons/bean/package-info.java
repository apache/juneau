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
 *   <li>{@link BeanType @BeanType} — marks a class as a bean and configures
 *       bean-level options (interface class, stop class, property order, etc.).
 *   <li>{@link BeanProp @BeanProp} — configures a bean property
 *       (name, getter, setter, etc.).
 *   <li>{@link BeanCtor @BeanCtor} — marks a constructor as the bean
 *       constructor and names its arguments.
 *   <li>{@link BeanIgnore @BeanIgnore} — marks a class, method, field, or
 *       constructor as not a bean / not a bean property.
 *   <li>{@link BeanConfig @BeanConfig} — applies bean-modeling settings
 *       declaratively at the config-annotation level.
 *   <li>{@link Name @Name} — alternative property name on a parameter.
 * </ul>
 *
 * <h5 class='topic'>Runtime types</h5>
 * <ul>
 *   <li>{@link BeanMeta} — metadata describing a bean class
 *       (properties, constructor, type info).
 *   <li>{@link BeanPropertyMeta} — metadata describing a single
 *       bean property.
 *   <li>{@link BeanMap} — {@link Map} view of a bean
 *       backed by its {@link BeanMeta}.
 *   <li>{@link BeanMapEntry} — {@link Map.Entry} view of a
 *       single bean property.
 *   <li>{@link BeanPropertyValue} — read-side property holder used by
 *       consumers that iterate bean properties.
 *   <li>{@link BeanPropertyConsumer} — callback for iterating bean
 *       properties without materializing a {@link BeanMap}.
 *   <li>{@link BeanProxyInvocationHandler} — {@link InvocationHandler}
 *       that backs interface-only bean proxies.
 *   <li>{@link BeanInterceptor} — pre/post hooks fired around bean
 *       property reads / writes.
 * </ul>
 *
 * <h5 class='topic'>SPI seams</h5>
 * <ul>
 *   <li>{@link BeanConfigContext} — immutable POJO carrying all
 *       bean-modeling configuration knobs plus SPI hooks. {@code MarshallingContext} (in
 *       {@code juneau-marshall}) installs the marshalling-side resolver / initializer / post-processor.
 *   <li>{@link BeanInfo} — abstract type-classification surface;
 *       {@code ClassMeta} extends it on the marshalling side.
 *   <li>{@link BeanTypeResolver} — resolves {@link Type}
 *       references to {@link BeanInfo} instances.
 *   <li>{@link BeanSession} — per-operation session surface (type
 *       conversion, sub-bean construction, CharSequence parsing).
 *   <li>{@link BeanFilter} — per-class filter surface;
 *       {@code MarshalledFilter} implements it.
 *   <li>{@link BeanRegistryLookup} — bean-dictionary lookup surface;
 *       {@code BeanRegistry} implements it.
 *   <li>{@link BeanMetaInitializer} — hook that lets marshalling-side
 *       code augment {@link BeanMeta} construction (filter discovery,
 *       type-name resolution, bean-registry assembly).
 *   <li>{@link BeanPropertyPostProcessor} — hook that lets marshalling-side
 *       code augment {@link BeanPropertyMeta} construction
 *       (swap / surrogate / format annotation processing).
 *   <li>{@link Delegate} — pluggable delegate-bean marker.
 *   <li>{@link PropertyNamer} — pluggable Java-name to bean-property-name
 *       transformer.
 * </ul>
 *
 * <h5 class='topic'>Thread safety</h5>
 * <p>
 * Practical thread-safety semantics for primary runtime types and SPI seams in this package:
 * </p>
 * <ul>
 *   <li><b>Not thread-safe (mutable, no internal synchronization):</b>
 *       {@link BeanMap}, {@link BeanProxyInvocationHandler},
 *       {@link BeanConfigContext.Builder}.
 *   <li><b>Thread-safe after construction (immutable metadata/value holders):</b>
 *       {@link BeanConfigContext}, {@link BeanMeta}, {@link BeanPropertyMeta},
 *       {@link BeanPropertyValue}, {@link BeanMapEntry}.
 *   <li><b>Thread safety depends on implementation/state:</b>
 *       {@link BeanSession}, {@link BeanFilter}, {@link BeanRegistryLookup}, {@link BeanMetaInitializer},
 *       {@link BeanPropertyPostProcessor}, {@link BeanPropertyConsumer}, {@link BeanInfo}.
 * </ul>
 */
package org.apache.juneau.commons.bean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Type;
import java.util.Map;
