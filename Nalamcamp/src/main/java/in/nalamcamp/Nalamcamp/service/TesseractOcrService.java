package in.nalamcamp.Nalamcamp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TesseractOcrService {

    @Value("${nalamcamp.ocr.tesseract-path:C:/Program Files/Tesseract-OCR/tesseract.exe}")
    private String tesseractPath;

    @Value("${nalamcamp.ocr.tessdata-path:}")
    private String tessdataPath;

    @Value("${nalamcamp.ocr.languages:eng+tam}")
    private String languages;

    public boolean isOcrAvailable() {
        File file = new File(tesseractPath);
        if (file.exists() && file.canExecute()) {
            return true;
        }

        try {
            Process process = new ProcessBuilder("tesseract", "--version").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String getResolvedExecutable() {
        File file = new File(tesseractPath);
        if (file.exists() && file.canExecute()) {
            return file.getAbsolutePath();
        }
        return "tesseract";
    }

    public String extractText(Path imagePath) {
        if (!isOcrAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tesseract OCR is not configured or unavailable on this system. " +
                            "Executable path: '" + tesseractPath + "'. Please configure TESSERACT_PATH with English and Tamil language data.");
        }

        Path outputBase = null;
        Path resultTxt = null;

        try {
            outputBase = Files.createTempFile("ocr_out_", "");
            List<String> command = new ArrayList<>();
            command.add(getResolvedExecutable());
            command.add(imagePath.toAbsolutePath().toString());
            command.add(outputBase.toAbsolutePath().toString());

            if (tessdataPath != null && !tessdataPath.isBlank()) {
                command.add("--tessdata-dir");
                command.add(tessdataPath.strip());
            }

            command.add("-l");
            command.add(languages);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(45, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OCR processing timed out");
            }

            resultTxt = Path.of(outputBase.toAbsolutePath() + ".txt");
            if (Files.exists(resultTxt)) {
                return Files.readString(resultTxt).strip();
            } else {
                return "";
            }
        } catch (IOException | InterruptedException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to run OCR on uploaded image: " + e.getMessage());
        } finally {
            if (outputBase != null) {
                try { Files.deleteIfExists(outputBase); } catch (Exception ignored) {}
            }
            if (resultTxt != null) {
                try { Files.deleteIfExists(resultTxt); } catch (Exception ignored) {}
            }
        }
    }

    public ParsedPosterFields parseFields(String text) {
        if (text == null || text.isBlank()) {
            return new ParsedPosterFields(null, null, null, null);
        }

        LocalDate date = extractDate(text);
        String organizer = extractOrganizer(text);
        String venue = extractVenue(text);
        String campName = extractCampName(text);

        return new ParsedPosterFields(organizer, date, venue, campName);
    }

    private LocalDate extractDate(String text) {
        // Pattern 1: YYYY-MM-DD
        Pattern p1 = Pattern.compile("\\b(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})\\b");
        Matcher m1 = p1.matcher(text);
        if (m1.find()) {
            try {
                int y = Integer.parseInt(m1.group(1));
                int m = Integer.parseInt(m1.group(2));
                int d = Integer.parseInt(m1.group(3));
                return LocalDate.of(y, m, d);
            } catch (Exception ignored) {}
        }

        // Pattern 2: DD-MM-YYYY or DD/MM/YYYY
        Pattern p2 = Pattern.compile("\\b(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})\\b");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) {
            try {
                int d = Integer.parseInt(m2.group(1));
                int m = Integer.parseInt(m2.group(2));
                int y = Integer.parseInt(m2.group(3));
                return LocalDate.of(y, m, d);
            } catch (Exception ignored) {}
        }

        return null;
    }

    private String extractOrganizer(String text) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String clean = line.strip();
            if (clean.toLowerCase().contains("volunteers") ||
                    clean.toLowerCase().contains("trust") ||
                    clean.toLowerCase().contains("foundation") ||
                    clean.toLowerCase().contains("hospital") ||
                    clean.toLowerCase().contains("club") ||
                    clean.toLowerCase().contains("society") ||
                    clean.toLowerCase().contains("organizer") ||
                    clean.toLowerCase().contains("organized by")) {
                return clean.replaceAll("(?i)organized by:?\\s*", "").strip();
            }
        }
        return null;
    }

    private String extractVenue(String text) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String clean = line.strip();
            if (clean.toLowerCase().contains("hall") ||
                    clean.toLowerCase().contains("school") ||
                    clean.toLowerCase().contains("mandapam") ||
                    clean.toLowerCase().contains("center") ||
                    clean.toLowerCase().contains("centre") ||
                    clean.toLowerCase().contains("road") ||
                    clean.toLowerCase().contains("ground") ||
                    clean.toLowerCase().contains("venue:")) {
                return clean.replaceAll("(?i)venue:?\\s*", "").strip();
            }
        }
        return null;
    }

    private String extractCampName(String text) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String clean = line.strip();
            if (clean.toLowerCase().contains("camp") ||
                    clean.contains("முகாம்") ||
                    clean.toLowerCase().contains("check-up") ||
                    clean.toLowerCase().contains("health") ||
                    clean.toLowerCase().contains("dental") ||
                    clean.toLowerCase().contains("eye")) {
                return clean;
            }
        }
        if (lines.length > 0 && !lines[0].isBlank()) {
            return lines[0].strip();
        }
        return null;
    }

    public record ParsedPosterFields(
            String suggestedOrganizer,
            LocalDate suggestedCampDate,
            String suggestedVenue,
            String suggestedCampName
    ) {}
}
