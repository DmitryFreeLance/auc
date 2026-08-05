package ru.autoauction.repo;
import org.springframework.data.jpa.repository.*;
import ru.autoauction.model.Bid;
import java.util.*;
public interface BidRepository extends JpaRepository<Bid,Long> {
  @EntityGraph(attributePaths="user") List<Bid> findByLotIdOrderByCreatedAtDesc(Long lotId);
  @EntityGraph(attributePaths="lot") List<Bid> findByUserIdOrderByCreatedAtDesc(Long userId);
  @EntityGraph(attributePaths="user") Optional<Bid> findFirstByLotIdOrderByAmountDescCreatedAtAsc(Long lotId);
  long countByLotId(Long lotId);
  long countByUserId(Long userId);
  @Query("select coalesce(sum(b.amount),0) from Bid b") long totalBidVolume();
}
