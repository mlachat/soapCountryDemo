package de.example.soap;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadValidatingInterceptor;

@Configuration
public class WebServiceConfiguration implements WsConfigurer {
    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(payloadValidator());
    }

    @Bean
    public PayloadValidatingInterceptor payloadValidator() {
        var validator = new PayloadValidatingInterceptor();
        validator.setSchema(new ClassPathResource("META-INF/schemas/country-types.xsd"));
        validator.setValidateRequest(true);
        validator.setValidateResponse(true);
        return validator;
    }
}
