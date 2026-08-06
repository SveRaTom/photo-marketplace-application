package photomarketplace.client.customoffer;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import photomarketplace.model.dto.customoffer.CreateCustomOfferRequestDTO;
import photomarketplace.model.dto.customoffer.CustomOfferDecisionRequestDTO;
import photomarketplace.model.dto.customoffer.CustomOfferResponseDTO;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "custom-offer-service",
        url = "${integration.custom-offer-service.base-url}")
public interface CustomOfferClient {

    @PostMapping("/api/custom-offers")
    CustomOfferResponseDTO create(@RequestBody CreateCustomOfferRequestDTO request);

    @GetMapping("/api/custom-offers/{customOfferId}")
    CustomOfferResponseDTO getById(@PathVariable("customOfferId") UUID customOfferId);

    @GetMapping(value = "/api/custom-offers", params = "clientId")
    List<CustomOfferResponseDTO> getForClient(@RequestParam("clientId") UUID clientId);

    @GetMapping(value = "/api/custom-offers", params = "photographerId")
    List<CustomOfferResponseDTO> getForPhotographer(@RequestParam("photographerId") UUID photographerId);

    @PutMapping("/api/custom-offers/{customOfferId}/decision")
    CustomOfferResponseDTO decide(
            @PathVariable("customOfferId") UUID customOfferId,
            @RequestParam("photographerId") UUID photographerId,
            @RequestBody CustomOfferDecisionRequestDTO request);

    @DeleteMapping("/api/custom-offers/{customOfferId}")
    void withdraw(@PathVariable("customOfferId") UUID customOfferId,
            @RequestParam("clientId") UUID clientId);
}
