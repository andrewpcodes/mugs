package com.overmild.mugs.repositories

import org.flywaydb.core.Flyway
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import spock.lang.Specification

import java.nio.file.Files

class MugRepositoryImplementationSpec extends Specification {

    // A database created before Flyway, matching V1__initial_schema.sql.
    static final String OLD_SCHEMA = '''
            CREATE TABLE mug_owners (id TEXT PRIMARY KEY, username TEXT NOT NULL UNIQUE, email TEXT NOT NULL UNIQUE,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);
            CREATE TABLE mugs (id TEXT PRIMARY KEY, owner_user_id TEXT NOT NULL, purchase_date TEXT NOT NULL,
                renewal_date TEXT NOT NULL, cost NUMERIC NOT NULL, size_ounces NUMERIC NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (owner_user_id) REFERENCES mug_owners (id) ON DELETE CASCADE);
            CREATE TABLE mug_history (id TEXT PRIMARY KEY, mug_id TEXT NOT NULL, added_by_user_id TEXT NOT NULL,
                entry TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (mug_id) REFERENCES mugs (id) ON DELETE CASCADE);
            CREATE TABLE mug_fills (id TEXT PRIMARY KEY, mug_id TEXT NOT NULL, filled_by_user_id TEXT NOT NULL,
                fill_date TEXT NOT NULL, amount_ounces NUMERIC NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (mug_id) REFERENCES mugs (id) ON DELETE CASCADE);
            '''

    SingleConnectionDataSource dataSource
    JdbcTemplate jdbcTemplate
    MugRepositoryImplementation repository

    def setup() {
        def file = Files.createTempFile("mugs-test", ".db")
        file.toFile().deleteOnExit()
        dataSource = new SingleConnectionDataSource("jdbc:sqlite:${file}", true)
        jdbcTemplate = new JdbcTemplate(dataSource)
        jdbcTemplate.execute("PRAGMA foreign_keys=ON")
        repository = new MugRepositoryImplementation(new NamedParameterJdbcTemplate(dataSource))
    }

    def cleanup() {
        dataSource.destroy()
    }

    private void migrate() {
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate()
    }

    private List<String> columns(String table) {
        jdbcTemplate.queryForList("PRAGMA table_info(${table})".toString()).collect { it.name as String }
    }

    private void insertOwner(String id) {
        jdbcTemplate.update("INSERT INTO mug_owners (id, username, email) VALUES (?, ?, ?)", id, id, "${id}@example.com".toString())
    }

    private void insertMug(String id, String ownerId, String createdAt = "2026-01-01T00:00:00Z") {
        jdbcTemplate.update(
                "INSERT INTO mugs (id, owner_user_id, purchase_date, renewal_date, cost, size_ounces, created_at, updated_at) VALUES (?, ?, '2025-01-15', '2026-01-15', 18.99, 12, ?, ?)",
                id, ownerId, createdAt, createdAt
        )
    }

    private void insertFills(String mugId, int count) {
        count.times { i ->
            jdbcTemplate.update(
                    "INSERT INTO mug_fills (id, mug_id, filled_by_user_id, fill_date, amount_ounces) VALUES (?, ?, 'owner-1', '2026-11-16', 12)",
                    "${mugId}-fill-${i}".toString(), mugId
            )
        }
    }

    private void insertHistory(String id, String mugId, String createdAt) {
        jdbcTemplate.update(
                "INSERT INTO mug_history (id, mug_id, added_by_user_id, entry, created_at) VALUES (?, ?, 'owner-1', 'Stout', ?)",
                id, mugId, createdAt
        )
    }

    private void givenTwoMugsWithFills() {
        migrate()
        insertOwner("owner-1")
        insertMug("mug-empty", "owner-1", "2026-01-01T00:00:00Z")
        insertMug("mug-filled", "owner-1", "2026-01-02T00:00:00Z")
        insertFills("mug-filled", 3)
        insertHistory("hist-1", "mug-empty", "2026-01-03T00:00:00Z")
    }

    def "migrations upgrade a database created before Flyway and keep existing rows"() {
        given:
        OLD_SCHEMA.split(';').findAll { it.trim() }.each { jdbcTemplate.execute(it) }
        jdbcTemplate.update("INSERT INTO mug_owners (id, username, email) VALUES ('owner-1', 'owner', 'o@example.com')")
        jdbcTemplate.update("INSERT INTO mugs (id, owner_user_id, purchase_date, renewal_date, cost, size_ounces) VALUES ('mug-1', 'owner-1', '2025-01-15', '2026-01-15', 18.99, 12)")
        jdbcTemplate.update("INSERT INTO mug_history (id, mug_id, added_by_user_id, entry) VALUES ('hist-1', 'mug-1', 'owner-1', 'Stout')")

        when:
        migrate()

        then:
        columns("mugs").contains("nickname")
        columns("mug_history").contains("price")
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mugs", Integer) == 1
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mug_history", Integer) == 1
    }

    def "migrations create a fresh database and are safe to run repeatedly"() {
        when:
        migrate()
        migrate()

        then:
        noExceptionThrown()
        columns("mugs").count { it == "nickname" } == 1
        columns("mug_history").count { it == "price" } == 1
    }

    def "findById returns the number of recorded fills, ignoring history entries"() {
        given:
        givenTwoMugsWithFills()

        expect:
        repository.findById("mug-filled").get().fillCount() == 3
        repository.findById("mug-empty").get().fillCount() == 0
    }

    def "mug lists return each mug's own fill count"() {
        given:
        givenTwoMugsWithFills()

        when:
        def all = repository.getAllMugs().get().collectEntries { [(it.id()): it.fillCount()] }
        def owned = repository.findByOwnerUserId("owner-1").collectEntries { [(it.id()): it.fillCount()] }

        then:
        all == ["mug-filled": 3, "mug-empty": 0]
        owned == ["mug-filled": 3, "mug-empty": 0]
    }

    def "updateMugByOwner returns the mug with its fill count"() {
        given:
        givenTwoMugsWithFills()

        when:
        def updated = repository.updateMugByOwner(
                "mug-filled", "owner-1",
                java.time.LocalDate.parse("2025-01-15"), java.time.LocalDate.parse("2026-01-15"),
                new BigDecimal("20"), new BigDecimal("12"), null, true,
                java.time.Instant.parse("2026-11-16T20:00:00Z")
        )

        then:
        updated.get().fillCount() == 3
        updated.get().updatedAt() == java.time.Instant.parse("2026-11-16T20:00:00Z")
    }

    def "updateMugByOwner keeps the stored nickname when asked to"() {
        given:
        givenTwoMugsWithFills()
        jdbcTemplate.update("UPDATE mugs SET nickname = 'The Hazy One' WHERE id = 'mug-filled'")

        when:
        def updated = repository.updateMugByOwner(
                "mug-filled", "owner-1",
                java.time.LocalDate.parse("2025-01-15"), java.time.LocalDate.parse("2026-01-15"),
                new BigDecimal("20"), new BigDecimal("12"), null, true,
                java.time.Instant.parse("2026-11-16T20:00:00Z")
        )

        then:
        updated.get().nickname() == "The Hazy One"
        updated.get().cost() == new BigDecimal("20")
    }

    def "addFill stores every field and increases the mug's fill count"() {
        given:
        givenTwoMugsWithFills()
        def fill = new com.overmild.mugs.models.entities.MugFillEntity(
                "fill-new", "mug-filled", "owner-1",
                java.time.LocalDate.parse("2026-11-16"), new BigDecimal("12.5"),
                java.time.Instant.parse("2026-11-17T03:00:00Z")
        )

        when:
        repository.addFill(fill)
        def row = jdbcTemplate.queryForMap("SELECT * FROM mug_fills WHERE id = 'fill-new'")

        then:
        row.mug_id == "mug-filled"
        row.filled_by_user_id == "owner-1"
        row.fill_date == "2026-11-16"
        new BigDecimal(row.amount_ounces.toString()) == new BigDecimal("12.5")
        row.created_at == "2026-11-17T03:00:00Z"
        repository.findById("mug-filled").get().fillCount() == 4
    }

    def "history entries round-trip price, keep old rows without one, and come back newest first"() {
        given:
        givenTwoMugsWithFills()
        repository.addHistoryEntry(new com.overmild.mugs.models.entities.MugHistoryEntity(
                "hist-priced", "mug-empty", "owner-1", "Hazy IPA",
                java.time.Instant.parse("2026-11-16T20:00:00Z"), new BigDecimal("6.50")
        ))
        repository.addHistoryEntry(new com.overmild.mugs.models.entities.MugHistoryEntity(
                "hist-unpriced", "mug-empty", "owner-1", "Stout",
                java.time.Instant.parse("2026-11-16T21:00:00Z"), null
        ))

        when:
        def history = repository.findHistoryByMugId("mug-empty")

        then:
        history*.id() == ["hist-unpriced", "hist-priced", "hist-1"]
        history[0].price() == null
        history[1].price() == new BigDecimal("6.5")
        history[2].price() == null
    }

    def "updateMugByOwner sets and clears the nickname, and every read returns it"() {
        given:
        givenTwoMugsWithFills()
        def update = { String nickname ->
            repository.updateMugByOwner(
                    "mug-filled", "owner-1",
                    java.time.LocalDate.parse("2025-01-15"), java.time.LocalDate.parse("2026-01-15"),
                    new BigDecimal("18.99"), new BigDecimal("12"), nickname, false,
                    java.time.Instant.parse("2026-11-16T20:00:00Z")
            )
        }

        expect: "a mug without a nickname reads back null"
        repository.findById("mug-filled").get().nickname() == null

        when:
        def updated = update("The Hazy One")

        then:
        updated.get().nickname() == "The Hazy One"
        repository.findById("mug-filled").get().nickname() == "The Hazy One"
        repository.getAllMugs().get().find { it.id() == "mug-filled" }.nickname() == "The Hazy One"
        repository.findByOwnerUserId("owner-1").find { it.id() == "mug-filled" }.nickname() == "The Hazy One"

        when:
        update(null)

        then:
        repository.findById("mug-filled").get().nickname() == null
    }

    def "deleting a mug also deletes its fills and history"() {
        given:
        givenTwoMugsWithFills()
        insertHistory("hist-2", "mug-filled", "2026-01-04T00:00:00Z")
        insertHistory("hist-3", "mug-filled", "2026-01-05T00:00:00Z")

        when:
        repository.deleteMugByOwner("mug-filled", "owner-1")

        then:
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mug_fills WHERE mug_id = 'mug-filled'", Integer) == 0
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mug_history WHERE mug_id = 'mug-filled'", Integer) == 0
    }
}
