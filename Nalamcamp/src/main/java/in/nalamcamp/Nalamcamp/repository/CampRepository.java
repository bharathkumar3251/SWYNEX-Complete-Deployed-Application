package in.nalamcamp.Nalamcamp.repository;

import in.nalamcamp.Nalamcamp.Camp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface CampRepository extends JpaRepository<Camp, Long> {

    @Modifying
    @Query("""
       update Camp c
       set c.status = 'APPROVED'
       where c.id = :campId and c.status = 'PENDING_REVIEW'
       """)
    int approvePendingCamp(@Param("campId") Long campId);

    List<Camp> findByStatus(String status);

    List<Camp> findByOrganizer_IdOrderByIdDesc(Long organizerId);

    List<Camp> findByStatusIn(Collection<String> statuses);

    @Query("""
       SELECT DISTINCT c FROM Camp c LEFT JOIN c.sessions s
       WHERE (c.status = 'APPROVED' OR c.status = 'CANCELLED')
         AND (:area IS NULL OR LOWER(c.area) LIKE LOWER(CONCAT('%', :area, '%')))
         AND (:campDate IS NULL OR c.campDate = :campDate)
         AND (:service IS NULL OR LOWER(s.serviceName) LIKE LOWER(CONCAT('%', :service, '%')))
       ORDER BY c.campDate ASC
       """)
    List<Camp> findPublicCamps(
            @Param("area") String area,
            @Param("campDate") LocalDate campDate,
            @Param("service") String service);
}
