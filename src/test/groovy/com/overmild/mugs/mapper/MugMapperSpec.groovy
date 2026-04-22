package com.overmild.mugs.mapper

import com.overmild.mugs.entity.LocationEntity
import com.overmild.mugs.entity.MugEntity
import com.overmild.mugs.entity.UserEntity
import com.overmild.mugs.model.Location
import com.overmild.mugs.model.Mug
import com.overmild.mugs.model.User
import spock.lang.Specification

class MugMapperSpec extends Specification {

    LocationMapper locationMapper = Mock()
    UserMapper userMapper = Mock()
    MugMapperImpl mapper = new MugMapperImpl()

    def setup() {
        def locationField = MugMapperImpl.getDeclaredField('locationMapper')
        locationField.accessible = true
        locationField.set(mapper, locationMapper)

        def userField = MugMapperImpl.getDeclaredField('userMapper')
        userField.accessible = true
        userField.set(mapper, userMapper)
    }

    def "toModel maps all mug fields including nested user and location"() {
        given:
        def id = UUID.randomUUID()
        def userId = UUID.randomUUID()
        def locationId = UUID.randomUUID()
        def userEntity = new UserEntity(id: userId, firstName: "John", lastName: "Doe", email: "john@example.com")
        def locationEntity = new LocationEntity(id: locationId, name: "Home")
        def entity = new MugEntity(id: id, displayName: "My Mug", user: userEntity, location: locationEntity)
        def user = new User(userId, "John", "Doe", "john@example.com")
        def location = new Location(locationId, "Home", null, null, null)

        userMapper.toModel(userEntity) >> user
        locationMapper.toModel(locationEntity) >> location

        when:
        Mug mug = mapper.toModel(entity)

        then:
        mug.id == id
        mug.displayName == "My Mug"
        mug.user.firstName == "John"
        mug.location.name == "Home"
    }

    def "toModel returns null for null input"() {
        expect:
        mapper.toModel(null) == null
    }

    def "toModel handles null user and location"() {
        given:
        def id = UUID.randomUUID()
        def entity = new MugEntity(id: id, displayName: "Simple Mug", user: null, location: null)

        userMapper.toModel(null) >> null
        locationMapper.toModel(null) >> null

        when:
        Mug mug = mapper.toModel(entity)

        then:
        mug.id == id
        mug.displayName == "Simple Mug"
        mug.user == null
        mug.location == null
    }

    def "toEntity maps id and displayName"() {
        given:
        def id = UUID.randomUUID()
        def mug = new Mug(id, "New Mug", null, null)

        userMapper.toEntity(null) >> null
        locationMapper.toEntity(null) >> null

        when:
        MugEntity entity = mapper.toEntity(mug)

        then:
        entity.id == id
        entity.displayName == "New Mug"
    }

    def "toEntity returns null for null input"() {
        expect:
        mapper.toEntity(null) == null
    }
}
