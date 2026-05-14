package si.um.feri.smartjobs.seed.user;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.educationLevel.repository.EducationLevelRepository;
import si.um.feri.smartjobs.experienceLevel.repository.ExperienceLevelRepository;
import si.um.feri.smartjobs.user.entity.User;
import si.um.feri.smartjobs.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;

@Component
public class UserSeed {

    private final UserRepository userRepository;
    private final EducationLevelRepository educationLevelRepository;
    private final ExperienceLevelRepository experienceLevelRepository;

    public UserSeed(UserRepository userRepository,
                    EducationLevelRepository educationLevelRepository,
                    ExperienceLevelRepository experienceLevelRepository) {
        this.userRepository = userRepository;
        this.educationLevelRepository = educationLevelRepository;
        this.experienceLevelRepository = experienceLevelRepository;
    }

    public void seed() {
        if (userRepository.count() > 0) return;

        var bachelor = educationLevelRepository.findById("edu-bachelor").orElseThrow();
        var master = educationLevelRepository.findById("edu-master").orElseThrow();
        var secondaryVocational = educationLevelRepository.findById("edu-secondary-vocational").orElseThrow();
        var higherVocational = educationLevelRepository.findById("edu-higher-vocational").orElseThrow();

        var junior = experienceLevelRepository.findById("exp-junior").orElseThrow();
        var mid = experienceLevelRepository.findById("exp-mid").orElseThrow();
        var senior = experienceLevelRepository.findById("exp-senior").orElseThrow();
        var entry = experienceLevelRepository.findById("exp-entry").orElseThrow();

        userRepository.saveAll(List.of(
               new User("user-001", "Ana", "Novak", LocalDate.of(2002, 3, 14), "dummy-hash", "ana.it@example.com", 12, LocalDate.now(), LocalDate.now(), bachelor, junior),
               new User("user-002", "Luka", "Kovač", LocalDate.of(1998, 5, 20), "dummy-hash", "luka.backend@example.com", 48, LocalDate.now(), LocalDate.now(), bachelor, mid),
               new User("user-003", "Maja", "Krajnc", LocalDate.of(1999, 6, 2), "dummy-hash", "maja.health@example.com", 24, LocalDate.now(), LocalDate.now(), secondaryVocational, entry),
               new User("user-004", "Marko", "Horvat", LocalDate.of(1995, 11, 20), "dummy-hash", "marko.accounting@example.com", 60, LocalDate.now(), LocalDate.now(), master, senior),
               new User("user-005", "Sara", "Zupan", LocalDate.of(2000, 1, 9), "dummy-hash", "sara.design@example.com", 24, LocalDate.now(), LocalDate.now(), bachelor, mid),
               new User("user-006", "Nina", "Kos", LocalDate.of(2001, 8, 12), "dummy-hash", "nina.sales@example.com", 12, LocalDate.now(), LocalDate.now(), secondaryVocational, entry),
               new User("user-007", "Tomaž", "Mlakar", LocalDate.of(1994, 4, 5), "dummy-hash", "tomaz.production@example.com", 72, LocalDate.now(), LocalDate.now(), higherVocational, senior),
               new User("user-008", "Eva", "Vidmar", LocalDate.of(1997, 9, 30), "dummy-hash", "eva.legal@example.com", 36, LocalDate.now(), LocalDate.now(), master, mid),
               new User("user-009", "Jan", "Rozman", LocalDate.of(2003, 2, 18), "dummy-hash", "jan.student@example.com", 0, LocalDate.now(), LocalDate.now(), bachelor, entry),
               new User("user-010", "Petra", "Medved", LocalDate.of(1996, 7, 7), "dummy-hash", "petra.teacher@example.com", 48, LocalDate.now(), LocalDate.now(), master, mid),
               new User("user-011", "Matej", "Pirc", LocalDate.of(1993, 12, 1), "dummy-hash", "matej.driver@example.com", 84, LocalDate.now(), LocalDate.now(), secondaryVocational, senior),
               new User("user-012", "Iva", "Janeva", LocalDate.of(2004, 10, 11), "dummy-hash", "iva.demo@example.com", 12, LocalDate.now(), LocalDate.now(), bachelor, junior)    ));
    }
}