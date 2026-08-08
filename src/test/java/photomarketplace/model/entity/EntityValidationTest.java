package photomarketplace.model.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import photomarketplace.model.entity.booking.Booking;
import photomarketplace.model.entity.offer.Offer;
import photomarketplace.model.entity.photo.Photo;
import photomarketplace.model.entity.review.Review;
import photomarketplace.model.entity.user.User;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class EntityValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @ParameterizedTest(name = "{index}: {1} must not be null")
    @MethodSource("requiredEntityProperties")
    void requiredEntityPropertyShouldRejectNull(final Object entity, final String propertyName) {
        final Set<ConstraintViolation<Object>> violations = validator.validateProperty(entity, propertyName);

        assertTrue(violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(propertyName)));
    }

    private static Stream<Arguments> requiredEntityProperties() {
        return Stream.of(
                arguments(Booking.builder().build(), "status"),
                arguments(Booking.builder().build(), "client"),
                arguments(Booking.builder().build(), "offer"),
                arguments(Offer.builder().build(), "photographer"),
                arguments(Photo.builder().build(), "offer"),
                arguments(Review.builder().build(), "rating"),
                arguments(Review.builder().build(), "author"),
                arguments(Review.builder().build(), "offer"),
                arguments(Review.builder().build(), "booking"),
                arguments(User.builder().build(), "role")
        );
    }
}
