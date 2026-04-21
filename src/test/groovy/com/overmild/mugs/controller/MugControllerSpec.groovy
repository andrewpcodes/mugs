package com.overmild.mugs.controller

import com.overmild.mugs.exception.GlobalExceptionHandler
import com.overmild.mugs.exception.ResourceNotFoundException
import com.overmild.mugs.model.Mug
import com.overmild.mugs.service.MugService
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MugControllerSpec extends Specification {

    MugService mugService = Mock()
    MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new MugController(mugService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build()

    def "GET /mugs returns 200"() {
        given:
        mugService.getAllMugs() >> []

        expect:
        mockMvc.perform(get("/mugs"))
                .andExpect(status().isOk())
    }

    def "GET /mugs/{id} returns 200"() {
        given:
        def id = UUID.randomUUID()
        mugService.getMugById(id) >> new Mug(id, "My Mug", null, null)

        expect:
        mockMvc.perform(get("/mugs/{id}", id))
                .andExpect(status().isOk())
    }

    def "GET /mugs/{id} returns 404 when mug not found"() {
        given:
        def id = UUID.randomUUID()
        mugService.getMugById(id) >> { throw new ResourceNotFoundException("Mug not found: $id") }

        expect:
        mockMvc.perform(get("/mugs/{id}", id))
                .andExpect(status().isNotFound())
    }

    def "POST /mugs returns 200"() {
        given:
        mugService.createMug(_) >> new Mug(UUID.randomUUID(), "My Mug", null, null)

        expect:
        mockMvc.perform(post("/mugs")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"displayName":"My Mug"}'))
                .andExpect(status().isOk())
    }

    def "POST /mugs returns 400 when displayName is missing"() {
        expect:
        mockMvc.perform(post("/mugs")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{}'))
                .andExpect(status().isBadRequest())
    }

    def "PUT /mugs returns 200"() {
        given:
        def id = UUID.randomUUID()
        mugService.updateMug(_) >> new Mug(id, "Updated Mug", null, null)

        expect:
        mockMvc.perform(put("/mugs")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id":"' + id + '","displayName":"Updated Mug"}'))
                .andExpect(status().isOk())
    }

    def "PUT /mugs returns 404 when mug not found"() {
        given:
        def id = UUID.randomUUID()
        mugService.updateMug(_) >> { throw new ResourceNotFoundException("Mug not found: $id") }

        expect:
        mockMvc.perform(put("/mugs")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id":"' + id + '","displayName":"Updated Mug"}'))
                .andExpect(status().isNotFound())
    }

    def "DELETE /mugs/{id} returns 200"() {
        given:
        def id = UUID.randomUUID()

        expect:
        mockMvc.perform(delete("/mugs/{id}", id))
                .andExpect(status().isOk())
    }
}
