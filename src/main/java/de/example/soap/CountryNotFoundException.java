package de.example.soap;

import javax.xml.namespace.QName;

import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;

public class CountryNotFoundException extends SOAPFaultException {
    public CountryNotFoundException(String code) {
        super(createFault(code));
    }

    private static SOAPFault createFault(String code) {
        try {
            return SOAPFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createFault(
                    "Unbekannter Ländercode: " + code,
                    new QName(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, "Client", "soap"));
        } catch (SOAPException exception) {
            throw new WebServiceException("SOAP-Fault konnte nicht erstellt werden", exception);
        }
    }
}
