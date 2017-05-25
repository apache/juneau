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
package org.apache.juneau.remoteable;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;

/**
 * Annotation applied to Java method arguments of interface proxies to denote that they are path variables on the request.
 * <p>
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 *
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod1/{foo}"</js>)
 * 		String myProxyMethod1(<ja>@Path</ja>(<js>"foo"</js>)</ja> String foo);
 * 	}
 * </p>
 * <p>
 * The argument can be any of the following types:
 * <ul class='spaced-list'>
 * 	<li><code>NameValuePairs</code> - Individual name-value pairs.
 * 	<li>Any serializable POJO - Converted to text using {@link UrlEncodingSerializer#serialize(PartType,Object)}.
 * 	<li><code>Map&lt;String,Object&gt;</code> - Individual name-value pairs.
 * 		Values are converted to text using {@link UrlEncodingSerializer#serialize(PartType,Object)}.
 * 	<li>A bean - Individual name-value pairs.
 * 		Values are converted to text using {@link UrlEncodingSerializer#serialize(PartType,Object)}.
 * </ul>
 * <p>
 * The annotation can also be applied to a bean property field or getter when the argument is annotated with
 *  {@link RequestBean @RequestBean}:
 * <p>
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 *
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod1/{foo}"</js>)
 * 		String myProxyMethod(<ja>@RequestBean</ja> MyRequestBean bean);
 * 	}
 *
 * 	<jk>public interface</jk> MyRequestBean {
 * 		<ja>@Path</ja>
 * 		String getFoo();
 * 	}
 * </p>
 * <p>
 * When used in a request bean, the {@link #value()} can be used to override the path variable name.
 * It can also be overridden via the {@link BeanProperty#name @BeanProperty.name()} annotation.
 * A name of <js>"*"</js> where the bean property value is a map or bean will cause the individual entries in the
 * map or bean to be expanded to path variables.
 */
@Documented
@Target({PARAMETER,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Path {

	/**
	 * The path parameter name.
	 * <p>
	 * A value of <js>"*"</js> indicates the value should be serialized as name/value pairs and is applicable
	 * for the following data types:
	 * <ul>
	 * 	<li><code>NameValuePairs</code>
	 * 	<li><code>Map&lt;String,Object&gt;</code>
	 * 	<li>A bean
	 * </ul>
	 */
	String value() default "*";

	/**
	 * Specifies the {@link PartSerializer} class used for serializing values to strings.
	 * <p>
	 * The default serializer converters values to UON notation.
	 * <p>
	 * This annotation is provided to allow values to be custom serialized.
	 */
	Class<? extends PartSerializer> serializer() default UrlEncodingSerializer.class;
}
