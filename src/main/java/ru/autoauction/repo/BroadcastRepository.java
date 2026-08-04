package ru.autoauction.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.autoauction.model.BroadcastLog;
public interface BroadcastRepository extends JpaRepository<BroadcastLog,Long> {}
