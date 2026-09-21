package de.example.soap;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CLIENT)
public class CountryNotFoundException extends RuntimeException {
    public CountryNotFoundException(String code) {
        super("Unbekannter Ländercode: " + code);
    }
}
