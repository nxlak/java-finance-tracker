package backend.academy.scrapper.repository;

import backend.academy.scrapper.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
