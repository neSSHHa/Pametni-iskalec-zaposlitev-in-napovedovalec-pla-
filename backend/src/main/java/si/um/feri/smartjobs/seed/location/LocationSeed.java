package si.um.feri.smartjobs.seed.location;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.location.entity.Location;
import si.um.feri.smartjobs.location.repository.LocationRepository;

import java.math.BigDecimal;
import java.util.List;

@Component
public class LocationSeed {

    private final LocationRepository repository;

    public LocationSeed(LocationRepository repository) {
        this.repository = repository;
    }

    public void seed() {
        if (repository.count() > 0) return;

        repository.saveAll(List.of(
                new Location("loc-ljubljana", null, "Ljubljana", "Central Slovenia", "Slovenia", new BigDecimal("46.0569"), new BigDecimal("14.5058")),
                new Location("loc-maribor", null, "Maribor", "Drava", "Slovenia", new BigDecimal("46.5547"), new BigDecimal("15.6459")),
                new Location("loc-celje", null, "Celje", "Savinja", "Slovenia", new BigDecimal("46.2397"), new BigDecimal("15.2677")),
                new Location("loc-kranj", null, "Kranj", "Upper Carniola", "Slovenia", new BigDecimal("46.2389"), new BigDecimal("14.3556")),
                new Location("loc-novo-mesto", null, "Novo mesto", "Southeast Slovenia", "Slovenia", new BigDecimal("45.8011"), new BigDecimal("15.1710")),
                new Location("loc-koper", null, "Koper", "Coastal-Karst", "Slovenia", new BigDecimal("45.5481"), new BigDecimal("13.7302")),
                new Location("loc-nova-gorica", null, "Nova Gorica", "Gorizia", "Slovenia", new BigDecimal("45.9560"), new BigDecimal("13.6484")),
                new Location("loc-murska-sobota", null, "Murska Sobota", "Mura", "Slovenia", new BigDecimal("46.6625"), new BigDecimal("16.1664")),
                new Location("loc-slovenia", null, null, null, "Slovenia", null, null),
                new Location("loc-sofia", null, "Sofia", "Sofia City", "Bulgaria", new BigDecimal("42.6977"), new BigDecimal("23.3219"))
        ));
    }
}