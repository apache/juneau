# Juneau

## Links:
* [Home Page](https://sites.google.com/site/apachejuneau)

## Contacts:
* [James Bognar - Salesforce](mailto:james.bognar@salesforce.com)
* [Peter Haumer - IBM](mailto:phaumer@us.ibm.com)

## Quick Examples:

Core library includes easy-to-use and customizable serializers and parsers.  The examples here are only a small taste of what's possible. 
Extensive examples are provided in the Javadocs.

Default serializers can often be used to serializers POJOs in a single line of code...
```Java
   // A simple POJO class
   public class Person {
      public String name = "John Smith";
      public int age = 21;
   }

   // Serialize a bean to JSON, XML, or HTML
   Person p = new Person();

   // Produces:
   // "{name:'John Smith',age:21}"
   String json = JsonSerializer.DEFAULT.serialize(p);

   // Produces:
   // <object>
   //   <name>John Smith</name>
   //   <age>21</age>
   // </object>
   String xml = XmlSerializer.DEFAULT.serialize(p);

   // Produces:
   // <table>
   //   <tr><th>key</th><th>value</th></tr>
   //   <tr><td>name</td><td>John Smith</td></tr>
   //   <tr><td>age</td><td>21</td></tr>
   // </table>
   String html = HtmlSerializer.DEFAULT.serialize(p);
```

Parsing back into POJOs is equally simple...
```Java
   // Use one of the predefined parsers.
   ReaderParser parser = JsonParser.DEFAULT;

   // Parse a JSON object (creates a generic ObjectMap).
   String json = "{name:'John Smith',age:21}";
   Map m1 = parser.parse(json, Map.class);

   // Parse a JSON string.
   json = "'foobar'";
   String s2 = parser.parse(json, String.class);

   // Parse a JSON number as a Long or Float.
   json = "123";
   Long l3 = parser.parse(json, Long.class);
   Float f3 = parser.parse(json, Float.class);

   // Parse a JSON object as a bean.
   json = "{name:'John Smith',age:21}";
   Person p4 = parser.parse(json, Person.class);

   // Parse a JSON object as a HashMap<String,Person>.
   json = "{a:{name:'John Smith',age:21},b:{name:'Joe Smith',age:42}}";
   Map<String,Person> m5 = parser.parseMap(json, HashMap.class, String.class, Person.class)

   // Parse a JSON array of integers as a Collection of Integers or int[] array.
   json = "[1,2,3]";
   List<Integer> l6 = parser.parseCollection(json, LinkedList.class, Integer.class);
   int[] i6 = parser.parse(json, int[].class);

```

Server component allows for annotated REST servlets that automatically support all language types...
```Java
   @RestResource(
      path="/systemProperties"
   )
   public class SystemPropertiesService extends RestServletJenaDefault {
   
      /** [OPTIONS /*] - Show resource options. */
      @RestMethod(name="OPTIONS", path="/*")
      public ResourceOptions getOptions(RestRequest req) {
         return new ResourceOptions(this, req);
      }
      
      /** [GET /] - Get all system properties. */
      @RestMethod(name="GET", path="/")
      public TreeMap<String,String> getSystemProperties() throws Throwable {
         return new TreeMap(System.getProperties());
      }
   
      /** [GET /{propertyName}] - Get system property with specified name. */
      @RestMethod(name="GET", path="/{propertyName}")
      public String getSystemProperty(@Attr String propertyName) throws Throwable {
         return System.getProperty(propertyName);
      }
      
      /** [PUT /{propertyName}] - Set system property with specified name. */
      @RestMethod(name="PUT", path="/{propertyName}", guards=AdminGuard.class)
      public Redirect setSystemProperty(@Attr String propertyName, @Content String value) {
         System.setProperty(propertyName, value);
         return new Redirect();
      }
   
      /** [DELETE /{propertyName}] - Delete system property with specified name. */
      @RestMethod(name="DELETE", path="/{propertyName}", guards=AdminGuard.class)
      public Redirect deleteSystemProperty(@Attr String propertyName) {
         System.clearProperty(propertyName);
         return new Redirect();
      }
   }
```

Client component allows you to easily interact with REST intefaces using POJOs...
```Java
   // Create a reusable JSON client.
   RestClient client = new RestClient(JsonSerializer.class, JsonParser.class);
   
   // The address of the root resource.
   String url = "http://localhost:9080/sample/addressBook";
   
   // Do a REST GET against a remote REST interface and convert
   // the response to an unstructured ObjectMap object.
   ObjectMap m1 = client.doGet(url).getResponse(ObjectMap.class);
   
   // Same as above, except parse the JSON as a bean.
   AddressBook a2 = client.doGet(url).getResponse(AddressBook.class);
   
   // Add a person to the address book.
   // Use XML as the transport medium.
   client = new RestClient(XmlSerializer.class, XmlParser.class);
   Person p = new Person("Joe Smith", 21);
   int returnCode = client.doPost(url + "/entries", p).execute();
```

A remote proxy interface API is also provided ...
```Java
   RestClient client = new RestClient(JsonSerializer.class, JsonParser.class)
		.setRemoteableUriServletUrl("https://localhost:9443/jazz/remote");
   	
   	// Execute a method on the server.
   	IAddressBook ab = client.getRemoteableProxy(IAddressBook.class);
   	ab.createPerson(...);
```




