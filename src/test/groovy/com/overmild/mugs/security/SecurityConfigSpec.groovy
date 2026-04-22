package com.overmild.mugs.security

import com.overmild.mugs.config.SecurityConfig
import com.overmild.mugs.model.Location
import com.overmild.mugs.model.Mug
import com.overmild.mugs.model.User
import com.overmild.mugs.service.AppUserDetailsService
import com.overmild.mugs.service.AuthService
import com.overmild.mugs.service.LocationService
import com.overmild.mugs.service.MugService
import com.overmild.mugs.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Verifies the security rules configured in {@link com.overmild.mugs.config.SecurityConfig}.
 *
 * <p>Rules under test:
 * <ul>
 *   <li>GET requests are public (no authentication required).</li>
 *   <li>POST /auth/register is public.</li>
 *   <li>POST, PUT, DELETE on resource endpoints require the ADMIN role.</li>
 *   <li>Requests with the USER role to protected endpoints are rejected with 403.</li>
 * </ul>
 */
@WebMvcTest
@Import(SecurityConfig)
class SecurityConfigSpec extends Specification {

    @Autowired
    WebApplicationContext wac

    @MockitoBean
    UserService userService

    @MockitoBean
    MugService mugService

    @MockitoBean
    LocationService locationService

    @MockitoBean
    AppUserDetailsService appUserDetailsService

    @MockitoBean
    AuthService authService

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build()
    }

    // ── GET endpoints are public ──────────────────────────────────────────────

    def "GET /users is public - returns 200 without credentials"() {
        given:
        when(userService.getAllUsers()).thenReturn([])

        expect:
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
    }

    def "GET /users/{id} is public - returns 200 without credentials"() {
        given:
        def id = UUID.randomUUID()
        when(userService.getUserById(id)).thenReturn(new User(id, "John", "Doe", "john@example.com"))

        expect:
        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
    }

    def "GET /mugs is public - returns 200 without credentials"() {
        given:
        when(mugService.getAllMugs()).thenReturn([])

        expect:
        mockMvc.perform(get("/mugs"))
                .andExpect(status().isOk())
    }

    def "GET /locations is public - returns 200 without credentials"() {
        given:
        when(locationService.getAllLocations()).thenReturn([])

        expect:
        mockMvc.perform(get("/locations"))
                .andExpect(status().isOk())
    }

    // ── POST /auth/register is public ─────────────────────────────────────────

    def "POST /auth/register is public - returns 201 without credentials"() {
        expect:
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"username":"alice","password":"secret123"}'))
                .andExpect(status().isCreated())
    }

    // ── Unauthenticated mutating requests are rejected ─────────────────────────

    def "POST /users returns 401 without credentials"() {
        expect:
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"firstName":"John","lastName":"Doe","email":"john@example.com"}'))
                .andExpect(status().isUnauthorized())
    }

    def "PUT /users returns 401 without credentials"() {
        given:
        def id = UUID.randomUUID()

        expect:
        mockMvc.perform(put("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id":"' + id + '","firstName":"Jane","lastName":"Doe","email":"jane@example.com"}'))
                .andExpect(status().isUnauthorized())
    }

    def "DELETE /users/{id} returns 401 without credentials"() {
        expect:
        mockMvc.perform(delete("/users/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
    }

    def "POST /mugs returns 401 without credentials"() {
        expect:
        mockMvc.perform(post("/mugs")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"displayName":"My Mug"}'))
                .andExpect(status().isUnauthorized())
    }

    def "DELETE /mugs/{id} returns 401 without credentials"() {
        expect:
        mockMvc.perform(delete("/mugs/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
    }

    def "POST /locations returns 401 without credentials"() {
        expect:
        mockMvc.perform(post("/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"name":"Cafe","description":"A nice cafe"}'))
                .andExpect(status().isUnauthorized())
    }

    def "DELETE /locations/{id} returns 401 without credentials"() {
        expect:
        mockMvc.perform(delete("/locations/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
    }

    // ── ADMIN role can perform mutating operations ─────────────────────────────

    def "POST /users with ADMIN credentials returns 200"() {
        given:
        when(userService.createUser(any())).thenReturn(new User(UUID.randomUUID(), "John", "Doe", "john@example.com"))

        expect:
        mockMvc.perform(post("/users")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"firstName":"John","lastName":"Doe","email":"john@example.com"}'))
                .andExpect(status().isOk())
    }

    def "DELETE /users/{id} with ADMIN credentials returns 200"() {
        expect:
        mockMvc.perform(delete("/users/{id}", UUID.randomUUID())
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
    }

    def "POST /mugs with ADMIN credentials returns 200"() {
        given:
        when(mugService.createMug(any())).thenReturn(new Mug(UUID.randomUUID(), "My Mug", null, null))

        expect:
        mockMvc.perform(post("/mugs")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"displayName":"My Mug"}'))
                .andExpect(status().isOk())
    }

    def "POST /locations with ADMIN credentials returns 200"() {
        given:
        when(locationService.createLocation(any())).thenReturn(new Location(UUID.randomUUID(), "Cafe", "A nice cafe", null, null))

        expect:
        mockMvc.perform(post("/locations")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"name":"Cafe","description":"A nice cafe"}'))
                .andExpect(status().isOk())
    }

    // ── USER role is denied access to mutating operations ─────────────────────

    def "POST /users with USER role returns 403"() {
        expect:
        mockMvc.perform(post("/users")
                .with(user("alice").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"firstName":"John","lastName":"Doe","email":"john@example.com"}'))
                .andExpect(status().isForbidden())
    }

    def "DELETE /mugs/{id} with USER role returns 403"() {
        expect:
        mockMvc.perform(delete("/mugs/{id}", UUID.randomUUID())
                .with(user("alice").roles("USER")))
                .andExpect(status().isForbidden())
    }

    def "PUT /locations with USER role returns 403"() {
        given:
        def id = UUID.randomUUID()

        expect:
        mockMvc.perform(put("/locations")
                .with(user("alice").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id":"' + id + '","name":"Updated Cafe","description":"Updated desc"}'))
                .andExpect(status().isForbidden())
    }
}
