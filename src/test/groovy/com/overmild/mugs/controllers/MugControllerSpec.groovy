package com.overmild.mugs.controllers

import com.overmild.mugs.configs.SecurityConfig
import com.overmild.mugs.configs.CorsConfig
import com.overmild.mugs.models.MugFillResponse
import com.overmild.mugs.models.MugHistoryResponse
import com.overmild.mugs.models.MugResponse
import com.overmild.mugs.services.MugService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

import java.math.BigDecimal
import java.time.LocalDate

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
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
    JwtDecoder jwtDecoder = Mock()

    def "gets mug by id"() {
        given:
        def response = new MugResponse()
                .id("mug-1")
                .ownerUserId("owner-1")
                .purchaseDate(LocalDate.parse("2025-01-15"))
                .renewalDate(LocalDate.parse("2026-01-15"))
                .cost(new BigDecimal("18.99"))
                .sizeOunces(new BigDecimal("12.0"))
                .fillCount(3)
                .nickname("The Hazy One")

        when:
        def result = mockMvc.perform(get("/api/mugs/{mugId}", "mug-1"))

        then:
        1 * mugService.getMugById("mug-1") >> response
        0 * _
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.id').value("mug-1"))
                .andExpect(jsonPath('$.fillCount').value(3))
                .andExpect(jsonPath('$.nickname').value("The Hazy One"))
    }

    def "gets mug history by mug id"() {
        given:
        def historyResponse = new MugHistoryResponse()
                .id("hist-1")
                .mugId("mug-1")
                .addedByUserId("owner-1")
                .entry("Replaced lid")
                .price(new BigDecimal("6.50"))
                .createdAt("2026-01-03T00:00:00Z")

        when:
        def result = mockMvc.perform(get("/api/mugs/{mugId}/history", "mug-1"))

        then:
        1 * mugService.getMugHistoryByMugId("mug-1") >> [historyResponse]
        0 * _
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$[0].id').value("hist-1"))
                .andExpect(jsonPath('$[0].mugId').value("mug-1"))
                .andExpect(jsonPath('$[0].addedByUserId').value("owner-1"))
                .andExpect(jsonPath('$[0].entry').value("Replaced lid"))
                .andExpect(jsonPath('$[0].price').value(6.5))
                .andExpect(jsonPath('$[0].createdAt').value("2026-01-03T00:00:00Z"))
    }

    def "gets mugs by owner id for authenticated user"() {
        given:
        def response = new MugResponse()
                .id("mug-1")
                .ownerUserId("owner-1")
                .purchaseDate(LocalDate.parse("2025-01-15"))
                .renewalDate(LocalDate.parse("2026-01-15"))
                .cost(new BigDecimal("18.99"))
                .sizeOunces(new BigDecimal("12.0"))
                .fillCount(3)

        when:
        def result = mockMvc.perform(
                get("/api/mugs/owners/{ownerId}", "owner-1")
                        .with(jwtSubject("owner-1"))
        )

        then:
        1 * mugService.getMugsByOwnerId("owner-1", "owner-1") >> [response]
        0 * _
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$[0].id').value("mug-1"))
                .andExpect(jsonPath('$[0].fillCount').value(3))
    }

    def "creates mug for authenticated user"() {
        given:
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
        ) >> response
        0 * _
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.id').value("mug-1"))
    }

    def "updates mug for authenticated user"() {
        given:
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
                new BigDecimal("14.0"),
                null
        ) >> response
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
        1 * mugService.addHistoryEntry("mug-1", "owner-1", "Replaced lid", null) >> historyResponse
        0 * _
        result.andExpect(status().isCreated())
                .andExpect(jsonPath('$.id').value("hist-1"))
    }

    def "requires login to list all mugs"() {
        when:
        def result = mockMvc.perform(get("/api/mugs"))

        then:
        0 * _
        result.andExpect(status().isUnauthorized())
    }

    def "lists all mugs for authenticated user"() {
        when:
        def result = mockMvc.perform(get("/api/mugs").with(jwtSubject("owner-1")))

        then:
        1 * mugService.getAllMugs() >> [new MugResponse().id("mug-1").fillCount(3)]
        0 * _
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$[0].fillCount').value(3))
    }

    def "records a fill for authenticated user"() {
        given:
        def fillResponse = new MugFillResponse()
                .id("fill-1")
                .mugId("mug-1")
                .filledByUserId("owner-1")
                .fillDate(LocalDate.parse("2026-11-16"))
                .amountOunces(new BigDecimal("12"))
                .createdAt("2026-11-17T03:00:00Z")

        when:
        def result = mockMvc.perform(
                post("/api/mugs/{mugId}/fills", "mug-1")
                        .with(jwtSubject("owner-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content('{"amountOunces":12,"fillDate":"2026-11-16"}')
        )

        then:
        1 * mugService.addFill("mug-1", "owner-1", new BigDecimal("12"), LocalDate.parse("2026-11-16")) >> fillResponse
        0 * _
        result.andExpect(status().isCreated())
                .andExpect(jsonPath('$.id').value("fill-1"))
                .andExpect(jsonPath('$.mugId').value("mug-1"))
                .andExpect(jsonPath('$.filledByUserId').value("owner-1"))
                .andExpect(jsonPath('$.fillDate').value("2026-11-16"))
                .andExpect(jsonPath('$.amountOunces').value(12))
                .andExpect(jsonPath('$.createdAt').value("2026-11-17T03:00:00Z"))
    }

    def "rejects an invalid fill body: #body"() {
        when:
        def result = mockMvc.perform(
                post("/api/mugs/{mugId}/fills", "mug-1")
                        .with(jwtSubject("owner-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        )

        then:
        0 * _
        result.andExpect(status().isBadRequest())

        where:
        body << [
                '{"amountOunces":0,"fillDate":"2026-11-16"}',
                '{"amountOunces":-1,"fillDate":"2026-11-16"}',
                '{"fillDate":"2026-11-16"}',
                '{"amountOunces":12}'
        ]
    }

    def "requires login to record a fill"() {
        when:
        def result = mockMvc.perform(
                post("/api/mugs/{mugId}/fills", "mug-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content('{"amountOunces":12,"fillDate":"2026-11-16"}')
        )

        then:
        0 * _
        result.andExpect(status().isUnauthorized())
    }

    def "returns #status as problem details when the service rejects a fill"() {
        when:
        def result = mockMvc.perform(
                post("/api/mugs/{mugId}/fills", "mug-1")
                        .with(jwtSubject("owner-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content('{"amountOunces":12,"fillDate":"2026-11-16"}')
        )

        then:
        1 * mugService.addFill("mug-1", "owner-1", new BigDecimal("12"), LocalDate.parse("2026-11-16")) >> {
            throw new ResponseStatusException(status, "rejected")
        }
        0 * _
        result.andExpect(status().is(status.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))

        where:
        status << [HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND]
    }

    def "adds mug history with a price"() {
        given:
        def historyResponse = new MugHistoryResponse()
                .id("hist-2")
                .entry("Hazy IPA")
                .price(new BigDecimal("6.50"))

        when:
        def result = mockMvc.perform(
                post("/api/mugs/{mugId}/history", "mug-1")
                        .with(jwtSubject("owner-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content('{"entry":"Hazy IPA","price":6.50}')
        )

        then:
        1 * mugService.addHistoryEntry("mug-1", "owner-1", "Hazy IPA", new BigDecimal("6.50")) >> historyResponse
        0 * _
        result.andExpect(status().isCreated())
                .andExpect(jsonPath('$.price').value(6.5))
    }

    def "rejects a negative history price"() {
        when:
        def result = mockMvc.perform(
                post("/api/mugs/{mugId}/history", "mug-1")
                        .with(jwtSubject("owner-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content('{"entry":"Hazy IPA","price":-1}')
        )

        then:
        0 * _
        result.andExpect(status().isBadRequest())
    }

    def "updates mug nickname for authenticated user"() {
        when:
        def result = mockMvc.perform(
                put("/api/mugs/{mugId}", "mug-1")
                        .with(jwtSubject("owner-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content('{"purchaseDate":"2025-01-15","renewalDate":"2026-01-15","cost":21.5,"sizeOunces":14.0,"nickname":"The Hazy One"}')
        )

        then:
        1 * mugService.updateMug("mug-1", "owner-1", _, _, _, _, "The Hazy One") >> new MugResponse().id("mug-1").nickname("The Hazy One")
        0 * _
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.nickname').value("The Hazy One"))
    }

    def "passes a padded nickname over 50 characters to the service, which trims and checks its length"() {
        given:
        def padded = "  " + ("x" * 50) + "  "

        when:
        def result = mockMvc.perform(
                put("/api/mugs/{mugId}", "mug-1")
                        .with(jwtSubject("owner-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content('{"purchaseDate":"2025-01-15","renewalDate":"2026-01-15","cost":21.5,"sizeOunces":14.0,"nickname":"' + padded + '"}')
        )

        then:
        1 * mugService.updateMug("mug-1", "owner-1", _, _, _, _, padded) >> new MugResponse().id("mug-1")
        0 * _
        result.andExpect(status().isOk())
    }

    def "rejects a history price with more than 2 decimal places"() {
        when:
        def result = mockMvc.perform(
                post("/api/mugs/{mugId}/history", "mug-1")
                        .with(jwtSubject("owner-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content('{"entry":"Hazy IPA","price":6.505}')
        )

        then:
        0 * _
        result.andExpect(status().isBadRequest())
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
