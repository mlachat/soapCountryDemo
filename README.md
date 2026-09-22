# Jakarta-SOAP-Demo mit Spring Boot 4 und Tomcat

Lauffähiger SOAP-1.1-Service mit **Spring Boot 4.0.8**, **Apache CXF 4.2.3** als Jakarta-XML-Web-Services-Runtime, eingebettetem **Tomcat 11** und Java 17 oder neuer. Maven 3.6.3+ wird benötigt. Keine Datenbank und kein separat installierter Tomcat erforderlich.

## Bauen und Integrationstests ausführen

```bash
mvn clean verify
```

Maven generiert das Jakarta-JAX-WS-Service-Interface und die Jakarta-JAXB-Klassen aus `countries.wsdl` und dem importierten `country-types.xsd`, erstellt das ausführbare JAR und führt anschließend die Integrationstests über Failsafe aus. Die generierten Klassen liegen in `target/generated-sources/cxf` und werden nicht manuell bearbeitet.

**Wichtig:** `mvn test` allein führt die `*IT`-Integrationstests nicht aus; dafür `mvn verify` verwenden. Die Berichte stehen in `target/failsafe-reports`.

## Starten

```bash
java -jar target/soap-demo-0.0.1-SNAPSHOT.jar
```

Alternativ, einschließlich WSDL-Codegenerierung:

```bash
mvn compile spring-boot:run
```

- SOAP-Endpunkt: <http://localhost:8080/ws/countries>
- WSDL (z. B. für den Import in SoapUI): <http://localhost:8080/ws/countries?wsdl>
- XSD: <http://localhost:8080/ws/countries?xsd=country-types.xsd>

Ein anderer Port kann mit `java -jar target/soap-demo-0.0.1-SNAPSHOT.jar --server.port=9090` gesetzt werden. Die veröffentlichte WSDL passt ihre Service-Adresse an den HTTP-Aufruf an.

## Beispielaufruf

Aus dem Projektverzeichnis:

```bash
curl --silent --show-error http://localhost:8080/ws/countries \
  -H 'Content-Type: text/xml; charset=UTF-8' \
  -H 'SOAPAction: "https://example.de/soap/countries/getCountry"' \
  --data-binary @examples/get-country.xml
```

Die Antwort enthält folgenden Payload (Namespace-Präfixe können abweichen):

```xml
<c:getCountryResponse xmlns:c="https://example.de/soap/countries">
  <c:country>
    <c:code>DE</c:code>
    <c:name>Deutschland</c:name>
    <c:capital>Berlin</c:capital>
    <c:currency>EUR</c:currency>
  </c:country>
</c:getCountryResponse>
```

Die Demo kennt `DE`, `AT` und `CH`. Zwei Großbuchstaben sind gemäß XSD erforderlich. Ein unbekannter Code wie `ZZ` erzeugt einen SOAP-Client-Fault mit HTTP 500. Fehlende Codes, Kleinbuchstaben, falsche Längen und zusätzliche Elemente werden vor dem Endpoint durch die XSD-Validierung ebenfalls als SOAP-Client-Fault zurückgewiesen.

## Aufbau

- `src/main/resources/META-INF/schemas/countries.wsdl`: fester WSDL-Vertrag (document/literal).
- `src/main/resources/META-INF/schemas/country-types.xsd`: Request-/Response-Schema, Grundlage der JAXB-Generierung.
- `src/main/java/de/example/soap/CountryEndpoint.java`: Implementierung des generierten `CountriesPort` mit `jakarta.jws.WebService` und unveränderlichen Beispieldaten.
- `src/main/java/de/example/soap/WebServiceConfiguration.java`: Veröffentlichung über `jakarta.xml.ws.Endpoint` mit CXF und XSD-Validierung von Anfragen und Antworten.
- `src/test/java/de/example/soap/CountrySoapIT.java`: echte HTTP-Integrationstests mit `@SpringBootTest(RANDOM_PORT)` und Java-HTTP-Client.
- `examples/get-country.xml`: vollständige SOAP-Beispielanfrage.

Der CXF-Starter registriert den Jakarta-Servlet unter `cxf.path=/ws`; der Endpoint wird unter `/countries` veröffentlicht. `spring-boot-starter-webmvc` bringt den eingebetteten Tomcat mit. `saaj-impl` stellt den Jakarta-SOAP-Provider für SOAP-Faults bereit.

## Jakarta-Packages und Annotationen

- `jakarta.jws.WebService` am Endpoint und generierten Service-Interface.
- `jakarta.jws.WebMethod`, `WebParam`, `WebResult` und `jakarta.jws.soap.SOAPBinding` am generierten Interface.
- `jakarta.xml.bind.annotation.*` an den generierten XML-Modellen.
- `jakarta.xml.ws.Endpoint`, `jakarta.xml.ws.soap.SOAPFaultException` und `jakarta.xml.soap.*` für Veröffentlichung und Fehlerbehandlung.
- `jakarta.xml.ws.Service` im Integrationstest für einen echten Aufruf über einen typisierten Jakarta-Client.

`javax.xml.namespace`, `javax.xml.parsers`, `javax.xml.validation`, `javax.xml.transform` und `javax.xml.xpath` gehören weiterhin zu **Java SE**. Diese Packages haben keine Jakarta-Entsprechung und bleiben deshalb unverändert. Spring-Boot-Konfigurations- und Testannotationen bleiben Spring-Annotationen.

Gegenüber der Spring-WS-Variante ändern sich die URLs: SOAP-Aufrufe gehen jetzt an `/ws/countries`, die WSDL liegt unter `/ws/countries?wsdl`. Der fachliche XML-Vertrag bleibt gleich.

Die Integrationstests prüfen Tomcat als Server, WSDL und erreichbaren Schema-Import, die dynamische Service-Adresse, alle drei Länder samt XSD-konformer Antwort, unbekannte Länder sowie fünf ungültige Requests. Ein zusätzlicher Test lädt die veröffentlichte WSDL und ruft den Service über einen Jakarta-JAX-WS-Client auf (insgesamt 12 Tests). Es werden keine HTTP- oder Endpoint-Mocks verwendet.

Die Anwendung ist eine lokale Demo ohne Authentifizierung und Persistenz.

Referenzen: [CXF 4.2 mit Spring Boot 4 und Jakarta EE 11](https://cxf.apache.org/docs/42-migration-guide.html), [WSDL-Codegenerierung](https://cxf.apache.org/docs/maven-cxf-codegen-plugin-wsdl-to-java.html).
