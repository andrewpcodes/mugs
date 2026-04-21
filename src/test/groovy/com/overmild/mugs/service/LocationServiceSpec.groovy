package com.overmild.mugs.service

import com.overmild.mugs.entity.AddressEmbeddable
import com.overmild.mugs.entity.LocationEntity
import com.overmild.mugs.mapper.LocationMapper
import com.overmild.mugs.model.Address
import com.overmild.mugs.model.Location
import com.overmild.mugs.repository.LocationRepository
import spock.lang.Specification

import java.util.Optional
import java.util.UUID

class LocationServiceSpec extends Specification {

    LocationRepository repository = Mock()
    LocationMapper locationMapper = Mock()
    LocationService service = new LocationService(repository, locationMapper)

    def "createLocation persists and returns location with address and photoUrl"() {
        given:
        def address = new Address(line1: "1 Main St", city: "Springfield", state: "IL", zipCode: "62701", country: "US")
        def input = new Location(null, "Cafe A", "desc", address, "https://example.com/a.jpg")
        def entity = new LocationEntity(name: "Cafe A", description: "desc",
                address: new AddressEmbeddable(line1: "1 Main St", city: "Springfield", state: "IL", zipCode: "62701", country: "US"),
                photoUrl: "https://example.com/a.jpg")
        def savedEntity = new LocationEntity(id: UUID.randomUUID(), name: "Cafe A", description: "desc",
                address: entity.address, photoUrl: "https://example.com/a.jpg")
        def expected = new Location(savedEntity.id, "Cafe A", "desc", address, "https://example.com/a.jpg")

        locationMapper.toEntity(input) >> entity
        repository.save(entity) >> savedEntity
        locationMapper.toModel(savedEntity) >> expected

        when:
        Location result = service.createLocation(input)

        then:
        result.name == "Cafe A"
        result.photoUrl == "https://example.com/a.jpg"
        result.address.line1 == "1 Main St"
        result.address.city == "Springfield"
    }

    def "getLocationById returns location with address and photoUrl"() {
        given:
        def id = UUID.randomUUID()
        def embeddable = new AddressEmbeddable(line1: "2 Oak Ave", city: "Shelbyville", state: "IL", zipCode: "62565", country: "US")
        def entity = new LocationEntity(id: id, name: "Cafe B", description: "oak cafe",
                address: embeddable, photoUrl: "https://example.com/b.jpg")
        def address = new Address(line1: "2 Oak Ave", city: "Shelbyville", state: "IL", zipCode: "62565", country: "US")
        def expected = new Location(id, "Cafe B", "oak cafe", address, "https://example.com/b.jpg")

        repository.findById(id) >> Optional.of(entity)
        locationMapper.toModel(entity) >> expected

        when:
        Location result = service.getLocationById(id)

        then:
        result.id == id
        result.photoUrl == "https://example.com/b.jpg"
        result.address.line1 == "2 Oak Ave"
    }

    def "getLocationById returns null when not found"() {
        given:
        def id = UUID.randomUUID()
        repository.findById(id) >> Optional.empty()

        when:
        Location result = service.getLocationById(id)

        then:
        result == null
    }

    def "updateLocation persists updated address and photoUrl"() {
        given:
        def id = UUID.randomUUID()
        def address = new Address(line1: "3 Elm St", city: "Capital City", state: "IL", zipCode: "62000", country: "US")
        def updated = new Location(id, "Cafe C", "updated", address, "https://example.com/c.jpg")
        def entity = new LocationEntity(id: id, name: "Cafe C", description: "updated",
                address: new AddressEmbeddable(line1: "3 Elm St", city: "Capital City", state: "IL", zipCode: "62000", country: "US"),
                photoUrl: "https://example.com/c.jpg")
        def expected = new Location(id, "Cafe C", "updated", address, "https://example.com/c.jpg")

        repository.findById(id) >> Optional.of(entity)
        locationMapper.toEntity(updated) >> entity
        repository.save(entity) >> entity
        locationMapper.toModel(entity) >> expected

        when:
        Location result = service.updateLocation(updated)

        then:
        result.name == "Cafe C"
        result.photoUrl == "https://example.com/c.jpg"
        result.address.line1 == "3 Elm St"
    }

    def "updateLocation returns null when location does not exist"() {
        given:
        def id = UUID.randomUUID()
        def location = new Location(id, "Ghost Cafe", null, null, null)
        repository.findById(id) >> Optional.empty()

        when:
        Location result = service.updateLocation(location)

        then:
        result == null
    }

    def "updateLocation returns null when id is null"() {
        given:
        def location = new Location(null, "No ID Cafe", null, null, null)

        when:
        Location result = service.updateLocation(location)

        then:
        result == null
        0 * repository._
    }
}
