package club.axinyue.easydeal.tempuser;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class TempUserRepository {
    private final JdbcTemplate jdbcTemplate;

    public TempUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TempUser> findByTokenHash(String tokenHash) {
        return jdbcTemplate.query("""
                        SELECT id, token_hash, username, locale, created_at, updated_at
                        FROM temp_users
                        WHERE token_hash = ?
                        """,
                (rs, rowNum) -> new TempUser(
                        rs.getLong("id"),
                        rs.getString("token_hash"),
                        rs.getString("username"),
                        rs.getString("locale"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                ),
                tokenHash
        ).stream().findFirst();
    }

    public TempUser create(String tokenHash, String username, String locale) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO temp_users (token_hash, username, locale)
                    VALUES (?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, tokenHash);
            statement.setString(2, username);
            statement.setString(3, locale);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        Long id = key == null ? null : key.longValue();
        Instant now = Instant.now();
        return new TempUser(id, tokenHash, username, locale, now, now);
    }
}
