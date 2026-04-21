package com.overmild.mugs.controller

import com.overmild.mugs.model.Mug
import com.overmild.mugs.model.User
import com.overmild.mugs.service.MugService
import com.overmild.mugs.service.UserService
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserControllerSpec extends Specification {

    UserService userService = Mock()
    MugService mugService = Mock()
    MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new UserController(userService, mugService))
            .build()

    def "GET /users returns 200"() {
        given:
        userService.getAllUsers() >> []

        expect:
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
    }

    def "GET /users/{id} returns 200 using path variable"() {
        given:
        def id = UUID.randomUUID()
        userService.getUserById(id) >> new User(id, "John", "Doe", "john@example.com")

        expect:
        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
    }

    def "GET /users/{userId}/mugs returns 200"() {
        given:
        def userId = UUID.randomUUID()
        mugService.getMugsByUserId(userId) >> []

        expect:
        mockMvc.perform(get("/users/{userId}/mugs", userId))
                .andExpect(status().isOk())
    }

    def "POST /users returns 200"() {
        given:
        userService.createUser(_) >> new User(UUID.randomUUID(), "John", "Doe", "john@example.com")

        expect:
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"firstName":"John","lastName":"Doe","email":"john@example.com"}'))
                .andExpect(status().isOk())
    }

    def "PUT /users returns 200"() {
        given:
        def id = UUID.randomUUID()
        userService.updateUser(_) >> new User(id, "Jane", "Doe", "jane@example.com")

        expect:
        mockMvc.perform(put("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id":"' + id + '","firstName":"Jane","lastName":"Doe","email":"jane@example.com"}'))
                .andExpect(status().isOk())
    }

    def "DELETE /users/{id} returns 200"() {
        given:
        def id = UUID.randomUUID()

        expect:
        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isOk())
    }

    def "old singular route GET /user/{id} is no longer available"() {
        expect:
        mockMvc.perform(get("/user/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
    }
}
