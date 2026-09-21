# SOAP-Demo mit Spring Boot 4 und Tomcat

Lauffähiger SOAP-1.1-Service mit **Spring Boot 4.0.8**, eingebettetem **Tomcat 11** und Java 17 oder neuer. Maven 3.6.3+ wird benötigt. Keine Datenbank und kein separat installierter Tomcat erforderlich.

## Bauen und Integrationstests ausführen

```bash
mvn clean verify
```

Maven generiert die Jakarta-JAXB-Klassen aus `country-types.xsd`, erstellt das ausführbare JAR und führt anschließend die Integrationstests über Failsafe aus. Die generierten Klassen liegen in `target/generated-sources/jaxb` und werden nicht manuell bearbeitet.

**Wichtig:** `mvn test` allein führt die `*IT`-Integrationstests nicht aus; dafür `mvn verify` verwenden. Die Berichte stehen in `target/failsafe-reports`.

## Starten

```bash
java -jar target/soap-demo-0.0.1-SNAPSHOT.jar
```

Alternativ, einschließlich JAXB-Generierung:

```bash
mvn compile spring-boot:run
```

- SOAP-Endpunkt: <http://localhost:8080/ws>
- WSDL (z. B. für den Import in SoapUI): <http://localhost:8080/ws/countries.wsdl>
- XSD: <http://localhost:8080/ws/country-types.xsd>

Ein anderer Port kann mit `java -jar target/soap-demo-0.0.1-SNAPSHOT.jar --server.port=9090` gesetzt werden. Die veröffentlichte WSDL passt ihre Service-Adresse an den HTTP-Aufruf an.

## Beispielaufruf

Aus dem Projektverzeichnis:

```bash
curl --silent --show-error http://localhost:8080/ws \
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
- `src/main/java/de/example/soap/CountryEndpoint.java`: Operation `getCountry` mit unveränderlichen Beispieldaten.
- `src/main/java/de/example/soap/WebServiceConfiguration.java`: XSD-Validierung von Anfragen und Antworten.
- `src/test/java/de/example/soap/CountrySoapIT.java`: echte HTTP-Integrationstests mit `@SpringBootTest(RANDOM_PORT)` und Java-HTTP-Client.
- `examples/get-country.xml`: vollständige SOAP-Beispielanfrage.

Spring Boot konfiguriert den Spring-WS-Servlet über `spring.webservices.*`. Der Starter `spring-boot-starter-webservices` bringt den eingebetteten Tomcat mit.

Die Integrationstests prüfen Tomcat als Server, WSDL und erreichbaren Schema-Import, die dynamische Service-Adresse, alle drei Länder samt XSD-konformer Antwort, unbekannte Länder sowie fünf ungültige Requests. Es werden keine HTTP- oder Endpoint-Mocks verwendet.

Die Anwendung ist eine lokale Demo ohne Authentifizierung und Persistenz.

Referenz: [Spring Boot Web Services](https://docs.spring.io/spring-boot/reference/io/webservices.html).
