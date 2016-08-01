/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server;

import java.io.*;

import javax.servlet.http.*;

import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.response.*;

/**
 * Defines the interface for handlers that convert POJOs to appropriate HTTP responses.
 * <p>
 * The {@link RestServlet} API uses the concept of registered response handlers for
 * 	converting objects returned by REST methods or set through {@link RestResponse#setOutput(Object)}
 * 	into appropriate HTTP responses.
 * <p>
 * Response handlers can be associated with {@link RestServlet RestServlets} through the following ways:
 * <ul>
 * 	<li>Through the {@link RestResource#responseHandlers @RestResource.responseHandlers} annotation.
 * 	<li>By overriding {@link RestServlet#createResponseHandlers()} and augmenting or creating your
 * 		own list of handlers.
 * </ul>
 * <p>
 * By default, {@link RestServlet RestServlets} are registered with the following response handlers:
 * <ul>
 * 	<li>{@link DefaultHandler} - Serializes POJOs using the Juno serializer API.
 * 	<li>{@link ReaderHandler} - Pipes the output of {@link Reader Readers} to the response writer ({@link RestResponse#getWriter()}).
 * 	<li>{@link InputStreamHandler} - Pipes the output of {@link InputStream InputStreams} to the response output stream ({@link RestResponse#getOutputStream()}).
 * 	<li>{@link RedirectHandler} - Handles {@link Redirect} objects.
 * </ul>
 * <p>
 * Response handlers can be used to process POJOs that cannot normally be handled through Juno serializers, or
 * 	because it's simply easier to define response handlers for special cases.
 * <p>
 * The following example shows how to create a response handler to handle special <code>Foo</code> objects outside the normal
 * 	Juno architecture.
 * <p class='bcode'>
 * 	<ja>@RestResource</ja>(
 * 		path=<js>"/example"</js>,
 * 		responseHandlers=FooHandler.<jk>class</jk>
 * 	)
 * 	<jk>public class</jk> Example <jk>extends</jk> RestServlet {
 *
 * 		<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/"</js>)
 * 		<jk>public</jk> Foo test1() {
 * 			<jk>return new</jk> Foo(<js>"123"</js>);
 * 		}
 *
 * 		<jk>public static class</jk> FooHandler <jk>implements</jk> ResponseHandler {
 * 			<ja>@Override</ja>
 * 			<jk>public boolean</jk> handle(RestRequest req, RestResponse res, Object output) <jk>throws</jk> IOException, RestException {
 * 				<jk>if</jk> (output <jk>instanceof</jk> Foo) {
 * 					Foo foo = (Foo)output;
 * 					<jc>// Set some headers and body content.</jc>
 * 					res.setHeader(<js>"Foo-ID"</js>, foo.getId());
 * 					res.getWriter().write(<js>"foo.id="</js> + foo.getId());
 * 					<jk>return true</jk>;  <jc>// We handled it.</jc>
 * 				}
 * 				<jk>return false</jk>;  <jc>// We didn't handle it.</jc>
 * 			}
 * 		}
 * 	}
 * </p>
 */
public interface ResponseHandler {

	/**
	 * Process this response if possible.
	 * This method should return <jk>false</jk> if it wasn't able to process the response.
	 *
	 * @param req The HTTP servlet request.
	 * @param res The HTTP servlet response;
	 * @param output The POJO returned by the REST method that now needs to be sent to the response.
	 * @return true If this handler handled the response.
	 * @throws IOException - If low-level exception occurred on output stream.  Results in a {@link HttpServletResponse#SC_INTERNAL_SERVER_ERROR} error.
	 * @throws RestException - If some other exception occurred.  Can be used to provide an appropriate HTTP response code and message.
	 */
	boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException;
}
