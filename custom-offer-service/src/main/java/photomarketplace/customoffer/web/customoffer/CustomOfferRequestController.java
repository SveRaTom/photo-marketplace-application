package photomarketplace.customoffer.web.customoffer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import photomarketplace.customoffer.model.dto.customoffer.CreateCustomOfferRequestDTO;
import photomarketplace.customoffer.model.dto.customoffer.CustomOfferDecisionRequestDTO;
import photomarketplace.customoffer.model.dto.customoffer.CustomOfferResponseDTO;
import photomarketplace.customoffer.service.CustomOfferRequestService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/custom-offers")
@RequiredArgsConstructor
public class CustomOfferRequestController {

    private final CustomOfferRequestService customOfferRequestService;

    @PostMapping
    public ResponseEntity<CustomOfferResponseDTO> create(
            @Valid @RequestBody final CreateCustomOfferRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.customOfferRequestService.create(request));
    }

    @GetMapping("/{customOfferId}")
    public CustomOfferResponseDTO getById(@PathVariable final UUID customOfferId) {
        return this.customOfferRequestService.getById(customOfferId);
    }

    @GetMapping(params = "clientId")
    public List<CustomOfferResponseDTO> getForClient(@RequestParam final UUID clientId) {
        return this.customOfferRequestService.getForClient(clientId);
    }

    @GetMapping(params = "photographerId")
    public List<CustomOfferResponseDTO> getForPhotographer(@RequestParam final UUID photographerId) {
        return this.customOfferRequestService.getForPhotographer(photographerId);
    }

    @PutMapping("/{customOfferId}/decision")
    public CustomOfferResponseDTO decide(
            @PathVariable final UUID customOfferId,
            @RequestParam final UUID photographerId,
            @Valid @RequestBody final CustomOfferDecisionRequestDTO request) {

        return this.customOfferRequestService.decide(customOfferId, photographerId, request);
    }

    @DeleteMapping("/{customOfferId}")
    public ResponseEntity<Void> withdraw(
            @PathVariable final UUID customOfferId,
            @RequestParam final UUID clientId) {

        this.customOfferRequestService.withdraw(customOfferId, clientId);
        return ResponseEntity.noContent().build();
    }
}
