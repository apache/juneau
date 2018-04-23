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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Contains metadata about a throwable on a REST Java method.
 */
public class RestMethodThrown {
	
	final Class<?> type;
	final int code;
	final ObjectMap metaData;
	
	RestMethodThrown(Class<?> type) {
		this.type = type;
		this.metaData = new ObjectMap();
		
		int code = 500;
		for (Response ri : ReflectionUtils.findAnnotationsParentFirst(Response.class, type)) {
			code = ObjectUtils.firstNonZero(ri.code(), ri.value(), code);
			metaData.appendSkipEmpty("description", joinnl(ri.description()));
			metaData.appendSkipEmpty("example", joinnl(ri.example()));
			metaData.appendSkipEmpty("headers", joinnl(ri.headers()));
			metaData.appendSkipEmpty("schema", joinnl(ri.schema()));
		}
		
		this.code = code;
	}
	
	/**
	 * Returns the return type of the Java method.
	 * 
	 * @return The return type of the Java method.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Returns the HTTP status code of the response.
	 * 
	 * @return The HTTP status code of the response.
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * Returns the Swagger metadata associated with this return.
	 * 
	 * @return A map of return metadata, never <jk>null</jk>.
	 */
	public ObjectMap getMetaData() {
		return metaData;
	}
}
