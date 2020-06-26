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
package org.apache.juneau.rest.annotation;

import java.lang.annotation.*;

import org.apache.juneau.jsonschema.annotation.*;

/**
 * TODO
 */
public class MethodAnnotationSwagger implements MethodSwagger {

	private String
		operationId = "",
		deprecated = "";

	private String[]
		summary = new String[0],
		description = new String[0],
		schemes = new String[0],
		consumes = new String[0],
		produces = new String[0],
		parameters = new String[0],
		responses = new String[0],
		tags = new String[0],
		value = new String[0];

	private ExternalDocs externalDocs = new ExternalDocsAnnotation();

	@Override /* MethodSwagger */
	public Class<? extends Annotation> annotationType() {
		return MethodSwagger.class;
	}

	@Override /* MethodSwagger */
	public String[] summary() {
		return summary;
	}

	@Override /* MethodSwagger */
	public String[] description() {
		return description;
	}

	@Override /* MethodSwagger */
	public String operationId() {
		return operationId;
	}

	@Override /* MethodSwagger */
	public String[] schemes() {
		return schemes;
	}

	@Override /* MethodSwagger */
	public String deprecated() {
		return deprecated;
	}

	@Override /* MethodSwagger */
	public String[] consumes() {
		return consumes;
	}

	@Override /* MethodSwagger */
	public String[] produces() {
		return produces;
	}

	@Override /* MethodSwagger */
	public ExternalDocs externalDocs() {
		return externalDocs;
	}

	@Override /* MethodSwagger */
	public String[] parameters() {
		return parameters;
	}

	@Override /* MethodSwagger */
	public String[] responses() {
		return responses;
	}

	@Override /* MethodSwagger */
	public String[] tags() {
		return tags;
	}

	@Override /* MethodSwagger */
	public String[] value() {
		return value;
	}
}
