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

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;
import static org.apache.juneau.rest.ResponseProcessor.*;
import static org.apache.juneau.rest.logging.RestLoggingDetail.*;
import static java.util.Collections.*;
import static java.util.logging.Level.*;
import static java.util.Optional.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.mstat.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.args.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;

/**
 * Contains all the configuration on a REST resource and the entry points for handling REST calls.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestContext}
 * </ul>
 */
@ConfigurableContext(nocache=true)
public class RestContext extends BeanContext {

	/**
	 * Represents a null value for the {@link Rest#contextClass()} annotation.
	 */
	@SuppressWarnings("javadoc")
	public static final class Null extends RestContext {
		public Null(RestContextBuilder builder) throws Exception {
			super(builder);
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "RestContext";

	/**
	 * Configuration property:  Bean store.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_beanStore REST_beanStore}
	 * 	<li><b>Name:</b>  <js>"RestContext.beanStore.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li>{@link org.apache.juneau.cp.BeanStore}
	 * 			<li><c>Class&lt;{@link org.apache.juneau.cp.BeanStore}&gt;</c>
	 * 		</ul>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.cp.BeanStore}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#beanStore()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#beanStore(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#beanStore(BeanStore)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The resolver used for resolving instances of child resources and various other beans including:
	 * <ul>
	 * 	<li>{@link RestLogger}
	 * 	<li>{@link SwaggerProvider}
	 * 	<li>{@link FileFinder}
	 * 	<li>{@link StaticFiles}
	 * </ul>
	 *
	 * <p>
	 * Note that the <c>SpringRestServlet</c> classes uses the <c>SpringBeanStore</c> class to allow for any
	 * Spring beans to be injected into your REST resources.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestInjection}
	 * </ul>
	 */
	public static final String REST_beanStore = PREFIX + ".beanStore.o";

	/**
	 * Configuration property:  Compression encoders.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_encoders REST_encoders}
	 * 	<li><b>Name:</b>  <js>"RestContext.encoders.o"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.encoders.Encoder}|Class&lt;{@link org.apache.juneau.encoders.Encoder}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#encoders()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#encoders()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#encoders(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#encoders(Encoder...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@Rest</ja>(encoders={GzipEncoder.<jk>class</jk>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.encoders(GzipEncoder.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			<jv>builder</jv>.addTo(<jsf>REST_encoders</jsf>, GzipEncoder.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.encoders(GzipEncoder.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestGet</ja>(encoders={MySpecialEncoder.<jk>class</jk>}, inherit={<js>"ENCODERS"</js>})
	 * 		<jk>public</jk> Object myMethod() {...}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(BeanContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestEncoders}
	 * </ul>
	 */
	public static final String REST_encoders = PREFIX + ".encoders.lo";

	/**
	 * Configuration property:  File finder.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_fileFinder REST_fileFinder}
	 * 	<li><b>Name:</b>  <js>"RestContext.fileFinder.o"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.cp.FileFinder}
	 * 	<li><b>Default:</b>  {@link #REST_fileFinderDefault}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#fileFinder()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#fileFinder(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#fileFinder(FileFinder)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContext#createFileFinder(Object,ContextProperties,BeanStore)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Used to retrieve localized files from the classpath for a variety of purposes including:
	 * <ul>
	 * 	<li>Resolution of {@link FileVar $F} variable contents.
	 * </ul>
	 *
	 * <p>
	 * The file finder can be accessed through the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestContext#getFileFinder()}
	 * 	<li class='jm'>{@link RestRequest#getFileFinder()}
	 * </ul>
	 *
	 * <p>
	 * The file finder is instantiated via the {@link RestContext#createFileFinder(Object,ContextProperties,BeanStore)} method which in turn instantiates
	 * based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself if it's an instance of {@link FileFinder}.
	 * 	<li>Looks for {@link #REST_fileFinder} setting.
	 * 	<li>Looks for a public <c>createFileFinder()</> method on the resource class with an optional {@link RestContext} argument.
	 * 	<li>Instantiates the default file finder as specified via {@link #REST_fileFinderDefault}.
	 * 	<li>Instantiates a {@link BasicFileFinder} which provides basic support for finding localized
	 * 		resources on the classpath and JVM working directory.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a file finder that looks for files in the /files working subdirectory, but overrides the find()
	 * 	// method for special handling of special cases.</jc>
	 * 	<jk>public class</jk> MyFileFinder <jk>extends</jk> BasicFileFinder {
	 *
	 * 		<jk>public</jk> MyFileFinder() {
	 * 			<jk>super</jk>(
	 * 				<jk>new</jk> FileFinderBuilder()
	 * 					.dir(<js>"/files"</js>)
	 *			);
	 * 		}
	 *
	 *		<ja>@Override</ja> <jc>// FileFinder</jc>
	 * 		<jk>protected</jk> Optional&lt;InputStream&gt; find(String <jv>name</jv>, Locale <jv>locale</jv>) <jk>throws</jk> IOException {
	 * 			<jc>// Do special handling or just call super.find().</jc>
	 * 			<jk>return super</jk>.find(<jv>name</jv>, <jv>locale</jv>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@Rest</ja>(fileFinder=MyFileFinder.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Created via createFileFinder() method.</jc>
	 * 		<jk>public</jk> FileFinder createFileFinder(RestContext <jv>context</jv>) <jk>throws</jk> Exception {
	 * 			<jk>return new</jk> MyFileFinder();
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.fileFinder(MyFileFinder.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			<jv>builder</jv>.set(<jsf>REST_fileFinder</jsf>, MyFileFinder.<jk>class</jk>));
	 *
	 * 			<jc>// Use a pre-instantiated object instead.</jc>
	 * 			<jv>builder</jv>.fileFinder(<jk>new</jk> MyFileFinder());
	 * 		}
	 *
	 * 		<jc>// Option #4 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.fileFinder(MyFileFinder.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Create a REST method that uses the file finder.</jc>
	 * 		<ja>@RestGet</ja>
	 * 		<jk>public</jk> InputStream foo(RestRequest <jv>req</jv>) {
	 * 			<jk>return</jk> <jv>req</jv>.getFileFinder().getStream(<js>"foo.json"</js>).orElseThrow(NotFound::<jk>new</jk>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String REST_fileFinder = PREFIX + ".fileFinder.o";

	/**
	 * Configuration property:  Default file finder.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_fileFinderDefault REST_fileFinderDefault}
	 * 	<li><b>Name:</b>  <js>"RestContext.fileFinderDefault.o"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.cp.FileFinder}
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.cp.BasicFileFinder}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#fileFinderDefault(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#fileFinderDefault(FileFinder)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The default file finder to use if not specified.
	 * <p>
	 * This setting is inherited from the parent context.
	 */
	public static final String REST_fileFinderDefault = PREFIX + ".fileFinderDefault.o";

	/**
	 * Configuration property:  Messages.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_messages REST_messages}
	 * 	<li><b>Name:</b>  <js>"RestContext.messages.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.utils.Tuple2}&lt;Class,String&gt;&gt;</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#messages()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#messages(String)},
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies the location of the resource bundle for this class if it's different from the class name.
	 *
	 * <p>
	 * By default, the resource bundle name is assumed to match the class name.  For example, given the class
	 * <c>MyClass.java</c>, the resource bundle is assumed to be <c>MyClass.properties</c>.  This property
	 * allows you to override this setting to specify a different location such as <c>MyMessages.properties</c> by
	 * specifying a value of <js>"MyMessages"</js>.
	 *
	 * <p>
	 * 	Resource bundles are searched using the following base name patterns:
	 * 	<ul>
	 * 		<li><js>"{package}.{name}"</js>
	 * 		<li><js>"{package}.i18n.{name}"</js>
	 * 		<li><js>"{package}.nls.{name}"</js>
	 * 		<li><js>"{package}.messages.{name}"</js>
	 * 	</ul>
	 *
	 * <p>
	 * This annotation is used to provide request-localized (based on <c>Accept-Language</c>) messages for the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestRequest#getMessage(String, Object...)}
	 * 	<li class='jm'>{@link RestContext#getMessages() RestContext.getMessages()}
	 * </ul>
	 *
	 * <p>
	 * Request-localized messages are also available by passing either of the following parameter types into your Java method:
	 * <ul class='javatree'>
	 * 	<li class='jc'>{@link ResourceBundle} - Basic Java resource bundle.
	 * 	<li class='jc'>{@link Messages} - Extended resource bundle with several convenience methods.
	 * </ul>
	 *
	 * The value can be a relative path like <js>"nls/Messages"</js>, indicating to look for the resource bundle
	 * <js>"com.foo.sample.nls.Messages"</js> if the resource class is in <js>"com.foo.sample"</js>, or it can be an
	 * absolute path like <js>"com.foo.sample.nls.Messages"</js>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<cc># Contents of org/apache/foo/nls/MyMessages.properties</cc>
	 *
	 * 	<ck>HelloMessage</ck> = <cv>Hello {0}!</cv>
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Contents of org/apache/foo/MyResource.java</jc>
	 *
	 * 	<ja>@Rest</ja>(messages=<js>"nls/MyMessages"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 *
	 * 		<ja>@RestGet</ja>(<js>"/hello/{you}"</js>)
	 * 		<jk>public</jk> Object helloYou(RestRequest <jv>req</jv>, Messages <jv>messages</jv>, <ja>@Path</ja>(<js>"name"</js>) String <jv>you</jv>) {
	 * 			String <jv>s</jv>;
	 *
	 * 			<jc>// Get it from the RestRequest object.</jc>
	 * 			<jv>s</jv> = <jv>req</jv>.getMessage(<js>"HelloMessage"</js>, <jv>you</jv>);
	 *
	 * 			<jc>// Or get it from the method parameter.</jc>
	 * 			<jv>s</jv> = <jv>messages</jv>.getString(<js>"HelloMessage"</js>, <jv>you</jv>);
	 *
	 * 			<jc>// Or get the message in a locale different from the request.</jc>
	 * 			<jv>s</jv> = <jv>messages</jv>.forLocale(Locale.<jsf>UK</jsf>).getString(<js>"HelloMessage"</js>, <jv>you</jv>);
	 *
	 * 			<jk>return</jk> <jv>s</jv>;
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>Mappings are cumulative from super classes.
	 * 		<br>Therefore, you can find and retrieve messages up the class-hierarchy chain.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link Messages}
	 * 	<li class='link'>{@doc RestMessages}
	 * </ul>
	 */
	public static final String REST_messages = PREFIX + ".messages.lo";

	/**
	 * Configuration property:  Parsers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_parsers REST_parsers}
	 * 	<li><b>Name:</b>  <js>"RestContext.parsers.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.parser.Parser}|Class&lt;{@link org.apache.juneau.parser.Parser}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#parsers()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#parsers()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#parsers(Parser...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#parsers(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#parsersReplace(Parser...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Adds class-level parsers to this resource.
	 *
	 * <p>
	 * Parsers are used to convert the body of HTTP requests into POJOs.
	 * <br>Any of the Juneau framework parsers can be used in this setting.
	 * <br>The parser selected is based on the request <c>Content-Type</c> header matched against the values returned by the following method
	 * using a best-match algorithm:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link Parser#getMediaTypes()}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(parsers={JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.parsers(JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but use pre-instantiated parsers.</jc>
	 * 			<jv>builder</jv>.parsers(JsonParser.<jsf>DEFAULT</jsf>, XmlParser.<jsf>DEFAULT</jsf>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			<jv>builder</jv>.set(<jsf>REST_parsers</jsf>, JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.parsers(JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestPost</ja>(parsers={HtmlParser.<jk>class</jk>})
	 * 		<jk>public</jk> Object myMethod(<ja>@Body</ja> MyPojo <jv>myPojo</jv>) {
	 * 			<jc>// Do something with your parsed POJO.</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>
	 * 		When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * 	<li>
	 * 		Typically, you'll want your resource to extend directly from {@link BasicRestServlet} which comes
	 * 		preconfigured with the following parsers:
	 * 		<ul>
	 * 			<li class='jc'>{@link JsonParser}
	 * 			<li class='jc'>{@link XmlParser}
	 * 			<li class='jc'>{@link HtmlParser}
	 * 			<li class='jc'>{@link UonParser}
	 * 			<li class='jc'>{@link UrlEncodingParser}
	 * 			<li class='jc'>{@link MsgPackParser}
	 * 			<li class='jc'>{@link PlainTextParser}
	 * 		</ul>
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestParsers}
	 * </ul>
	 */
	public static final String REST_parsers = PREFIX + ".parsers.lo";

	/**
	 * Configuration property:  HTTP part parser.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_partParser REST_partParser}
	 * 	<li><b>Name:</b>  <js>"RestContext.partParser.o"</js>
	 * 	<li><b>Data type:</b>  <c>{@link org.apache.juneau.httppart.HttpPartParser}|Class&lt;{@link org.apache.juneau.httppart.HttpPartParser}&gt;</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.oapi.OpenApiParser}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#partParser()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#partParser(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#partParser(HttpPartParser)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the {@link HttpPartParser} to use for parsing headers, query/form parameters, and URI parts.
	 *
	 * <p>
	 * The default value is {@link OpenApiParser} which allows for both plain-text and URL-Encoded-Object-Notation values.
	 * <br>If your parts contain text that can be confused with UON (e.g. <js>"(foo)"</js>), you can switch to
	 * {@link SimplePartParser} which treats everything as plain text.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(partParser=SimplePartParser.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.partParser(SimplePartParser.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			<jv>builder</jv>.set(<jsf>REST_partParser</jsf>, SimplePartParser.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.partParser(SimplePartParser.<jk>class</jk>);
	 * 		}
	 *
	 * 		<ja>@RestPost</ja>(...)
	 * 		<jk>public</jk> Object myMethod(<ja>@Header</ja>(<js>"My-Header"</js>) MyParsedHeader <jv>h</jv>, <ja>@Query</ja>(<js>"myquery"</js>) MyParsedQuery <jv>q</jv>) {
	 * 			<jc>// Do something with your parsed parts.</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>
	 * 		When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 */
	public static final String REST_partParser = PREFIX + ".partParser.o";

	/**
	 * Configuration property:  HTTP part serializer.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_partSerializer REST_partSerializer}
	 * 	<li><b>Name:</b>  <js>"RestContext.partSerializer.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li>{@link org.apache.juneau.httppart.HttpPartSerializer}
	 * 			<li><c>Class&lt;{@link org.apache.juneau.httppart.HttpPartSerializer}&gt;</c>
	 * 		</ul>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.oapi.OpenApiSerializer}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#partSerializer()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#partSerializer(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#partSerializer(HttpPartSerializer)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the {@link HttpPartSerializer} to use for serializing headers, query/form parameters, and URI parts.
	 *
	 * <p>
	 * The default value is {@link OpenApiSerializer} which serializes based on OpenAPI rules, but defaults to UON notation for beans and maps, and
	 * plain text for everything else.
	 * <br>Other options include:
	 * <ul>
	 * 	<li class='jc'>{@link SimplePartSerializer} - Always serializes to plain text.
	 * 	<li class='jc'>{@link UonSerializer} - Always serializers to UON.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(partSerializer=SimplePartSerializer.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.partSerializer(SimplePartSerializer.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			<jv>builder</jv>.set(<jsf>REST_partSerializer</jsf>, SimplePartSerializer.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.partSerializer(SimplePartSerializer.<jk>class</jk>);
	 * 		}
	 *
	 * 		<ja>@RestPost</ja>(...)
	 * 		<jk>public</jk> Object myMethod(RestResponse <jv>res</jv>) {
	 * 			<jc>// Set a header to a POJO.</jc>
	 * 			<jv>res</jv>.setHeader(<js>"My-Header"</js>, <jk>new</jk> MyPojo());
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>
	 * 		When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 */
	public static final String REST_partSerializer = PREFIX + ".partSerializer.o";

	/**
	 * Configuration property:  Render response stack traces in responses.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_renderResponseStackTraces REST_renderResponseStackTraces}
	 * 	<li><b>Name:</b>  <js>"RestContext.renderResponseStackTraces.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RestContext.renderResponseStackTraces</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_RENDERRESPONSESTACKTRACES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#renderResponseStackTraces()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#renderResponseStackTraces(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#renderResponseStackTraces()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(renderResponseStackTraces=<jk>true</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.renderResponseStackTraces();
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			<jv>builder</jv>.set(<jsf>REST_renderResponseStackTraces</jsf>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.renderResponseStackTraces();
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Useful for debugging, although allowing stack traces to be rendered may cause security concerns so use
	 * 		caution when enabling.
	 * 	<li>
	 * 		This setting is available through the following method:
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContext#isRenderResponseStackTraces() RestContext.isRenderResponseStackTraces()}
	 * 		</ul>
	 * 		That method is used by {@link #handleError(RestCall, Throwable)}.
	 * </ul>
	 */
	public static final String REST_renderResponseStackTraces = PREFIX + ".renderResponseStackTraces.b";

	/**
	 * Configuration property:  Serializers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_serializers REST_serializers}
	 * 	<li><b>Name:</b>  <js>"RestContext.serializers.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.serializer.Serializer}|Class&lt;{@link org.apache.juneau.serializer.Serializer}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#serializers()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#serializers()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#serializers(Serializer...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#serializers(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#serializersReplace(Serializer...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#serializersReplace(Class...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Adds class-level serializers to this resource.
	 *
	 * <p>
	 * Serializer are used to convert POJOs to HTTP response bodies.
	 * <br>Any of the Juneau framework serializers can be used in this setting.
	 * <br>The serializer selected is based on the request <c>Accept</c> header matched against the values returned by the following method
	 * using a best-match algorithm:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link Serializer#getMediaTypeRanges()}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(serializers={JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.serializers(JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but use pre-instantiated parsers.</jc>
	 * 			<jv>builder</jv>.serializers(JsonSerializer.<jsf>DEFAULT</jsf>, XmlSerializer.<jsf>DEFAULT</jsf>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			<jv>builder</jv>.set(<jsf>REST_serializers</jsf>, JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.serializers(JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestGet</ja>(serializers={HtmlSerializer.<jk>class</jk>})
	 * 		<jk>public</jk> MyPojo myMethod() {
	 * 			<jc>// Return a POJO to be serialized.</jc>
	 * 			<jk>return new</jk> MyPojo();
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>
	 * 		When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * 	<li>
	 * 		Typically, you'll want your resource to extend directly from {@link BasicRestServlet} which comes
	 * 		preconfigured with the following serializers:
	 * 		<ul>
	 * 			<li class='jc'>{@link HtmlDocSerializer}
	 * 			<li class='jc'>{@link HtmlStrippedDocSerializer}
	 * 			<li class='jc'>{@link HtmlSchemaDocSerializer}
	 * 			<li class='jc'>{@link JsonSerializer}
	 * 			<li class='jc'>{@link SimpleJsonSerializer}
	 * 			<li class='jc'>{@link JsonSchemaSerializer}
	 * 			<li class='jc'>{@link XmlDocSerializer}
	 * 			<li class='jc'>{@link UonSerializer}
	 * 			<li class='jc'>{@link UrlEncodingSerializer}
	 * 			<li class='jc'>{@link MsgPackSerializer}
	 * 			<li class='jc'>{@link SoapXmlSerializer}
	 * 			<li class='jc'>{@link PlainTextSerializer}
	 * 		</ul>
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestSerializers}
	 * </ul>
	 * <p>
	 */
	public static final String REST_serializers = PREFIX + ".serializers.lo";

	/**
	 * Configuration property:  Static file finder.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_staticFiles REST_staticFiles}
	 * 	<li><b>Name:</b>  <js>"RestContext.staticFiles.o"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.rest.StaticFiles}
	 * 	<li><b>Default:</b>  {@link #REST_staticFilesDefault}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#staticFiles()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#staticFiles(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#staticFiles(StaticFiles)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContext#createStaticFiles(Object,ContextProperties,BeanStore)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Used to retrieve localized files to be served up as static files through the REST API via the following
	 * predefined methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link BasicRestObject#getHtdoc(String, Locale)}.
	 * 	<li class='jm'>{@link BasicRestServlet#getHtdoc(String, Locale)}.
	 * </ul>
	 *
	 * <p>
	 * The static file finder can be accessed through the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestContext#getStaticFiles()}
	 * 	<li class='jm'>{@link RestRequest#getStaticFiles()}
	 * </ul>
	 *
	 * <p>
	 * The static file finder is instantiated via the {@link RestContext#createStaticFiles(Object,ContextProperties,BeanStore)} method which in turn instantiates
	 * based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link StaticFiles}.
	 * 	<li>Looks in {@link #REST_staticFiles} setting.
	 * 	<li>Looks for a public <c>createStaticFiles()</> method on the resource class with an optional {@link RestContext} argument.
	 * 	<li>Instantiates a {@link BasicStaticFiles} which provides basic support for finding localized
	 * 		resources on the classpath and JVM working directory..
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a static file finder that looks for files in the /files working subdirectory, but overrides the find()
	 * 	// and resolve methods for special handling of special cases and adds a Foo header to all requests.</jc>
	 * 	<jk>public class</jk> MyStaticFiles <jk>extends</jk> StaticFiles {
	 *
	 * 		<jk>public</jk> MyStaticFiles() <jk>extends</jk> BasicStaticFiles {
	 * 			<jk>super</jk>(
	 * 				<jk>new</jk> StaticFilesBuilder()
	 * 					.dir(<js>"/files"</js>)
	 * 					.headers(BasicStringHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>))
	 * 			);
	 * 		}
	 *
	 *		<ja>@Override</ja> <jc>// FileFinder</jc>
	 * 		<jk>protected</jk> Optional&lt;InputStream&gt; find(String <jv>name</jv>, Locale <jv>locale</jv>) <jk>throws</jk> IOException {
	 * 			<jc>// Do special handling or just call super.find().</jc>
	 * 			<jk>return super</jk>.find(<jv>name</jv>, <jv>locale</jv>);
	 * 		}
	 *
	 *		<ja>@Override</ja> <jc>// staticFiles</jc>
	 * 		<jk>public</jk> Optional&lt;BasicHttpResource&gt; resolve(String <jv>path</jv>, Locale <jv>locale</jv>) {
	 * 			<jc>// Do special handling or just call super.resolve().</jc>
	 * 			<jk>return super</jk>.resolve(<jv>path</jv>, <jv>locale</jv>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@Rest</ja>(staticFiles=MyStaticFiles.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Created via createStaticFiles() method.</jc>
	 * 		<jk>public</jk> StaticFiles createStaticFiles(RestContext <jv>context</jv>) <jk>throws</jk> Exception {
	 * 			<jk>return new</jk> MyStaticFiles();
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.staticFiles(MyStaticFiles.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			<jv>builder</jv>.set(<jsf>REST_staticFiles</jsf>, MyStaticFiles.<jk>class</jk>));
	 *
	 * 			<jc>// Use a pre-instantiated object instead.</jc>
	 * 			<jv>builder</jv>.staticFiles(<jk>new</jk> MyStaticFiles());
	 * 		}
	 *
	 * 		<jc>// Option #4 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.staticFiles(MyStaticFiles.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Create a REST method that uses the static files finder.</jc>
	 * 		<ja>@RestGet<ja>(<js>"/htdocs/*"</js>)
	 * 		<jk>public</jk> HttpResource htdocs(RestRequest <jv>req</jv>, <ja>@Path</ja>("/*") String <jv>path</jv>, Locale <jv>locale</jv>) <jk>throws</jk> NotFound {
	 * 			<jk>return</jk> <jv>req</jv>.getStaticFiles().resolve(<jv>path</jv>, <jv>locale</jv>).orElseThrow(NotFound::<jk>new</jk>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String REST_staticFiles = PREFIX + ".staticFiles.o";

	/**
	 * Configuration property:  Static file finder default.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_staticFilesDefault REST_staticFilesDefault}
	 * 	<li><b>Name:</b>  <js>"RestContext.staticFilesDefault.o"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.rest.StaticFiles}
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.rest.BasicStaticFiles}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#staticFilesDefault(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#staticFilesDefault(StaticFiles)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The default static file finder.
	 * <p>
	 * This setting is inherited from the parent context.
	 */
	public static final String REST_staticFilesDefault = PREFIX + ".staticFilesDefault.o";

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	private static final Map<Class<?>, RestContext> REGISTRY = new ConcurrentHashMap<>();

	/**
	 * Returns a registry of all created {@link RestContext} objects.
	 *
	 * @return An unmodifiable map of resource classes to {@link RestContext} objects.
	 */
	public static final Map<Class<?>, RestContext> getGlobalRegistry() {
		return Collections.unmodifiableMap(REGISTRY);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Supplier<?> resource;
	private final Class<?> resourceClass;

	private final RestContextBuilder builder;
	private final boolean
		allowBodyParam,
		renderResponseStackTraces;
	private final String
		clientVersionHeader,
		uriAuthority,
		uriContext;
	private final String path, fullPath;
	private final UrlPathMatcher pathMatcher;

	private final Set<String> allowedMethodParams, allowedHeaderParams, allowedMethodHeaders;

	private final Class<? extends RestOpArg>[] opArgs, hookMethodArgs;
	private final HttpPartSerializer partSerializer;
	private final HttpPartParser partParser;
	private final JsonSchemaGenerator jsonSchemaGenerator;
	private final List<MediaType> consumes, produces;
	private final HeaderList defaultRequestHeaders, defaultResponseHeaders;
	private final NamedAttributeList defaultRequestAttributes;
	private final ResponseProcessor[] responseProcessors;
	private final Messages messages;
	private final Config config;
	private final VarResolver varResolver;
	private final RestOperations restOperations;
	private final RestChildren restChildren;
	private final Logger logger;
	private final SwaggerProvider swaggerProvider;
	private final BasicHttpException initException;
	private final RestContext parentContext;
	private final BeanStore rootBeanStore;
	private final BeanStore beanStore;
	private final UriResolution uriResolution;
	private final UriRelativity uriRelativity;
	private final MethodExecStore methodExecStore;
	private final ThrownStore thrownStore;
	private final ConcurrentHashMap<Locale,Swagger> swaggerCache = new ConcurrentHashMap<>();
	private final Instant startTime;
	private final Map<Class<?>,ResponseBeanMeta> responseBeanMetas = new ConcurrentHashMap<>();
	final Charset defaultCharset;
	final long maxInput;

	final BeanRef<RestLogger> callLoggerDefault;

	final Enablement debugDefault;

	// Lifecycle methods
	private final MethodInvoker[]
		postInitMethods,
		postInitChildFirstMethods,
		startCallMethods,
		endCallMethods,
		destroyMethods;

	private final RestOpInvoker[]
		preCallMethods,
		postCallMethods;

	private final FileFinder fileFinder;
	private final StaticFiles staticFiles;
	private final RestLogger callLogger;
	private final DebugEnablement debugEnablement;

	private final ThreadLocal<RestCall> call = new ThreadLocal<>();

	// Gets set when postInitChildFirst() gets called.
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	/**
	 * Constructor.
	 *
	 * @param resource
	 * 	The REST servlet or bean annotated with <ja>@Rest</ja>.
	 * @return A new builder object.
	 * @throws ServletException Something bad happened.
	 */
	public static RestContextBuilder create(Object resource) throws ServletException {
		return new RestContextBuilder(Optional.empty(), Optional.empty(), resource.getClass(), Optional.of(resource)).init(resource);
	}

	/**
	 * Constructor.
	 *
	 * @param parent
	 * 	The parent context if the REST bean was registered via {@link Rest#children()}.
	 * 	<br>Will be <jk>null</jk> if the bean is a top-level resource.
	 * @param servletConfig
	 * 	The servlet config passed into the servlet by the servlet container.
	 * @param resourceClass
	 * 	The class annotated with <ja>@Rest</ja>.
	 * @return A new builder object.
	 * @throws ServletException Something bad happened.
	 */
	static RestContextBuilder create(RestContext parentContext, ServletConfig servletConfig, Class<?> resourceClass, Object resource) throws ServletException {
		return new RestContextBuilder(Optional.ofNullable(parentContext), Optional.ofNullable(servletConfig), resourceClass, Optional.ofNullable(resource));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 * @throws Exception If any initialization problems were encountered.
	 */
	public RestContext(RestContextBuilder builder) throws Exception {
		super(builder.getContextProperties());

		startTime = Instant.now();

		REGISTRY.put(builder.resourceClass, this);

		BasicHttpException _initException = null;

		try {
			ContextProperties cp = getContextProperties();

			this.builder = builder;

			this.resourceClass = builder.resourceClass;
			this.resource = builder.resource;
			Object r = getResource();

			RestContext parent = parentContext = builder.parentContext;

			rootBeanStore = createBeanStore(r, cp, parent);

			BeanStore bf = beanStore = BeanStore.of(rootBeanStore, r);
			bf.addBean(BeanStore.class, bf);
			bf.addBean(RestContext.class, this);
			bf.addBean(Object.class, r);

			bf.addBean(ContextProperties.class, cp);

			Logger l = logger = createLogger(r, cp, bf);
			bf.addBean(Logger.class, l);

			ThrownStore ts = thrownStore = createThrownStore(r, cp, parent, bf);
			bf.addBean(ThrownStore.class, ts);

			methodExecStore = createMethodExecStore(r, cp, bf, ts);
			bf.addBean(MethodExecStore.class, methodExecStore);

			Messages m = messages = createMessages(r, cp);

			VarResolver vr = varResolver = createVarResolver(r, cp, bf, m);
			bf.addBean(VarResolver.class, vr);

			config = builder.config.resolving(vr.createSession());
			bf.addBean(Config.class, config);

			responseProcessors = createResponseProcessors(r, builder, bf).toArray();

			callLoggerDefault = builder.callLoggerDefault;
			debugDefault = builder.debugDefault;

			callLogger = createCallLogger(r, builder, bf, l, ts);
			bf.addBean(RestLogger.class, callLogger);

			partSerializer = createPartSerializer(r, cp, bf);
			bf.addBean(HttpPartSerializer.class, partSerializer);

			partParser = createPartParser(r, cp, bf);
			bf.addBean(HttpPartParser.class, partParser);

			jsonSchemaGenerator = createJsonSchemaGenerator(r, cp, bf);
			bf.addBean(JsonSchemaGenerator.class, jsonSchemaGenerator);

			FileFinder ff = fileFinder = createFileFinder(r, cp, bf);
			bf.addBean(FileFinder.class, ff);

			staticFiles = createStaticFiles(r, cp, bf);
			bf.addBean(StaticFiles.class, staticFiles);

			defaultRequestHeaders = createDefaultRequestHeaders(r, builder, bf).build();
			defaultResponseHeaders = createDefaultResponseHeaders(r, builder, bf).build();
			defaultRequestAttributes = createDefaultRequestAttributes(r, builder, bf);

			opArgs = createOpArgs(r, builder, bf).asArray();
			hookMethodArgs = createHookMethodArgs(r, cp, bf).asArray();

			uriContext = builder.uriContext;
			uriAuthority = builder.uriAuthority;
			uriResolution = builder.uriResolution;
			uriRelativity = builder.uriRelativity;

			allowBodyParam = ! builder.disableBodyParam;
			allowedHeaderParams = newCaseInsensitiveSet(ofNullable(builder.allowedHeaderParams).map(x -> "NONE".equals(x) ? "" : x).orElse(""));
			allowedMethodParams = newCaseInsensitiveSet(ofNullable(builder.allowedMethodParams).map(x -> "NONE".equals(x) ? "" : x).orElse(""));
			allowedMethodHeaders = newCaseInsensitiveSet(ofNullable(builder.allowedMethodHeaders).map(x -> "NONE".equals(x) ? "" : x).orElse(""));
			renderResponseStackTraces = cp.getBoolean(REST_renderResponseStackTraces).orElse(false);
			clientVersionHeader = builder.clientVersionHeader;
			defaultCharset = builder.defaultCharset;
			maxInput = builder.maxInput;

			debugEnablement = createDebugEnablement(r, builder, bf);

			path = ofNullable(builder.path).orElse("");
			fullPath = (builder.parentContext == null ? "" : (builder.parentContext.fullPath + '/')) + path;

			String p = path;
			if (! p.endsWith("/*"))
				p += "/*";
			pathMatcher = UrlPathMatcher.of(p);

			startCallMethods = createStartCallMethods(r, cp, bf).stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			endCallMethods = createEndCallMethods(r, cp, bf).stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			postInitMethods = createPostInitMethods(r, cp, bf).stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			postInitChildFirstMethods = createPostInitChildFirstMethods(r, cp, bf).stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			destroyMethods = createDestroyMethods(r, cp, bf).stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);

			preCallMethods = createPreCallMethods(r, cp, bf).stream().map(this::toRestOpInvoker).toArray(RestOpInvoker[]:: new);
			postCallMethods = createPostCallMethods(r, cp, bf).stream().map(this::toRestOpInvoker).toArray(RestOpInvoker[]:: new);

			restOperations = createRestOperations(r, builder, bf);

			List<RestOpContext> opContexts = restOperations.getOpContexts();

			if (builder.produces != null)
				produces = AList.unmodifiable(builder.produces);
			else {
				Set<MediaType> s = opContexts.isEmpty() ? emptySet() : new LinkedHashSet<>(opContexts.get(0).getSerializers().getSupportedMediaTypes());
				opContexts.forEach(x -> s.retainAll(x.getSerializers().getSupportedMediaTypes()));
				produces = AList.unmodifiable(s);
			}

			if (builder.consumes != null)
				consumes = AList.unmodifiable(builder.consumes);
			else {
				Set<MediaType> s = opContexts.isEmpty() ? emptySet() : new LinkedHashSet<>(opContexts.get(0).getParsers().getSupportedMediaTypes());
				opContexts.forEach(x -> s.retainAll(x.getParsers().getSupportedMediaTypes()));
				consumes = AList.unmodifiable(s);
			}

			restChildren = createRestChildren(r, builder, bf, builder.inner);

			swaggerProvider = createSwaggerProvider(r, builder, bf, ff, m, vr);

		} catch (BasicHttpException e) {
			_initException = e;
			throw e;
		} catch (Exception e) {
			_initException = new InternalServerError(e);
			throw e;
		} finally {
			initException = _initException;
		}
	}

	private MethodInvoker toMethodInvoker(Method m) {
		return new MethodInvoker(m, getMethodExecStats(m));
	}

	private MethodInvoker toRestOpInvoker(Method m) {
		return new RestOpInvoker(m, findHookMethodArgs(m, getBeanStore()), getMethodExecStats(m));
	}

	private Set<String> newCaseInsensitiveSet(String value) {
		Set<String> s = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER) {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean contains(Object v) {
				return v == null ? false : super.contains(v);
			}
		};
		for (String v : StringUtils.split(value))
			s.add(v);
		return Collections.unmodifiableSet(s);
	}

	/**
	 * Instantiates the bean store for this REST resource.
	 *
	 * <p>
	 * The bean store is typically used for passing in injected beans into REST contexts and for storing beans
	 * created by the REST context.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself if it's an instance of {@link BeanStore}.
	 * 	<li>Looks for {@link #REST_beanStore} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#beanStore(Class)}/{@link RestContextBuilder#beanStore(BeanStore)}
	 * 			<li>{@link Rest#beanStore()}.
	 * 		</ul>
	 * 	<li>Instantiates a new {@link BeanStore}.
	 * 		Uses the parent context's root bean store as the parent bean store if this is a child resource.
	 * </ul>
	 *
	 * <p>
	 * Your REST class can also implement a create method called <c>createBeanStore()</c> to instantiate your own
	 * bean store.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bpcode w800'>
	 * 	<ja>@Rest</ja>
	 * 	<jk>public class</jk> MyRestClass {
	 *
	 * 		<jk>public</jk> BeanStore createBeanStore(Optional&lt;BeanStore&gt; <jv>parentBeanStore</jv>) <jk>throws</jk> Exception {
	 * 			<jc>// Create your own bean store here.</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The <c>createBeanStore()</c> method can be static or non-static can contain any of the following arguments:
	 * <ul>
	 * 	<li><c>{@link Optional}&lt;{@link BeanStore}&gt;</c> - The parent root bean store if this is a child resource.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link #REST_beanStore}
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param parent
	 * 	The parent context if the REST bean was registered via {@link Rest#children()}.
	 * 	<br>Will be <jk>null</jk> if the bean is a top-level resource.
	 * @return The bean store for this REST resource.
	 * @throws Exception If bean store could not be instantiated.
	 */
	protected BeanStore createBeanStore(Object resource, ContextProperties properties, RestContext parent) throws Exception {

		BeanStore x = null;

		if (resource instanceof BeanStore)
			x = (BeanStore)resource;

		if (x == null)
			x = properties.getIfType(REST_beanStore, BeanStore.class).orElse(null);

		if (x == null)
			x = createBeanStoreBuilder(resource, properties, parent).build();

		x = BeanStore
			.of(x, resource)
			.addBean(BeanStore.class, x)
			.beanCreateMethodFinder(BeanStore.class, resource)
			.find("createBeanStore")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the builder for the {@link BeanStore} for this context.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param parent
	 * 	The parent context if the REST bean was registered via {@link Rest#children()}.
	 * 	<br>Will be <jk>null</jk> if the bean is a top-level resource.
	 * @return The bean store builder for this REST resource.
	 * @throws Exception If bean store could not be instantiated.
	 */
	protected BeanStoreBuilder createBeanStoreBuilder(Object resource, ContextProperties properties, RestContext parent) throws Exception {

		Class<? extends BeanStore> c = properties.getIfClass(REST_beanStore, BeanStore.class).orElse(null);

		BeanStore root = parent == null ? null : parent.rootBeanStore;

		BeanStoreBuilder x = BeanStore
			.create()
			.parent(root)
			.implClass(c)
			.outer(resource);

		x = BeanStore
			.create()
			.parent(root)
			.outer(resource)
			.build()
			.addBean(BeanStoreBuilder.class, x)
			.beanCreateMethodFinder(BeanStoreBuilder.class, resource)
			.find("createBeanStoreBuilder")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the file finder for this REST resource.
	 *
	 * <p>
	 * The file finder is used to retrieve localized files from the classpath.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link FileFinder}.
	 * 	<li>Looks for {@link #REST_fileFinder} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#fileFinder(Class)}/{@link RestContextBuilder#fileFinder(FileFinder)}
	 * 			<li>{@link Rest#fileFinder()}.
	 * 		</ul>
	 * 	<li>Resolves it via the {@link #createBeanStore(Object,ContextProperties,RestContext) bean store} registered in this context (including Spring beans if using SpringRestServlet).
	 * 	<li>Looks for value in {@link #REST_fileFinderDefault} setting.
	 * 	<li>Instantiates via {@link #createFileFinderBuilder(Object,ContextProperties,BeanStore)}.
	 * </ul>
	 *
	 * <p>
	 * Your REST class can also implement a create method called <c>createFileFinder()</c> to instantiate your own
	 * file finder.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bpcode w800'>
	 * 	<ja>@Rest</ja>
	 * 	<jk>public class</jk> MyRestClass {
	 *
	 * 		<jk>public</jk> FileFinder createFileFinder() <jk>throws</jk> Exception {
	 * 			<jc>// Create your own file finder here.</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The <c>createFileFinder()</c> method can be static or non-static can contain any of the following arguments:
	 * <ul>
	 * 	<li>{@link FileFinder} - The file finder that would have been returned by this method.
	 * 	<li>{@link FileFinderBuilder} - The file finder returned by {@link #createFileFinderBuilder(Object,ContextProperties,BeanStore)}.
	 * 	<li>{@link RestContext} - This REST context.
	 * 	<li>{@link BeanStore} - The bean store of this REST context.
	 * 	<li>Any {@doc RestInjection injected bean} types.  Use {@link Optional} arguments for beans that may not exist.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link #REST_fileFinder}
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The file finder for this REST resource.
	 * @throws Exception If file finder could not be instantiated.
	 */
	protected FileFinder createFileFinder(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		FileFinder x = null;

		if (resource instanceof FileFinder)
			x = (FileFinder)resource;

		if (x == null)
			x = properties.getIfType(REST_fileFinder, FileFinder.class).orElse(null);

		if (x == null)
			x = beanStore.getBean(FileFinder.class).orElse(null);

		if (x == null)
			x = properties.getIfType(REST_fileFinderDefault, FileFinder.class).orElse(null);

		if (x == null)
			x = createFileFinderBuilder(resource, properties, beanStore).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(FileFinder.class, x)
			.beanCreateMethodFinder(FileFinder.class, resource)
			.find("createFileFinder")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the file finder builder for this REST resource.
	 *
	 * <p>
	 * Allows subclasses to intercept and modify the builder used by the {@link #createFileFinder(Object,ContextProperties,BeanStore)} method.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The file finder builder for this REST resource.
	 * @throws Exception If file finder builder could not be instantiated.
	 */
	protected FileFinderBuilder createFileFinderBuilder(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		Class<? extends FileFinder> c = properties.getIfClass(REST_fileFinder, FileFinder.class).orElse(null);

		if (c == null)
			c = properties.getIfClass(REST_fileFinderDefault, FileFinder.class).orElse(null);

		FileFinderBuilder x = FileFinder
			.create()
			.beanStore(beanStore)
			.implClass(c)
			.dir("static")
			.dir("htdocs")
			.cp(getResourceClass(), "htdocs", true)
			.cp(getResourceClass(), "/htdocs", true)
			.caching(1_000_000)
			.exclude("(?i).*\\.(class|properties)");

		x = BeanStore
			.of(beanStore, resource)
			.addBean(FileFinderBuilder.class, x)
			.beanCreateMethodFinder(FileFinderBuilder.class, resource)
			.find("createFileFinderBuilder")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the static files finder for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of FileFinder.
	 * 	<li>Looks for {@link #REST_staticFiles} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#staticFiles(Class)}/{@link RestContextBuilder#staticFiles(StaticFiles)}
	 * 			<li>{@link Rest#staticFiles()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createStaticFiles()</> method that returns {@link StaticFiles} on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>{@link BasicFileFinder}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Looks for value in {@link #REST_staticFilesDefault} setting.
	 * 	<li>Instantiates a {@link BasicStaticFiles}.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link #REST_staticFiles}
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The file finder for this REST resource.
	 * @throws Exception If file finder could not be instantiated.
	 */
	protected StaticFiles createStaticFiles(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		StaticFiles x = null;

		if (resource instanceof StaticFiles)
			x = (StaticFiles)resource;

		if (x == null)
			x = properties.getIfType(REST_staticFiles, StaticFiles.class).orElse(null);

		if (x == null)
			x = beanStore.getBean(StaticFiles.class).orElse(null);

		if (x == null)
			x = properties.getIfType(REST_staticFilesDefault, StaticFiles.class).orElse(null);

		if (x == null)
			x = (StaticFiles)createStaticFilesBuilder(resource, properties, beanStore).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(StaticFiles.class, x)
			.beanCreateMethodFinder(StaticFiles.class, resource)
			.find("createStaticFiles")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the static files builder for this REST resource.
	 *
	 * <p>
	 * Allows subclasses to intercept and modify the builder used by the {@link #createStaticFiles(Object,ContextProperties,BeanStore)} method.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The static files builder for this REST resource.
	 * @throws Exception If static files builder could not be instantiated.
	 */
	@SuppressWarnings("unchecked")
	protected StaticFilesBuilder createStaticFilesBuilder(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		Class<? extends StaticFiles> c = properties.getIfClass(REST_staticFiles, StaticFiles.class).orElse(null);

		if (c == null)
			c = properties.getIfClass(REST_staticFilesDefault, StaticFiles.class).orElse(null);

		StaticFilesBuilder x = StaticFiles
			.create()
			.beanStore(beanStore)
			.implClass((Class<? extends FileFinder>)c)
			.dir("static")
			.dir("htdocs")
			.cp(getResourceClass(), "htdocs", true)
			.cp(getResourceClass(), "/htdocs", true)
			.caching(1_000_000)
			.exclude("(?i).*\\.(class|properties)")
			.headers(cacheControl("max-age=86400, public"));

		x = BeanStore
			.of(beanStore, resource)
			.addBean(StaticFilesBuilder.class, x)
			.beanCreateMethodFinder(StaticFilesBuilder.class, resource)
			.find("createStaticFilesBuilder")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the call logger this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of RestLogger.
	 * 	<li>Looks for REST call logger set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#callLogger(Class)}/{@link RestContextBuilder#callLogger(RestLogger)}
	 * 			<li>{@link Rest#callLogger()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createCallLogger()</> method that returns {@link RestLogger} on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>{@link BasicFileFinder}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Looks for {@link RestContextBuilder#callLoggerDefault(RestLogger)}.
	 * 	<li>Instantiates a {@link BasicFileFinder}.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#callLogger(Class)}
	 * 	<li class='jm'>{@link RestContextBuilder#callLogger(RestLogger)}
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @param logger
	 * 	The Java logger to use for logging messages.
	 * 	<br>Created by {@link #createLogger(Object,ContextProperties,BeanStore)}.
	 * @param thrownStore
	 * 	The thrown exception statistics store.
	 * 	<br>Created by {@link #createThrownStore(Object,ContextProperties,RestContext,BeanStore)}.
	 * @return The file finder for this REST resource.
	 * @throws Exception If file finder could not be instantiated.
	 */
	protected RestLogger createCallLogger(Object resource, RestContextBuilder builder, BeanStore beanStore, Logger logger, ThrownStore thrownStore) throws Exception {

		RestLogger x = null;

		if (resource instanceof RestLogger)
			x = (RestLogger)resource;

		if (x == null)
			x = builder.callLogger.value().orElse(null);

		if (x == null)
			x = beanStore.getBean(RestLogger.class).orElse(null);

		if (x == null)
			x = builder.callLoggerDefault.value().orElse(null);

		if (x == null)
			x = createCallLoggerBuilder(resource, builder, beanStore, logger, thrownStore).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(RestLogger.class, x)
			.beanCreateMethodFinder(RestLogger.class, resource)
			.find("createCallLogger")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the call logger builder for this REST resource.
	 *
	 * <p>
	 * Allows subclasses to intercept and modify the builder used by the {@link #createCallLogger(Object,RestContextBuilder,BeanStore,Logger,ThrownStore)} method.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @param logger
	 * 	The Java logger to use for logging messages.
	 * 	<br>Created by {@link #createLogger(Object,ContextProperties,BeanStore)}.
	 * @param thrownStore
	 * 	The thrown exception statistics store.
	 * 	<br>Created by {@link #createThrownStore(Object,ContextProperties,RestContext,BeanStore)}.
	 * @return The call logger builder for this REST resource.
	 * @throws Exception If call logger builder could not be instantiated.
	 */
	protected RestLoggerBuilder createCallLoggerBuilder(Object resource, RestContextBuilder builder, BeanStore beanStore, Logger logger, ThrownStore thrownStore) throws Exception {

		Class<? extends RestLogger> c = builder.callLogger.type().orElse(null);

		if (c == null)
			c = builder.callLoggerDefault.type().orElse(null);

		RestLoggerBuilder x = RestLogger
			.create()
			.beanStore(beanStore)
			.implClass(c)
			.normalRules(  // Rules when debugging is not enabled.
				RestLoggerRule.create()  // Log 500+ errors with status-line and header information.
					.statusFilter(a -> a >= 500)
					.level(SEVERE)
					.requestDetail(HEADER)
					.responseDetail(HEADER)
					.build(),
				RestLoggerRule.create()  // Log 400-500 errors with just status-line information.
					.statusFilter(a -> a >= 400)
					.level(WARNING)
					.requestDetail(STATUS_LINE)
					.responseDetail(STATUS_LINE)
					.build()
			)
			.debugRules(  // Rules when debugging is enabled.
				RestLoggerRule.create()  // Log everything with full details.
					.level(SEVERE)
					.requestDetail(ENTITY)
					.responseDetail(ENTITY)
					.build()
			)
			.logger(logger)
			.thrownStore(thrownStore);

		x = BeanStore
			.of(beanStore, resource)
			.addBean(RestLoggerBuilder.class, x)
			.beanCreateMethodFinder(RestLoggerBuilder.class, resource)
			.find("createCallLoggerBuilder")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the response handlers for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for response processors set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#responseProcessors(Class...)}/{@link RestContextBuilder#responseProcessors(ResponseProcessor...)}
	 * 			<li>{@link Rest#responseProcessors()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createResponseProcessors()</> method that returns <c>{@link ResponseProcessor}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a <c>ResponseProcessor[0]</c>.
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The response handlers for this REST resource.
	 * @throws Exception If response handlers could not be instantiated.
	 */
	protected ResponseProcessorList createResponseProcessors(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {

		ResponseProcessorList.Builder x = builder.responseProcessors.beanStore(beanStore);

		x = BeanStore
			.of(beanStore, resource)
			.addBean(ResponseProcessorList.Builder.class, x)
			.beanCreateMethodFinder(ResponseProcessorList.Builder.class, resource)
			.find("createResponseProcessors")
			.withDefault(x)
			.run();

		return x.build();
	}

	/**
	 * Instantiates the HTTP part serializer for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link HttpPartSerializer}.
	 * 	<li>Looks for {@link #REST_partSerializer} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partSerializer(Class)}/{@link RestContextBuilder#partSerializer(HttpPartSerializer)}
	 * 			<li>{@link Rest#partSerializer()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createPartSerializer()</> method that returns <c>{@link HttpPartSerializer}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates an {@link OpenApiSerializer}.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link #REST_partSerializer}
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The HTTP part serializer for this REST resource.
	 * @throws Exception If serializer could not be instantiated.
	 */
	protected HttpPartSerializer createPartSerializer(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		HttpPartSerializer x = null;

		if (resource instanceof HttpPartSerializer)
			x = (HttpPartSerializer)resource;

		if (x == null)
			x = properties.getInstance(REST_partSerializer, HttpPartSerializer.class, beanStore).orElse(null);

		if (x == null)
			x = beanStore.getBean(HttpPartSerializer.class).orElse(null);

		if (x == null)
			x = OpenApiSerializer.create().apply(properties).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(HttpPartSerializer.class, x)
			.beanCreateMethodFinder(HttpPartSerializer.class, resource)
			.find("createPartSerializer")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the HTTP part parser for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link HttpPartParser}.
	 * 	<li>Looks for {@link #REST_partParser} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partParser(Class)}/{@link RestContextBuilder#partParser(HttpPartParser)}
	 * 			<li>{@link Rest#partParser()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createPartParser()</> method that returns <c>{@link HttpPartParser}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates an {@link OpenApiSerializer}.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link #REST_partParser}
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The HTTP part parser for this REST resource.
	 * @throws Exception If parser could not be instantiated.
	 */
	protected HttpPartParser createPartParser(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		HttpPartParser x = null;

		if (resource instanceof HttpPartParser)
			x = (HttpPartParser)resource;

		if (x == null)
			x = properties.getInstance(REST_partParser, HttpPartParser.class, beanStore).orElse(null);

		if (x == null)
			x = beanStore.getBean(HttpPartParser.class).orElse(null);

		if (x == null)
			x = OpenApiParser.create().apply(properties).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(HttpPartParser.class, x)
			.beanCreateMethodFinder(HttpPartParser.class, resource)
			.find("createPartParser")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the REST method parameter resolvers for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for REST op args set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#restOpArgs(Class...)}/{@link RestContextBuilder#restOpArgs(Class...)}
	 * 			<li>{@link Rest#restOpArgs()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createRestParams()</> method that returns <c>{@link Class}[]</c>.
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a default set of parameters.
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The REST method parameter resolvers for this REST resource.
	 * @throws Exception If parameter resolvers could not be instantiated.
	 */
	protected RestOpArgList createOpArgs(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {

		RestOpArgList.Builder x = builder.restOpArgs;

		x = BeanStore
			.of(beanStore, resource)
			.addBean(RestOpArgList.Builder.class, x)
			.beanCreateMethodFinder(RestOpArgList.Builder.class, resource)
			.find("createRestOperationArgs")
			.withDefault(x)
			.run();

		return x.build();
	}

	/**
	 * Instantiates the hook method parameter resolvers for this REST resource.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The REST method parameter resolvers for this REST resource.
	 * @throws Exception If parameter resolvers could not be instantiated.
	 */
	@SuppressWarnings("unchecked")
	protected RestOpArgList createHookMethodArgs(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		RestOpArgList.Builder x = RestOpArgList.create();

		x.append(
			ConfigArg.class,
			HeaderArg.class,
			HttpServletRequestArg.class,
			HttpServletResponseArg.class,
			InputStreamArg.class,
			LocaleArg.class,
			MessagesArg.class,
			MethodArg.class,
			OutputStreamArg.class,
			ReaderArg.class,
			ResourceBundleArg.class,
			RestContextArg.class,
			RestRequestArg.class,
			ServetInputStreamArg.class,
			ServletOutputStreamArg.class,
			TimeZoneArg.class,
			WriterArg.class,
			DefaultArg.class
		);

		x = BeanStore
			.of(beanStore, resource)
			.addBean(RestOpArgList.Builder.class, x)
			.beanCreateMethodFinder(RestOpArgList.Builder.class, resource)
			.find("createHookMethodArgs")
			.withDefault(x)
			.run();

		return x.build();
	}

	/**
	 * Instantiates logger for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for a static or non-static <c>createLogger()</> method that returns <c>{@link Logger}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates via <c>Logger.<jsm>getLogger</jsm>(<jv>resource</jv>.getClass().getName())</c>.
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The logger for this REST resource.
	 * @throws Exception If logger could not be instantiated.
	 */
	protected Logger createLogger(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		Logger x = beanStore.getBean(Logger.class).orElse(null);

		if (x == null)
			x = Logger.getLogger(className(resource));

		x = BeanStore
			.of(beanStore, resource)
			.addBean(Logger.class, x)
			.beanCreateMethodFinder(Logger.class, resource)
			.find("createLogger")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the JSON schema generator for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for a static or non-static <c>createJsonSchemaGenerator()</> method that returns <c>{@link JsonSchemaGenerator}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a new {@link JsonSchemaGenerator} using the property store of this context..
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The JSON schema generator for this REST resource.
	 * @throws Exception If JSON schema generator could not be instantiated.
	 */
	protected JsonSchemaGenerator createJsonSchemaGenerator(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {
		JsonSchemaGenerator x = beanStore.getBean(JsonSchemaGenerator.class).orElse(null);

		if (x == null)
			x = createJsonSchemaGeneratorBuilder(resource, properties, beanStore).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(JsonSchemaGenerator.class, x)
			.beanCreateMethodFinder(JsonSchemaGenerator.class, resource)
			.find("createJsonSchemaGenerator")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the JSON-schema generator builder for this REST resource.
	 *
	 * <p>
	 * Allows subclasses to intercept and modify the builder used by the {@link #createJsonSchemaGenerator(Object,ContextProperties,BeanStore)} method.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The JSON-schema generator builder for this REST resource.
	 * @throws Exception If JSON-schema generator builder could not be instantiated.
	 */
	protected JsonSchemaGeneratorBuilder createJsonSchemaGeneratorBuilder(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {
		JsonSchemaGeneratorBuilder x = JsonSchemaGenerator
			.create()
			.apply(properties);

		x = BeanStore
			.of(beanStore, resource)
			.addBean(JsonSchemaGeneratorBuilder.class, x)
			.beanCreateMethodFinder(JsonSchemaGeneratorBuilder.class, resource)
			.find("createJsonSchemaGeneratorBuilder")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the REST info provider for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link SwaggerProvider}.
	 * 	<li>Looks for swagger provider set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#swaggerProvider(Class)}/{@link RestContextBuilder#swaggerProvider(SwaggerProvider)}
	 * 			<li>{@link Rest#swaggerProvider()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createSwaggerProvider()</> method that returns {@link SwaggerProvider} on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a default {@link BasicSwaggerProvider}.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#swaggerProvider(Class)}
	 * 	<li class='jm'>{@link RestContextBuilder#swaggerProvider(SwaggerProvider)}
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @param fileFinder The file finder configured on this bean created by {@link #createFileFinder(Object,ContextProperties,BeanStore)}.
	 * @param messages The localized messages configured on this bean created by {@link #createMessages(Object,ContextProperties)}.
	 * @param varResolver The variable resolver configured on this bean created by {@link #createVarResolver(Object,ContextProperties,BeanStore,Messages)}.
	 * @return The info provider for this REST resource.
	 * @throws Exception If info provider could not be instantiated.
	 */
	protected SwaggerProvider createSwaggerProvider(Object resource, RestContextBuilder builder, BeanStore beanStore, FileFinder fileFinder, Messages messages, VarResolver varResolver) throws Exception {

		SwaggerProvider x = builder.swaggerProvider.value().orElse(null);

		if (resource instanceof SwaggerProvider)
			x = (SwaggerProvider)resource;

		if (x == null)
			x = beanStore.getBean(SwaggerProvider.class).orElse(null);

		if (x == null)
			x = createSwaggerProviderBuilder(resource, builder, beanStore, fileFinder, messages, varResolver).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(SwaggerProvider.class, x)
			.beanCreateMethodFinder(SwaggerProvider.class, resource)
			.find("createSwaggerProvider")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the REST API builder for this REST resource.
	 *
	 * <p>
	 * Allows subclasses to intercept and modify the builder used by the {@link #createSwaggerProvider(Object,RestContextBuilder,BeanStore,FileFinder,Messages,VarResolver)} method.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @param fileFinder The file finder configured on this bean created by {@link #createFileFinder(Object,ContextProperties,BeanStore)}.
	 * @param messages The localized messages configured on this bean created by {@link #createMessages(Object,ContextProperties)}.
	 * @param varResolver The variable resolver configured on this bean created by {@link #createVarResolver(Object,ContextProperties,BeanStore,Messages)}.
	 * @return The REST API builder for this REST resource.
	 * @throws Exception If REST API builder could not be instantiated.
	 */
	protected SwaggerProviderBuilder createSwaggerProviderBuilder(Object resource, RestContextBuilder builder, BeanStore beanStore, FileFinder fileFinder, Messages messages, VarResolver varResolver) throws Exception {

		Class<? extends SwaggerProvider> c = builder.swaggerProvider.type().orElse(null);

		SwaggerProviderBuilder x = SwaggerProvider
				.create()
				.beanStore(beanStore)
				.fileFinder(fileFinder)
				.messages(messages)
				.varResolver(varResolver)
				.jsonSchemaGenerator(createJsonSchemaGenerator(resource, builder.getContextProperties(), beanStore))
				.implClass(c);

		x = BeanStore
			.of(beanStore, resource)
			.addBean(SwaggerProviderBuilder.class, x)
			.beanCreateMethodFinder(SwaggerProviderBuilder.class, resource)
			.find("createSwaggerProviderBuilder")
			.withDefault(x)
			.run();

		return x;

	}

	/**
	 * Instantiates the variable resolver for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for a static or non-static <c>createVarResolver()</> method that returns <c>{@link VarResolver}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a new {@link VarResolver} using the variables returned by {@link #createVars(Object,ContextProperties,BeanStore)}.
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @param messages The localized messages of this bean.
	 * @return The variable resolver for this REST resource.
	 * @throws Exception If variable resolver could not be instantiated.
	 */
	protected VarResolver createVarResolver(Object resource, ContextProperties properties, BeanStore beanStore, Messages messages) throws Exception {

		VarResolver x = beanStore.getBean(VarResolver.class).orElse(null);

		if (x == null)
			x = builder.varResolverBuilder
				.vars(createVars(resource, properties, beanStore))
				.bean(Messages.class, messages)
				.build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(VarResolver.class, x)
			.beanCreateMethodFinder(VarResolver.class, resource)
			.find("createVarResolver")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the variable resolver variables for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for a static or non-static <c>createVars()</> method that returns <c>{@link VarList}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a new {@link VarList} using default variables.
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The variable resolver variables for this REST resource.
	 * @throws Exception If variable resolver variables could not be instantiated.
	 */
	@SuppressWarnings("unchecked")
	protected VarList createVars(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		VarList x = beanStore.getBean(VarList.class).orElse(null);

		if (x == null)
			x = VarList.of(
				FileVar.class,
				LocalizationVar.class,
				RequestAttributeVar.class,
				RequestFormDataVar.class,
				RequestHeaderVar.class,
				RequestPathVar.class,
				RequestQueryVar.class,
				RequestVar.class,
				RequestSwaggerVar.class,
				SerializedRequestAttrVar.class,
				ServletInitParamVar.class,
				SwaggerVar.class,
				UrlVar.class,
				UrlEncodeVar.class,
				HtmlWidgetVar.class
			);

		x = BeanStore
			.of(beanStore, resource)
			.addBean(VarList.class, x)
			.beanCreateMethodFinder(VarList.class, resource)
			.find("createVars")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the thrown exception store for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for a static or non-static <c>createThrownStore()</> method that returns <c>{@link ThrownStore}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Returns {@link ThrownStore#GLOBAL}.
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param parent
	 * 	The parent context if the REST bean was registered via {@link Rest#children()}.
	 * 	<br>Will be <jk>null</jk> if the bean is a top-level resource.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The stack trace store for this REST resource.
	 * @throws Exception If stack trace store could not be instantiated.
	 */
	protected ThrownStore createThrownStore(Object resource, ContextProperties properties, RestContext parent, BeanStore beanStore) throws Exception {

		ThrownStore x = beanStore.getBean(ThrownStore.class).orElse(null);

		if (x == null)
			x = createThrownStoreBuilder(resource, properties, parent, beanStore).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(ThrownStore.class, x)
			.beanCreateMethodFinder(ThrownStore.class, resource)
			.find("createThrownStore")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the thrown exception store builder for this REST resource.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param parent
	 * 	The parent context if the REST bean was registered via {@link Rest#children()}.
	 * 	<br>Will be <jk>null</jk> if the bean is a top-level resource.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The stack trace store for this REST resource.
	 * @throws Exception If stack trace store could not be instantiated.
	 */
	protected ThrownStoreBuilder createThrownStoreBuilder(Object resource, ContextProperties properties, RestContext parent, BeanStore beanStore) throws Exception {

		ThrownStore p = parent == null ? null : parent.thrownStore;

		ThrownStoreBuilder x = ThrownStore
			.create()
			.parent(p)
			.beanStore(beanStore);

		x = BeanStore
			.of(beanStore, resource)
			.addBean(ThrownStoreBuilder.class, x)
			.beanCreateMethodFinder(ThrownStoreBuilder.class, resource)
			.find("createThrownStoreBuilder")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the method execution statistics store for this REST resource.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @param thrownStore
	 * 	The thrown exception statistics store.
	 * 	<br>Created by {@link #createThrownStore(Object,ContextProperties,RestContext,BeanStore)}.
	 * @return The stack trace store for this REST resource.
	 * @throws Exception If stack trace store could not be instantiated.
	 */
	protected MethodExecStore createMethodExecStore(Object resource, ContextProperties properties, BeanStore beanStore, ThrownStore thrownStore) throws Exception {

		MethodExecStore x = beanStore.getBean(MethodExecStore.class).orElse(null);

		if (x == null)
			x = createMethodExecStoreBuilder(resource, properties, beanStore, thrownStore).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(MethodExecStore.class, x)
			.beanCreateMethodFinder(MethodExecStore.class, resource)
			.find("cxreateMethodExecStore")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the method execution statistics store for this REST resource.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @param thrownStore
	 * 	The thrown exception statistics store.
	 * 	<br>Created by {@link #createThrownStore(Object,ContextProperties,RestContext,BeanStore)}.
	 * @return The stack trace store for this REST resource.
	 * @throws Exception If stack trace store could not be instantiated.
	 */
	protected MethodExecStoreBuilder createMethodExecStoreBuilder(Object resource, ContextProperties properties, BeanStore beanStore, ThrownStore thrownStore) throws Exception {

		MethodExecStoreBuilder x = MethodExecStore
			.create()
			.thrownStore(thrownStore)
			.beanStore(beanStore);

		x = BeanStore
			.of(beanStore, resource)
			.addBean(MethodExecStoreBuilder.class, x)
			.beanCreateMethodFinder(MethodExecStoreBuilder.class, resource)
			.find("createMethodExecStoreBuilder")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the default request headers for this REST object.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The default request headers for this REST object.
	 * @throws Exception If stack trace store could not be instantiated.
	 */
	protected HeaderListBuilder createDefaultRequestHeaders(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {

		HeaderListBuilder x = builder.defaultRequestHeaders;

		x = BeanStore
			.of(beanStore, resource)
			.addBean(HeaderListBuilder.class, x)
			.beanCreateMethodFinder(HeaderListBuilder.class, resource)
			.find("createDefaultRequestHeaders")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the default response headers for this REST object.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If stack trace store could not be instantiated.
	 */
	protected HeaderListBuilder createDefaultResponseHeaders(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {

		HeaderListBuilder x = builder.defaultResponseHeaders;

		x = BeanStore
			.of(beanStore, resource)
			.addBean(HeaderListBuilder.class, x)
			.beanCreateMethodFinder(HeaderListBuilder.class, resource)
			.find("createDefaultResponseHeaders")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the default response headers for this REST object.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If stack trace store could not be instantiated.
	 */
	protected NamedAttributeList createDefaultRequestAttributes(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {

		NamedAttributeList x = builder.defaultRequestAttributes;

		x = BeanStore
			.of(beanStore, resource)
			.addBean(NamedAttributeList.class, x)
			.beanCreateMethodFinder(NamedAttributeList.class, resource)
			.find("createDefaultRequestAttributes")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the debug enablement bean for this REST object.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The debug enablement bean for this REST object.
	 * @throws Exception If bean could not be created.
	 */
	protected DebugEnablement createDebugEnablement(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {
		DebugEnablement x = null;

		if (resource instanceof DebugEnablement)
			x = (DebugEnablement)resource;

		if (x == null)
			x = builder.debugEnablement.value().orElse(null);

		if (x == null)
			x = beanStore.getBean(DebugEnablement.class).orElse(null);

		if (x == null)
			x = createDebugEnablementBuilder(resource, builder, beanStore).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(DebugEnablement.class, x)
			.beanCreateMethodFinder(DebugEnablement.class, resource)
			.find("createDebugEnablement")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the debug enablement bean builder for this REST object.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The debug enablement bean builder for this REST object.
	 * @throws Exception If bean builder could not be created.
	 */
	protected DebugEnablementBuilder createDebugEnablementBuilder(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {

		Class<? extends DebugEnablement> c = builder.debugEnablement.type().orElse(null);

		DebugEnablementBuilder x = DebugEnablement
			.create()
			.beanStore(beanStore)
			.implClass(c);

		x = BeanStore
			.of(beanStore, resource)
			.addBean(DebugEnablementBuilder.class, x)
			.beanCreateMethodFinder(DebugEnablementBuilder.class, resource)
			.find("createDebugEnablementBuilder")
			.withDefault(x)
			.run();

		Enablement defaultDebug = builder.debug;

		if (defaultDebug == null)
			defaultDebug = builder.debugDefault;

		if (defaultDebug == null)
			defaultDebug = isDebug() ? Enablement.ALWAYS : Enablement.NEVER;

		x.defaultEnable(defaultDebug);

		for (Map.Entry<String,String> e : splitMap(ofNullable(builder.debugOn).orElse(""), true).entrySet()) {
			String k = e.getKey(), v = e.getValue();
			if (v.isEmpty())
				v = "ALWAYS";
			if (! k.isEmpty())
				x.enable(Enablement.fromString(v), k);
		}

		for (MethodInfo mi : ClassInfo.ofProxy(resource).getPublicMethods()) {
			Optional<String> o = mi.getAnnotationGroupList(RestOp.class).getValues(String.class, "debug").stream().filter(y->!y.isEmpty()).findFirst();
			if (o.isPresent())
				x.enable(Enablement.fromString(o.get()), mi.getFullName());
		}

		return x;
	}

	/**
	 * Instantiates the messages for this REST object.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @return The messages for this REST object.
	 * @throws Exception An error occurred.
	 */
	protected Messages createMessages(Object resource, ContextProperties properties) throws Exception {

		Messages x = createMessagesBuilder(resource, properties).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(Messages.class, x)
			.beanCreateMethodFinder(Messages.class, resource)
			.find("createMessages")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the Messages builder for this REST resource.
	 *
	 * <p>
	 * Allows subclasses to intercept and modify the builder used by the {@link #createMessages(Object,ContextProperties)} method.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @return The messages builder for this REST resource.
	 * @throws Exception If messages builder could not be instantiated.
	 */
	protected MessagesBuilder createMessagesBuilder(Object resource, ContextProperties properties) throws Exception {

		Tuple2<Class<?>,String>[] mbl = properties.getInstanceArray(REST_messages, Tuple2.class).orElse(new Tuple2[0]);
		MessagesBuilder x = null;

		for (int i = mbl.length-1; i >= 0; i--) {
			Class<?> c = firstNonNull(mbl[i].getA(), resource.getClass());
			String value = mbl[i].getB();
			if (isJsonObject(value,true)) {
				MessagesString ms = SimpleJson.DEFAULT.read(value, MessagesString.class);
				x = Messages.create(c).name(ms.name).baseNames(split(ms.baseNames, ',')).locale(ms.locale).parent(x == null ? null : x.build());
			} else {
				x = Messages.create(c).name(value).parent(x == null ? null : x.build());
			}
		}

		if (x == null)
			x = Messages.create(resource.getClass());

		x = BeanStore
			.of(beanStore, resource)
			.addBean(MessagesBuilder.class, x)
			.beanCreateMethodFinder(MessagesBuilder.class, resource)
			.find("createMessagesBuilder")
			.withDefault(x)
			.run();

		return x;
	}

	private static class MessagesString {
		public String name;
		public String[] baseNames;
		public String locale;
	}

	/**
	 * Creates the set of {@link RestOpContext} objects that represent the methods on this resource.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The builder for the {@link RestOperations} object.
	 * @throws Exception An error occurred.
	 */
	protected RestOperations createRestOperations(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {

		RestOperations x = createRestOperationsBuilder(resource, builder, beanStore).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(RestOperations.class, x)
			.beanCreateMethodFinder(RestOperations.class, resource)
			.find("createRestOperations")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the REST methods builder for this REST resource.
	 *
	 * <p>
	 * Allows subclasses to intercept and modify the builder used by the {@link #createRestOperations(Object,RestContextBuilder,BeanStore)} method.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The REST methods builder for this REST resource.
	 * @throws Exception If REST methods builder could not be instantiated.
	 */
	protected RestOperationsBuilder createRestOperationsBuilder(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {

		RestOperationsBuilder x = RestOperations
			.create()
			.beanStore(beanStore)
			.implClass(builder.operationsClass);

		ClassInfo rci = ClassInfo.of(resource);

		for (MethodInfo mi : rci.getPublicMethods()) {
			AnnotationList al = mi.getAnnotationGroupList(RestOp.class);

			// Also include methods on @Rest-annotated interfaces.
			if (al.size() == 0) {
				for (Method mi2 : mi.getMatching()) {
					Class<?> ci2 = mi2.getDeclaringClass();
					if (ci2.isInterface() && ci2.getAnnotation(Rest.class) != null) {
						al.add(AnnotationInfo.of(MethodInfo.of(mi2), RestOpAnnotation.DEFAULT));
					}
				}
			}
			if (al.size() > 0) {
				try {
					if (mi.isNotPublic())
						throw new RestServletException("@RestOp method {0}.{1} must be defined as public.", rci.inner().getName(), mi.getSimpleName());

					RestOpContext roc = RestOpContext
						.create(mi.inner(), this)
						.beanStore(beanStore)
						.contextClass(builder.opContextClass)
						.build();

					String httpMethod = roc.getHttpMethod();

					// RRPC is a special case where a method returns an interface that we
					// can perform REST calls against.
					// We override the CallMethod.invoke() method to insert our logic.
					if ("RRPC".equals(httpMethod)) {

						RestOpContext roc2 = RestOpContext
							.create(mi.inner(), this)
							.dotAll()
							.beanStore(rootBeanStore)
							.contextClass(RrpcRestOpContext.class)
							.build();
						x
							.add("GET", roc2)
							.add("POST", roc2);

					} else {
						x.add(roc);
					}
				} catch (Throwable e) {
					throw new RestServletException(e, "Problem occurred trying to initialize methods on class {0}", rci.inner().getName());
				}
			}
		}

		x = BeanStore
			.of(beanStore, resource)
			.addBean(RestOperationsBuilder.class, x)
			.beanCreateMethodFinder(RestOperationsBuilder.class, resource)
			.find("createRestOperationsBuilder")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Creates the builder for the children of this resource.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @param servletConfig
	 * 	The servlet config passed into the servlet by the servlet container.
	 * @return The builder for the {@link RestChildren} object.
	 * @throws Exception An error occurred.
	 */
	protected RestChildren createRestChildren(Object resource, RestContextBuilder builder, BeanStore beanStore, ServletConfig servletConfig) throws Exception {

		RestChildren x = createRestChildrenBuilder(resource, builder, beanStore, servletConfig).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(RestChildren.class, x)
			.beanCreateMethodFinder(RestChildren.class, resource)
			.find("createRestChildren")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the REST children builder for this REST resource.
	 *
	 * <p>
	 * Allows subclasses to intercept and modify the builder used by the {@link #createRestChildren(Object,RestContextBuilder,BeanStore,ServletConfig)} method.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @param servletConfig
	 * 	The servlet config passed into the servlet by the servlet container.
	 * @return The REST children builder for this REST resource.
	 * @throws Exception If REST children builder could not be instantiated.
	 */
	protected RestChildrenBuilder createRestChildrenBuilder(Object resource, RestContextBuilder builder, BeanStore beanStore, ServletConfig servletConfig) throws Exception {

		RestChildrenBuilder x = RestChildren
			.create()
			.beanStore(beanStore)
			.implClass(builder.childrenClass);

		// Initialize our child resources.
		for (Object o : builder.children) {
			String path = null;

			if (o instanceof RestChild) {
				RestChild rc = (RestChild)o;
				path = rc.path;
				o = rc.resource;
			}

			RestContextBuilder cb = null;

			if (o instanceof Class) {
				Class<?> oc = (Class<?>)o;
				// Don't allow specifying yourself as a child.  Causes an infinite loop.
				if (oc == builder.resourceClass)
					continue;
				cb = RestContext.create(this, servletConfig, oc, null);
				BeanStore bf = BeanStore.of(beanStore, resource).addBean(RestContextBuilder.class, cb);
				if (bf.getBean(oc).isPresent()) {
					o = (Supplier<?>)()->bf.getBean(oc).get();  // If we resolved via injection, always get it this way.
				} else {
					o = bf.createBean(oc);
				}
			} else {
				cb = RestContext.create(this, servletConfig, o.getClass(), o);
			}

			if (path != null)
				cb.path(path);

			RestContext cc = cb.init(o).build();

			MethodInfo mi = ClassInfo.of(o).getMethod("setContext", RestContext.class);
			if (mi != null)
				mi.accessible().invoke(o, cc);

			x.add(cc);
		}

		x = BeanStore
			.of(beanStore, resource)
			.addBean(RestChildrenBuilder.class, x)
			.beanCreateMethodFinder(RestChildrenBuilder.class, resource)
			.find("createRestChildrenBuilder")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the list of {@link HookEvent#START_CALL} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createStartCallMethods(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.START_CALL)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createStartCallMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Instantiates the list of {@link HookEvent#END_CALL} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createEndCallMethods(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.END_CALL)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createEndCallMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Instantiates the list of {@link HookEvent#POST_INIT} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createPostInitMethods(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.POST_INIT)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createPostInitMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Instantiates the list of {@link HookEvent#POST_INIT_CHILD_FIRST} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createPostInitChildFirstMethods(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.POST_INIT_CHILD_FIRST)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createPostInitChildFirstMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Instantiates the list of {@link HookEvent#DESTROY} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createDestroyMethods(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.DESTROY)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createDestroyMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Instantiates the list of {@link HookEvent#PRE_CALL} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createPreCallMethods(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.PRE_CALL)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createPreCallMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Instantiates the list of {@link HookEvent#POST_CALL} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param properties
	 * 	The properties of this bean.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createPostCallMethods(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.POST_CALL)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createPostCallMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Returns the bean store associated with this context.
	 *
	 * <p>
	 * The bean store is used for instantiating child resource classes.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link #REST_beanStore}
	 * </ul>
	 *
	 * @return The resource resolver associated with this context.
	 */
	protected BeanStore getBeanStore() {
		return beanStore;
	}

	/**
	 * Returns the time statistics gatherer for the specified method.
	 *
	 * @param m The method to get statistics for.
	 * @return The cached time-stats object.
	 */
	protected MethodExecStats getMethodExecStats(Method m) {
		return this.methodExecStore.getStats(m);
	}

	/**
	 * Returns the variable resolver for this servlet.
	 *
	 * <p>
	 * Variable resolvers are used to replace variables in property values.
	 * They can be nested arbitrarily deep.
	 * They can also return values that themselves contain other variables.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		messages=<js>"nls/Messages"</js>,
	 * 		properties={
	 * 			<ja>@Property</ja>(name=<js>"title"</js>,value=<js>"$L{title}"</js>),  <jc>// Localized variable in Messages.properties</jc>
	 * 			<ja>@Property</ja>(name=<js>"javaVendor"</js>,value=<js>"$S{java.vendor,Oracle}"</js>),  <jc>// System property with default value</jc>
	 * 			<ja>@Property</ja>(name=<js>"foo"</js>,value=<js>"bar"</js>),
	 * 			<ja>@Property</ja>(name=<js>"bar"</js>,value=<js>"baz"</js>),
	 * 			<ja>@Property</ja>(name=<js>"v1"</js>,value=<js>"$R{foo}"</js>),  <jc>// Request variable.  value="bar"</jc>
	 * 			<ja>@Property</ja>(name=<js>"v1"</js>,value=<js>"$R{foo,bar}"</js>),  <jc>// Request variable.  value="bar"</jc>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyRestResource <jk>extends</jk> BasicRestServlet {
	 * </p>
	 *
	 * <p>
	 * A typical usage pattern involves using variables inside the {@link HtmlDocConfig @HtmlDocConfig} annotation:
	 * <p class='bcode w800'>
	 * 	<ja>@RestGet</ja>(<js>"/{name}/*"</js>)
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		navlinks={
	 * 			<js>"up: $R{requestParentURI}"</js>,
	 * 			<js>"api: servlet:/api"</js>,
	 * 			<js>"stats: servlet:/stats"</js>,
	 * 			<js>"editLevel: servlet:/editLevel?logger=$A{attribute.name, OFF}"</js>
	 * 		}
	 * 		header={
	 * 			<js>"&lt;h1&gt;$L{MyLocalizedPageTitle}&lt;/h1&gt;"</js>
	 * 		},
	 * 		aside={
	 * 			<js>"$F{resources/AsideText.html}"</js>
	 * 		}
	 * 	)
	 * 	<jk>public</jk> LoggerEntry getLogger(RestRequest <jv>req</jv>, <ja>@Path</ja> String <jv>name</jv>) <jk>throws</jk> Exception {
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#vars(Class...)} - For adding custom vars.
	 * 	<li class='link'>{@doc RestSvlVariables}
	 * 	<li class='link'>{@doc RestSvlVariables}
	 * </ul>
	 *
	 * @return The var resolver in use by this resource.
	 */
	public VarResolver getVarResolver() {
		return varResolver;
	}

	/**
	 * Returns the config file associated with this servlet.
	 *
	 * <p>
	 * The config file is identified via one of the following:
	 * <ul class='javatree'>
	 * 	<li class='ja'>{@link Rest#config()}
	 * 	<li class='jm'>{@link RestContextBuilder#config(Config)}
	 * </ul>
	 *
	 * @return
	 * 	The resolving config file associated with this servlet.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Config getConfig() {
		return config;
	}


	/**
	 * Returns the path for this resource as defined by the {@link Rest#path() @Rest(path)} annotation or
	 * {@link RestContextBuilder#path(String)} method.
	 *
	 * <p>
	 * If path is not specified, returns <js>""</js>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContextBuilder#path(String)}
	 * </ul>
	 *
	 * @return The servlet path.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns the path for this resource as defined by the {@link Rest#path() @Rest(path)} annotation or
	 * {@link RestContextBuilder#path(String)} method concatenated with those on all parent classes.
	 *
	 * <p>
	 * If path is not specified, returns <js>""</js>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#path(String)}
	 * </ul>
	 *
	 * @return The full path.
	 */
	public String getFullPath() {
		return fullPath;
	}

	/**
	 * Returns the call logger to use for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#callLogger(Class)}
	 * 	<li class='jm'>{@link RestContextBuilder#callLogger(RestLogger)}
	 * </ul>
	 *
	 * @return
	 * 	The call logger to use for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RestLogger getCallLogger() {
		return callLogger;
	}

	/**
	 * Returns the resource bundle used by this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link #REST_messages}
	 * </ul>
	 *
	 * @return
	 * 	The resource bundle for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Messages getMessages() {
		return messages;
	}

	/**
	 * Returns the Swagger provider used by this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#swaggerProvider(Class)}
	 * 	<li class='jm'>{@link RestContextBuilder#swaggerProvider(SwaggerProvider)}
	 * </ul>
	 *
	 * @return
	 * 	The information provider for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public SwaggerProvider getSwaggerProvider() {
		return swaggerProvider;
	}

	/**
	 * Returns the resource object.
	 *
	 * <p>
	 * This is the instance of the class annotated with the {@link Rest @Rest} annotation, usually
	 * an instance of {@link RestServlet}.
	 *
	 * @return
	 * 	The resource object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Object getResource() {
		return resource.get();
	}

	/**
	 * Returns the servlet init parameter returned by {@link ServletConfig#getInitParameter(String)}.
	 *
	 * @param name The init parameter name.
	 * @return The servlet init parameter, or <jk>null</jk> if not found.
	 */
	public String getServletInitParameter(String name) {
		return builder.getInitParameter(name);
	}

	/**
	 * Returns the child resources associated with this servlet.
	 *
	 * @return
	 * 	An unmodifiable map of child resources.
	 * 	Keys are the {@link Rest#path() @Rest(path)} annotation defined on the child resource.
	 */
	public Map<String,RestContext> getChildResources() {
		return restChildren.asMap();
	}

	/**
	 * Returns whether it's safe to render stack traces in HTTP responses.
	 *
	 * @return <jk>true</jk> if setting is enabled.
	 */
	public boolean isRenderResponseStackTraces() {
		return renderResponseStackTraces;
	}

	/**
	 * Returns whether it's safe to pass the HTTP body as a <js>"body"</js> GET parameter.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContextBuilder#disableBodyParam()}
	 * </ul>
	 *
	 * @return <jk>true</jk> if setting is enabled.
	 */
	public boolean isAllowBodyParam() {
		return allowBodyParam;
	}

	/**
	 * Allowed header URL parameters.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Rest#allowedHeaderParams}
	 * 	<li class='jm'>{@link RestContextBuilder#allowedHeaderParams(String)}
	 * </ul>
	 *
	 * @return
	 * 	The header names allowed to be passed as URL parameters.
	 * 	<br>The set is case-insensitive ordered and unmodifiable.
	 */
	public Set<String> getAllowedHeaderParams() {
		return allowedHeaderParams;
	}

	/**
	 * Allowed method headers.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Rest#allowedMethodHeaders}
	 * 	<li class='jm'>{@link RestContextBuilder#allowedMethodHeaders(String)}
	 * </ul>
	 *
	 * @return
	 * 	The method names allowed to be passed as <c>X-Method</c> headers.
	 * 	<br>The set is case-insensitive ordered and unmodifiable.
	 */
	public Set<String> getAllowedMethodHeaders() {
		return allowedMethodHeaders;
	}

	/**
	 * Allowed method URL parameters.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Rest#allowedMethodParams}
	 * 	<li class='jm'>{@link RestContextBuilder#allowedMethodParams(String)}
	 * </ul>
	 *
	 * @return
	 * 	The method names allowed to be passed as <c>method</c> URL parameters.
	 * 	<br>The set is case-insensitive ordered and unmodifiable.
	 */
	public Set<String> getAllowedMethodParams() {
		return allowedMethodParams;
	}

	/**
	 * Returns the name of the client version header name used by this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Rest#clientVersionHeader}
	 * 	<li class='jm'>{@link RestContextBuilder#clientVersionHeader(String)}
	 * </ul>
	 *
	 * @return
	 * 	The name of the client version header used by this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public String getClientVersionHeader() {
		return clientVersionHeader;
	}

	/**
	 * Returns the file finder associated with this context.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_fileFinder}
	 * </ul>
	 *
	 * @return
	 * 	The file finder for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public FileFinder getFileFinder() {
		return fileFinder;
	}

	/**
	 * Returns the static files associated with this context.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 *
	 * @return
	 * 	The static files for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public StaticFiles getStaticFiles() {
		return staticFiles;
	}

	/**
	 * Returns the logger associated with this context.
	 *
	 * @return
	 * 	The logger for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Returns the stack trace database associated with this context.
	 *
	 * @return
	 * 	The stack trace database for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public ThrownStore getThrownStore() {
		return thrownStore;
	}

	/**
	 * Returns the HTTP-part parser associated with this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @return
	 * 	The HTTP-part parser associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartParser getPartParser() {
		return partParser;
	}

	/**
	 * Returns the HTTP-part serializer associated with this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partSerializer}
	 * </ul>
	 *
	 * @return
	 * 	The HTTP-part serializer associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartSerializer getPartSerializer() {
		return partSerializer;
	}

	/**
	 * Returns the JSON-Schema generator associated with this resource.
	 *
	 * @return
	 * 	The HTTP-part serializer associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public JsonSchemaGenerator getJsonSchemaGenerator() {
		return jsonSchemaGenerator;
	}

	/**
	 * Returns the explicit list of supported accept types for this resource.
	 *
	 * <p>
	 * Consists of the media types for production common to all operations on this class.
	 *
	 * <p>
	 * Can be overridden by {@link RestContextBuilder#produces(MediaType...)}.
	 *
	 * @return
	 * 	An unmodifiable list of supported <c>Accept</c> header values for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public List<MediaType> getProduces() {
		return produces;
	}

	/**
	 * Returns the explicit list of supported content types for this resource.
	 *
	 * <p>
	 * Consists of the media types for consumption common to all operations on this class.
	 *
	 * <p>
	 * Can be overridden by {@link RestContextBuilder#consumes(MediaType...)}.
	 *
	 * @return
	 * 	An unmodifiable list of supported <c>Content-Type</c> header values for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public List<MediaType> getConsumes() {
		return consumes;
	}

	/**
	 * Returns the default request headers for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#defaultRequestHeaders(org.apache.http.Header...)}
	 * </ul>
	 *
	 * @return
	 * 	The default request headers for this resource in an unmodifiable list.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HeaderList getDefaultRequestHeaders() {
		return defaultRequestHeaders;
	}

	/**
	 * Returns the default request attributes for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#defaultRequestAttributes(NamedAttribute...)}
	 * </ul>
	 *
	 * @return
	 * 	The default request headers for this resource in an unmodifiable list.
	 * 	<br>Never <jk>null</jk>.
	 */
	public NamedAttributeList getDefaultRequestAttributes() {
		return defaultRequestAttributes;
	}

	/**
	 * Returns the default response headers for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#defaultResponseHeaders(org.apache.http.Header...)}
	 * </ul>
	 *
	 * @return
	 * 	The default response headers for this resource in an unmodifiable list.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HeaderList getDefaultResponseHeaders() {
		return defaultResponseHeaders;
	}

	/**
	 * Returns the authority path of the resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#uriAuthority(String)}
	 * </ul>
	 *
	 * @return
	 * 	The authority path of this resource.
	 * 	<br>If not specified, returns the context path of the ascendant resource.
	 */
	public String getUriAuthority() {
		if (uriAuthority != null)
			return uriAuthority;
		if (parentContext != null)
			return parentContext.getUriAuthority();
		return null;
	}

	/**
	 * Returns the context path of the resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#uriContext(String)}
	 * </ul>
	 *
	 * @return
	 * 	The context path of this resource.
	 * 	<br>If not specified, returns the context path of the ascendant resource.
	 */
	public String getUriContext() {
		if (uriContext != null)
			return uriContext;
		if (parentContext != null)
			return parentContext.getUriContext();
		return null;
	}

	/**
	 * Returns the setting on how relative URIs should be interpreted as relative to.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#uriRelativity(UriRelativity)}
	 * </ul>
	 *
	 * @return
	 * 	The URI-resolution relativity setting value.
	 * 	<br>Never <jk>null</jk>.
	 */
	public UriRelativity getUriRelativity() {
		return uriRelativity;
	}

	/**
	 * Returns the setting on how relative URIs should be resolved.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#uriResolution(UriResolution)}
	 * </ul>
	 *
	 * @return
	 * 	The URI-resolution setting value.
	 * 	<br>Never <jk>null</jk>.
	 */
	public UriResolution getUriResolution() {
		return uriResolution;
	}

	/**
	 * Returns the REST Java methods defined in this resource.
	 *
	 * <p>
	 * These are the methods annotated with the {@link RestOp @RestOp} annotation.
	 *
	 * @return
	 * 	An unmodifiable map of Java method names to call method objects.
	 */
	public List<RestOpContext> getOpContexts() {
		return restOperations.getOpContexts();
	}

	/**
	 * Returns timing information on all method executions on this class.
	 *
	 * <p>
	 * Timing information is maintained for any <ja>@RestResource</ja>-annotated and hook methods.
	 *
	 * @return A list of timing statistics ordered by average execution time descending.
	 */
	public List<MethodExecStats> getMethodExecStats() {
		return methodExecStore.getStats().stream().sorted(Comparator.comparingLong(MethodExecStats::getTotalTime).reversed()).collect(Collectors.toList());
	}

	/**
	 * Gives access to the internal stack trace database.
	 *
	 * @return The stack trace database.
	 */
	public RestContextStats getStats() {
		return new RestContextStats(startTime, getMethodExecStats());
	}

	/**
	 * Returns the resource class type.
	 *
	 * @return The resource class type.
	 */
	public Class<?> getResourceClass() {
		return resourceClass;
	}

	/**
	 * Returns the builder that created this context.
	 *
	 * @return The builder that created this context.
	 */
	public ServletConfig getBuilder() {
		return builder;
	}

	/**
	 * Returns the path matcher for this context.
	 *
	 * @return The path matcher for this context.
	 */
	public UrlPathMatcher getPathMatcher() {
		return pathMatcher;
	}

	/**
	 * Returns the root bean store for this context.
	 *
	 * @return The root bean store for this context.
	 */
	public BeanStore getRootBeanStore() {
		return rootBeanStore;
	}

	/**
	 * Returns the swagger for the REST resource.
	 *
	 * @param locale The locale of the swagger to return.
	 * @return The swagger as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Swagger> getSwagger(Locale locale) {
		Swagger s = swaggerCache.get(locale);
		if (s == null) {
			try {
				s = swaggerProvider.getSwagger(this, locale);
				if (s != null)
					swaggerCache.put(locale, s);
			} catch (Exception e) {
				throw toHttpException(e, InternalServerError.class);
			}
		}
		return Optional.ofNullable(s);
	}

	/**
	 * Returns the timing information returned by {@link #getMethodExecStats()} in a readable format.
	 *
	 * @return A report of all method execution times ordered by .
	 */
	public String getMethodExecStatsReport() {
		StringBuilder sb = new StringBuilder()
			.append(" Method                         Runs      Running   Errors   Avg          Total     \n")
			.append("------------------------------ --------- --------- -------- ------------ -----------\n");
		getMethodExecStats()
			.stream()
			.sorted(Comparator.comparingDouble(MethodExecStats::getTotalTime).reversed())
			.forEach(x -> sb.append(String.format("%30s %9d %9d %9d %10dms %10dms\n", x.getMethod(), x.getRuns(), x.getRunning(), x.getErrors(), x.getAvgTime(), x.getTotalTime())));
		return sb.toString();
	}

	/**
	 * Finds the {@link RestOpArg} instances to handle resolving objects on the calls to the specified Java method.
	 *
	 * @param m The Java method being called.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The array of resolvers.
	 */
	protected RestOpArg[] findRestOperationArgs(Method m, BeanStore beanStore) {

		MethodInfo mi = MethodInfo.of(m);
		List<ClassInfo> pt = mi.getParamTypes();
		RestOpArg[] ra = new RestOpArg[pt.size()];

		beanStore = BeanStore.of(beanStore, getResource());

		for (int i = 0; i < pt.size(); i++) {
			ParamInfo pi = mi.getParam(i);
			beanStore.addBean(ParamInfo.class, pi);
			for (Class<? extends RestOpArg> c : opArgs) {
				try {
					ra[i] = beanStore.createBean(c);
					if (ra[i] != null)
						break;
				} catch (ExecutableException e) {
					throw new InternalServerError(e.unwrap(), "Could not resolve parameter {0} on method {1}.", i, mi.getFullName());
				}
			}
			if (ra[i] == null)
				throw new InternalServerError("Could not resolve parameter {0} on method {1}.", i, mi.getFullName());
		}

		return ra;
	}

	/**
	 * Finds the {@link RestOpArg} instances to handle resolving objects on pre-call and post-call Java methods.
	 *
	 * @param m The Java method being called.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link #createBeanStore(Object,ContextProperties,RestContext)}.
	 * @return The array of resolvers.
	 */
	protected RestOpArg[] findHookMethodArgs(Method m, BeanStore beanStore) {
		MethodInfo mi = MethodInfo.of(m);
		List<ClassInfo> pt = mi.getParamTypes();
		RestOpArg[] ra = new RestOpArg[pt.size()];

		beanStore = BeanStore.of(beanStore, getResource());

		for (int i = 0; i < pt.size(); i++) {
			ParamInfo pi = mi.getParam(i);
			beanStore.addBean(ParamInfo.class, pi);
			for (Class<? extends RestOpArg> c : hookMethodArgs) {
				try {
					ra[i] = beanStore.createBean(c);
					if (ra[i] != null)
						break;
				} catch (ExecutableException e) {
					throw new InternalServerError(e.unwrap(), "Could not resolve parameter {0} on method {1}.", i, mi.getFullName());
				}
			}
			if (ra[i] == null)
				throw new InternalServerError("Could not resolve parameter {0} on method {1}.", i, mi.getFullName());
		}

		return ra;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Call handling
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Wraps an incoming servlet request/response pair into a single {@link RestCall} object.
	 *
	 * <p>
	 * This is the first method called by {@link #execute(Object, HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param req The rest request.
	 * @param res The rest response.
	 * @return The wrapped request/response pair.
	 */
	protected RestCall createCall(Object resource, HttpServletRequest req, HttpServletResponse res) {
		return new RestCall(resource, this, req, res).logger(getCallLogger());
	}

	/**
	 * Creates a {@link RestRequest} object based on the specified incoming {@link HttpServletRequest} object.
	 *
	 * <p>
	 * This method is called immediately after {@link #startCall(RestCall)} has been called.
	 *
	 * @param call The current REST call.
	 * @return The wrapped request object.
	 * @throws Exception If any errors occur trying to interpret the request.
	 */
	public RestRequest createRequest(RestCall call) throws Exception {
		return new RestRequest(call);
	}

	/**
	 * Creates a {@link RestResponse} object based on the specified incoming {@link HttpServletResponse} object
	 * and the request returned by {@link #createRequest(RestCall)}.
	 *
	 * @param call The current REST call.
	 * @return The wrapped response object.
	 * @throws Exception If any errors occur trying to interpret the request or response.
	 */
	public RestResponse createResponse(RestCall call) throws Exception {
		return new RestResponse(call);
	}

	/**
	 * The main service method.
	 *
	 * <p>
	 * Subclasses can optionally override this method if they want to tailor the behavior of requests.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * 	<br>Note that this bean may not be the same bean used during initialization as it may have been replaced at runtime.
	 * @param r1 The incoming HTTP servlet request object.
	 * @param r2 The incoming HTTP servlet response object.
	 * @throws ServletException General servlet exception.
	 * @throws IOException Thrown by underlying stream.
	 */
	public void execute(Object resource, HttpServletRequest r1, HttpServletResponse r2) throws ServletException, IOException {

		RestCall call = createCall(resource, r1, r2);

		// Must be careful not to bleed thread-locals.
		if (this.call.get() != null)
			System.err.println("WARNING:  Thread-local call object was not cleaned up from previous request.  " + this + ", thread=["+Thread.currentThread().getId()+"]");
		this.call.set(call);

		try {

			if (initException != null)
				throw initException;

			// If the resource path contains variables (e.g. @Rest(path="/f/{a}/{b}"), then we want to resolve
			// those variables and push the servletPath to include the resolved variables.  The new pathInfo will be
			// the remainder after the new servletPath.
			// Only do this for the top-level resource because the logic for child resources are processed next.
			if (pathMatcher.hasVars() && parentContext == null) {
				String sp = call.getServletPath();
				String pi = call.getPathInfoUndecoded();
				UrlPath upi2 = UrlPath.of(pi == null ? sp : sp + pi);
				UrlPathMatch uppm = pathMatcher.match(upi2);
				if (uppm != null && ! uppm.hasEmptyVars()) {
					call.pathVars(uppm.getVars());
					call.request(
						new OverrideableHttpServletRequest(call.getRequest())
							.pathInfo(nullIfEmpty(urlDecode(uppm.getSuffix())))
							.servletPath(uppm.getPrefix())
					);
				} else {
					call.debug(isDebug(call)).status(SC_NOT_FOUND).finish();
					return;
				}
			}

			// If this resource has child resources, try to recursively call them.
			Optional<RestChildMatch> childMatch = restChildren.findMatch(call);
			if (childMatch.isPresent()) {
				UrlPathMatch uppm = childMatch.get().getPathMatch();
				RestContext rc = childMatch.get().getChildContext();
				if (! uppm.hasEmptyVars()) {
					call.pathVars(uppm.getVars());
					HttpServletRequest childRequest = new OverrideableHttpServletRequest(call.getRequest())
						.pathInfo(nullIfEmpty(urlDecode(uppm.getSuffix())))
						.servletPath(call.getServletPath() + uppm.getPrefix());
					rc.execute(rc.getResource(), childRequest, call.getResponse());
				} else {
					call.debug(isDebug(call)).status(SC_NOT_FOUND).finish();
				}
				return;
			}

			call.debug(isDebug(call));

			startCall(call);

			// If the specified method has been defined in a subclass, invoke it.
			try {
				restOperations.findOperation(call).invoke(call);
			} catch (NotFound e) {
				if (call.getStatus() == 0)
					call.status(404);
				call.exception(e);
				handleNotFound(call);
			}

			if (call.hasOutput()) {
				// Now serialize the output if there was any.
				// Some subclasses may write to the OutputStream or Writer directly.
				processResponse(call);
			}


		} catch (Throwable e) {
			handleError(call, convertThrowable(e));
		} finally {
			clearState();
		}

		call.finish();
		finishCall(call);
	}

	private boolean isDebug(RestCall call) {
		return debugEnablement.isDebug(this, call.getRequest());
	}

	/**
	 * Returns the debug enablement bean for this context.
	 *
	 * @return The debug enablement bean for this context.
	 */
	public DebugEnablement getDebugEnablement() {
		return debugEnablement;
	}

	/**
	 * The main method for serializing POJOs passed in through the {@link RestResponse#setOutput(Object)} method or
	 * returned by the Java method.
	 *
	 * <p>
	 * Subclasses may override this method if they wish to modify the way the output is rendered or support other output
	 * formats.
	 *
	 * <p>
	 * The default implementation simply iterates through the response handlers on this resource
	 * looking for the first one whose {@link ResponseProcessor#process(RestCall)} method returns
	 * <jk>true</jk>.
	 *
	 * @param call The HTTP call.
	 * @throws IOException Thrown by underlying stream.
	 * @throws BasicHttpException Non-200 response.
	 * @throws NotImplemented No registered response processors could handle the call.
	 */
	public void processResponse(RestCall call) throws IOException, BasicHttpException, NotImplemented {

		// Loop until we find the correct processor for the POJO.
		int loops = 5;
		for (int i = 0; i < responseProcessors.length; i++) {
			int j = responseProcessors[i].process(call);
			if (j == FINISHED)
				return;
			if (j == RESTART) {
				if (loops-- < 0)
					throw new InternalServerError("Too many processing loops.");
				i = -1;  // Start over.
			}
		}

		Object output = call.getRestResponse().getOutput().get().orElse(null);
		throw new NotImplemented("No response processors found to process output of type ''{0}''", className(output));
	}

	/**
	 * Method that can be subclassed to allow uncaught throwables to be treated as other types of throwables.
	 *
	 * <p>
	 * The default implementation looks at the throwable class name to determine whether it can be converted to another type:
	 *
	 * <ul>
	 * 	<li><js>"*AccessDenied*"</js> - Converted to {@link Unauthorized}.
	 * 	<li><js>"*Empty*"</js>,<js>"*NotFound*"</js> - Converted to {@link NotFound}.
	 * </ul>
	 *
	 * @param t The thrown object.
	 * @return The converted thrown object.
	 */
	public Throwable convertThrowable(Throwable t) {

		ClassInfo ci = ClassInfo.ofc(t);
		if (ci.is(InvocationTargetException.class)) {
			t = ((InvocationTargetException)t).getTargetException();
			ci = ClassInfo.ofc(t);
		}

		if (ci.is(HttpRuntimeException.class)) {
			t = ((HttpRuntimeException)t).getInner();
			ci = ClassInfo.ofc(t);
		}

		if (ci.hasAnnotation(Response.class))
			return t;

		if (t instanceof ParseException || t instanceof InvalidDataConversionException)
			return new BadRequest(t);

		String n = className(t);

		if (n.contains("AccessDenied") || n.contains("Unauthorized"))
			return new Unauthorized(t);

		if (n.contains("Empty") || n.contains("NotFound"))
			return new NotFound(t);

		return t;
	}

	/**
	 * Handle the case where a matching method was not found.
	 *
	 * <p>
	 * Subclasses can override this method to provide a 2nd-chance for specifying a response.
	 * The default implementation will simply throw an exception with an appropriate message.
	 *
	 * @param call The HTTP call.
	 * @throws Exception Any exception can be thrown.
	 */
	public void handleNotFound(RestCall call) throws Exception {
		String pathInfo = call.getPathInfo();
		String methodUC = call.getMethod();
		int rc = call.getStatus();
		String onPath = pathInfo == null ? " on no pathInfo"  : String.format(" on path '%s'", pathInfo);
		if (rc == SC_NOT_FOUND)
			throw new NotFound("Method ''{0}'' not found on resource with matching pattern{1}.", methodUC, onPath);
		else if (rc == SC_PRECONDITION_FAILED)
			throw new PreconditionFailed("Method ''{0}'' not found on resource{1} with matching matcher.", methodUC, onPath);
		else if (rc == SC_METHOD_NOT_ALLOWED)
			throw new MethodNotAllowed("Method ''{0}'' not found on resource{1}.", methodUC, onPath);
		else
			throw new ServletException("Invalid method response: " + rc, call.getException());
	}

	/**
	 * Method for handling response errors.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own custom error response handling.
	 *
	 * @param call The rest call.
	 * @param e The exception that occurred.
	 * @throws IOException Can be thrown if a problem occurred trying to write to the output stream.
	 */
	public synchronized void handleError(RestCall call, Throwable e) throws IOException {

		call.exception(e);

		if (call.isDebug())
			e.printStackTrace();

		int code = 500;

		ClassInfo ci = ClassInfo.ofc(e);
		Response r = ci.getLastAnnotation(Response.class);
		if (r != null)
			if (r.code().length > 0)
				code = r.code()[0];

		BasicHttpException e2 = (e instanceof BasicHttpException ? (BasicHttpException)e : BasicHttpException.create(BasicHttpException.class).causedBy(e).statusCode(code).build());

		HttpServletRequest req = call.getRequest();
		HttpServletResponse res = call.getResponse();

		Throwable t = null;
		if (e instanceof HttpRuntimeException)
			t = ((HttpRuntimeException)e).getInner();
		if (t == null)
			t = e2.getRootCause();
		if (t != null) {
			Thrown t2 = thrown(t);
			res.setHeader(t2.getName(), t2.getValue());
		}

		try {
			res.setContentType("text/plain");
			res.setHeader("Content-Encoding", "identity");
			int statusCode = e2.getStatusLine().getStatusCode();
			res.setStatus(statusCode);

			PrintWriter w = null;
			try {
				w = res.getWriter();
			} catch (IllegalStateException x) {
				w = new PrintWriter(new OutputStreamWriter(res.getOutputStream(), UTF8));
			}

			try (PrintWriter w2 = w) {
				String httpMessage = RestUtils.getHttpResponseText(statusCode);
				if (httpMessage != null)
					w2.append("HTTP ").append(String.valueOf(statusCode)).append(": ").append(httpMessage).append("\n\n");
				if (isRenderResponseStackTraces())
					e.printStackTrace(w2);
				else
					w2.append(e2.getFullStackMessage(true));
			}

		} catch (Exception e1) {
			req.setAttribute("Exception", e1);
		}
	}

	/**
	 * Called at the start of a request to invoke all {@link HookEvent#START_CALL} methods.
	 *
	 * @param call The current request.
	 * @throws BasicHttpException If thrown from call methods.
	 */
	protected void startCall(RestCall call) throws BasicHttpException {
		for (MethodInvoker x : startCallMethods) {
			try {
				x.invokeUsingFactory(call.getBeanStore(), call.getContext().getResource());
			} catch (ExecutableException e) {
				throw toHttpException(e.unwrap(), InternalServerError.class);
			}
		}
	}

	/**
	 * Called during a request to invoke all {@link HookEvent#PRE_CALL} methods.
	 *
	 * @param call The current request.
	 * @throws BasicHttpException If thrown from call methods.
	 */
	protected void preCall(RestCall call) throws BasicHttpException {
		for (RestOpInvoker m : preCallMethods)
			m.invokeFromCall(call, getResource());
	}

	/**
	 * Called during a request to invoke all {@link HookEvent#POST_CALL} methods.
	 *
	 * @param call The current request.
	 * @throws BasicHttpException If thrown from call methods.
	 */
	protected void postCall(RestCall call) throws BasicHttpException {
		for (RestOpInvoker m : postCallMethods)
			m.invokeFromCall(call, getResource());
	}

	/**
	 * Called at the end of a request to invoke all {@link HookEvent#END_CALL} methods.
	 *
	 * <p>
	 * This is the very last method called in {@link #execute(Object, HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param call The current request.
	 */
	protected void finishCall(RestCall call) {
		for (MethodInvoker x : endCallMethods) {
			try {
				x.invokeUsingFactory(call.getBeanStore(), call.getResource());
			} catch (ExecutableException e) {
				logger.log(Level.WARNING, e.unwrap(), ()->format("Error occurred invoking finish-call method ''{0}''.", x.getFullName()));
			}
		}
	}

	/**
	 * Called during servlet initialization to invoke all {@link HookEvent#POST_INIT} methods.
	 *
	 * @return This object (for method chaining).
	 * @throws ServletException Error occurred.
	 */
	public synchronized RestContext postInit() throws ServletException {
		if (initialized.get())
			return this;
		Object resource = getResource();
		MethodInfo mi = ClassInfo.of(getResource()).getMethod("setContext", RestContext.class);
		if (mi != null) {
			try {
				mi.accessible().invoke(resource, this);
			} catch (ExecutableException e) {
				throw new ServletException(e.unwrap());
			}
		}
		for (MethodInvoker x : postInitMethods) {
			try {
				x.invokeUsingFactory(beanStore, getResource());
			} catch (ExecutableException e) {
				throw new ServletException(e.unwrap());
			}
		}
		restChildren.postInit();
		return this;
	}

	/**
	 * Called during servlet initialization to invoke all {@link HookEvent#POST_INIT_CHILD_FIRST} methods.
	 *
	 * @return This object (for method chaining).
	 * @throws ServletException Error occurred.
	 */
	public RestContext postInitChildFirst() throws ServletException {
		if (initialized.get())
			return this;
		restChildren.postInitChildFirst();
		for (MethodInvoker x : postInitChildFirstMethods) {
			try {
				x.invokeUsingFactory(beanStore, getResource());
			} catch (ExecutableException e) {
				throw new ServletException(e.unwrap());
			}
		}
		initialized.set(true);
		return this;
	}

	/**
	 * Called during servlet destruction to invoke all {@link HookEvent#DESTROY} methods.
	 */
	protected void destroy() {
		for (MethodInvoker x : destroyMethods) {
			try {
				x.invokeUsingFactory(beanStore, getResource());
			} catch (ExecutableException e) {
				getLogger().log(Level.WARNING, e.unwrap(), ()->format("Error occurred invoking servlet-destroy method ''{0}''.", x.getFullName()));
			}
		}

		restChildren.destroy();
	}

	/**
	 * Returns the HTTP request object for the current request.
	 *
	 * @return The HTTP request object, or <jk>null</jk> if it hasn't been created.
	 */
	public RestRequest getRequest() {
		return getCall().getRestRequest();
	}

	/**
	 * Returns the HTTP response object for the current request.
	 *
	 * @return The HTTP response object, or <jk>null</jk> if it hasn't been created.
	 */
	public RestResponse getResponse() {
		return getCall().getRestResponse();
	}

	/**
	 * Returns the HTTP call for the current request.
	 *
	 * @return The HTTP call for the current request, never <jk>null</jk>?
	 * @throws InternalServerError If no active request exists on the current thread.
	 */
	public RestCall getCall() {
		RestCall rc = call.get();
		if (rc == null)
			throw new InternalServerError("No active request on current thread.");
		return rc;
	}

	/**
	 * If the specified object is annotated with {@link Response}, this returns the response metadata about that object.
	 *
	 * @param o The object to check.
	 * @return The response metadata, or <jk>null</jk> if it wasn't annotated with {@link Response}.
	 */
	public ResponseBeanMeta getResponseBeanMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponseBeanMeta rbm = responseBeanMetas.get(c);
		if (rbm == null) {
			rbm = ResponseBeanMeta.create(c, getContextProperties());
			if (rbm == null)
				rbm = ResponseBeanMeta.NULL;
			responseBeanMetas.put(c, rbm);
		}
		if (rbm == ResponseBeanMeta.NULL)
			return null;
		return rbm;
	}

	/**
	 * Clear any request state information on this context.
	 * This should always be called in a finally block in the RestServlet.
	 */
	void clearState() {
		call.remove();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"RestContext",
				OMap
					.create()
					.filtered()
					.a("allowBodyParam", allowBodyParam)
					.a("allowedMethodHeader", allowedMethodHeaders)
					.a("allowedMethodParams", allowedMethodParams)
					.a("allowedHeaderParams", allowedHeaderParams)
					.a("beanStore", beanStore)
					.a("clientVersionHeader", clientVersionHeader)
					.a("consumes", consumes)
					.a("defaultRequestHeaders", defaultRequestHeaders)
					.a("defaultResponseHeaders", defaultResponseHeaders)
					.a("fileFinder", fileFinder)
					.a("opArgs", opArgs)
					.a("partParser", partParser)
					.a("partSerializer", partSerializer)
					.a("produces", produces)
					.a("renderResponseStackTraces", renderResponseStackTraces)
					.a("responseProcessors", responseProcessors)
					.a("staticFiles", staticFiles)
					.a("swaggerProvider", swaggerProvider)
					.a("uriAuthority", uriAuthority)
					.a("uriContext", uriContext)
					.a("uriRelativity", uriRelativity)
					.a("uriResolution", uriResolution)
			);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers.
	//-----------------------------------------------------------------------------------------------------------------

}
