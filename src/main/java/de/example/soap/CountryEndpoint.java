package de.example.soap;

import java.util.Map;

import de.example.soap.contract.Country;
import de.example.soap.contract.GetCountryRequest;
import de.example.soap.contract.GetCountryResponse;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class CountryEndpoint {
    public static final String NAMESPACE = "https://example.de/soap/countries";
    private static final Map<String, CountryData> COUNTRIES = Map.of(
            "DE", new CountryData("Deutschland", "Berlin", "EUR"),
            "AT", new CountryData("Österreich", "Wien", "EUR"),
            "CH", new CountryData("Schweiz", "Bern", "CHF"));

    @PayloadRoot(namespace = NAMESPACE, localPart = "getCountryRequest")
    @ResponsePayload
    public GetCountryResponse getCountry(@RequestPayload GetCountryRequest request) {
        var data = COUNTRIES.get(request.getCode());
        if (data == null) {
            throw new CountryNotFoundException(request.getCode());
        }
        var country = new Country();
        country.setCode(request.getCode());
        country.setName(data.name());
        country.setCapital(data.capital());
        country.setCurrency(data.currency());
        var response = new GetCountryResponse();
        response.setCountry(country);
        return response;
    }

    private record CountryData(String name, String capital, String currency) { }
}
