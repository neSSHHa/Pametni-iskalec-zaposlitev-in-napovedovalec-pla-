import { useLayoutEffect, useRef } from "react";
import * as am5 from "@amcharts/amcharts5";
import * as am5map from "@amcharts/amcharts5/map";
import am5themes_Animated from "@amcharts/amcharts5/themes/Animated";
import am5geodata_worldLow from "@amcharts/amcharts5-geodata/worldLow";
import { europeCountryIds } from "../../data/motionJobs.js";

const countryAliases = {
  GB: ["GB", "UK"],
  UK: ["GB", "UK"],
};

const SLOVENIA_CENTER = { latitude: 46.1512, longitude: 14.9955 };
const SLOVENIA_ZOOM = 64;

function countryMatches(cityCountry, countryCode) {
  if (!countryCode) return true;
  const aliases = countryAliases[countryCode] || [countryCode];
  return aliases.includes(cityCountry);
}

export default function EuropeJobsMap({ countries, cities }) {
  const chartRef = useRef(null);
  const apiRef = useRef(null);

  useLayoutEffect(() => {
    if (!chartRef.current) return undefined;

    const root = am5.Root.new(chartRef.current);
    root.setThemes([am5themes_Animated.new(root)]);

    const chart = root.container.children.push(
      am5map.MapChart.new(root, {
        projection: am5map.geoMercator(),
        panX: "translateX",
        panY: "translateY",
        wheelY: "none",
        wheelX: "none",
        pinchZoom: true,
        minZoomLevel: 2.25,
        maxZoomLevel: 84,
        homeZoomLevel: SLOVENIA_ZOOM,
        homeGeoPoint: SLOVENIA_CENTER,
      }),
    );

    const polygonSeries = chart.series.push(
      am5map.MapPolygonSeries.new(root, {
        geoJSON: am5geodata_worldLow,
        include: europeCountryIds,
      }),
    );

    polygonSeries.mapPolygons.template.setAll({
      tooltipText: "{name}",
      interactive: true,
      cursorOverStyle: "pointer",
      fill: am5.color(0xe7edf3),
      stroke: am5.color(0xaebdca),
      strokeWidth: 0.8,
      templateField: "polygonSettings",
    });

    polygonSeries.mapPolygons.template.states.create("hover", {
      fill: am5.color(0xffffff),
      stroke: am5.color(0x69f5ff),
      strokeWidth: 1.4,
    });

    polygonSeries.data.setAll(
      countries.map((country) => ({
        id: country.mapId,
        name: country.name,
        polygonSettings: {
          fill: am5.color(country.color),
          fillOpacity: 0.86,
        },
      })),
    );

    const countryPoints = chart.series.push(
      am5map.MapPointSeries.new(root, {
        latitudeField: "lat",
        longitudeField: "lng",
      }),
    );

    countryPoints.bullets.push((root, _series, dataItem) => {
      const context = dataItem.dataContext;
      const container = am5.Container.new(root, {
        centerX: am5.p50,
        centerY: am5.p50,
        cursorOverStyle: "pointer",
        tooltipText: `${context.name}: ${context.jobs} jobs`,
      });

      container.children.push(
        am5.Circle.new(root, {
          radius: 18,
          fill: am5.color(0xffffff),
          stroke: am5.color(context.color),
          strokeWidth: 3,
        }),
      );

      container.children.push(
        am5.Label.new(root, {
          text: "{jobs}",
          populateText: true,
          centerX: am5.p50,
          centerY: am5.p50,
          fontWeight: "800",
          fontSize: 12,
          fill: am5.color(0x17212b),
        }),
      );

      container.events.on("click", () => {
        chart.zoomToGeoPoint({ latitude: context.lat, longitude: context.lng }, 16, true, 720);
        window.setTimeout(() => showCities(context.code), 220);
      });

      return am5.Bullet.new(root, { sprite: container });
    });

    countryPoints.data.setAll(countries);

    const cityPoints = chart.series.push(
      am5map.MapPointSeries.new(root, {
        latitudeField: "lat",
        longitudeField: "lng",
      }),
    );
    const cityPointData = [...cities].sort((a, b) => Number(a.jobs || 0) - Number(b.jobs || 0));

    cityPoints.bullets.push((root, _series, dataItem) => {
      const context = dataItem.dataContext;
      const jobs = Number(context.jobs || 0);
      const radius = jobs >= 1000 ? 25 : jobs >= 200 ? 18 : 14;
      const container = am5.Container.new(root, {
        centerX: am5.p50,
        centerY: am5.p50,
        zIndex: jobs,
        tooltipText: `${context.name}: ${context.jobs} jobs`,
      });

      container.children.push(
        am5.Circle.new(root, {
          radius,
          fill: am5.color(context.color),
          stroke: am5.color(0xffffff),
          strokeWidth: 3,
        }),
      );

      container.children.push(
        am5.Label.new(root, {
          text: "{jobs}",
          populateText: true,
          centerX: am5.p50,
          centerY: am5.p50,
          fontSize: jobs >= 1000 ? 12 : 11,
          fontWeight: "800",
          fill: am5.color(0xffffff),
        }),
      );

      return am5.Bullet.new(root, { sprite: container });
    });

    cityPoints.data.setAll(cityPointData);
    cityPoints.hide(0);

    let cityMode = false;
    let activeCountryCode = null;
    const showCities = (countryCode) => {
      activeCountryCode = countryCode || activeCountryCode;
      const visibleCities = activeCountryCode
        ? cityPointData.filter((city) => countryMatches(city.country, activeCountryCode))
        : cityPointData;

      cityPoints.data.setAll(visibleCities);
      cityPoints.show(240);
      countryPoints.hide(220);
      cityMode = true;
    };

    const showCountries = () => {
      countryPoints.show(240);
      cityPoints.hide(220);
      activeCountryCode = null;
      cityMode = false;
    };

    polygonSeries.mapPolygons.template.events.on("click", (event) => {
      const context = event.target.dataItem?.dataContext;
      const country = countries.find((item) => item.mapId === context?.id);

      if (event.target.dataItem) {
        polygonSeries.zoomToDataItem(event.target.dataItem, false);
      }

      if (country) {
        window.setTimeout(() => {
          chart.zoomToGeoPoint({ latitude: country.lat, longitude: country.lng }, 16, true, 560);
          showCities(country.code);
        }, 220);
      }
    });

    chart.on("zoomLevel", (zoomLevel) => {
      const level = zoomLevel ?? 0;
      if (level > 5 && !cityMode) showCities();
      if (level < 3.25 && cityMode) showCountries();
    });

    const node = chartRef.current;
    const wheel = { delta: 0, last: 0, timer: 0 };
    const onWheel = (event) => {
      if (event.target.closest(".map-controls")) return;
      event.preventDefault();
      window.clearTimeout(wheel.timer);
      wheel.delta += Math.sign(event.deltaY) * Math.min(Math.abs(event.deltaY), 110);
      wheel.timer = window.setTimeout(() => {
        wheel.delta = 0;
      }, 180);

      const now = Date.now();
      if (Math.abs(wheel.delta) < 90 || now - wheel.last < 90) return;

      const currentZoom = chart.get("zoomLevel") ?? 2.75;
      const nextZoom = Math.min(Math.max(currentZoom * Math.exp((-wheel.delta / 300) * Math.log(2)), 2.25), 84);
      const rect = node.getBoundingClientRect();

      chart.zoomToPoint({ x: event.clientX - rect.left, y: event.clientY - rect.top }, nextZoom, false, 170);
      wheel.delta = 0;
      wheel.last = now;
    };

    node.addEventListener("wheel", onWheel, { passive: false });

    polygonSeries.events.on("datavalidated", () => {
      chart.goHome(0);
      window.setTimeout(() => {
        chart.zoomToGeoPoint(SLOVENIA_CENTER, SLOVENIA_ZOOM, true, 500);
      }, 120);
    });
    chart.appear(800, 100);

    apiRef.current = {
      reset: () => {
        showCountries();
        chart.zoomToGeoPoint(SLOVENIA_CENTER, SLOVENIA_ZOOM, true, 650);
      },
      zoomIn: () => chart.zoomToGeoPoint(chart.geoPoint(), Math.min((chart.get("zoomLevel") ?? 2.75) * 2, 84), true, 260),
      zoomOut: () => chart.zoomToGeoPoint(chart.geoPoint(), Math.max((chart.get("zoomLevel") ?? 2.75) / 1.75, 2.25), true, 260),
    };

    return () => {
      window.clearTimeout(wheel.timer);
      node.removeEventListener("wheel", onWheel);
      apiRef.current = null;
      root.dispose();
    };
  }, [cities, countries]);

  return (
    <div className="motion-map">
      <div className="map-controls">
        <button type="button" onClick={() => apiRef.current?.zoomIn()}>+</button>
        <button type="button" onClick={() => apiRef.current?.zoomOut()}>-</button>
        <button type="button" onClick={() => apiRef.current?.reset()}>Slovenia</button>
      </div>
      <div className="map-help">Click a country, scroll to zoom</div>
      <div className="map-canvas" ref={chartRef}></div>
    </div>
  );
}
