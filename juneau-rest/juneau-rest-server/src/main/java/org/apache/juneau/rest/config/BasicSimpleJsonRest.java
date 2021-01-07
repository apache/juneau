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
package org.apache.juneau.rest.config;

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.annotation.*;

/**
 * Basic configuration for a REST resource that supports simplified JSON transport.
 */
@Rest(

	// Default serializers for all Java methods in the class.
	serializers={
		SimpleJsonSerializer.class,
	},

	// Default parsers for all Java methods in the class.
	parsers={
		SimpleJsonParser.class,
	},

	// Optional external configuration file.
	config="$S{juneau.configFile,SYSTEM_DEFAULT}",

	defaultAccept="text/json+simple"
)
@SerializerConfig(
	// Enable automatic resolution of URI objects to root-relative values.
	uriResolution="ROOT_RELATIVE"
)
@BeanConfig(
	// When parsing generated beans, ignore unknown properties that may only exist as getters and not setters.
	ignoreUnknownBeanProperties="true"
)
public interface BasicSimpleJsonRest {}
