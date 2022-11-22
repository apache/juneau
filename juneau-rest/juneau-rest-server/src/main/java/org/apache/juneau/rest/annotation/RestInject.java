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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.arg.*;
import org.apache.juneau.rest.converter.*;
import org.apache.juneau.rest.debug.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.matcher.*;
import org.apache.juneau.rest.processor.*;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.stats.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Rest bean injection annotation.
 *
 * <p>
 * Used on methods of {@link Rest}-annotated classes to denote methods and fields that override and customize beans
 * used by the REST framework.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	<jc>// Rest resource that uses a customized call logger.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyRest <jk>extends</jk> BasicRestServlet {
 *
 * 		<jc>// Option #1:  As a field.</jc>
 * 		<ja>@RestInject</ja>
 * 		CallLogger <jf>myCallLogger</jf> = CallLogger.<jsm>create</jsm>().logger(<js>"mylogger"</js>).build();
 *
 * 		<jc>// Option #2:  As a method.</jc>
 * 		<ja>@RestInject</ja>
 * 		<jk>public</jk> CallLogger myCallLogger() {
 * 			<jk>return</jk> CallLogger.<jsm>create</jsm>().logger(<js>"mylogger"</js>).build();
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * 	The {@link RestInject#name()}/{@link RestInject#value()} attributes are used to differentiate between named beans.
 * </p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	<jc>// Customized default request headers.</jc>
 * 	<ja>@RestInject</ja>(<js>"defaultRequestHeaders"</js>)
 * 	HeaderList <jf>defaultRequestHeaders</jf> = HeaderList.<jsm>create</jsm>().set(ContentType.<jsf>TEXT_PLAIN</jsf>).build();
 *
 * 	<jc>// Customized default response headers.</jc>
 * 	<ja>@RestInject</ja>(<js>"defaultResponseHeaders"</js>)
 * 	HeaderList <jf>defaultResponseHeaders</jf> = HeaderList.<jsm>create</jsm>().set(ContentType.<jsf>TEXT_PLAIN</jsf>).build();
 * </p>
 *
 * <p>
 * 	The {@link RestInject#methodScope()} attribute is used to define beans in the scope of specific {@link RestOp}-annotated methods.
 * </p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	<jc>// Set a default header on a specific REST method.</jc>
 * 	<jc>// Input parameter is the default header list builder with all annotations applied.</jc>
 * 	<ja>@RestInject</ja>(name=<js>"defaultRequestHeaders"</js>, methodScope=<js>"myRestMethod"</js>)
 * 	<jk>public</jk> HeaderList.Builder myRequestHeaders(HeaderList.Builder <jv>builder</jv>) {
 * 		<jk>return</jk> <jv>builder</jv>.set(ContentType.<jsf>TEXT_PLAIN</jsf>);
 * 	}
 *
 * 	<jc>// Method that picks up default header defined above.</jc>
 * 	<ja>@RestGet</ja>
 * 	<jk>public</jk> Object myRestMethod(ContentType <jv>contentType</jv>) { ... }
 * </p>
 *
 * <p>
 * 	This annotation can also be used to inject arbitrary beans into the bean store which allows them to be
 * 	passed as resolved parameters on {@link RestOp}-annotated methods.
 * </p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	<jc>// Custom beans injected into the bean store.</jc>
 * 	<ja>@RestInject</ja> MyBean <jv>myBean1</jv> = <jk>new</jk> MyBean();
 * 	<ja>@RestInject</ja>(<js>"myBean2"</js>) MyBean <jv>myBean2</jv> = <jk>new</jk> MyBean();
 *
 * 	<jc>// Method that uses injected beans.</jc>
 * 	<ja>@RestGet</ja>
 * 	<jk>public</jk> Object doGet(MyBean <jv>myBean1</jv>, <ja>@Named</ja>(<js>"myBean2"</js>) MyBean <jv>myBean2</jv>) { ... }
 * </p>
 *
 * <p>
 * 	This annotation can also be used on uninitialized fields.  When fields are uninitialized, they will
 * 	be set during initialization based on beans found in the bean store.
 * </p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	<jc>// Fields that get set during initialization based on beans found in the bean store.</jc>
 * 	<ja>@RestInject</ja> CallLogger <jf>callLogger</jf>;
 * 	<ja>@RestInject</ja> BeanStore <jf>beanStore</jf>;  <jc>// Note that BeanStore itself can be accessed this way.</jc>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Methods and fields can be static or non-static.
 * 	<li class='note'>Any injectable beans (including spring beans) can be passed as arguments into methods.
 * 	<li class='note'>Bean names are required when multiple beans of the same type exist in the bean store.
 * 	<li class='note'>By default, the injected bean scope is class-level (applies to the entire class).  The
 * 		{@link RestInject#methodScope()} annotation can be used to apply to method-level only (when applicable).
 * </ul>
 *
 * <p>
 * Any of the following types can be customized via injection:
 * <table class='w800 styled'>
 * 	<tr><th>Bean class</td><th>Bean qualifying names</th><th>Scope</th></tr>
 * 	<tr><td>{@link BeanContext}<br>{@link org.apache.juneau.BeanContext.Builder}</td><td></td><td>class<br>method</td></tr>
 * 	<tr><td>{@link BeanStore}<br>{@link org.apache.juneau.cp.BeanStore.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link CallLogger}<br>{@link org.apache.juneau.rest.logger.CallLogger.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link Config}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link DebugEnablement}<br>{@link org.apache.juneau.rest.debug.DebugEnablement.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link EncoderSet}<br>{@link org.apache.juneau.encoders.EncoderSet.Builder}</td><td></td><td>class<br>method</td></tr>
 * 	<tr><td>{@link FileFinder}<br>{@link org.apache.juneau.cp.FileFinder.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link HeaderList}<br>{@link org.apache.juneau.http.header.HeaderList}</td><td><js>"defaultRequestHeaders"</js><br><js>"defaultResponseHeaders"</js></td><td>class<br>method</td></tr>
 * 	<tr><td>{@link HttpPartParser}<br>{@link org.apache.juneau.httppart.HttpPartParser.Creator}</td><td></td><td>class<br>method</td></tr>
 * 	<tr><td>{@link HttpPartSerializer}<br>{@link org.apache.juneau.httppart.HttpPartSerializer.Creator}</td><td></td><td>class<br>method</td></tr>
 * 	<tr><td>{@link JsonSchemaGenerator}<br>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder}</td><td></td><td>class<br>method</td></tr>
 * 	<tr><td>{@link Logger}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link Messages}<br>{@link org.apache.juneau.cp.Messages.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link MethodExecStore}<br>{@link org.apache.juneau.rest.stats.MethodExecStore.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link MethodList}</td><td><js>"destroyMethods"</js><br><js>"endCallMethods"</js><br><js>"postCallMethods"</js><br><js>"postInitChildFirstMethods"</js><br><js>"postInitMethods"</js><br><js>"preCallMethods"</js><br><js>"startCallMethods"</js></td><td>class</td></tr>
 * 	<tr><td>{@link NamedAttributeMap}<br>{@link org.apache.juneau.rest.httppart.NamedAttributeMap}</td><td><js>"defaultRequestAttributes"</js></td><td>class<br>method</td></tr>
 * 	<tr><td>{@link ParserSet}<br>{@link org.apache.juneau.parser.ParserSet.Builder}</td><td></td><td>class<br>method</td></tr>
 * 	<tr><td>{@link PartList}<br>{@link org.apache.juneau.http.part.PartList}</td><td><js>"defaultRequestQueryData"</js><br><js>"defaultRequestFormData"</js></td><td>method</td></tr>
 * 	<tr><td>{@link ResponseProcessorList}<br>{@link org.apache.juneau.rest.processor.ResponseProcessorList.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link RestChildren}<br>{@link org.apache.juneau.rest.RestChildren.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link RestConverterList}<br>{@link org.apache.juneau.rest.converter.RestConverterList.Builder}</td><td></td><td>method</td></tr>
 * 	<tr><td>{@link RestGuardList}<br>{@link org.apache.juneau.rest.guard.RestGuardList.Builder}</td><td></td><td>method</td></tr>
 * 	<tr><td>{@link RestMatcherList}<br>{@link org.apache.juneau.rest.matcher.RestMatcherList.Builder}</td><td></td><td>method</td></tr>
 * 	<tr><td>{@link RestOpArgList}<br>{@link org.apache.juneau.rest.arg.RestOpArgList.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link RestOperations}<br>{@link org.apache.juneau.rest.RestOperations.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link SerializerSet}<br>{@link org.apache.juneau.serializer.SerializerSet.Builder}</td><td></td><td>class<br>method</td></tr>
 * 	<tr><td>{@link StaticFiles}<br>{@link org.apache.juneau.rest.staticfile.StaticFiles.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link SwaggerProvider}<br>{@link org.apache.juneau.rest.swagger.SwaggerProvider.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link ThrownStore}<br>{@link org.apache.juneau.rest.stats.ThrownStore.Builder}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link UrlPathMatcherList}</td><td></td><td>method</td></tr>
 * 	<tr><td>{@link VarList}</td><td></td><td>class</td></tr>
 * 	<tr><td>{@link VarResolver}<br>{@link org.apache.juneau.svl.VarResolver.Builder}</td><td></td><td>class</td></tr>
 * </table>
 */
@Target({METHOD,FIELD})
@Retention(RUNTIME)
@Inherited
public @interface RestInject {

	/**
	 * The bean name to use to distinguish beans of the same type for different purposes.
	 *
	 * <p>
	 * For example, there are two {@link HeaderList} beans:  <js>"defaultRequestHeaders"</js> and <js>"defaultResponseHeaders"</js>.  This annotation
	 * would be used to differentiate between them.
	 *
	 * @return The bean name to use to distinguish beans of the same type for different purposes, or blank if bean type is unique.
	 */
	String name() default "";


	/**
	 * Same as {@link #name()}.
	 *
	 * @return The bean name to use to distinguish beans of the same type for different purposes, or blank if bean type is unique.
	 */
	String value() default "";

	/**
	 * The short names of the methods that this annotation applies to.
	 *
	 * <p>
	 * Can use <js>"*"</js> to apply to all methods.
	 *
	 * <p>
	 * Ignored for class-level scope.
	 *
	 * @return The short names of the methods that this annotation applies to, or empty if class-scope.
	 */
	String[] methodScope() default {};
}
