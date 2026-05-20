package si.um.feri.smartjobs.cv.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class CvTextExtractionService {

    private final Tika tika = new Tika();

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CV file is required.");
        }

        try {
            return tika.parseToString(file.getInputStream()).trim();
        } catch (IOException | TikaException e) {
            throw new IllegalStateException("Could not extract text from CV file.", e);
        }
    }
}
