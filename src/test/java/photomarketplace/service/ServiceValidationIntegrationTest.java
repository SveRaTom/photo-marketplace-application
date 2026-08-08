package photomarketplace.service;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import photomarketplace.model.dto.customoffer.CustomOfferDecisionRequestDTO;
import photomarketplace.model.dto.customoffer.CustomOfferRequestDTO;
import photomarketplace.model.dto.user.ProfileUpdateDTO;
import photomarketplace.model.dto.user.UserRegisterRequestDTO;
import photomarketplace.model.dto.user.UserRoleUpdateDTO;
import photomarketplace.service.booking.BookingService;
import photomarketplace.service.customoffer.CustomOfferService;
import photomarketplace.service.offer.OfferService;
import photomarketplace.service.photo.PhotoService;
import photomarketplace.service.review.ReviewService;
import photomarketplace.service.user.AdminUserService;
import photomarketplace.service.user.UserService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ServiceValidationIntegrationTest {

    @Autowired
    private OfferService offerService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private CustomOfferService customOfferService;

    @SuppressWarnings("DataFlowIssue")
    @Test
    void offerServiceShouldRejectNullRequest() {
        assertThrows(
                ConstraintViolationException.class,
                () -> this.offerService.createOffer(null, UUID.randomUUID())
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void bookingServiceShouldRejectNullRequest() {
        assertThrows(
                ConstraintViolationException.class,
                () -> this.bookingService.createBooking(UUID.randomUUID(), null, UUID.randomUUID())
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void photoServiceShouldRejectNullRequest() {
        assertThrows(
                ConstraintViolationException.class,
                () -> this.photoService.createPhoto(UUID.randomUUID(), null, UUID.randomUUID())
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void reviewServiceShouldRejectNullRequest() {
        assertThrows(
                ConstraintViolationException.class,
                () -> this.reviewService.createReview(UUID.randomUUID(), null, UUID.randomUUID())
        );
    }

    @Test
    void userServiceShouldRejectInvalidRegistrationRequest() {
        final UserRegisterRequestDTO invalidRequest = UserRegisterRequestDTO.builder().build();

        assertThrows(
                ConstraintViolationException.class,
                () -> this.userService.register(invalidRequest)
        );
    }

    @Test
    void userServiceShouldRejectInvalidProfileUpdate() {
        final ProfileUpdateDTO invalidRequest = ProfileUpdateDTO.builder().build();

        assertThrows(
                ConstraintViolationException.class,
                () -> this.userService.updateProfile(UUID.randomUUID(), invalidRequest)
        );
    }

    @Test
    void adminUserServiceShouldRejectInvalidRoleUpdate() {
        final UserRoleUpdateDTO invalidRequest = UserRoleUpdateDTO.builder().build();

        assertThrows(
                ConstraintViolationException.class,
                () -> this.adminUserService.updateUserRole(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        invalidRequest)
        );
    }

    @Test
    void customOfferServiceShouldRejectInvalidCreateRequest() {
        final CustomOfferRequestDTO invalidRequest = CustomOfferRequestDTO.builder().build();

        assertThrows(
                ConstraintViolationException.class,
                () -> this.customOfferService.createCustomOffer(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        invalidRequest)
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void customOfferServiceShouldRejectInvalidDecisionRequest() {
        final CustomOfferDecisionRequestDTO invalidRequest = new CustomOfferDecisionRequestDTO(null, null);

        assertThrows(
                ConstraintViolationException.class,
                () -> this.customOfferService.decideCustomOffer(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        invalidRequest)
        );
    }
}
