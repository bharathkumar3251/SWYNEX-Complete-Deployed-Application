package in.nalamcamp.Nalamcamp.repository;

import in.nalamcamp.Nalamcamp.PosterCheck;
import in.nalamcamp.Nalamcamp.PosterCheck.PosterCheckStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PosterCheckRepository extends JpaRepository<PosterCheck, Long> {

    Optional<PosterCheck> findByPosterCheckId(String posterCheckId);

    List<PosterCheck> findByStatus(PosterCheckStatus status);
}
