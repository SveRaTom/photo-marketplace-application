package photomarketplace.customoffer.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "custom_offer_requests")
public class CustomOfferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private UUID clientId;

    @NotNull
    @Column(nullable = false)
    private UUID photographerId;

    @NotNull
    @Column(nullable = false)
    private UUID offerId;

    @NotNull
    @Future
    @Column(nullable = false)
    private LocalDate eventDate;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String location;

    @NotBlank
    @Size(min = 10, max = 2000)
    @Column(nullable = false, length = 2000)
    private String message;

    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(precision = 10, scale = 2)
    private BigDecimal proposedPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CustomOfferStatus status = CustomOfferStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void accept(final BigDecimal proposedPrice) {
        this.proposedPrice = proposedPrice;
        this.status = CustomOfferStatus.ACCEPTED;
    }

    public void decline() {
        this.proposedPrice = null;
        this.status = CustomOfferStatus.DECLINED;
    }

    public void withdraw() {
        this.status = CustomOfferStatus.WITHDRAWN;
    }
}
