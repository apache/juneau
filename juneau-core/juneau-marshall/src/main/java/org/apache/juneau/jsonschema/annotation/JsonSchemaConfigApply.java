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
package org.apache.juneau.jsonschema.annotation;

import static org.apache.juneau.jsonschema.JsonSchemaGenerator.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Applies {@link JsonSchemaConfig} annotations to a {@link PropertyStoreBuilder}.
 */
public class JsonSchemaConfigApply extends ConfigApply<JsonSchemaConfig> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public JsonSchemaConfigApply(Class<JsonSchemaConfig> c, VarResolverSession r) {
		super(c, r);
	}

	@Override
	public void apply(AnnotationInfo<JsonSchemaConfig> ai, PropertyStoreBuilder psb) {
		JsonSchemaConfig a = ai.getAnnotation();
		if (! a.addDescriptionsTo().isEmpty())
			psb.set(JSONSCHEMA_addDescriptionsTo, string(a.addDescriptionsTo()));
		if (! a.addExamplesTo().isEmpty())
			psb.set(JSONSCHEMA_addExamplesTo, string(a.addExamplesTo()));
		if (! a.allowNestedDescriptions().isEmpty())
			psb.set(JSONSCHEMA_allowNestedDescriptions, bool(a.allowNestedDescriptions()));
		if (! a.allowNestedExamples().isEmpty())
			psb.set(JSONSCHEMA_allowNestedExamples, bool(a.allowNestedExamples()));
		if (a.beanDefMapper() != BeanDefMapper.Null.class)
			psb.set(JSONSCHEMA_beanDefMapper, a.beanDefMapper());
		for (CS e : a.defaultSchemas())
			psb.addTo(JSONSCHEMA_defaultSchemas, e.k().getName(), objectMap(e.v(), "defaultSchemas"));
		if (! a.ignoreTypes().isEmpty())
			psb.set(JSONSCHEMA_ignoreTypes, string(a.ignoreTypes()));
		if (! a.useBeanDefs().isEmpty())
			psb.set(JSONSCHEMA_useBeanDefs, bool(a.useBeanDefs()));

		if (a.annotateSchema().length > 0)
			psb.addTo(BEAN_annotations, a.annotateSchema());
	}
}
