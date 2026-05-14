package si.um.feri.smartjobs.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndPasswordHash(String email, String passwordHash);
}