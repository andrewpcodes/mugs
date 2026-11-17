package com.overmild.mugs.services

import com.overmild.mugs.mappers.MugMapper
import com.overmild.mugs.models.entities.MugEntity
import com.overmild.mugs.repositories.MugRepository
import org.mapstruct.factory.Mappers
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MugServiceSpec extends Specification {

    // 03:00 UTC on Nov 17: still Nov 16 in New York, already 17:00 on Nov 17 in UTC+14.
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T03:00:00Z"), ZoneOffset.UTC)

    MugRepository mugRepository = Mock()
    MugService mugService

    def setup() {
        mugService = new MugService(mugRepository, Mappers.getMapper(MugMapper), CLOCK)
    }

    private static MugEntity mug(String ownerUserId = "owner-1", String sizeOunces = "12") {
        new MugEntity(
                "mug-1",
                ownerUserId,
                LocalDate.parse("2025-01-15"),
                LocalDate.parse("2026-01-15"),
                new BigDecimal("18.99"),
                new BigDecimal(sizeOunces),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                null,
                0
        )
    }

    def "createMug returns a new mug with no fills and no nickname"() {
        when:
        def created = mugService.createMug(
                "owner-1",
                LocalDate.parse("2025-01-15"),
                LocalDate.parse("2026-01-15"),
                new BigDecimal("18.99"),
                new BigDecimal("12")
        )

        then:
        1 * mugRepository.createMug({
            it.ownerUserId() == "owner-1" &&
                    it.createdAt() == Instant.parse("2026-11-17T03:00:00Z") &&
                    it.updatedAt() == Instant.parse("2026-11-17T03:00:00Z")
        })
        0 * _
        created.ownerUserId == "owner-1"
        created.fillCount == 0
        created.nickname == null
    }

    def "addFill saves a fill with the given date"() {
        when:
        def fill = mugService.addFill("mug-1", "owner-1", new BigDecimal("12"), LocalDate.parse("2026-11-16"))

        then:
        1 * mugRepository.findById("mug-1") >> Optional.of(mug())
        1 * mugRepository.addFill({ it.fillDate() == LocalDate.parse("2026-11-16") })
        0 * _
        fill.mugId == "mug-1"
        fill.filledByUserId == "owner-1"
        fill.fillDate == LocalDate.parse("2026-11-16")
        fill.amountOunces == new BigDecimal("12")
        fill.createdAt == "2026-11-17T03:00:00Z"
        fill.id
    }

    def "addFill keeps an explicit past date"() {
        when:
        def fill = mugService.addFill("mug-1", "owner-1", new BigDecimal("8"), LocalDate.parse("2026-11-01"))

        then:
        1 * mugRepository.findById("mug-1") >> Optional.of(mug())
        1 * mugRepository.addFill(_)
        fill.fillDate == LocalDate.parse("2026-11-01")
    }

    def "addFill accepts a date that is already today somewhere ahead of New York"() {
        when:
        def fill = mugService.addFill("mug-1", "owner-1", new BigDecimal("12"), LocalDate.parse("2026-11-17"))

        then:
        1 * mugRepository.findById("mug-1") >> Optional.of(mug())
        1 * mugRepository.addFill(_)
        fill.fillDate == LocalDate.parse("2026-11-17")
    }

    def "addFill rejects a missing #field without touching the repository"() {
        when:
        mugService.addFill("mug-1", "owner-1", amount, fillDate)

        then:
        0 * _
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST

        where:
        field          | amount               | fillDate
        "amount"       | null                 | LocalDate.parse("2026-11-16")
        "fill date"    | new BigDecimal("12") | null
    }

    def "addFill rejects #reason"() {
        when:
        mugService.addFill("mug-1", requester, new BigDecimal(amount), fillDate)

        then:
        1 * mugRepository.findById("mug-1") >> Optional.ofNullable(existing)
        0 * mugRepository.addFill(_)
        def ex = thrown(ResponseStatusException)
        ex.statusCode == status

        where:
        reason                       | requester | amount | fillDate                       | existing  || status
        "more than the mug holds"    | "owner-1" | "12.5" | LocalDate.parse("2026-11-16")  | mug()     || HttpStatus.BAD_REQUEST
        "a date future everywhere"   | "owner-1" | "12"   | LocalDate.parse("2026-11-18")  | mug()     || HttpStatus.BAD_REQUEST
        "someone other than owner"   | "other-1" | "12"   | LocalDate.parse("2026-11-16")  | mug()     || HttpStatus.FORBIDDEN
        "a mug that does not exist"  | "owner-1" | "12"   | LocalDate.parse("2026-11-16")  | null      || HttpStatus.NOT_FOUND
    }

    def "addHistoryEntry saves price #price"() {
        when:
        def entry = mugService.addHistoryEntry("mug-1", "owner-1", "Hazy IPA", price)

        then:
        1 * mugRepository.findById("mug-1") >> Optional.of(mug())
        1 * mugRepository.addHistoryEntry({ it.price() == price })
        0 * _
        entry.price == price

        where:
        price << [new BigDecimal("6.50"), new BigDecimal("0"), null]
    }

    def "addHistoryEntry still rejects #reason"() {
        when:
        mugService.addHistoryEntry("mug-1", requester, "Hazy IPA", new BigDecimal("6.50"))

        then:
        1 * mugRepository.findById("mug-1") >> Optional.ofNullable(existing)
        0 * mugRepository.addHistoryEntry(_)
        def ex = thrown(ResponseStatusException)
        ex.statusCode == status

        where:
        reason                      | requester | existing || status
        "someone other than owner"  | "other-1" | mug()    || HttpStatus.FORBIDDEN
        "a mug that does not exist" | "owner-1" | null     || HttpStatus.NOT_FOUND
    }

    def "updateMug normalizes nickname #input to #expected (keep stored: #keep)"() {
        when:
        mugService.updateMug(
                "mug-1", "owner-1",
                LocalDate.parse("2025-01-15"), LocalDate.parse("2026-01-15"),
                new BigDecimal("18.99"), new BigDecimal("12"), input
        )

        then:
        1 * mugRepository.updateMugByOwner("mug-1", "owner-1", _, _, _, _, expected, keep, Instant.parse("2026-11-17T03:00:00Z")) >> Optional.of(mug())
        0 * _

        where:
        input                         || expected       | keep
        "  The Hazy One "             || "The Hazy One" | false
        "The Hazy One"                || "The Hazy One" | false
        null                          || null           | true
        ""                            || null           | false
        "   "                         || null           | false
        "  " + "x" * 50 + "  "        || "x" * 50       | false
        " " * 60                      || null           | false
    }

    def "updateMug rejects a nickname longer than 50 characters after trimming"() {
        when:
        mugService.updateMug(
                "mug-1", "owner-1",
                LocalDate.parse("2025-01-15"), LocalDate.parse("2026-01-15"),
                new BigDecimal("18.99"), new BigDecimal("12"), " " + "x" * 51 + " "
        )

        then:
        0 * _
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "updateMug still reports #status when the update does not apply"() {
        when:
        mugService.updateMug(
                "mug-1", "owner-1",
                LocalDate.parse("2025-01-15"), LocalDate.parse("2026-01-15"),
                new BigDecimal("18.99"), new BigDecimal("12"), "Nick"
        )

        then:
        1 * mugRepository.updateMugByOwner(*_) >> Optional.empty()
        1 * mugRepository.findById("mug-1") >> Optional.ofNullable(existing)
        def ex = thrown(ResponseStatusException)
        ex.statusCode == status

        where:
        existing        || status
        mug("other-1")  || HttpStatus.FORBIDDEN
        null            || HttpStatus.NOT_FOUND
        mug()           || HttpStatus.CONFLICT
    }
}
