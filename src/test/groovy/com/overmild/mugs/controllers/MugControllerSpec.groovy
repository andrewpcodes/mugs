package com.overmild.mugs.controllers

import com.overmild.mugs.configs.SecurityConfig
import com.overmild.mugs.configs.CorsConfig
import com.overmild.mugs.mappers.MugMapper
import com.overmild.mugs.models.MugHistoryResponse
import com.overmild.mugs.models.MugResponse
import com.overmild.mugs.models.entities.MugEntity
import com.overmild.mugs.models.entities.MugHistoryEntity
import com.overmild.mugs.services.MugService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = MugController)
@Import([SecurityConfig, CorsConfig])
class MugControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    MugService mugService = Mock()

    @SpringBean
    MugMapper mugMapper = Mock()

    @SpringBean
    JwtDecoder jwtDecoder = Mock()

    def "gets mug by id"() {
        given:
        def entity = new MugEntity(
                "mug-1",
                "owner-1",
                LocalDate.parse("2025-01-15"),
                LocalDate.parse("2026-01-15"),
                new BigDecimal("18.99"),
                new BigDecimal("12.0"),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z")
        )
        def response = new MugResponse()
                .id("mug-1")
                .ownerUserId("owner-1")
                .purchaseDate(LocalDate.parse("2025-01-15"))
                .renewalDate(LocalDate.parse("2026-01-15"))
                .cost(new BigDecimal("18.99"))
                .sizeOunces(new BigDecimal("12.0"))

        when:
        def result = mockMvc.perform(get("/api/mugs/{mugId}", "mug-1"))

        then:
        1 * mugService.getMugById("mug-1") >> entity
        1 * mugMapper.toMugResponse(entity) >> response
        0 * _
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.id').value("mug-1"))
    }

    def "gets mug history by mug id"() {
        given:
        def historyEntity = new MugHistoryEntity(
                "hist-1",
                "mug-1",
                "owner-1",
                "Replaced lid",
                Instant.parse("2026-01-03T00:00:00Z")
        )
        def historyResponse = new MugHistoryResponse()
                .id("hist-1")
                .mugId("mug-1")
                .addedByUserId("owner-1")
                .entry("Replaced lid")
                .createdAt("2026-01-03T00:00:00Z")

        when:
        def result = mockMvc.perform(get("/api/mugs/{mugId}/history", "mug-1"))

        then:
        1 * mugService.getMugHistoryByMugId("mug-1") >> [historyEntity]
        1 * mugMapper.toMugHistoryResponse(historyEntity) >> historyResponse
        0 * _
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$[0].id').value("hist-1"))
    }

    def "gets mugs by owner id for authenticated user"() {
        given:
        def entity = new MugEntity(
                "mug-1",
                "owner-1",
                LocalDate.parse("2025-01-15"),
                LocalDate.parse("2026-01-15"),
                new BigDecimal("18.99"),
                new BigDecimal("12.0"),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z")
        )
        def response = new MugResponse()
                .id("mug-1")
                .ownerUserId("owner-1")
                .purchaseDate(LocalDate.parse("2025-01-15"))
                .renewalDate(LocalDate.parse("2026-01-15"))
                .cost(new BigDecimal("18.99"))
                .sizeOunces(new BigDecimal("12.0"))

        when:
        def result = mockMvc.perform(
                get("/api/mugs/owners/{ownerId}", "owner-1")
                        .with(jwtSubject("owner-1"))
        )

        then:
        1 * mugService.getMugsByOwnerId("owner-1", "owner-1") >> [entity]
        1 * mugMapper.toMugResponse(entity) >> response
        0 * _
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$[0].id').value("mug-1"))
    }

    def "creates mug for authenticated user"() {
        given:
        def entity = new MugEntity(
                "mug-1",
                "owner-1",
                LocalDate.parse("2025-01-15"),
                LocalDate.parse("2026-01-15"),
                new BigDecimal("18.99"),
                new BigDecimal("12.0"),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z")
        )
        def response = new MugResponse()
                .id("mug-1")
                .ownerUserId("owner-1")
                .purchaseDate(LocalDate.parse("2025-01-15"))
                .renewalDate(LocalDate.parse("2026-01-15"))
                .cost(new BigDecimal("18.99"))
                .sizeOunces(new BigDecimal("12.0"))

        when:
        def result = mockMvc.perform(
                post("/api/mugs")
                        .with(jwtSubject("owner-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content('{"purchaseDate":"2025-01-15","renewalDate":"2026-01-15","cost":18.99,"sizeOunces":12.0}')
        )

        then:
        1 * mugService.createMug(
                "owner-1",
                LocalDate.parse("2025-01-15"),
                LocalDate.parse("2026-01-15"),
                new BigDecimal("18.99"),
                new BigDecimal("12.0")
        ) >> entity
        1 * mugMapper.toMugResponse(entity) >> response
        0 * _
        result.andExpect(status().isCreated())
                .andExpect(jsonPath('$.id').value("mug-1"))
    }

    def "updates mug for authenticated user"() {
        given:
        def entity = new MugEntity(
                "mug-1",
                "owner-1",
                LocalDate.parse("2025-01-15"),
                LocalDate.parse("2026-01-15"),
                new BigDecimal("21.50"),
                new BigDecimal("14.0"),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z")
        )
        def response = new MugResponse()
                .id("mug-1")
                .ownerUserId("owner-1")
                .purchaseDate(LocalDate.parse("2025-01-15"))
                .renewalDate(LocalDate.parse("2026-01-15"))
                .cost(new BigDecimal("21.50"))
                .sizeOunces(new BigDecimal("14.0"))

        when:
        def result = mockMvc.perform(
                put("/api/mugs/{mugId}", "mug-1")
                        .with(jwtSubject("owner-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content('{"purchaseDate":"2025-01-15","renewalDate":"2026-01-15","cost":21.5,"sizeOunces":14.0}')
        )

        then:
        1 * mugService.updateMug(
                "mug-1",
                "owner-1",
                LocalDate.parse("2025-01-15"),
                LocalDate.parse("2026-01-15"),
                new BigDecimal("21.5"),
                new BigDecimal("14.0")
        ) >> entity
        1 * mugMapper.toMugResponse(entity) >> response
        0 * _
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.cost').value(21.5))
    }

    def "deletes mug for authenticated user"() {
        when:
        def result = mockMvc.perform(
                delete("/api/mugs/{mugId}", "mug-1")
                        .with(jwtSubject("owner-1"))
        )

        then:
        1 * mugService.deleteMug("mug-1", "owner-1")
        0 * _
        result.andExpect(status().isNoContent())
    }

    def "adds mug history for authenticated user"() {
        given:
        def historyEntity = new MugHistoryEntity(
                "hist-1",
                "mug-1",
                "owner-1",
                "Replaced lid",
                Instant.parse("2026-01-03T00:00:00Z")
        )
        def historyResponse = new MugHistoryResponse()
                .id("hist-1")
                .mugId("mug-1")
                .addedByUserId("owner-1")
                .entry("Replaced lid")
                .createdAt("2026-01-03T00:00:00Z")

        when:
        def result = mockMvc.perform(
                post("/api/mugs/{mugId}/history", "mug-1")
                        .with(jwtSubject("owner-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content('{"entry":"Replaced lid"}')
        )

        then:
        1 * mugService.addHistoryEntry("mug-1", "owner-1", "Replaced lid") >> historyEntity
        1 * mugMapper.toMugHistoryResponse(historyEntity) >> historyResponse
        0 * _
        result.andExpect(status().isCreated())
                .andExpect(jsonPath('$.id').value("hist-1"))
    }

    def "allows cors preflight from the vite dev origin"() {
        when:
        def result = mockMvc.perform(
                options("/api/mugs")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        )

        then:
        0 * _
        result.andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtSubject(String subject) {
        return jwt().jwt { Jwt.Builder builder ->
            builder.subject(subject)
        }
    }
}
