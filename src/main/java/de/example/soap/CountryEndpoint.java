package de.example.soap;

import java.util.Map;

import de.example.soap.contract.Country;
import de.example.soap.contract.GetCountryRequest;
import de.example.soap.contract.GetCountryResponse;
import de.example.soap.contract.CountriesPort;
import jakarta.jws.WebService;

@WebService(endpointInterface = "de.example.soap.contract.CountriesPort",
        targetNamespace = CountryEndpoint.NAMESPACE,
        serviceName = "CountriesService", portName = "CountriesSoap11Port",
        wsdlLocation = "META-INF/schemas/countries.wsdl")
public class CountryEndpoint implements CountriesPort {
    public static final String NAMESPACE = "https://example.de/soap/countries";
    private static final Map<String, CountryData> COUNTRIES = Map.of(
            "DE", new CountryData("Deutschland", "Berlin", "EUR"),
            "AT", new CountryData("Österreich", "Wien", "EUR"),
            "CH", new CountryData("Schweiz", "Bern", "CHF"));

    @Override
    public GetCountryResponse getCountry(GetCountryRequest request) {
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
