package ru.autoauction.repo;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.autoauction.model.Bid;
import java.util.*;
public interface BidRepository extends JpaRepository<Bid,Long> {
  @EntityGraph(attributePaths="user") List<Bid> findByLotIdOrderByCreatedAtDesc(Long lotId);
  @EntityGraph(attributePaths={"lot","lot.media"}) List<Bid> findByUserIdOrderByCreatedAtDesc(Long userId);
  @EntityGraph(attributePaths="user") Optional<Bid> findFirstByLotIdOrderByAmountDescCreatedAtAsc(Long lotId);
  long countByLotId(Long lotId);
  long countByUserId(Long userId);
  @Modifying @Query("delete from Bid b where b.lot.id=:lotId") int deleteByLotId(@Param("lotId") Long lotId);
  @Query("select count(distinct b.user.id) from Bid b where b.lot.id=:lotId") long countParticipantsByLotId(@Param("lotId") Long lotId);
  @Query("select coalesce(sum(b.amount),0) from Bid b") long totalBidVolume();
}
