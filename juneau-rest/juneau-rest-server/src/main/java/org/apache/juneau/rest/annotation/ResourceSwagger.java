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

import org.apache.juneau.config.vars.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.svl.vars.*;

/**
 * Extended annotation for {@link RestResource#swagger() @RestResource.swagger()}.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.OptionsPages">Overview &gt; juneau-rest-server &gt; OPTIONS pages</a>
 * </ul>
 */
public @interface ResourceSwagger {

	/**
	 * Optional contact information for the exposed API.
	 * 
	 * <p>
	 * It is used to populate the Swagger contact field and to display on HTML pages.
	 * 
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	{
	 * 		name: string,
	 * 		url: string,
	 * 		email: string
	 * 	}
	 * </p>
	 * 
	 * <p>
	 * The default value pulls the description from the <code>contact</code> entry in the servlet resource bundle.
	 * (e.g. <js>"contact = {name:'John Smith',email:'john.smith@foo.bar'}"</js> or
	 * <js>"MyServlet.contact = {name:'John Smith',email:'john.smith@foo.bar'}"</js>).
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			contact=<js>"{name:'John Smith',email:'john.smith@foo.bar'}"</js>
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link FileVar $F} 
	 * {@link ServletInitParamVar $I},
	 * {@link IfVar $IF}
	 * {@link LocalizationVar $L}
	 * {@link RequestAttributeVar $RA} 
	 * {@link RequestFormDataVar $RF} 
	 * {@link RequestHeaderVar $RH} 
	 * {@link RequestPathVar $RP} 
	 * {@link RequestQueryVar $RQ} 
	 * {@link RequestVar $R} 
	 * {@link SystemPropertiesVar $S}
	 * {@link SerializedRequestAttrVar $SA}
	 * {@link SwitchVar $SW}
	 * {@link UrlVar $U}
	 * {@link UrlEncodeVar $UE}
	 * {@link WidgetVar $W}
	 * 
	 * <p>
	 * Corresponds to the swagger field <code>/info/contact</code>.
	 */
	String contact() default "";

	/**
	 * Optional external documentation information for the exposed API.
	 * 
	 * <p>
	 * It is used to populate the Swagger external documentation field and to display on HTML pages.
	 * 
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	{
	 * 		description: string,
	 * 		url: string
	 * 	}
	 * </p>
	 * 
	 * <p>
	 * The default value pulls the description from the <code>externalDocs</code> entry in the servlet resource bundle.
	 * (e.g. <js>"externalDocs = {url:'http://juneau.apache.org'}"</js> or
	 * <js>"MyServlet.externalDocs = {url:'http://juneau.apache.org'}"</js>).
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			externalDocs=<js>"{url:'http://juneau.apache.org'}"</js>
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link FileVar $F} 
	 * {@link ServletInitParamVar $I},
	 * {@link IfVar $IF}
	 * {@link LocalizationVar $L}
	 * {@link RequestAttributeVar $RA} 
	 * {@link RequestFormDataVar $RF} 
	 * {@link RequestHeaderVar $RH} 
	 * {@link RequestPathVar $RP} 
	 * {@link RequestQueryVar $RQ} 
	 * {@link RequestVar $R} 
	 * {@link SystemPropertiesVar $S}
	 * {@link SerializedRequestAttrVar $SA}
	 * {@link SwitchVar $SW}
	 * {@link UrlVar $U}
	 * {@link UrlEncodeVar $UE}
	 * {@link WidgetVar $W}
	 * 
	 * <p>
	 * Corresponds to the swagger field <code>/tags</code>.
	 */
	String externalDocs() default "";

	/**
	 * Optional license information for the exposed API.
	 * 
	 * <p>
	 * It is used to populate the Swagger license field and to display on HTML pages.
	 * 
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	{
	 * 		name: string,
	 * 		url: string
	 * 	}
	 * </p>
	 * 
	 * <p>
	 * The default value pulls the description from the <code>license</code> entry in the servlet resource bundle.
	 * (e.g. <js>"license = {name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js> or
	 * <js>"MyServlet.license = {name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js>).
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			license=<js>"{name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js>
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link FileVar $F} 
	 * {@link ServletInitParamVar $I},
	 * {@link IfVar $IF}
	 * {@link LocalizationVar $L}
	 * {@link RequestAttributeVar $RA} 
	 * {@link RequestFormDataVar $RF} 
	 * {@link RequestHeaderVar $RH} 
	 * {@link RequestPathVar $RP} 
	 * {@link RequestQueryVar $RQ} 
	 * {@link RequestVar $R} 
	 * {@link SystemPropertiesVar $S}
	 * {@link SerializedRequestAttrVar $SA}
	 * {@link SwitchVar $SW}
	 * {@link UrlVar $U}
	 * {@link UrlEncodeVar $UE}
	 * {@link WidgetVar $W}
	 * 
	 * <p>
	 * Corresponds to the swagger field <code>/info/license</code>.
	 */
	String license() default "";

	/**
	 * Optional tagging information for the exposed API.
	 * 
	 * <p>
	 * It is used to populate the Swagger tags field and to display on HTML pages.
	 * 
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	[
	 * 		{
	 * 			name: string,
	 * 			description: string,
	 * 			externalDocs: {
	 * 				description: string,
	 * 				url: string
	 * 			}
	 * 		}
	 * 	]
	 * </p>
	 * 
	 * <p>
	 * The default value pulls the description from the <code>tags</code> entry in the servlet resource bundle.
	 * (e.g. <js>"tags = [{name:'Foo',description:'Foobar'}]"</js> or
	 * <js>"MyServlet.tags = [{name:'Foo',description:'Foobar'}]"</js>).
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			tags=<js>"[{name:'Foo',description:'Foobar'}]"</js>
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link FileVar $F} 
	 * {@link ServletInitParamVar $I},
	 * {@link IfVar $IF}
	 * {@link LocalizationVar $L}
	 * {@link RequestAttributeVar $RA} 
	 * {@link RequestFormDataVar $RF} 
	 * {@link RequestHeaderVar $RH} 
	 * {@link RequestPathVar $RP} 
	 * {@link RequestQueryVar $RQ} 
	 * {@link RequestVar $R} 
	 * {@link SystemPropertiesVar $S}
	 * {@link SerializedRequestAttrVar $SA}
	 * {@link SwitchVar $SW}
	 * {@link UrlVar $U}
	 * {@link UrlEncodeVar $UE}
	 * {@link WidgetVar $W}
	 * 
	 * <p>
	 * Corresponds to the swagger field <code>/tags</code>.
	 */
	String tags() default "";
	
	/**
	 * Optional servlet terms-of-service for this API.
	 * 
	 * <p>
	 * It is used to populate the Swagger terms-of-service field.
	 * 
	 * <p>
	 * The default value pulls the description from the <code>termsOfService</code> entry in the servlet resource bundle.
	 * (e.g. <js>"termsOfService = foo"</js> or <js>"MyServlet.termsOfService = foo"</js>).
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link FileVar $F} 
	 * {@link ServletInitParamVar $I},
	 * {@link IfVar $IF}
	 * {@link LocalizationVar $L}
	 * {@link RequestAttributeVar $RA} 
	 * {@link RequestFormDataVar $RF} 
	 * {@link RequestHeaderVar $RH} 
	 * {@link RequestPathVar $RP} 
	 * {@link RequestQueryVar $RQ} 
	 * {@link RequestVar $R} 
	 * {@link SystemPropertiesVar $S}
	 * {@link SerializedRequestAttrVar $SA}
	 * {@link SwitchVar $SW}
	 * {@link UrlVar $U}
	 * {@link UrlEncodeVar $UE}
	 * {@link WidgetVar $W}
	 * 
	 * <p>
	 * Corresponds to the swagger field <code>/info/termsOfService</code>.
	 */
	String termsOfService() default "";

	/**
	 * Provides the version of the application API (not to be confused with the specification version).
	 * 
	 * <p>
	 * It is used to populate the Swagger version field and to display on HTML pages.
	 * 
	 * <p>
	 * The default value pulls the description from the <code>version</code> entry in the servlet resource bundle.
	 * (e.g. <js>"version = 2.0"</js> or <js>"MyServlet.version = 2.0"</js>).
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link FileVar $F} 
	 * {@link ServletInitParamVar $I},
	 * {@link IfVar $IF}
	 * {@link LocalizationVar $L}
	 * {@link RequestAttributeVar $RA} 
	 * {@link RequestFormDataVar $RF} 
	 * {@link RequestHeaderVar $RH} 
	 * {@link RequestPathVar $RP} 
	 * {@link RequestQueryVar $RQ} 
	 * {@link RequestVar $R} 
	 * {@link SystemPropertiesVar $S}
	 * {@link SerializedRequestAttrVar $SA}
	 * {@link SwitchVar $SW}
	 * {@link UrlVar $U}
	 * {@link UrlEncodeVar $UE}
	 * {@link WidgetVar $W}
	 * 
	 * <p>
	 * Corresponds to the swagger field <code>/info/version</code>.
	 */
	String version() default "";
}
