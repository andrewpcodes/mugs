package com.overmild.mugs.repositories;

import com.overmild.mugs.model.Mug;
import com.overmild.mugs.model.MugHistoryEntry;
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
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MugRepositoryImplementation(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Mug> findById(String mugId) {
        List<Mug> mugs = jdbcTemplate.query(
                """
                        SELECT id, owner_user_id, purchase_date, renewal_date, cost, size_ounces, created_at, updated_at
                        FROM mugs
                        WHERE id == :id
                        """,
                Map.of("id", mugId),
                mugRowMapper()
        );
        return mugs.stream().findFirst();
    }

    @Override
    public List<MugHistoryEntry> findHistoryByMugId(String mugId) {
        return jdbcTemplate.query(
                """
                        SELECT id, mug_id, added_by_user_id, entry, created_at
                        FROM mug_history
                        WHERE mug_id == :mugId
                        ORDER BY created_at DESC
                        """,
                Map.of("mugId", mugId),
                historyRowMapper()
        );
    }

    @Override
    public List<Mug> findByOwnerUserId(String ownerUserId) {
        return jdbcTemplate.query(
                """
                        SELECT id, owner_user_id, purchase_date, renewal_date, cost, size_ounces, created_at, updated_at
                        FROM mugs
                        WHERE owner_user_id == :ownerUserId
                        ORDER BY created_at DESC
                        """,
                Map.of("ownerUserId", ownerUserId),
                mugRowMapper()
        );
    }

    @Override
    public void createMug(Mug mug) {
        jdbcTemplate.update(
                """
                        INSERT INTO mugs (id, owner_user_id, purchase_date, renewal_date, cost, size_ounces, created_at, updated_at)
                        VALUES (:id, :ownerUserId, :purchaseDate, :renewalDate, :cost, :sizeOunces, :createdAt, :updatedAt)
                        """,
                new MapSqlParameterSource()
                        .addValue("id", mug.id())
                        .addValue("ownerUserId", mug.ownerUserId())
                        .addValue("purchaseDate", mug.purchaseDate().toString())
                        .addValue("renewalDate", mug.renewalDate().toString())
                        .addValue("cost", mug.cost())
                        .addValue("sizeOunces", mug.sizeOunces())
                        .addValue("createdAt", mug.createdAt().toString())
                        .addValue("updatedAt", mug.updatedAt().toString())
        );
    }

    @Override
    public boolean updateMugByOwner(
            String mugId,
            String ownerUserId,
            LocalDate purchaseDate,
            LocalDate renewalDate,
            BigDecimal cost,
            BigDecimal sizeOunces
    ) {
        int updatedRows = jdbcTemplate.update(
                """
                        UPDATE mugs
                        SET purchase_date = :purchaseDate,
                            renewal_date = :renewalDate,
                            cost = :cost,
                            size_ounces = :sizeOunces,
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
                        .addValue("updatedAt", Instant.now().toString())
        );
        return updatedRows > 0;
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
    public void addHistoryEntry(MugHistoryEntry historyEntry) {
        jdbcTemplate.update(
                """
                        INSERT INTO mug_history (id, mug_id, added_by_user_id, entry, created_at)
                        VALUES (:id, :mugId, :addedByUserId, :entry, :createdAt)
                        """,
                new MapSqlParameterSource()
                        .addValue("id", historyEntry.id())
                        .addValue("mugId", historyEntry.mugId())
                        .addValue("addedByUserId", historyEntry.addedByUserId())
                        .addValue("entry", historyEntry.entry())
                        .addValue("createdAt", historyEntry.createdAt().toString())
        );
    }

    private RowMapper<Mug> mugRowMapper() {
        return (resultSet, rowNum) -> new Mug(
                resultSet.getString("id"),
                resultSet.getString("owner_user_id"),
                LocalDate.parse(resultSet.getString("purchase_date")),
                LocalDate.parse(resultSet.getString("renewal_date")),
                resultSet.getBigDecimal("cost"),
                resultSet.getBigDecimal("size_ounces"),
                parseInstant(resultSet, "created_at"),
                parseInstant(resultSet, "updated_at")
        );
    }

    private RowMapper<MugHistoryEntry> historyRowMapper() {
        return (resultSet, rowNum) -> new MugHistoryEntry(
                resultSet.getString("id"),
                resultSet.getString("mug_id"),
                resultSet.getString("added_by_user_id"),
                resultSet.getString("entry"),
                parseInstant(resultSet, "created_at")
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
