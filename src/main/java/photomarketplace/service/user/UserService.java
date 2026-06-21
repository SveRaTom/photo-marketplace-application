package photomarketplace.service.user;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.mapper.user.UserMapper;
import photomarketplace.model.dto.user.UserLoginRequestDTO;
import photomarketplace.model.dto.user.UserRegisterRequestDTO;
import photomarketplace.model.entity.user.User;
import photomarketplace.repository.user.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Autowired
    public UserService(final PasswordEncoder passwordEncoder, final UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public UserDTO login(final UserLoginRequestDTO userLoginRequest) {
        final Optional<User> optionalUser = this.userRepository.findByEmail(userLoginRequest.getEmail());

        if (optionalUser.isEmpty() ||
                !this.passwordEncoder.matches(userLoginRequest.getPassword(), optionalUser.get().getPassword())) {
            throw new RuntimeException("Email or password do not match!");
        }

        return UserMapper.toUserDTO(optionalUser.get());
    }

    public void register(final UserRegisterRequestDTO userRegisterRequest) {
        this.userRepository.findByEmail(userRegisterRequest.getEmail())
                .ifPresent(_ -> {
                    throw new RuntimeException("User with this email already exists!");
                });

        final String encodedPassword = this.passwordEncoder.encode(userRegisterRequest.getPassword());
        userRegisterRequest.setPassword(encodedPassword);

        final User userEntity = UserMapper.toUserEntity(userRegisterRequest);

        this.userRepository.save(userEntity);
    }

    public UserDTO getUserById(final UUID id) {
        return UserMapper.toUserDTO(getUser(id));
    }

    private User getUser(UUID id) {
        return this.userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User with id [%s] does not exist.".formatted(id)));
    }

    public List<UserDTO> getAllUsers() {
        return this.userRepository.findAll().stream()
                .map(UserMapper::toUserDTO)
                .toList();
    }
}
