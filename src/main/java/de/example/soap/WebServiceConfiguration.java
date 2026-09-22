package de.example.soap;

import java.util.Map;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class WebServiceConfiguration {
    @Bean
    public CountryEndpoint countryEndpoint() {
        return new CountryEndpoint();
    }

    @Bean(destroyMethod = "stop")
    public Endpoint countriesService(Bus bus, CountryEndpoint countryEndpoint) {
        var endpoint = new EndpointImpl(bus, countryEndpoint);
        endpoint.setProperties(Map.of("schema-validation-enabled", "BOTH"));
        endpoint.publish("/countries");
        return endpoint;
    }
}
