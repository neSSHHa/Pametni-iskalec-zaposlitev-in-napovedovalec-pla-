package si.um.feri.smartjobs.location.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.location.entity.Location;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, String> {
    List<Location> findByCityContainingIgnoreCase(String city);
    List<Location> findByRegionContainingIgnoreCase(String region);
    List<Location> findByCountryContainingIgnoreCase(String country);
}