package club.axinyue.easydeal.tempuser;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UsernameProvider {
    private static final String DEFAULT_LOCALE = "zh";
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, List<String>> usernameCache = new ConcurrentHashMap<>();

    public String randomUsername(Locale locale) {
        String localeKey = resolveLocaleKey(locale);
        List<String> names = usernameCache.computeIfAbsent(localeKey, this::loadUsernames);
        String name = names.get(secureRandom.nextInt(names.size()));
        return limitUsername(name + "-" + randomSuffix());
    }

    public String resolveLocaleKey(Locale locale) {
        if (locale == null || locale.getLanguage() == null || locale.getLanguage().isBlank()) {
            return DEFAULT_LOCALE;
        }

        String languageTag = locale.toLanguageTag();
        if (resourceExists(languageTag)) {
            return languageTag;
        }
        if (resourceExists(locale.getLanguage())) {
            return locale.getLanguage();
        }
        return DEFAULT_LOCALE;
    }

    private List<String> loadUsernames(String localeKey) {
        ClassPathResource resource = new ClassPathResource("usernames/usernames_" + localeKey + ".txt");
        if (!resource.exists()) {
            resource = new ClassPathResource("usernames/usernames_" + DEFAULT_LOCALE + ".txt");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> names = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith("#"))
                    .toList();
            if (names.isEmpty()) {
                throw new IllegalStateException("Username resource is empty: " + resource.getPath());
            }
            return names;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load username resource: " + resource.getPath(), e);
        }
    }

    private boolean resourceExists(String localeKey) {
        return new ClassPathResource("usernames/usernames_" + localeKey + ".txt").exists();
    }

    private String randomSuffix() {
        int value = secureRandom.nextInt(0x1000000);
        return String.format("%06x", value);
    }

    private String limitUsername(String username) {
        if (username.length() <= 64) {
            return username;
        }
        return username.substring(0, 57) + "-" + randomSuffix();
    }
}
