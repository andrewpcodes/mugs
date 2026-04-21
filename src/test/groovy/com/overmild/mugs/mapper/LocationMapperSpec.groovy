package com.overmild.mugs.mapper

import com.overmild.mugs.entity.AddressEmbeddable
import com.overmild.mugs.entity.LocationEntity
import com.overmild.mugs.model.Address
import com.overmild.mugs.model.Location
import spock.lang.Specification

class LocationMapperSpec extends Specification {

    LocationMapper mapper = new LocationMapperImpl()

    def "toModel maps all fields including address and photoUrl"() {
        given:
        def embeddable = new AddressEmbeddable(
            line1: "123 Main St",
            line2: "Apt 4",
            city: "Springfield",
            state: "IL",
            zipCode: "62701",
            country: "US"
        )
        def entity = new LocationEntity(
            name: "Test Cafe",
            description: "A nice place",
            address: embeddable,
            photoUrl: "https://example.com/photo.jpg"
        )

        when:
        Location model = mapper.toModel(entity)

        then:
        model.name == "Test Cafe"
        model.description == "A nice place"
        model.photoUrl == "https://example.com/photo.jpg"
        model.address != null
        model.address.line1 == "123 Main St"
        model.address.line2 == "Apt 4"
        model.address.city == "Springfield"
        model.address.state == "IL"
        model.address.zipCode == "62701"
        model.address.country == "US"
    }

    def "toModel handles null address"() {
        given:
        def entity = new LocationEntity(
            name: "Minimal Cafe",
            description: "No address",
            address: null,
            photoUrl: null
        )

        when:
        Location model = mapper.toModel(entity)

        then:
        model.name == "Minimal Cafe"
        model.address == null
        model.photoUrl == null
    }

    def "toEntity maps all fields including address and photoUrl"() {
        given:
        def address = new Address(
            line1: "456 Oak Ave",
            city: "Shelbyville",
            state: "IL",
            zipCode: "62565",
            country: "US"
        )
        def location = new Location(null, "Oak Cafe", "Another place", address, "https://example.com/oak.jpg")

        when:
        LocationEntity entity = mapper.toEntity(location)

        then:
        entity.name == "Oak Cafe"
        entity.description == "Another place"
        entity.photoUrl == "https://example.com/oak.jpg"
        entity.address != null
        entity.address.line1 == "456 Oak Ave"
        entity.address.city == "Shelbyville"
        entity.address.state == "IL"
        entity.address.zipCode == "62565"
        entity.address.country == "US"
    }

    def "toEntity ignores managed fields"() {
        given:
        def location = new Location(null, "Simple Cafe", null, null, null)

        when:
        LocationEntity entity = mapper.toEntity(location)

        then:
        entity.mugs == null
        entity.createdAt == null
        entity.modifiedAt == null
    }

    def "round-trip toEntity then toModel preserves all fields"() {
        given:
        def address = new Address(
            line1: "789 Elm St",
            line2: "Suite 1",
            city: "Capital City",
            state: "IL",
            zipCode: "62000",
            country: "US"
        )
        def original = new Location(null, "Round-Trip Cafe", "Full round trip", address, "https://example.com/rt.jpg")

        when:
        LocationEntity entity = mapper.toEntity(original)
        Location result = mapper.toModel(entity)

        then:
        result.name == original.name
        result.description == original.description
        result.photoUrl == original.photoUrl
        result.address.line1 == original.address.line1
        result.address.line2 == original.address.line2
        result.address.city == original.address.city
        result.address.state == original.address.state
        result.address.zipCode == original.address.zipCode
        result.address.country == original.address.country
    }
}
