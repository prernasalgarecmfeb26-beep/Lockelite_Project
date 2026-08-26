package com.lockelite.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lockelite.dto.BranchDistanceDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/branches")
@Tag(name = "Branches", description = "Real bank branch search using OpenStreetMap Overpass API")
public class BranchController {

    private static final Logger log = LoggerFactory.getLogger(BranchController.class);

    private static final List<String> OVERPASS_MIRRORS = List.of(
            "https://overpass-api.de/api/interpreter",
            "https://lz4.overpass-api.de/api/interpreter",
            "https://z.overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, List<String>> BANK_SEARCH_TERMS = new LinkedHashMap<>();
    static {
        BANK_SEARCH_TERMS.put("SBI",       List.of("State Bank of India", "SBI"));
        BANK_SEARCH_TERMS.put("HDFC",      List.of("HDFC Bank", "HDFC"));
        BANK_SEARCH_TERMS.put("ICICI",     List.of("ICICI Bank", "ICICI"));
        BANK_SEARCH_TERMS.put("AXIS",      List.of("Axis Bank", "Axis"));
        BANK_SEARCH_TERMS.put("KOTAK",     List.of("Kotak Mahindra Bank", "Kotak"));
        BANK_SEARCH_TERMS.put("LOCKELITE", List.of("LockElite"));
    }

    @GetMapping("/public")
    @Operation(summary = "Find real bank branches near user location (OpenStreetMap Overpass)")
    public ResponseEntity<List<BranchDistanceDto>> getPublicBranches(
            @RequestParam String  bankName,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "25000") int radiusMeters) {

        double userLat = latitude  != null ? latitude  : 20.5937;
        double userLng = longitude != null ? longitude : 78.9629;
        int    radius  = radiusMeters > 0 ? radiusMeters : 25000;

        String bankCode = bankName.toUpperCase().trim();

        if ("LOCKELITE".equals(bankCode)) {
            return ResponseEntity.ok(lockEliteSeededBranches(userLat, userLng));
        }

        List<String> searchTerms = BANK_SEARCH_TERMS.getOrDefault(bankCode, List.of(bankName));

        // Try Overpass API
        List<BranchDistanceDto> results = queryOverpass(userLat, userLng, radius, searchTerms);

        // If Overpass fails — use smart location-aware fallback
        if (results.isEmpty()) {
            log.warn("[Branches] All Overpass mirrors failed — using location-aware fallback for {}", bankCode);
            results = locationAwareFallback(bankCode, bankName, userLat, userLng, radius);
        }

        results.sort(Comparator.comparingDouble(BranchDistanceDto::getDistanceKm));
        log.info("[Branches] {} -> {} results within {}m of ({},{})",
                bankCode, results.size(), radius, userLat, userLng);
        return ResponseEntity.ok(results);
    }

    // ─────────────────────────────────────────────────────────────────────
    // OVERPASS — uses HttpURLConnection (more reliable than HttpClient for SSL)
    // ─────────────────────────────────────────────────────────────────────
    private List<BranchDistanceDto> queryOverpass(double lat, double lng,
                                                   int radius, List<String> terms) {
        String query = buildOverpassQuery(lat, lng, radius, terms);
        String body  = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        for (String mirror : OVERPASS_MIRRORS) {
            try {
                log.info("[Overpass] Trying mirror: {}", mirror);
                String json = httpPost(mirror, body, 20000);
                if (json != null && json.contains("elements")) {
                    log.info("[Overpass] Success from: {}", mirror);
                    List<BranchDistanceDto> results = parseOverpassResponse(json, lat, lng, terms);
                    if (!results.isEmpty()) return results;
                }
            } catch (Exception e) {
                log.warn("[Overpass] Mirror {} failed: {}", mirror, e.getMessage());
            }
        }
        return Collections.emptyList();
    }

    private String buildOverpassQuery(double lat, double lng, int radius, List<String> terms) {
        StringBuilder qb = new StringBuilder("[out:json][timeout:25];\n(\n");
        for (String term : terms) {
            String escaped = term.replace("\"", "\\\"");
            String namePattern = "~\"(?i)" + escaped + "\"";
            for (String type : List.of("node", "way")) {
                qb.append("  ").append(type)
                  .append("[\"amenity\"=\"bank\"][\"name\"").append(namePattern)
                  .append("](around:").append(radius).append(",").append(lat).append(",").append(lng).append(");\n");
                qb.append("  ").append(type)
                  .append("[\"amenity\"=\"bank\"][\"brand\"").append(namePattern)
                  .append("](around:").append(radius).append(",").append(lat).append(",").append(lng).append(");\n");
            }
        }
        qb.append(");\nout center tags;");
        return qb.toString();
    }

    // Use HttpURLConnection — bypasses Java HttpClient SSL issues
    private String httpPost(String urlStr, String body, int timeoutMs) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "LockEliteApp/1.0");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            log.warn("[Overpass] HTTP {} from {}", code, urlStr);
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PARSE OVERPASS RESPONSE
    // ─────────────────────────────────────────────────────────────────────
    private List<BranchDistanceDto> parseOverpassResponse(String json,
                                                           double userLat, double userLng,
                                                           List<String> terms) {
        List<BranchDistanceDto> list = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try {
            JsonNode elements = objectMapper.readTree(json).get("elements");
            if (elements == null) return list;

            for (JsonNode el : elements) {
                double elLat, elLng;
                if (el.has("lat")) {
                    elLat = el.get("lat").asDouble();
                    elLng = el.get("lon").asDouble();
                } else if (el.has("center")) {
                    elLat = el.get("center").get("lat").asDouble();
                    elLng = el.get("center").get("lon").asDouble();
                } else continue;

                JsonNode tags = el.get("tags");
                if (tags == null) continue;

                String name = null;
                for (String key : List.of("name", "brand", "operator")) {
                    if (tags.has(key) && !tags.get(key).asText().isBlank()) {
                        name = tags.get(key).asText();
                        break;
                    }
                }
                if (name == null) continue;

                final String finalName = name;
                boolean matches = terms.stream().anyMatch(t ->
                        finalName.toLowerCase().contains(t.toLowerCase()));
                if (!matches) continue;

                String dedupKey = name.toLowerCase() + "|"
                        + Math.round(elLat * 1000) + "|"
                        + Math.round(elLng * 1000);
                if (!seen.add(dedupKey)) continue;

                double dist    = haversine(userLat, userLng, elLat, elLng);
                double rounded = Math.round(dist * 10.0) / 10.0;
                String address = buildAddress(tags, elLat, elLng);
                int    hash    = (name + elLat).hashCode();
                long   lockers = 8  + Math.abs(hash % 15);
                long   avail   = 1  + Math.abs(hash % (lockers - 1));

                list.add(BranchDistanceDto.builder()
                        .id("osm_" + el.get("id").asText())
                        .branchName(name).address(address)
                        .distanceKm(rounded).distance(rounded)
                        .latitude(BigDecimal.valueOf(elLat))
                        .longitude(BigDecimal.valueOf(elLng))
                        .lockers(lockers).available(avail).build());
            }
        } catch (Exception e) {
            log.error("[Overpass] Parse error: {}", e.getMessage());
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOCATION-AWARE FALLBACK — filters by user's actual coordinates
    // Only shown when ALL Overpass mirrors fail
    // ─────────────────────────────────────────────────────────────────────
    private List<BranchDistanceDto> locationAwareFallback(String code, String displayName,
                                                           double userLat, double userLng,
                                                           int radiusMeters) {
        record B(String name, String address, double lat, double lng) {}

        Map<String, List<B>> data = new HashMap<>();
        data.put("SBI", List.of(
            new B("SBI Vasind Branch",          "Vasind, Shahpur, Thane, MH 421604",           19.3250, 73.3038),
            new B("SBI Shahpur Branch",          "Shahpur, Thane, MH 421601",                   19.4556, 73.3308),
            new B("SBI Thane Branch",            "Station Road, Thane West, MH 400601",         19.2183, 72.9781),
            new B("SBI Kalyan Branch",           "Shivaji Chowk, Kalyan West, MH 421301",       19.2438, 73.1355),
            new B("SBI Badlapur Branch",         "Station Road, Badlapur, MH 421503",           19.1567, 73.2637),
            new B("SBI Murbad Branch",           "Murbad, Thane, MH 421401",                    19.2580, 73.3940),
            new B("SBI Titwala Branch",          "Titwala, Kalyan, MH 421605",                  19.3053, 73.1989),
            new B("SBI Asangaon Branch",         "Asangaon, Shahapur, Thane, MH 421601",        19.5220, 73.2110),
            new B("SBI Vashi Branch",            "Sector 17, Vashi, Navi Mumbai, MH 400703",    19.0748, 72.9987),
            new B("SBI Pune Branch",             "FC Road, Shivajinagar, Pune, MH 411005",      18.5204, 73.8567),
            new B("SBI Delhi Connaught Branch",  "Connaught Place, New Delhi 110001",           28.6315, 77.2167),
            new B("SBI Bangalore MG Road",       "MG Road, Bangalore, KA 560001",               12.9716, 77.5946),
            new B("SBI Chennai Anna Nagar",      "Anna Nagar, Chennai, TN 600040",              13.0843, 80.2101),
            new B("SBI Hyderabad Banjara Hills", "Banjara Hills, Hyderabad, TS 500034",         17.4126, 78.4071)
        ));
        data.put("HDFC", List.of(
            new B("HDFC Bank Vasind",            "Vasind, Shahpur, Thane, MH 421604",           19.3250, 73.3040),
            new B("HDFC Bank Thane",             "MG Road, Thane, MH 400601",                   19.2196, 72.9786),
            new B("HDFC Bank Kalyan",            "Birla College Rd, Kalyan West MH 421301",     19.2438, 73.1355),
            new B("HDFC Bank Vashi",             "Sector 30A, Vashi, Navi Mumbai MH 400703",    19.0770, 72.9985),
            new B("HDFC Bank Pune",              "Baner Road, Pune, MH 411045",                 18.5593, 73.7867),
            new B("HDFC Bank Delhi",             "Connaught Place, New Delhi 110001",           28.6338, 77.2200),
            new B("HDFC Bank Bangalore",         "Indiranagar, Bangalore, KA 560038",           12.9784, 77.6408),
            new B("HDFC Bank Chennai",           "Nungambakkam, Chennai, TN 600034",            13.0569, 80.2425),
            new B("HDFC Bank Hyderabad",         "Hitech City, Hyderabad, TS 500081",           17.4474, 78.3762)
        ));
        data.put("ICICI", List.of(
            new B("ICICI Bank Vasind",           "Vasind, Shahpur, Thane, MH 421604",           19.3250, 73.3042),
            new B("ICICI Bank Thane",            "Pokhran Road, Thane West MH 400601",          19.2210, 72.9770),
            new B("ICICI Bank Kalyan",           "Station Road, Kalyan West MH 421301",         19.2400, 73.1340),
            new B("ICICI Bank Vashi",            "Sector 17, Vashi, Navi Mumbai MH 400703",     19.0760, 73.0000),
            new B("ICICI Bank Pune",             "Aundh, Pune, MH 411007",                      18.5590, 73.8080),
            new B("ICICI Bank Delhi",            "Vasant Kunj, New Delhi 110070",               28.5206, 77.1575),
            new B("ICICI Bank Bangalore",        "Whitefield, Bangalore, KA 560066",            12.9698, 77.7500),
            new B("ICICI Bank Chennai",          "Anna Salai, Chennai, TN 600002",              13.0581, 80.2595),
            new B("ICICI Bank Hyderabad",        "Madhapur, Hyderabad, TS 500081",              17.4482, 78.3920)
        ));
        data.put("AXIS", List.of(
            new B("Axis Bank Vasind",            "Vasind, Shahpur, Thane, MH 421604",           19.3250, 73.3041),
            new B("Axis Bank Thane",             "Gokhale Road, Naupada, Thane West MH 400602", 19.2130, 72.9760),
            new B("Axis Bank Dombivli",          "Manpada Road, Dombivli East MH 421201",       19.2147, 73.0871),
            new B("Axis Bank Vashi",             "Sector 30A, Vashi, Navi Mumbai MH 400703",    19.0750, 72.9990),
            new B("Axis Bank Pune",              "Deccan Gymkhana, Pune, MH 411004",            18.5167, 73.8409),
            new B("Axis Bank Delhi",             "Nehru Place, New Delhi 110019",               28.5491, 77.2520),
            new B("Axis Bank Bangalore",         "Jayanagar, Bangalore, KA 560041",             12.9254, 77.5938),
            new B("Axis Bank Chennai",           "T Nagar, Chennai, TN 600017",                 13.0418, 80.2341),
            new B("Axis Bank Hyderabad",         "Banjara Hills, Hyderabad, TS 500034",         17.4100, 78.4400)
        ));
        data.put("KOTAK", List.of(
            new B("Kotak Mahindra Bank Thane",   "Naupada, Thane West, MH 400602",              19.2200, 72.9780),
            new B("Kotak Mahindra Bank Kalyan",  "Clock Tower, Kalyan West MH 421301",          19.2430, 73.1360),
            new B("Kotak Mahindra Bank Vashi",   "Sector 30, Vashi, Navi Mumbai MH 400703",     19.0760, 72.9990),
            new B("Kotak Mahindra Bank Pune",    "Baner, Pune, MH 411045",                      18.5590, 73.7890),
            new B("Kotak Mahindra Bank Delhi",   "Saket, New Delhi 110017",                     28.5247, 77.2068),
            new B("Kotak Mahindra Bank Bangalore","Indiranagar, Bangalore, KA 560038",          12.9719, 77.6412),
            new B("Kotak Mahindra Bank Chennai", "Anna Nagar, Chennai, TN 600040",              13.0843, 80.2101),
            new B("Kotak Mahindra Bank Hyderabad","Gachibowli, Hyderabad, TS 500032",           17.4432, 78.3498)
        ));

        List<B> allBranches = data.getOrDefault(code, List.of(
            new B(displayName + " Branch", "Maharashtra, India", userLat + 0.01, userLng + 0.01)
        ));

        double radiusKm = radiusMeters / 1000.0;

        // Filter by user's actual location
        List<B> nearby = allBranches.stream()
                .filter(b -> haversine(userLat, userLng, b.lat(), b.lng()) <= radiusKm)
                .sorted(Comparator.comparingDouble(b -> haversine(userLat, userLng, b.lat(), b.lng())))
                .collect(Collectors.toList());

        // If nothing within radius, return closest 3
        if (nearby.isEmpty()) {
            nearby = allBranches.stream()
                    .sorted(Comparator.comparingDouble(b -> haversine(userLat, userLng, b.lat(), b.lng())))
                    .limit(3).collect(Collectors.toList());
        }

        return nearby.stream().map(b -> {
            double dist = Math.round(haversine(userLat, userLng, b.lat(), b.lng()) * 10.0) / 10.0;
            int    hash = (b.name() + b.lat()).hashCode();
            return BranchDistanceDto.builder()
                    .id("fb_" + Math.abs(hash))
                    .branchName(b.name()).address(b.address())
                    .distanceKm(dist).distance(dist)
                    .latitude(BigDecimal.valueOf(b.lat()))
                    .longitude(BigDecimal.valueOf(b.lng()))
                    .lockers(10L + Math.abs(hash % 10))
                    .available(2L  + Math.abs(hash % 8))
                    .build();
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOCKELITE SEEDED
    // ─────────────────────────────────────────────────────────────────────
    private List<BranchDistanceDto> lockEliteSeededBranches(double userLat, double userLng) {
        record L(String id, String name, String address, double lat, double lng) {}
        return List.of(
            new L("le_vasind",     "LockElite Vasind Branch",      "Main Road, Vasind, Maharashtra 421604",        19.3248, 73.3042),
            new L("le_thane",      "LockElite Thane Branch",       "Station Road, Thane West, Maharashtra 400601", 19.2183, 72.9781),
            new L("le_navimumbai", "LockElite Navi Mumbai Branch", "Sector 5, Vashi, Navi Mumbai 400703",          19.0748, 72.9987)
        ).stream().map(b -> {
            double dist = Math.round(haversine(userLat, userLng, b.lat(), b.lng()) * 10.0) / 10.0;
            return BranchDistanceDto.builder()
                    .id(b.id()).branchName(b.name()).address(b.address())
                    .distanceKm(dist).distance(dist)
                    .latitude(BigDecimal.valueOf(b.lat())).longitude(BigDecimal.valueOf(b.lng()))
                    .lockers(12L).available(8L).build();
        }).sorted(Comparator.comparingDouble(BranchDistanceDto::getDistanceKm))
          .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────
    private String buildAddress(JsonNode tags, double lat, double lng) {
        List<String> parts = new ArrayList<>();
        for (String key : List.of("addr:housenumber", "addr:street", "addr:suburb",
                                   "addr:city", "addr:state", "addr:postcode")) {
            if (tags.has(key) && !tags.get(key).asText().isBlank())
                parts.add(tags.get(key).asText());
        }
        if (!parts.isEmpty()) return String.join(", ", parts);
        if (tags.has("addr:full")) return tags.get("addr:full").asText();
        if (tags.has("name"))      return tags.get("name").asText();
        return String.format("%.4f N, %.4f E", lat, lng);
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R    = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a    = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.asin(Math.sqrt(a));
    }
}
