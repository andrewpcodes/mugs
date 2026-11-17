package com.overmild.mugs.repositories;

import com.overmild.mugs.models.entities.MugEntity;
import com.overmild.mugs.models.entities.MugFillEntity;
import com.overmild.mugs.models.entities.MugHistoryEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MugRepositoryImplementation implements MugRepository {
    // Callers append WHERE, then GROUP BY m.id, then ORDER BY.
    private static final String MUG_SELECT = """
            SELECT m.id, m.owner_user_id, m.purchase_date, m.renewal_date, m.cost, m.size_ounces, m.created_at, m.updated_at, m.nickname,
                   COUNT(f.id) AS fill_count
            FROM mugs m
            LEFT JOIN mug_fills f ON f.mug_id = m.id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MugRepositoryImplementation(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<List<MugEntity>> getAllMugs() {
        return Optional.of(jdbcTemplate.query(
                MUG_SELECT + """
                        GROUP BY m.id
                        ORDER BY m.created_at DESC
                        """,
                mugRowMapper()
        ));
    }

    @Override
    public Optional<MugEntity> findById(String mugId) {
        List<MugEntity> mugEntities = jdbcTemplate.query(
                MUG_SELECT + """
                        WHERE m.id == :id
                        GROUP BY m.id
                        """,
                Map.of("id", mugId),
                mugRowMapper()
        );
        return mugEntities.stream().findFirst();
    }

    @Override
    public List<MugHistoryEntity> findHistoryByMugId(String mugId) {
        return jdbcTemplate.query(
                """
                        SELECT id, mug_id, added_by_user_id, entry, price, created_at
                        FROM mug_history
                        WHERE mug_id == :mugId
                        ORDER BY created_at DESC
                        """,
                Map.of("mugId", mugId),
                historyRowMapper()
        );
    }

    @Override
    public List<MugEntity> findByOwnerUserId(String ownerUserId) {
        return jdbcTemplate.query(
                MUG_SELECT + """
                        WHERE m.owner_user_id == :ownerUserId
                        GROUP BY m.id
                        ORDER BY m.created_at DESC
                        """,
                Map.of("ownerUserId", ownerUserId),
                mugRowMapper()
        );
    }

    @Override
    public void createMug(MugEntity mugEntity) {
        jdbcTemplate.update(
                """
                        INSERT INTO mugs (id, owner_user_id, purchase_date, renewal_date, cost, size_ounces, created_at, updated_at)
                        VALUES (:id, :ownerUserId, :purchaseDate, :renewalDate, :cost, :sizeOunces, :createdAt, :updatedAt)
                        """,
                new MapSqlParameterSource()
                        .addValue("id", mugEntity.id())
                        .addValue("ownerUserId", mugEntity.ownerUserId())
                        .addValue("purchaseDate", mugEntity.purchaseDate().toString())
                        .addValue("renewalDate", mugEntity.renewalDate().toString())
                        .addValue("cost", mugEntity.cost())
                        .addValue("sizeOunces", mugEntity.sizeOunces())
                        .addValue("createdAt", mugEntity.createdAt().toString())
                        .addValue("updatedAt", mugEntity.updatedAt().toString())
        );
    }

    @Override
    public Optional<MugEntity> updateMugByOwner(
            String mugId,
            String ownerUserId,
            LocalDate purchaseDate,
            LocalDate renewalDate,
            BigDecimal cost,
            BigDecimal sizeOunces,
            String nickname,
            boolean keepNickname,
            Instant updatedAt
    ) {
        int updatedRows = jdbcTemplate.update(
                """
                        UPDATE mugs
                        SET purchase_date = :purchaseDate,
                            renewal_date = :renewalDate,
                            cost = :cost,
                            size_ounces = :sizeOunces,
                            nickname = CASE WHEN :keepNickname THEN nickname ELSE :nickname END,
                            updated_at = :updatedAt
                        WHERE id == :mugId
                          AND owner_user_id = :ownerUserId
                        """,
                new MapSqlParameterSource()
                        .addValue("mugId", mugId)
                        .addValue("ownerUserId", ownerUserId)
                        .addValue("purchaseDate", purchaseDate.toString())
                        .addValue("renewalDate", renewalDate.toString())
                        .addValue("cost", cost)
                        .addValue("sizeOunces", sizeOunces)
                        .addValue("nickname", nickname)
                        .addValue("keepNickname", keepNickname)
                        .addValue("updatedAt", updatedAt.toString())
        );
        return updatedRows > 0 ? findById(mugId) : Optional.empty();
    }

    @Override
    public boolean deleteMugByOwner(String mugId, String ownerUserId) {
        int deletedRows = jdbcTemplate.update(
                """
                        DELETE FROM mugs
                        WHERE id == :mugId
                          AND owner_user_id = :ownerUserId
                        """,
                Map.of("mugId", mugId, "ownerUserId", ownerUserId)
        );
        return deletedRows > 0;
    }

    @Override
    public void addHistoryEntry(MugHistoryEntity historyEntry) {
        jdbcTemplate.update(
                """
                        INSERT INTO mug_history (id, mug_id, added_by_user_id, entry, price, created_at)
                        VALUES (:id, :mugId, :addedByUserId, :entry, :price, :createdAt)
                        """,
                new MapSqlParameterSource()
                        .addValue("id", historyEntry.id())
                        .addValue("mugId", historyEntry.mugId())
                        .addValue("addedByUserId", historyEntry.addedByUserId())
                        .addValue("entry", historyEntry.entry())
                        .addValue("price", historyEntry.price())
                        .addValue("createdAt", historyEntry.createdAt().toString())
        );
    }

    @Override
    public void addFill(MugFillEntity fill) {
        jdbcTemplate.update(
                """
                        INSERT INTO mug_fills (id, mug_id, filled_by_user_id, fill_date, amount_ounces, created_at)
                        VALUES (:id, :mugId, :filledByUserId, :fillDate, :amountOunces, :createdAt)
                        """,
                new MapSqlParameterSource()
                        .addValue("id", fill.id())
                        .addValue("mugId", fill.mugId())
                        .addValue("filledByUserId", fill.filledByUserId())
                        .addValue("fillDate", fill.fillDate().toString())
                        .addValue("amountOunces", fill.amountOunces())
                        .addValue("createdAt", fill.createdAt().toString())
        );
    }

    private RowMapper<MugEntity> mugRowMapper() {
        return (resultSet, rowNum) -> new MugEntity(
                resultSet.getString("id"),
                resultSet.getString("owner_user_id"),
                LocalDate.parse(resultSet.getString("purchase_date")),
                LocalDate.parse(resultSet.getString("renewal_date")),
                resultSet.getBigDecimal("cost"),
                resultSet.getBigDecimal("size_ounces"),
                parseInstant(resultSet, "created_at"),
                parseInstant(resultSet, "updated_at"),
                resultSet.getString("nickname"),
                resultSet.getInt("fill_count")
        );
    }

    private RowMapper<MugHistoryEntity> historyRowMapper() {
        return (resultSet, rowNum) -> new MugHistoryEntity(
                resultSet.getString("id"),
                resultSet.getString("mug_id"),
                resultSet.getString("added_by_user_id"),
                resultSet.getString("entry"),
                parseInstant(resultSet, "created_at"),
                resultSet.getBigDecimal("price")
        );
    }

    private Instant parseInstant(ResultSet resultSet, String columnName) throws SQLException {
        String rawValue = resultSet.getString(columnName);
        try {
            return Instant.parse(rawValue);
        } catch (DateTimeParseException ignored) {
            LocalDateTime localDateTime = LocalDateTime.parse(rawValue, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
    }
}
