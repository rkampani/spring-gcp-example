package com.example.springgcpexample.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;


@Component
public class DependencyVersionVerifier {

    private static final Log log = LogFactory.getLog(DependencyVersionVerifier.class);
    private static final List<String> SUPPORTED_VERSIONS = Arrays.asList("3.2", "3.3", "3.4", "3.5");
    // Simplified compatibility matrix: {Spring Boot, Spring Cloud, spring-cloud-gcp}
    private static final Map<String, Map<String, String>> COMPATIBILITY_MATRIX = new HashMap<>();

    static {
        // Compatible combinations
        COMPATIBILITY_MATRIX.put("3.2", new HashMap<>() {{
            put("2023.0", "5.9.0"); // Spring Boot 3.2.x, Spring Cloud 2023.0.x, spring-cloud-gcp 5.x
        }});
        COMPATIBILITY_MATRIX.put("3.3", new HashMap<>() {{
            put("2024.0", "4.10.0"); // Spring Boot 3.3.x, Spring Cloud 2024.0.x, spring-cloud-gcp 4.x
        }});
        COMPATIBILITY_MATRIX.put("3.4", new HashMap<>() {{
            put("2024.0", "4.10.0"); // Spring Boot 3.4.x, Spring Cloud 2024.0.x, spring-cloud-gcp 4.x
        }});
    }

    private final List<String> acceptedBootVersions;
    private final boolean compatibilityCheckEnabled;
    public DependencyVersionVerifier(
            @Value("${spring.cloud.compatibility-verifier.enabled:true}") boolean compatibilityCheckEnabled
    ) {
        this.acceptedBootVersions = Collections.unmodifiableList(SUPPORTED_VERSIONS);
        this.compatibilityCheckEnabled = compatibilityCheckEnabled;
    }

    public VerificationResult verify() {
        if (!compatibilityCheckEnabled) {
            log.info("Compatibility verification is disabled via spring.cloud.compatibility-verifier.enabled=false");
            return VerificationResult.compatible();
        }
        final String bootVersion = getSpringBootVersion();
        final String cloudVersion = getSpringCloudVersion();
        final String gcpVersion = getSpringCloudGcpVersion();

        log.info("Detected versions: Spring Boot [" + bootVersion + "], Spring Cloud [" + cloudVersion + "], spring-cloud-gcp [" + gcpVersion + "]");

        final String strippedBootVersion = normalizeVersion(bootVersion);
        if (!acceptedBootVersions.contains(strippedBootVersion)) {
            return VerificationResult.notCompatible(
                    "Spring Boot [" + bootVersion + "] is not in accepted versions: " + acceptedBootVersions,
                    action()
            );
        }

        final Map<String, String> compatibleCloudVersions = COMPATIBILITY_MATRIX.get(strippedBootVersion);
        if (compatibleCloudVersions == null || !compatibleCloudVersions.containsKey(normalizeVersion(cloudVersion))) {
            return VerificationResult.notCompatible(
                    "Spring Cloud [" + cloudVersion + "] is not compatible with Spring Boot [" + bootVersion + "]",
                    action()
            );
        }

        final String expectedGcpVersion = compatibleCloudVersions.get(normalizeVersion(cloudVersion));
        if (!normalizeVersion(gcpVersion).equals(normalizeVersion(expectedGcpVersion))) {
            return VerificationResult.notCompatible(
                    "spring-cloud-gcp [" + gcpVersion + "] is not compatible with Spring Boot [" + bootVersion + "] and Spring Cloud [" + cloudVersion + "]. Expected version: [" + expectedGcpVersion + "]",
                    action()
            );
        }

        return VerificationResult.compatible();
    }

    private String getSpringBootVersion() {
        final String version = SpringBootVersion.getVersion();
        return StringUtils.hasText(version) ? version : "unknown";
    }

    private String getSpringCloudVersion() {
        try {
           final Package pkg = org.springframework.cloud.commons.util.IdUtils.class.getPackage();
            String version = pkg.getImplementationVersion();
            return StringUtils.hasText(version) ? version : "unknown";
        } catch (Exception e) {
            log.warn("Cannot determine Spring Cloud version: " + e.getMessage());
            return "unknown";
        }
    }

    private String getSpringCloudGcpVersion() {
        try {
            // Use a known class from spring-cloud-gcp to get version
            final Package pkg = com.google.cloud.spring.core.GcpProjectIdProvider.class.getPackage();
            final String version = pkg.getImplementationVersion();
            return StringUtils.hasText(version) ? version : "unknown";
        } catch (Exception e) {
            log.warn("Cannot determine spring-cloud-gcp version: " + e.getMessage());
            return "unknown";
        }
    }

    private String action() {
        return String.format(
                "Update your dependencies to a compatible combination.\n" +
                        "See the Spring Cloud GCP compatibility matrix: [https://github.com/GoogleCloudPlatform/spring-cloud-gcp#compatibility-with-spring-project-versions]\n" +
                        "Learn more about Spring Boot: [https://spring.io/projects/spring-boot#learn]\n" +
                        "Learn more about Spring Cloud: [https://spring.io/projects/spring-cloud#overview]\n" +
                        "To disable this check, set: [spring.cloud.compatibility-verifier.enabled=false]",
                acceptedBootVersions
        );
    }
    private static String normalizeVersion(String version) {
        if (!StringUtils.hasText(version) || "unknown".equals(version)) {
            return version == null ? "" : version;
        }
        return stripMinorVersion(stripWildCardFromVersion(version));
    }
    private static String stripMinorVersion(String version) {
        if (!StringUtils.hasText(version) || "unknown".equals(version)) {
            return version == null ? "" : version;
        }
        int firstDot = version.indexOf('.');
        int secondDot = version.indexOf('.', firstDot + 1);
        return secondDot > 0 ? version.substring(0, secondDot) : version;
    }

    private static String stripWildCardFromVersion(String version) {
        if (version.endsWith(".x")) {
            return version.substring(0, version.indexOf(".x"));
        }
        return version;
    }

    // VerificationResult interface
    public interface VerificationResult {
        static VerificationResult compatible() {
            return new VerificationResult() {
                @Override
                public boolean isCompatible() {
                    return true;
                }
            };
        }

        static VerificationResult notCompatible(String error, String action) {
            return new VerificationResult() {
                @Override
                public boolean isCompatible() {
                    return false;
                }

                @Override
                public String getError() {
                    return error;
                }

                @Override
                public String getAction() {
                    return action;
                }
            };
        }

        boolean isCompatible();
        default String getError() { return null; }
        default String getAction() { return null; }
    }
}