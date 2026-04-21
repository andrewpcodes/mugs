package com.overmild.mugs.controller

import com.overmild.mugs.exception.GlobalExceptionHandler
import com.overmild.mugs.exception.ResourceNotFoundException
import com.overmild.mugs.model.Location
import com.overmild.mugs.service.LocationService
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class LocationControllerSpec extends Specification {

    LocationService locationService = Mock()
    MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new LocationController(locationService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build()

    def "GET /locations returns 200"() {
        given:
        locationService.getAllLocations() >> []

        expect:
        mockMvc.perform(get("/locations"))
                .andExpect(status().isOk())
    }

    def "GET /locations/{id} returns 200"() {
        given:
        def id = UUID.randomUUID()
        locationService.getLocationById(id) >> new Location(id, "Cafe", "A nice cafe", null, null)

        expect:
        mockMvc.perform(get("/locations/{id}", id))
                .andExpect(status().isOk())
    }

    def "GET /locations/{id} returns 404 when location not found"() {
        given:
        def id = UUID.randomUUID()
        locationService.getLocationById(id) >> { throw new ResourceNotFoundException("Location not found: $id") }

        expect:
        mockMvc.perform(get("/locations/{id}", id))
                .andExpect(status().isNotFound())
    }

    def "POST /locations returns 200"() {
        given:
        locationService.createLocation(_) >> new Location(UUID.randomUUID(), "Cafe", "A nice cafe", null, null)

        expect:
        mockMvc.perform(post("/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"name":"Cafe","description":"A nice cafe"}'))
                .andExpect(status().isOk())
    }

    def "POST /locations returns 400 when name is missing"() {
        expect:
        mockMvc.perform(post("/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"description":"A nice cafe"}'))
                .andExpect(status().isBadRequest())
    }

    def "PUT /locations returns 200"() {
        given:
        def id = UUID.randomUUID()
        locationService.updateLocation(_) >> new Location(id, "Updated Cafe", "Updated desc", null, null)

        expect:
        mockMvc.perform(put("/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id":"' + id + '","name":"Updated Cafe","description":"Updated desc"}'))
                .andExpect(status().isOk())
    }

    def "PUT /locations returns 404 when location not found"() {
        given:
        def id = UUID.randomUUID()
        locationService.updateLocation(_) >> { throw new ResourceNotFoundException("Location not found: $id") }

        expect:
        mockMvc.perform(put("/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id":"' + id + '","name":"Updated Cafe","description":"Updated desc"}'))
                .andExpect(status().isNotFound())
    }

    def "DELETE /locations/{id} returns 200"() {
        given:
        def id = UUID.randomUUID()

        expect:
        mockMvc.perform(delete("/locations/{id}", id))
                .andExpect(status().isOk())
    }

    def "old singular route GET /location/{id} is no longer available"() {
        expect:
        mockMvc.perform(get("/location/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
    }
}
