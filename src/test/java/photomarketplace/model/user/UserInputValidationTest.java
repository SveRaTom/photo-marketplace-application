package photomarketplace.model.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import photomarketplace.model.dto.user.UserLoginRequestDTO;
import photomarketplace.model.dto.user.UserRegisterRequestDTO;
import photomarketplace.model.entity.user.User;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class UserInputValidationTest {

    private static final String EMAIL_SUFFIX = "@example.com";

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

    @ParameterizedTest(name = "{index}: {0}.{1} rejects a value over its maximum length")
    @MethodSource("valuesOverMaximumLength")
    void valueOverMaximumLengthShouldViolateSizeConstraint(
            final Class<?> modelType,
            final String propertyName,
            final String value) {

        final Set<? extends ConstraintViolation<?>> violations = validateValue(modelType, propertyName, value);

        assertTrue(hasSizeViolation(violations));
    }

    @ParameterizedTest(name = "{index}: {0}.{1} accepts its maximum length")
    @MethodSource("valuesAtMaximumLength")
    void valueAtMaximumLengthShouldNotViolateSizeConstraint(
            final Class<?> modelType,
            final String propertyName,
            final String value) {

        final Set<? extends ConstraintViolation<?>> violations = validateValue(modelType, propertyName, value);

        assertFalse(hasSizeViolation(violations));
    }

    private static Stream<Arguments> valuesOverMaximumLength() {
        return Stream.of(
                arguments(UserRegisterRequestDTO.class, "username", "u".repeat(51)),
                arguments(UserRegisterRequestDTO.class, "email", emailOfLength(255)),
                arguments(UserRegisterRequestDTO.class, "password", "p".repeat(73)),
                arguments(UserLoginRequestDTO.class, "email", emailOfLength(255)),
                arguments(UserLoginRequestDTO.class, "password", "p".repeat(73)),
                arguments(User.class, "username", "u".repeat(51)),
                arguments(User.class, "email", emailOfLength(255))
        );
    }

    private static Stream<Arguments> valuesAtMaximumLength() {
        return Stream.of(
                arguments(UserRegisterRequestDTO.class, "username", "u".repeat(50)),
                arguments(UserRegisterRequestDTO.class, "email", emailOfLength(254)),
                arguments(UserRegisterRequestDTO.class, "password", "p".repeat(72)),
                arguments(UserLoginRequestDTO.class, "email", emailOfLength(254)),
                arguments(UserLoginRequestDTO.class, "password", "p".repeat(72)),
                arguments(User.class, "username", "u".repeat(50)),
                arguments(User.class, "email", emailOfLength(254))
        );
    }

    private static Set<? extends ConstraintViolation<?>> validateValue(
            final Class<?> modelType,
            final String propertyName,
            final String value) {

        return validator.validateValue(modelType, propertyName, value);
    }

    private static boolean hasSizeViolation(final Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .anyMatch(violation -> violation.getConstraintDescriptor()
                        .getAnnotation()
                        .annotationType()
                        .equals(Size.class));
    }

    private static String emailOfLength(final int length) {
        return "a".repeat(length - EMAIL_SUFFIX.length()) + EMAIL_SUFFIX;
    }
}
