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

/**
 * Identifies a remote proxy interface against a REST interface.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-rest-server.RemoteableProxies'>Overview &gt; juneau-rest-server &gt; Remoteable Proxies</a>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-rest-client.3rdPartyProxies'>Overview &gt; juneau-rest-client &gt; Interface Proxies Against 3rd-party REST Interfaces</a>
 * </ul>
 */
@Documented
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Remoteable {

	/**
	 * The absolute or relative path of the REST service.
	 *
	 * <p>
	 * When a relative path is specified, it's relative to the root-url defined on the <code>RestClient</code> used
	 * to instantiate the interface.
	 *
	 * <p>
	 * When no path is specified, the path is assumed to be the class name (e.g.
	 * <js>"http://localhost/root-url/org.foo.MyInterface"</js>)
	 */
	String path() default "";

	/**
	 * Identifies which methods on the interface should be exposed through the proxy.
	 *
	 * <p>
	 * The options are:
	 * <ul>
	 * 	<li><js>"DECLARED"</js> (default) - Only methods declared on the immediate interface/class are exposed.
	 * 		Methods on parent interfaces/classes are ignored.
	 * 	<li><js>"ANNOTATED"</js> - Only methods annotated with {@link RemoteMethod} are exposed.
	 * 	<li><js>"ALL"</js> - All methods defined on the interface or class are exposed.
	 * </ul>
	 */
	String expose() default "";

	/**
	 * Enable method signature paths.
	 *
	 * <p>
	 * When enabled, the HTTP paths for Java methods will default to the full method signature when not specified via {@link RemoteMethod#path() @RemoteMethod(path)}.
	 *
	 * <p>
	 * For example, the HTTP path for the <code>createPerson</code> method below is <js>"createPerson(org.apache.addressbook.CreatePerson)"</js>.
	 *
	 * <p class='bcode w800'>
	 * 	<jk>package</jk> org.apache.addressbook;
	 *
	 * 	<ja>@Remoteable</ja>(useMethodSignatures=<jk>true</jk>)
	 * 	<jk>public interface</jk> IAddressBook {
	 * 		Person createPerson(CreatePerson cp) <jk>throws</jk> Exception;
	 * 	}
	 *
	 * 	<jk>public class</jk> CreatePerson {...}
	 * </p>
	 *
	 * <p>
	 * By default, if you do not specify a <ja>@Remoteable</ja> annotation on your class, the default value for this setting is <jk>true</jk>.
	 * <br>So the path for the <code>createPerson</code> method shown is the same as above:
	 *
	 * <p class='bcode w800'>
	 * 	<jk>public interface</jk> IAddressBook {
	 * 		Person createPerson(CreatePerson cp) <jk>throws</jk> Exception;
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The path can always be overridden using the {@link RemoteMethod#path() @RemoteMethod(path)} setting like so:
	 *
	 * <p class='bcode w800'>
	 * 	<jk>public interface</jk> IAddressBook {
	 * 		<ja>@RemoteMethod</ja>(path=<jk>"/people"</jk>)
	 * 		Person createPerson(CreatePerson cp) <jk>throws</jk> Exception;
	 * 	}
	 * </p>
	 *
	 * <p>
	 * If this setting is NOT enabled, then we infer the HTTP method and path from the Java method name.
	 * <br>In the example below, the HTTP method is detected as <js>"POST"</js> and the path is <js>"/person"</js>.
	 *
	 * <p class='bcode w800'>
	 * 	<ja>@Remoteable</ja>
	 * 	<jk>public interface</jk> IAddressBook {
	 * 		Person postPerson(CreatePerson cp) <jk>throws</jk> Exception;
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-rest-client.3rdPartyProxies.MethodNames'>Overview &gt; juneau-rest-client &gt; Interface Proxies Against 3rd-party REST Interfaces &gt; Method Names</a>
	 * </ul>
	 */
	boolean useMethodSignatures() default false;
}
