package in.nalamcamp.Nalamcamp.repository;

import in.nalamcamp.Nalamcamp.CampSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CampSessionRepository extends JpaRepository<CampSession, Long> {

    List<CampSession> findByCamp_IdOrderByStartTimeAsc(Long campId);


    @Query(
            value = "SELECT * FROM camp_sessions WHERE id = :id FOR UPDATE",
            nativeQuery = true
    )
    Optional<CampSession> findByIdForUpdate(@Param("id") Long id);
}
