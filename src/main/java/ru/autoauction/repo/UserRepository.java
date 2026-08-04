package ru.autoauction.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.autoauction.model.AppUser;
import java.util.Optional;
public interface UserRepository extends JpaRepository<AppUser,Long> { Optional<AppUser> findByMaxUserId(Long maxUserId); long countByRegisteredTrue(); }
