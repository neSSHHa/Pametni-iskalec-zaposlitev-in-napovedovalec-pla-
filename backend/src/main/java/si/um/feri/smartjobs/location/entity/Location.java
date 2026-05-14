package si.um.feri.smartjobs.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "Location")
public class Location {

    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 150)
    private String cityDistrict;

    @Column(length = 150)
    private String city;

    @Column(length = 150)
    private String region;

    @Column(length = 150)
    private String country;

    private BigDecimal latitude;
    private BigDecimal longitude;

    public Location() {}

    public Location(String id, String cityDistrict, String city, String region, String country,
                    BigDecimal latitude, BigDecimal longitude) {
        this.id = id;
        this.cityDistrict = cityDistrict;
        this.city = city;
        this.region = region;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public String getCityDistrict() { return cityDistrict; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getCountry() { return country; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
}