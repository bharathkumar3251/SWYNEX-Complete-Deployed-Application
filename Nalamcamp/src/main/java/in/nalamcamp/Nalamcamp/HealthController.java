package in.nalamcamp.Nalamcamp;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> checkHealth() {
        return Map.of(
                "application", "NalamCamp",
                "status", "UP"
        );
    }
}