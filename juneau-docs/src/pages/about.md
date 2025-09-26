# About Apache Juneau™

## Quick Links

### Project Resources
- <a href="https://github.com/apache/juneau" target="_blank">GitHub</a> - Source code repository
- <a href="https://github.com/apache/juneau/wiki" target="_blank">Wiki</a> - Community documentation

### Documentation & Reports
- <a href="/site/apidocs/" target="_blank">Javadocs</a> - API documentation
- <a href="/site/" target="_blank">Maven Site</a> - Complete project reports
- <a href="/site/xref/" target="_blank">Source Cross-Reference</a> - Browsable source code
- <a href="/site/surefire.html" target="_blank">Test Reports</a> - Unit test results
- <a href="/site/jacoco-aggregate/" target="_blank">Code Coverage</a> - Test coverage reports

---

## Overview

Apache Juneau™ is a single cohesive Java ecosystem consisting of the following parts:

| Group | Component | Description |
|-------|-----------|-------------|
| **juneau-core** | [juneau-marshall](/docs/topics/JuneauMarshallBasics) | POJO marshalling support for JSON, JSON5, XML, HTML, URL-encoding, UON, MessagePack, and CSV using no external module dependencies. |
| | [juneau-marshall-rdf](/docs/topics/Module-juneau-marshall-rdf) | Extended marshalling support for RDF/XML, N3, N-Tuple, and Turtle. |
| | [juneau-dto](/docs/topics/JuneauDtoBasics) | A variety of predefined DTOs for serializing and parsing languages such as HTML5, Swagger and ATOM. |
| | [juneau-config](/docs/topics/JuneauConfigBasics) | A sophisticated configuration file API. |
| | [juneau-assertions](/docs/topics/JuneauAssertionBasics) | A fluent assertions API. |
| **juneau-rest** | [juneau-rest-server](/docs/topics/JuneauRestServerBasics) | A universal REST server API for creating Swagger-based self-documenting REST interfaces using POJOs, simply deployed as one or more top-level servlets in any Servlet 3.1.0+ container. |
| | [juneau-rest-server-springboot](/docs/topics/JuneauRestServerSpringbootBasics) | Spring Boot integration. |
| | [juneau-rest-client](/docs/topics/JuneauRestClientBasics) | A universal REST client API for interacting with Juneau or 3rd-party REST interfaces using POJOs and proxy interfaces. |

Questions via email to <a href="mailto:dev@juneau.apache.org?Subject=Apache%20Juneau%20question" target="_blank">dev@juneau.apache.org</a> are always welcome.

Juneau is packed with features that may not be obvious at first. Users are encouraged to ask for code reviews by providing links to specific source files such as through GitHub. Not only can we help you with feedback, but it helps us understand usage patterns to further improve the product.

## Features

The [juneau-marshall](/docs/topics/JuneauMarshallBasics) and [juneau-marshall-rdf](/docs/topics/Module-juneau-marshall-rdf) modules provides memory-efficient typed and untyped POJO serializing and parsing for a variety of languages.

```java
// A simple bean
public class Person {
    public String name = "John Smith";
    public int age = 21;
}

// Produces: "{"name":"John Smith","age":21}"
String json = Json.of(new Person());

// Parse back into a bean.
Person person = Json.to(json, Person.class);

// Various other languages.
String json5 = Json5.of(person);
String xml = Xml.of(person);
String html = Html.of(person);
String urlEncoding = UrlEncoding.of(person);
String uon = Uon.of(person);
String openApi = OpenApi.of(person);
String rdfXml = RdfXml.of(person);
String rdfXmlAbbriev = RdfXmlAbbrev.of(person);
String n3 = N3.of(person);
String nTriple = NTriple.of(person);
String turtle = Turtle.of(person);
String plainText = PlainText.of(person);
String csv = Csv.of(person);
byte[] msgPack = MsgPack.of(person);
```

The [juneau-rest-server](/docs/topics/JuneauRestServerBasics) and [juneau-rest-client](/docs/topics/JuneauRestClientBasics) libraries provide server and client side REST capabilities that can be used by themselves, or together to create simplified yet sophisticated Java-based REST communications layers that completely hide away the complexities of the REST protocol from end-users.

```java
// Server-side endpoint	
@Rest(path="/petstore")
public class PetStoreRest extends BasicRestServlet implements BasicUniversalConfig  {
    
    @RestPost(path="/pets", guards=AdminGuard.class)
    public Ok addPet(
        @Content CreatePet createPetBean, 
        @Header("E-Tag") UUID etag, 
        @Query("debug") boolean debug
    ) throws BadRequest, Unauthorized, InternalServerError {
        // Process request here.
        return Ok.OK;  // Standard 400-OK response.
    }
}
```

```java
// Client-side Java interface that describes the REST endpoint
@Remote(path="/petstore")
public interface PetStoreClient {
    
    @RemotePost("/pets")
    Ok addPet(
        @Content CreatePet createPet, 
        @Header("E-Tag") UUID etag, 
        @Query("debug") boolean debug
    ) throws BadRequest, Unauthorized, InternalServerError;
}
```

```java
// Use a RestClient with default JSON 5 support and BASIC auth.
RestClient client = RestClient.create().json5().basicAuth(...).build();

// Instantiate our proxy interface.
PetStoreClient store = client.getRemote(PetStoreClient.class, "http://localhost:10000");

// Use it to create a pet.
CreatePet createPet = new CreatePet("Fluffy", 9.99);
Pet pet = store.addPet(createPet, UUID.randomUUID(), true);
```

The [juneau-dto](/docs/topics/JuneauDtoBasics) module contains several predefined POJOs for generating commonly-used document types that are designed to be used with the Juneau Marshaller APIs for both serializing and parsing. For example, you can build HTML DOMs in Java.

```java
import static org.apache.juneau.dto.html5.HtmlBuilder.*;

// An HTML table	
Object mytable = 	
    table(
        tr(
            th("c1"),
            th("c2")
        ),
        tr(
            td("v1"),
            td("v2")
        )
    );
    
String html = Html.of(mytable);
```

```xml
<table>
    <tr>
        <th>c1</th>
        <th>c2</th>
    </tr>
    <tr>
        <td>v1</td>
        <td>v2</td>
    </tr>
</table>
```

The [juneau-config](/docs/topics/JuneauConfigBasics) module contains a powerful API for creating and using INI-style config files.

```ini
# A set of entries
[Section1]

# An integer
key1 = 1

# A boolean
key2 = true

# An array
key3 = 1,2,3

# A POJO
key4 = http://bar
```

```java
// Create a Config object
Config config = Config.create().name("MyConfig.cfg").build();

// Read values from section #1
int key1 = config.getInt("Section1/key1");
boolean key2 = config.getBoolean("Section1/key2");
int[] key3 = config.getObject("Section1/key3", int[].class);
URL key4 = config.getObject("Section1/key4", URL.class);
```

The [juneau-assertions](/docs/topics/JuneauAssertionBasics) module in Juneau is a powerful API for performing fluent style assertions.

```java
import static org.apache.juneau.assertions.Assertions.*;

// Check the contents of a string.
// "as" methods perform a transformation.
// "is" methods perform an assertion.
assertString("foo, bar")
    .asSplit(",")
    .asTrimmed()
    .is("foo", "bar");

// Extract a subset of properties from a list of beans and compare using Simplified JSON.
List<MyBean> myListOfBeans = ...;
assertBeanList(myListOfBeans)
    .asPropertyMaps("a,b")
    .asJson().is("[{a:1,b:'foo'}]");
```

### Other Features

- Fast memory-efficient serialization.
- Fast, safe, memory-efficient parsing. Parsers are not susceptible to deserialization attacks.
- KISS is our mantra! No auto-wiring. No code generation. No dependency injection. Just add it to your classpath and use it. Extremely simple unit testing!
- Enjoyable to use
- Tiny - ~1MB
- Exhaustively tested
- Lots of up-to-date documentation and examples
- Minimal module dependencies making them optimal for uber-jars 
- Built on top of Servlet and Apache HttpClient APIs that allow you to use the newest HTTP/2 features such as request/response multiplexing and server push.
