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

import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;

/**
 * Annotation applied to Java method arguments of interface proxies to denote a bean with remoteable annotations.
 * <p>
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 *
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod/{p1}/{p2}"</js>)
 * 		String myProxyMethod(<ja>@RequestBean</ja> MyRequestBean bean);
 * 	}
 *
 * 	<jk>public interface</jk> MyRequestBean {
 *
 * 		<ja>@Path</ja>
 * 		String getP1();
 *
 * 		<ja>@Path</ja>(<js>"p2"</js>)
 * 		String getX();
 *
 * 		<ja>@Query</ja>
 * 		String getQ1();
 *
 * 		<ja>@Query</ja>
 * 		<ja>@BeanProperty</ja>(name=<js>"q2"</js>)
 * 		String getQuery2();
 *
 * 		<ja>@QueryIfNE</ja>(<js>"q3"</js>)
 * 		String getQuery3();
 *
 * 		<ja>@QueryIfNE</ja>
 * 		Map&lt;String,Object&gt; getExtraQueries();
 *
 * 		<ja>@FormData</ja>
 * 		String getF1();
 *
 * 		<ja>@FormData</ja>
 * 		<ja>@BeanProperty</ja>(name=<js>"f2"</js>)
 * 		String getFormData2();
 *
 * 		<ja>@FormDataIfNE</ja>(<js>"f3"</js>)
 * 		String getFormData3();
 *
 * 		<ja>@FormDataIfNE</ja>
 * 		Map&lt;String,Object&gt; getExtraFormData();
 *
 * 		<ja>@Header</ja>
 * 		String getH1();
 *
 * 		<ja>@Header</ja>
 * 		<ja>@BeanProperty</ja>(name=<js>"H2"</js>)
 * 		String getHeader2();
 *
 * 		<ja>@HeaderIfNE</ja>(<js>"H3"</js>)
 * 		String getHeader3();
 *
 * 		<ja>@HeaderIfNE</ja>
 * 		Map&lt;String,Object&gt; getExtraHeaders();
 * 	}
 * </p>
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#Remoteable.3rdParty'>Interface
 * 		proxies against 3rd-party REST interfaces</a>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.remoteable</a>
 * </ul>
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface RequestBean {

	/**
	 * Specifies the {@link PartSerializer} class used for serializing values to strings.
	 * <p>
	 * The default value defaults to the using the part serializer defined on the client which by default is
	 * {@link UrlEncodingSerializer}.
	 * <p>
	 * This annotation is provided to allow values to be custom serialized.
	 */
	Class<? extends PartSerializer> serializer() default PartSerializer.class;
}
