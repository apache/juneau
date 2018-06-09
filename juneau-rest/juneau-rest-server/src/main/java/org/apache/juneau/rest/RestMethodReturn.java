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

import static org.apache.juneau.rest.util.AnnotationUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Contains metadata about the return type on a REST Java method.
 */
public class RestMethodReturn {
	
	private final Type type;
	private final int code;
	private final ObjectMap metaData;
	
	RestMethodReturn(Type type) {
		this.type = type;
		
		ObjectMap om = new ObjectMap();
		
		int code = 200;
		if (type instanceof Class)
		for (Response ri : ReflectionUtils.findAnnotationsParentFirst(Response.class, (Class<?>)type)) {
			code = ObjectUtils.firstNonZero(ri.code(), ri.value(), code);
			om = merge(om, ri);
		}
		
		this.metaData = om.unmodifiable();
		 
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
	 * Returns the HTTP code code of the response.
	 * 
	 * @return The HTTP code code of the response.
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
