package ru.autoauction.repo;
import org.springframework.data.jpa.repository.*;
import ru.autoauction.model.*;
import java.util.*;
import java.time.Instant;
public interface LotRepository extends JpaRepository<Lot,Long> {
  List<Lot> findAllByOrderByCreatedAtDesc();
  Optional<Lot> findFirstByStatusOrderByEndsAtAsc(LotStatus status);
  List<Lot> findByStatusAndStartsAtLessThanEqualAndEndsAtGreaterThanOrderByEndsAtAsc(LotStatus status, Instant startedBefore, Instant endsAfter);
  long countByStatus(LotStatus status);
}
