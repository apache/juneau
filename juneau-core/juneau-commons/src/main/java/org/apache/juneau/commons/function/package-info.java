/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Functional programming utilities including enhanced function interfaces, consumers, suppliers,
 * tuples, exception-handling variants, and large-dataset streaming APIs.
 *
 * <a id="BeanStreaming"></a>
 * <h2 class='topic'>Large-Dataset Streaming</h2>
 * <div class='topic'>
 *
 * 	<p>
 * 	The following interfaces enable serialization and parsing of large datasets without loading
 * 	all elements into memory. They integrate directly into the Juneau marshalling framework so
 * 	any serializer or parser works transparently.
 * 	</p>
 *
 * 	<ul class='javatree'>
 * 		<li class='jic'>{@link org.apache.juneau.commons.function.BeanSupplier} - Serialize-side lifecycle interface; extends {@link java.lang.Iterable}
 * 		<li class='jic'>{@link org.apache.juneau.commons.function.BeanConsumer} - Parse-side lifecycle interface; extends {@link org.apache.juneau.commons.function.ThrowingConsumer}
 * 		<li class='jic'>{@link org.apache.juneau.commons.function.BeanChannel} - Round-trip interface; extends both {@link org.apache.juneau.commons.function.BeanSupplier} and {@link org.apache.juneau.commons.function.BeanConsumer}
 * 		<li class='jc'>{@link org.apache.juneau.commons.function.ListBeanChannel} - Built-in in-memory {@link org.apache.juneau.commons.function.BeanChannel} backed by an {@link java.util.ArrayList}
 * 		<li class='jic'>{@link org.apache.juneau.commons.function.BeanFactory} - Universal factory interface for DI-framework-managed instantiation
 * 	</ul>
 *
 * 	<!-- ============================================================================================================ -->
 * 	<a id="BeanStreaming.Lifecycle"></a>
 * 	<h3 class='topic'>Lifecycle</h3>
 * 	<div class='topic'>
 *
 * 	<p>
 * 	Both interfaces follow the same three-phase lifecycle driven by the framework:
 * 	</p>
 *
 * 	<table class='styled'>
 * 		<tr><th>Phase</th><th>BeanSupplier (serialization)</th><th>BeanConsumer (parsing)</th></tr>
 * 		<tr><td><b>Setup</b></td><td>{@link org.apache.juneau.commons.function.BeanSupplier#begin()} — open cursor, execute query</td><td>{@link org.apache.juneau.commons.function.BeanConsumer#begin()} — open connection, prepare statement</td></tr>
 * 		<tr><td><b>Transfer</b></td><td>{@link org.apache.juneau.commons.function.BeanSupplier#iterator()} — yield one bean per call</td><td>{@link org.apache.juneau.commons.function.BeanConsumer#acceptThrows(Object)} — receive one bean per call</td></tr>
 * 		<tr><td><b>Error</b></td><td>{@link org.apache.juneau.commons.function.BeanSupplier#onError(Exception)} — rollback / log; rethrow to stop</td><td>{@link org.apache.juneau.commons.function.BeanConsumer#onError(Exception)} — rollback / log; absorb to skip-and-continue</td></tr>
 * 		<tr><td><b>Cleanup</b></td><td>{@link org.apache.juneau.commons.function.BeanSupplier#complete()} — close cursor, connection</td><td>{@link org.apache.juneau.commons.function.BeanConsumer#complete()} — final commit, close statement</td></tr>
 * 	</table>
 *
 * 	<p>
 * 	{@code complete()} is always called — even when {@code onError()} rethrows — so it is safe to
 * 	use for resource cleanup in all cases.
 * 	</p>
 *
 * 	</div>
 *
 * 	<!-- ============================================================================================================ -->
 * 	<a id="BeanStreaming.Supplier"></a>
 * 	<h3 class='topic'>BeanSupplier — Serializing from a database cursor</h3>
 * 	<div class='topic'>
 *
 * 	<p>
 * 	The following example streams a large {@code Employee} table directly from the database to the
 * 	HTTP response as a JSON array, without loading any rows into memory. A Spring-injected
 * 	{@code DataSource} is supplied via a factory registered with {@link org.apache.juneau.commons.function.BeanFactory}.
 * 	</p>
 *
 * 	<h5 class='figure'>Bean class with factory annotation</h5>
 * 	<p class='bjava'>
 * 	<jk>package</jk> com.example;
 *
 * 	<ja>@Bean</ja>(factory=EmployeeSupplier.Factory.<jk>class</jk>)
 * 	<jk>public class</jk> EmployeeSupplier <jk>implements</jk> BeanSupplier&lt;Employee&gt; {
 *
 * 		<jk>private final</jk> DataSource <jv>ds</jv>;
 * 		<jk>private</jk> Connection <jv>conn</jv>;
 * 		<jk>private</jk> ResultSet <jv>rs</jv>;
 *
 * 		<jk>public</jk> EmployeeSupplier(DataSource <jv>ds</jv>) {
 * 			<jk>this</jk>.<jv>ds</jv> = <jv>ds</jv>;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> begin() <jk>throws</jk> Exception {
 * 			<jv>conn</jv> = <jv>ds</jv>.getConnection();
 * 			<jk>var</jk> <jv>stmt</jv> = <jv>conn</jv>.prepareStatement(
 * 				<js>"SELECT id, name, department FROM employee ORDER BY id"</js>);
 * 			<jv>rs</jv> = <jv>stmt</jv>.executeQuery();
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Iterator&lt;Employee&gt; iterator() {
 * 			<jk>return new</jk> Iterator&lt;&gt;() {
 * 				<ja>@Override</ja> <jk>public boolean</jk> hasNext() { <jk>return</jk> ResultSetIterator.hasNext(<jv>rs</jv>); }
 * 				<ja>@Override</ja> <jk>public</jk> Employee next() { <jk>return</jk> Employee.fromRow(<jv>rs</jv>); }
 * 			};
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> onError(Exception <jv>e</jv>) <jk>throws</jk> Exception {
 * 			<jk>throw</jk> <jv>e</jv>; <jc>// propagate; complete() will still close the cursor</jc>
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> complete() <jk>throws</jk> Exception {
 * 			<jk>if</jk> (<jv>rs</jv> != <jk>null</jk>) <jv>rs</jv>.close();
 * 			<jk>if</jk> (<jv>conn</jv> != <jk>null</jk>) <jv>conn</jv>.close();
 * 		}
 *
 * 		<jc>// Factory retrieved from the Spring ApplicationContext via BeanStore.</jc>
 * 		<jk>public static class</jk> Factory <jk>implements</jk> BeanFactory&lt;EmployeeSupplier&gt; {
 *
 * 			<jk>private final</jk> DataSource <jv>ds</jv>;
 *
 * 			<jk>public</jk> Factory(DataSource <jv>ds</jv>) { <jk>this</jk>.<jv>ds</jv> = <jv>ds</jv>; }
 *
 * 			<ja>@Override</ja>
 * 			<jk>public</jk> EmployeeSupplier create() {
 * 				<jk>return new</jk> EmployeeSupplier(<jv>ds</jv>);
 * 			}
 * 		}
 * 	}
 * 	</p>
 *
 * 	<h5 class='figure'>Spring REST endpoint</h5>
 * 	<p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> EmployeeResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<ja>@Inject</ja>
 * 		<jk>private</jk> EmployeeSupplier.Factory <jv>supplierFactory</jv>;
 *
 * 		<ja>@RestGet</ja>(<js>"/employees"</js>)
 * 		<jk>public</jk> EmployeeSupplier getEmployees() {
 * 			<jk>return</jk> <jv>supplierFactory</jv>.create(); <jc>// framework calls begin(), iterates, calls complete()</jc>
 * 		}
 * 	}
 * 	</p>
 *
 * 	<p>
 * 	The serializer calls {@code begin()} before iterating, serializes each {@code Employee} bean
 * 	as it arrives from the cursor, and always calls {@code complete()} afterward to close the
 * 	cursor and connection — regardless of whether serialization succeeded or failed.
 * 	</p>
 *
 * 	</div>
 *
 * 	<!-- ============================================================================================================ -->
 * 	<a id="BeanStreaming.Consumer"></a>
 * 	<h3 class='topic'>BeanConsumer — Parsing into a database table</h3>
 * 	<div class='topic'>
 *
 * 	<p>
 * 	The following example accepts a large JSON array of {@code Employee} beans in an HTTP request
 * 	body and bulk-inserts them into the database via JDBC, committing every 500 rows. On error
 * 	it rolls back and rethrows; on completion it performs a final commit and closes resources.
 * 	</p>
 *
 * 	<h5 class='figure'>BeanConsumer with batch commits</h5>
 * 	<p class='bjava'>
 * 	<ja>@Bean</ja>(factory=EmployeeConsumer.Factory.<jk>class</jk>)
 * 	<jk>public class</jk> EmployeeConsumer <jk>implements</jk> BeanConsumer&lt;Employee&gt; {
 *
 * 		<jk>private static final int</jk> <jsf>BATCH_SIZE</jsf> = 500;
 *
 * 		<jk>private final</jk> DataSource <jv>ds</jv>;
 * 		<jk>private</jk> Connection <jv>conn</jv>;
 * 		<jk>private</jk> PreparedStatement <jv>stmt</jv>;
 * 		<jk>private int</jk> <jv>count</jv>;
 *
 * 		<jk>public</jk> EmployeeConsumer(DataSource <jv>ds</jv>) {
 * 			<jk>this</jk>.<jv>ds</jv> = <jv>ds</jv>;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> begin() <jk>throws</jk> Exception {
 * 			<jv>conn</jv> = <jv>ds</jv>.getConnection();
 * 			<jv>conn</jv>.setAutoCommit(<jk>false</jk>);
 * 			<jv>stmt</jv> = <jv>conn</jv>.prepareStatement(
 * 				<js>"INSERT INTO employee (name, department) VALUES (?, ?)"</js>);
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> acceptThrows(Employee <jv>emp</jv>) <jk>throws</jk> Exception {
 * 			<jv>stmt</jv>.setString(1, <jv>emp</jv>.getName());
 * 			<jv>stmt</jv>.setString(2, <jv>emp</jv>.getDepartment());
 * 			<jv>stmt</jv>.executeUpdate();
 * 			<jk>if</jk> (++<jv>count</jv> % <jsf>BATCH_SIZE</jsf> == 0)
 * 				<jv>conn</jv>.commit(); <jc>// periodic batch commit</jc>
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> onError(Exception <jv>e</jv>) <jk>throws</jk> Exception {
 * 			<jv>conn</jv>.rollback();
 * 			<jk>throw</jk> <jv>e</jv>; <jc>// stop parsing; complete() will still close resources</jc>
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> complete() <jk>throws</jk> Exception {
 * 			<jv>conn</jv>.commit(); <jc>// final commit for the last partial batch</jc>
 * 			<jv>stmt</jv>.close();
 * 			<jv>conn</jv>.close();
 * 		}
 *
 * 		<jk>public static class</jk> Factory <jk>implements</jk> BeanFactory&lt;EmployeeConsumer&gt; {
 *
 * 			<jk>private final</jk> DataSource <jv>ds</jv>;
 *
 * 			<jk>public</jk> Factory(DataSource <jv>ds</jv>) { <jk>this</jk>.<jv>ds</jv> = <jv>ds</jv>; }
 *
 * 			<ja>@Override</ja>
 * 			<jk>public</jk> EmployeeConsumer create() {
 * 				<jk>return new</jk> EmployeeConsumer(<jv>ds</jv>);
 * 			}
 * 		}
 * 	}
 * 	</p>
 *
 * 	<h5 class='figure'>Parsing via parseToBeanConsumer</h5>
 * 	<p class='bjava'>
 * 	<jc>// Direct API usage — framework calls begin(), acceptThrows() per element, complete().</jc>
 * 	<jk>var</jk> <jv>consumer</jv> = <jv>consumerFactory</jv>.create();
 * 	JsonParser.<jsf>DEFAULT</jsf>.getSession().parseToBeanConsumer(<jv>inputStream</jv>, <jv>consumer</jv>, Employee.<jk>class</jk>);
 * 	</p>
 *
 * 	<h5 class='figure'>Spring REST endpoint</h5>
 * 	<p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> EmployeeResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<ja>@Inject</ja>
 * 		<jk>private</jk> EmployeeConsumer.Factory <jv>consumerFactory</jv>;
 *
 * 		<ja>@RestPost</ja>(<js>"/employees/bulk"</js>)
 * 		<jk>public void</jk> importEmployees(RestRequest <jv>req</jv>) <jk>throws</jk> Exception {
 * 			<jk>var</jk> <jv>consumer</jv> = <jv>consumerFactory</jv>.create();
 * 			<jv>req</jv>.getBody().parseToBeanConsumer(<jv>consumer</jv>, Employee.<jk>class</jk>);
 * 		}
 * 	}
 * 	</p>
 *
 * 	<p>
 * 	The {@code onError()} default rethrows, stopping parsing immediately. Override it to absorb
 * 	the exception (log-and-skip) for fault-tolerant ingestion of partially-invalid input:
 * 	</p>
 *
 * 	<h5 class='figure'>Fault-tolerant ingestion (skip bad records)</h5>
 * 	<p class='bjava'>
 * 	BeanConsumer&lt;Employee&gt; <jv>consumer</jv> = <jk>new</jk> BeanConsumer&lt;&gt;() {
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> acceptThrows(Employee <jv>emp</jv>) <jk>throws</jk> Exception {
 * 			validateAndInsert(<jv>emp</jv>);
 * 		}
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> onError(Exception <jv>e</jv>) {
 * 			log.warn(<js>"Skipping invalid record: {}"</js>, <jv>e</jv>.getMessage());
 * 			<jc>// absorb — parsing continues to the next element</jc>
 * 		}
 * 	};
 * 	</p>
 *
 * 	</div>
 *
 * 	<!-- ============================================================================================================ -->
 * 	<a id="BeanStreaming.Channel"></a>
 * 	<h3 class='topic'>BeanChannel — Round-trip marshalling on a single property</h3>
 * 	<div class='topic'>
 *
 * 	<p>
 * 	{@link org.apache.juneau.commons.function.BeanChannel} extends both
 * 	{@link org.apache.juneau.commons.function.BeanSupplier} and
 * 	{@link org.apache.juneau.commons.function.BeanConsumer}, allowing the same property to drive
 * 	both serialization and parsing. The implementation itself determines direction at runtime.
 * 	</p>
 *
 * 	<h5 class='figure'>In-memory channel — ListBeanChannel</h5>
 * 	<p class='bjava'>
 * 	<jk>public class</jk> EmployeeCollection {
 *
 * 		<jk>private final</jk> ListBeanChannel&lt;Employee&gt; <jv>employees</jv> = <jk>new</jk> ListBeanChannel&lt;&gt;();
 *
 * 		<jc>// No setter needed — parser calls acceptThrows() on the existing instance.</jc>
 * 		<ja>@Beanp</ja>(elementType=Employee.<jk>class</jk>)
 * 		<jk>public</jk> ListBeanChannel&lt;Employee&gt; getEmployees() { <jk>return</jk> <jv>employees</jv>; }
 * 	}
 *
 * 	<jc>// Serialize — channel iterated as a sequence.</jc>
 * 	String <jv>json</jv> = Json5Serializer.<jsf>DEFAULT</jsf>.serialize(<jv>collection</jv>);
 *
 * 	<jc>// Parse — channel populated via acceptThrows().</jc>
 * 	Json5Parser.<jsf>DEFAULT</jsf>.parse(<jv>json</jv>, EmployeeCollection.<jk>class</jk>);
 * 	</p>
 *
 * 	<h5 class='figure'>Database-backed channel</h5>
 * 	<p class='bjava'>
 * 	<ja>@Bean</ja>(factory=EmployeeChannel.Factory.<jk>class</jk>)
 * 	<jk>public class</jk> EmployeeChannel <jk>implements</jk> BeanChannel&lt;Employee&gt; {
 *
 * 		<jk>private final</jk> DataSource <jv>ds</jv>;
 * 		<jk>private</jk> Connection <jv>conn</jv>;
 * 		<jk>private</jk> ResultSet <jv>rs</jv>;
 * 		<jk>private</jk> PreparedStatement <jv>insertStmt</jv>;
 * 		<jk>private int</jk> <jv>insertCount</jv>;
 *
 * 		<jk>public</jk> EmployeeChannel(DataSource <jv>ds</jv>) {
 * 			<jk>this</jk>.<jv>ds</jv> = <jv>ds</jv>;
 * 		}
 *
 * 		<jc>// ---- BeanSupplier side (serialization) ----</jc>
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> begin() <jk>throws</jk> Exception {
 * 			<jv>conn</jv> = <jv>ds</jv>.getConnection();
 * 			<jv>insertStmt</jv> = <jv>conn</jv>.prepareStatement(
 * 				<js>"INSERT INTO employee (name, department) VALUES (?, ?)"</js>);
 * 			<jv>conn</jv>.setAutoCommit(<jk>false</jk>);
 * 			<jk>var</jk> <jv>qStmt</jv> = <jv>conn</jv>.prepareStatement(
 * 				<js>"SELECT name, department FROM employee ORDER BY id"</js>);
 * 			<jv>rs</jv> = <jv>qStmt</jv>.executeQuery();
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Iterator&lt;Employee&gt; iterator() {
 * 			<jk>return new</jk> Iterator&lt;&gt;() {
 * 				<ja>@Override</ja> <jk>public boolean</jk> hasNext() { <jk>return</jk> ResultSetIterator.hasNext(<jv>rs</jv>); }
 * 				<ja>@Override</ja> <jk>public</jk> Employee next() { <jk>return</jk> Employee.fromRow(<jv>rs</jv>); }
 * 			};
 * 		}
 *
 * 		<jc>// ---- BeanConsumer side (parsing) ----</jc>
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> acceptThrows(Employee <jv>emp</jv>) <jk>throws</jk> Exception {
 * 			<jv>insertStmt</jv>.setString(1, <jv>emp</jv>.getName());
 * 			<jv>insertStmt</jv>.setString(2, <jv>emp</jv>.getDepartment());
 * 			<jv>insertStmt</jv>.executeUpdate();
 * 			<jk>if</jk> (++<jv>insertCount</jv> % 500 == 0) <jv>conn</jv>.commit();
 * 		}
 *
 * 		<jc>// ---- Shared lifecycle ----</jc>
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> onError(Exception <jv>e</jv>) <jk>throws</jk> Exception {
 * 			<jk>if</jk> (<jv>conn</jv> != <jk>null</jk>) <jv>conn</jv>.rollback();
 * 			<jk>throw</jk> <jv>e</jv>;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> complete() <jk>throws</jk> Exception {
 * 			<jk>if</jk> (<jv>conn</jv> != <jk>null</jk>) {
 * 				<jv>conn</jv>.commit();
 * 				<jk>if</jk> (<jv>rs</jv> != <jk>null</jk>) <jv>rs</jv>.close();
 * 				<jk>if</jk> (<jv>insertStmt</jv> != <jk>null</jk>) <jv>insertStmt</jv>.close();
 * 				<jv>conn</jv>.close();
 * 			}
 * 		}
 *
 * 		<jk>public static class</jk> Factory <jk>implements</jk> BeanFactory&lt;EmployeeChannel&gt; {
 * 			<jk>private final</jk> DataSource <jv>ds</jv>;
 * 			<jk>public</jk> Factory(DataSource <jv>ds</jv>) { <jk>this</jk>.<jv>ds</jv> = <jv>ds</jv>; }
 * 			<ja>@Override</ja>
 * 			<jk>public</jk> EmployeeChannel create() { <jk>return new</jk> EmployeeChannel(<jv>ds</jv>); }
 * 		}
 * 	}
 * 	</p>
 *
 * 	</div>
 *
 * 	<!-- ============================================================================================================ -->
 * 	<a id="BeanStreaming.SpringIntegration"></a>
 * 	<h3 class='topic'>Spring Integration via BeanFactory and BeanStore</h3>
 * 	<div class='topic'>
 *
 * 	<p>
 * 	Spring-managed beans (e.g. a {@code DataSource}) can be injected into streaming implementations
 * 	without constructor scanning or reflection. Register the factories as Spring beans, then wire
 * 	the {@code SpringBeanStore} into the Juneau context so the marshaller can resolve them at
 * 	runtime.
 * 	</p>
 *
 * 	<h5 class='figure'>Spring configuration</h5>
 * 	<p class='bjava'>
 * 	<ja>@Configuration</ja>
 * 	<jk>public class</jk> JuneauStreamingConfig {
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> EmployeeSupplier.Factory employeeSupplierFactory(DataSource <jv>ds</jv>) {
 * 			<jk>return new</jk> EmployeeSupplier.Factory(<jv>ds</jv>);
 * 		}
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> EmployeeConsumer.Factory employeeConsumerFactory(DataSource <jv>ds</jv>) {
 * 			<jk>return new</jk> EmployeeConsumer.Factory(<jv>ds</jv>);
 * 		}
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> EmployeeChannel.Factory employeeChannelFactory(DataSource <jv>ds</jv>) {
 * 			<jk>return new</jk> EmployeeChannel.Factory(<jv>ds</jv>);
 * 		}
 * 	}
 * 	</p>
 *
 * 	<h5 class='figure'>Wiring the SpringBeanStore into a REST resource</h5>
 * 	<p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> EmployeeResource <jk>extends</jk> BasicSpringRestServlet {
 *
 * 		<ja>@Inject</ja>
 * 		<jk>private</jk> ApplicationContext <jv>appCtx</jv>;
 *
 * 		<ja>@Override</ja>
 * 		<jk>protected</jk> BeanContext.Builder createBeanContext(RestContext.Builder <jv>rcBuilder</jv>) {
 * 			<jk>return super</jk>.createBeanContext(<jv>rcBuilder</jv>)
 * 				.beanStore(<jk>new</jk> SpringBeanStore(<jv>appCtx</jv>));
 * 		}
 * 	}
 * 	</p>
 *
 * 	<p>
 * 	With this wiring in place, any class annotated with
 * 	{@code @Bean(factory=EmployeeSupplier.Factory.class)} is automatically instantiated by
 * 	retrieving the factory from the Spring {@code ApplicationContext} and calling
 * 	{@link org.apache.juneau.commons.function.BeanFactory#create()}. No manual construction or
 * 	injection is needed in individual REST methods.
 * 	</p>
 *
 * 	<!-- ============================================================================================================ -->
 * 	<a id="BeanStreaming.Annotations"></a>
 * 	<h4 class='topic'>Supporting Annotations</h4>
 * 	<div class='topic'>
 *
 * 	<table class='styled'>
 * 		<tr>
 * 			<th>Annotation</th>
 * 			<th>Target</th>
 * 			<th>Purpose</th>
 * 		</tr>
 * 		<tr>
 * 			<td>{@code @Bean(factory=X.class)}</td>
 * 			<td>Class</td>
 * 			<td>Specifies the {@link org.apache.juneau.commons.function.BeanFactory} class used to instantiate this type during parsing. The factory is resolved from the {@code BeanStore}.</td>
 * 		</tr>
 * 		<tr>
 * 			<td>{@code @Beanp(factory=X.class)}</td>
 * 			<td>Bean property</td>
 * 			<td>Specifies a property-level factory for instantiating the value of a specific bean property.</td>
 * 		</tr>
 * 		<tr>
 * 			<td>{@code @Beanp(elementType=Y.class)}</td>
 * 			<td>Bean property</td>
 * 			<td>Declares the element type for generic streaming properties ({@code Stream&lt;Y&gt;}, {@code BeanSupplier&lt;Y&gt;}, etc.) overcoming Java type erasure. Also supports narrowing to concrete implementation types.</td>
 * 		</tr>
 * 	</table>
 *
 * 	</div>
 *
 * 	</div>
 *
 * 	<!-- ============================================================================================================ -->
 * 	<a id="BeanStreaming.Supplier.Unwrapping"></a>
 * 	<h3 class='topic'>Supplier&lt;T&gt; single-value unwrapping</h3>
 * 	<div class='topic'>
 *
 * 	<p>
 * 	{@link java.util.function.Supplier Supplier&lt;T&gt;} (the standard JDK interface) is treated as a
 * 	single-value lazy wrapper. Serializers call {@code get()} and serialize the result
 * 	transparently. Nested {@code Supplier} chains are unwrapped recursively up to a depth of 10.
 * 	</p>
 *
 * 	<p>
 * 	Note: {@link org.apache.juneau.commons.function.BeanSupplier} is <em>not</em> unwrapped — it
 * 	is treated as an {@link java.lang.Iterable} sequence, not a single-value wrapper.
 * 	</p>
 *
 * 	<p class='bjava'>
 * 	<jc>// Serialized as the string "hello" — Supplier is unwrapped.</jc>
 * 	Supplier&lt;String&gt; <jv>lazy</jv> = () -&gt; <js>"hello"</js>;
 * 	String <jv>json</jv> = Json5Serializer.<jsf>DEFAULT</jsf>.serialize(<jv>lazy</jv>); <jc>// 'hello'</jc>
 *
 * 	<jc>// Nested Supplier chains are also unwrapped.</jc>
 * 	Supplier&lt;Supplier&lt;Integer&gt;&gt; <jv>nested</jv> = () -&gt; () -&gt; 42;
 * 	<jv>json</jv> = Json5Serializer.<jsf>DEFAULT</jsf>.serialize(<jv>nested</jv>); <jc>// 42</jc>
 * 	</p>
 *
 * 	</div>
 *
 * </div>
 */
package org.apache.juneau.commons.function;
