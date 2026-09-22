package de.example.soap;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import de.example.soap.contract.CountriesPort;
import de.example.soap.contract.GetCountryRequest;
import jakarta.xml.ws.Service;
import org.apache.cxf.frontend.ClientProxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CountrySoapIT {
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private ServletWebServerApplicationContext context;

    @Test
    void runsOnEmbeddedTomcat() {
        assertThat(context.getWebServer().getClass().getSimpleName()).isEqualTo("TomcatWebServer");
    }

    @Test
    void jakartaClientCallsServiceUsingPublishedWsdl() throws Exception {
        var service = Service.create(URI.create(baseUrl() + "/ws/countries?wsdl").toURL(),
                new QName(CountryEndpoint.NAMESPACE, "CountriesService"));
        var proxy = service.getPort(new QName(CountryEndpoint.NAMESPACE, "CountriesSoap11Port"),
                CountriesPort.class);
        var cxfClient = ClientProxy.getClient(proxy);
        cxfClient.getRequestContext().put("jakarta.xml.ws.client.connectionTimeout", "5000");
        cxfClient.getRequestContext().put("jakarta.xml.ws.client.receiveTimeout", "10000");
        try {
            var request = new GetCountryRequest();
            request.setCode("DE");
            var country = proxy.getCountry(request).getCountry();
            assertThat(country.getCode()).isEqualTo("DE");
            assertThat(country.getCapital()).isEqualTo("Berlin");
        } finally {
            cxfClient.destroy();
        }
    }

    @Test
    void publishesWsdlWithReachableSchemaAndActualServiceAddress() throws Exception {
        var response = get("/ws/countries?wsdl");
        assertThat(response.statusCode()).isEqualTo(200);
        var wsdl = xml(response.body());
        assertThat(value(wsdl, "/w:definitions/@targetNamespace")).isEqualTo(CountryEndpoint.NAMESPACE);
        assertThat(value(wsdl, "/w:definitions/w:portType/w:operation/@name")).isEqualTo("getCountry");
        assertThat(value(wsdl, "//s:address/@location")).isEqualTo(baseUrl() + "/ws/countries");
        var schemaUri = URI.create(baseUrl() + "/ws/countries?wsdl")
                .resolve(value(wsdl, "//xs:import/@schemaLocation"));
        var schemaResponse = client.send(HttpRequest.newBuilder(schemaUri).timeout(Duration.ofSeconds(10))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(schemaResponse.statusCode()).isEqualTo(200);
        assertThat(value(xml(schemaResponse.body()), "/xs:schema/@targetNamespace"))
                .isEqualTo(CountryEndpoint.NAMESPACE);
    }

    @ParameterizedTest
    @CsvSource({"DE, Deutschland, Berlin, EUR", "AT, Österreich, Wien, EUR", "CH, Schweiz, Bern, CHF"})
    void returnsCountryOverHttp(String code, String name, String capital, String currency) throws Exception {
        var response = post("<c:code>" + code + "</c:code>");
        assertThat(response.statusCode()).isEqualTo(200);
        var document = xml(response.body());
        var country = "/soap:Envelope/soap:Body/c:getCountryResponse/c:country/";
        assertThat(value(document, country + "c:code")).isEqualTo(code);
        assertThat(value(document, country + "c:name")).isEqualTo(name);
        assertThat(value(document, country + "c:capital")).isEqualTo(capital);
        assertThat(value(document, country + "c:currency")).isEqualTo(currency);
        var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var schema = schemaFactory.newSchema(getClass().getResource("/META-INF/schemas/country-types.xsd"));
        var payload = (Node) xpath().evaluate("/soap:Envelope/soap:Body/c:getCountryResponse",
                document, XPathConstants.NODE);
        schema.newValidator().validate(new DOMSource(payload));
    }

    @Test
    void unknownCountryReturnsClientFault() throws Exception {
        var response = post("<c:code>ZZ</c:code>");
        var document = assertClientFault(response);
        assertThat(value(document, "/soap:Envelope/soap:Body/soap:Fault/faultstring"))
                .isEqualTo("Unbekannter Ländercode: ZZ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "<c:code/>", "<c:code>de</c:code>", "<c:code>DEU</c:code>",
            "<c:code>DE</c:code><c:unexpected/>"})
    void invalidRequestReturnsValidationFault(String content) throws Exception {
        var document = assertClientFault(post(content));
        assertThat(value(document, "/soap:Envelope/soap:Body/soap:Fault/faultstring"))
                .contains("cvc-");
    }

    private Document assertClientFault(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(500);
        var document = xml(response.body());
        var faultCode = (Node) xpath().evaluate("/soap:Envelope/soap:Body/soap:Fault/faultcode",
                document, XPathConstants.NODE);
        assertThat(faultCode).isNotNull();
        var parts = faultCode.getTextContent().split(":", 2);
        assertThat(parts).hasSize(2);
        assertThat(parts[1]).isEqualTo("Client");
        assertThat(faultCode.lookupNamespaceURI(parts[0])).isEqualTo(SOAP_NS);
        return document;
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String content) throws Exception {
        var body = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                               xmlns:c="https://example.de/soap/countries">
                    <soap:Body><c:getCountryRequest>%s</c:getCountryRequest></soap:Body>
                </soap:Envelope>
                """.formatted(content);
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl() + "/ws/countries"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "text/xml; charset=UTF-8")
                .header("SOAPAction", "\"https://example.de/soap/countries/getCountry\"")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private static Document xml(String input) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(input)));
    }

    private static String value(Document document, String expression) throws Exception {
        return xpath().evaluate(expression, document);
    }

    private static XPath xpath() {
        var namespaces = Map.of("soap", SOAP_NS, "c", CountryEndpoint.NAMESPACE,
                "w", "http://schemas.xmlsoap.org/wsdl/", "s", "http://schemas.xmlsoap.org/wsdl/soap/",
                "xs", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        var xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                return namespaces.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
            }
            public String getPrefix(String namespaceURI) {
                return namespaces.entrySet().stream().filter(e -> e.getValue().equals(namespaceURI))
                        .map(Map.Entry::getKey).findFirst().orElse(null);
            }
            public Iterator<String> getPrefixes(String namespaceURI) {
                return namespaces.entrySet().stream().filter(e -> e.getValue().equals(namespaceURI))
                        .map(Map.Entry::getKey).iterator();
            }
        });
        return xpath;
    }
}
