package si.um.feri.smartjobs.seed.job;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.experienceLevel.entity.ExperienceLevel;
import si.um.feri.smartjobs.experienceLevel.repository.ExperienceLevelRepository;
import si.um.feri.smartjobs.job.entity.Job;
import si.um.feri.smartjobs.job.repository.JobRepository;
import si.um.feri.smartjobs.location.entity.Location;
import si.um.feri.smartjobs.location.repository.LocationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class JobSeed {

    private final JobRepository jobRepository;
    private final ExperienceLevelRepository experienceLevelRepository;
    private final LocationRepository locationRepository;

    public JobSeed(JobRepository jobRepository,
                   ExperienceLevelRepository experienceLevelRepository,
                   LocationRepository locationRepository) {
        this.jobRepository = jobRepository;
        this.experienceLevelRepository = experienceLevelRepository;
        this.locationRepository = locationRepository;
    }

    public void seed() {
        if (jobRepository.count() > 0) return;

        var intern = experienceLevelRepository.findById("exp-intern").orElseThrow();
        var entry = experienceLevelRepository.findById("exp-entry").orElseThrow();
        var junior = experienceLevelRepository.findById("exp-junior").orElseThrow();
        var mid = experienceLevelRepository.findById("exp-mid").orElseThrow();
        var senior = experienceLevelRepository.findById("exp-senior").orElseThrow();
        var lead = experienceLevelRepository.findById("exp-lead").orElseThrow();
        var notSpecified = experienceLevelRepository.findById("exp-not-specified").orElseThrow();

        var ljubljana = locationRepository.findById("loc-ljubljana").orElseThrow();
        var maribor = locationRepository.findById("loc-maribor").orElseThrow();
        var celje = locationRepository.findById("loc-celje").orElseThrow();
        var kranj = locationRepository.findById("loc-kranj").orElseThrow();
        var novoMesto = locationRepository.findById("loc-novo-mesto").orElseThrow();
        var koper = locationRepository.findById("loc-koper").orElseThrow();
        var novaGorica = locationRepository.findById("loc-nova-gorica").orElseThrow();
        var murskaSobota = locationRepository.findById("loc-murska-sobota").orElseThrow();
        var slovenia = locationRepository.findById("loc-slovenia").orElseThrow();
        var sofia = locationRepository.findById("loc-sofia").orElseThrow();

        jobRepository.saveAll(List.of(
                job("job-001", "Endava", "Junior Java Developer", "Iščemo junior Java razvijalca z znanjem Spring Boot, SQL, Git in osnovami Dockerja.", 12, "1800", "2800", junior, ljubljana),
                job("job-002", "Comtrade", "Java Backend Developer", "Razvoj backend storitev v Javi, Spring Boot, Hibernate in PostgreSQL.", 36, "2500", "4200", mid, ljubljana),
                job("job-003", "Outfit7", "Senior Java Engineer", "Senior Java engineer za razvoj visoko obremenjenih sistemov, Spring Boot, Docker, Kubernetes in CI/CD.", 60, "4000", "6500", senior, ljubljana),
                job("job-004", "Digital Solutions", "Frontend React Developer", "Razvoj uporabniških vmesnikov z React, JavaScript, TypeScript in Git. Možnost hibridnega dela.", 24, "2200", "3500", mid, ljubljana),
                job("job-005", "VisionApps", "Junior Frontend Developer", "Iščemo začetnika z znanjem JavaScript, React, komunikativnostjo in pripravljenostjo za učenje.", 12, "1500", "2300", junior, maribor),
                job("job-006", "CloudTech", "DevOps Engineer", "Delo z Docker, Kubernetes, CI/CD procesi in podporo razvojnim ekipam.", 36, "3000", "5000", mid, ljubljana),
                job("job-007", "DataLab", "Database Developer", "Delo z SQL, PostgreSQL in MySQL podatkovnimi bazami, optimizacija poizvedb in podatkovni modeli.", 24, "2400", "3800", mid, celje),
                job("job-008", "SmartSoft", "Full Stack Developer", "Full stack razvoj z Java, Spring Boot, React, SQL in Git.", 36, "2800", "4800", mid, ljubljana),
                job("job-009", "CodeFactory", "Intern Software Developer", "Praksa za študenta računalništva: Java, Git, SQL in osnovno reševanje problemov.", 0, "900", "1300", intern, maribor),
                job("job-010", "CyberCore", "Backend Team Lead", "Vodenje backend ekipe, arhitektura Java sistemov, Spring Boot, Docker in komunikacija z naročniki.", 84, "5000", "7500", lead, ljubljana),
                job("job-011", "WebStudio", "Angular Developer", "Razvoj poslovnih aplikacij z Angular, TypeScript, Git in REST API integracijami.", 24, "2300", "3700", mid, koper),
                job("job-012", "AI Labs", "Python Developer", "Razvoj podatkovnih orodij v Pythonu, delo z bazami podatkov in reševanje kompleksnih problemov.", 24, "2500", "4000", mid, ljubljana),

                job("job-013", "UKC Maribor", "Srednja medicinska sestra", "Izvajanje zdravstvene nege, pomoč pacientom, opazovanje stanja pacientov in delo v intenzivni enoti.", 12, null, null, entry, maribor),
                job("job-014", "Dom starejših občanov Ljutomer", "Bolničar negovalec", "Pomoč pri zdravstveni negi, premikanju stanovalcev, opazovanju stanja in osnovni oskrbi.", 12, null, null, entry, murskaSobota),
                job("job-015", "Splošna bolnišnica Murska Sobota", "Zdravstveni administrator", "Administrativna podpora v zdravstvu, priprava dokumentacije, komunikacija s pacienti in urejanje evidenc.", 12, "1300", "1900", entry, murskaSobota),
                job("job-016", "UKC Ljubljana", "Medicinska sestra v intenzivni negi", "Delo v intenzivni negi, patient care, nursing care, spremljanje vitalnih znakov in timsko delo.", 36, null, null, mid, ljubljana),
                job("job-017", "Zdravstveni dom Nova Gorica", "Zdravstveni sodelavec", "Organizacija prevozov pacientov, zdravstvena administracija, komunikacija in koordinacija nalog.", 24, null, null, mid, novaGorica),
                job("job-018", "Lekarna Center", "Farmacevtski tehnik", "Svetovanje pacientom, delo z zdravili, odgovornost, komunikacija in natančnost.", 24, "1600", "2400", mid, ljubljana),

                job("job-019", "Odvetniška zbornica Slovenije", "Strokovni sodelavec", "Pogoji: diploma pravne fakultete, pravniški državni izpit, pravno raziskovanje in dokumentacija.", 24, null, null, mid, ljubljana),
                job("job-020", "Okrožno sodišče v Ljubljani", "Višji pravosodni svetovalec", "Priprava pravnih podlag, odločitev, obrazložitev, court procedures in legal research.", 36, null, null, senior, ljubljana),
                job("job-021", "Upravna enota Celje", "Upravni svetovalec", "Vodenje upravnih postopkov, administrative procedures, javna naročila in komunikacija s strankami.", 24, null, null, mid, celje),
                job("job-022", "LJ Urbanistični zavod", "Sodelavec za razpisno dokumentacijo", "Priprava razpisne dokumentacije, public procurement, analiza zahtev in administrativni postopki.", 24, null, null, mid, ljubljana),

                job("job-023", "WHC", "Senior računovodja", "Priprava letnih poročil, obračun DDV, knjiženje, davčna poročila in spremljanje zakonodaje.", 60, "2200", "3500", senior, ljubljana),
                job("job-024", "Enter Kranj", "Knjigovodja", "Vodenje knjigovodskih evidenc, knjiženje računov, Vasco, DDV in obračun plač.", 24, "1500", "2300", mid, kranj),
                job("job-025", "FinServis", "Junior računovodja", "Pomoč pri računovodstvu, invoicing, bookkeeping, MS Office in komunikacija s strankami.", 12, "1300", "1900", junior, maribor),
                job("job-026", "TaxPro", "Davčni svetovalec", "Tax reporting, VAT, accounting, priprava dokumentacije in svetovanje podjetjem.", 48, "2500", "4200", senior, ljubljana),
                job("job-027", "Payroll Plus", "Specialist za obračun plač", "Payroll, accounting, urejanje evidenc, odgovornost in delo z MS Office.", 36, "1900", "3000", mid, celje),

                job("job-028", "POT paper on track", "Grafični oblikovalec", "Grafično oblikovanje, Adobe Creative Cloud, layout design, fotografiranje in skeniranje.", 24, null, null, mid, ljubljana),
                job("job-029", "Creative Studio", "Junior Graphic Designer", "Oblikovanje materialov, Adobe Creative Cloud, photo editing, teamwork in komunikacija.", 12, "1200", "1800", junior, koper),
                job("job-030", "PrintLab", "Prelamljalec", "Layout design, graphic design, priprava tiskovin, Adobe Creative Cloud in natančnost.", 24, "1400", "2100", mid, celje),

                job("job-031", "Ino Grafično Podjetje", "Tiskar", "Priprava in tisk izdelkov, delo s stroji, nadzor kakovosti in priprava materiala.", null, "1200", "1500", notSpecified, celje),
                job("job-032", "Freudenberg", "Upravljalec strojev", "Pripravljanje in nadziranje delovanja strojev, pakiranje, skladiščenje in quality control.", null, null, null, notSpecified, slovenia),
                job("job-033", "Unior", "Obdelovalec kovin", "Delo v proizvodnji, machine operation, CNC, visual inspection in skrb za kakovost.", 12, "1400", "2200", entry, celje),
                job("job-034", "Kofra", "Procesni kontrolor", "Quality control, visual inspection, preverjanje izdelkov in dokumentiranje kakovosti.", 24, "1600", "2400", mid, kranj),
                job("job-035", "ElektroServis", "Vzdrževalec elektro naprav", "Electrical maintenance, technical maintenance, popravila naprav in preventivno vzdrževanje.", 36, "1800", "2800", mid, maribor),
                job("job-036", "MehanoTech", "Strojni vzdrževalec", "Mechanical maintenance, maintenance, delo s stroji in odpravljanje napak.", 36, "1800", "2700", mid, novoMesto),
                job("job-037", "WHC", "Delavec v proizvodnji", "Assembly, packaging, visual inspection, delo po navodilih in skrb za urejeno delovno okolje.", null, null, null, entry, novoMesto),

                job("job-038", "Mercator", "Prodajalec", "Prodaja blaga, pomoč kupcem, cash register, customer service in urejanje prodajnega prostora.", null, null, null, entry, slovenia),
                job("job-039", "Eurospin", "Prodajalec-blagajničar", "Delo na blagajni, customer service, dopolnjevanje polic in skrb za čistočo.", null, null, null, entry, ljubljana),
                job("job-040", "Lidl", "Prodajalec", "Sales, cash register, customer service, odgovornost in timsko delo.", null, null, null, entry, ljubljana),
                job("job-041", "Ekosistem Plus", "Regionalni komercialist", "Terensko delo, sales, customer service, communication in driving license B.", 24, "1800", "3200", mid, slovenia),
                job("job-042", "LPP Fashion", "Prodajni svetovalec", "Svetovanje kupcem, sales, customer service, komunikacija in urejanje trgovine.", 12, null, null, entry, novoMesto),

                job("job-043", "Splošna bolnišnica Murska Sobota", "Kuhar", "Priprava in kuhanje hrane, food preparation, cooking in skrb za čistočo kuhinje.", 12, null, null, entry, murskaSobota),
                job("job-044", "Agata Gostinstvo", "Natakar", "Serving, customer service, komunikacija z gosti in skrb za urejenost lokala.", null, null, null, entry, maribor),
                job("job-045", "Hotel Mantova", "Dostavljavec hrane", "Delivery, driving license B, komunikacija s strankami in fleksibilen delovni čas.", null, "8", "8", entry, slovenia),

                job("job-046", "Logistika Plus", "Skladiščnik", "Warehouse work, inventory management, packaging, reliability in teamwork.", 12, "1300", "1900", entry, celje),
                job("job-047", "ENLES", "Voznik tovornega vozila", "Truck driving, delivery, odgovornost, reliability in prevoz blaga.", 24, "1700", "2600", mid, slovenia),

                job("job-048", "Osnovna šola Ljubljana", "Učitelj razrednega pouka", "Teaching, classroom management, communication, teamwork in delo z otroki.", 12, null, null, mid, ljubljana),
                job("job-049", "Univerza v Ljubljani", "Visokošolski učitelj", "Teaching, communication, research, project work and classroom management.", 60, null, null, senior, ljubljana),

                job("job-050", "TELUS Digital", "Solutions Consultant s slovenščino in angleščino", "Delo s strankami, Slovenian, English, communication, customer service in plačana relokacija v Sofijo.", 12, null, null, mid, sofia)
        ));
    }

    private Job job(String id,
                    String companyName,
                    String jobName,
                    String description,
                    Integer requiredExperience,
                    String minSalary,
                    String maxSalary,
                    ExperienceLevel experienceLevel,
                    Location location) {
        return new Job(
                id,
                companyName,
                jobName,
                description,
                requiredExperience,
                null,
                null,
                "CareerJet",
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now(),
                minSalary == null ? null : new BigDecimal(minSalary),
                maxSalary == null ? null : new BigDecimal(maxSalary),
                experienceLevel,
                location
        );
    }
}