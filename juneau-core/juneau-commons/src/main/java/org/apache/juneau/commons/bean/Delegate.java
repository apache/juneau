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
package org.apache.juneau.commons.bean;

/**
 * An object that represents another object, often wrapping that object.
 *
 * <p>
 * <b>*** Internal Interface - Not intended for external use ***</b>
 *
 * <p>
 * For example, {@code BeanMap} is a map representation of a bean.
 *
 * <p>
 * The returned type info is a {@link BeanTypeInfo} so the bean-modeling layer does not depend
 * on the marshalling-side {@code ClassMeta}.  Marshalling-side implementations narrow the return
 * type via Java covariant returns (e.g. {@code ClassMeta<T> getClassMeta()}).
 *
 * @param <T> The represented class type.
 */
public interface Delegate<T> {

	/**
	 * The {@link BeanTypeInfo} of the class of the represented object.
	 *
	 * @return The class type of the represented object.
	 */
	BeanTypeInfo<T> getClassMeta();
}
