package com.overmild.mugs.mapper

import com.overmild.mugs.entity.UserEntity
import com.overmild.mugs.model.User
import spock.lang.Specification

class UserMapperSpec extends Specification {

    UserMapper mapper = new UserMapperImpl()

    def "toModel maps all user fields"() {
        given:
        def id = UUID.randomUUID()
        def entity = new UserEntity(id: id, firstName: "John", lastName: "Doe", email: "john@example.com")

        when:
        User user = mapper.toModel(entity)

        then:
        user.id == id
        user.firstName == "John"
        user.lastName == "Doe"
        user.email == "john@example.com"
    }

    def "toModel returns null for null input"() {
        expect:
        mapper.toModel(null) == null
    }

    def "toEntity maps all user fields"() {
        given:
        def id = UUID.randomUUID()
        def user = new User(id, "Jane", "Smith", "jane@example.com")

        when:
        UserEntity entity = mapper.toEntity(user)

        then:
        entity.id == id
        entity.firstName == "Jane"
        entity.lastName == "Smith"
        entity.email == "jane@example.com"
    }

    def "toEntity ignores managed fields"() {
        given:
        def user = new User(null, "Bob", "Jones", "bob@example.com")

        when:
        UserEntity entity = mapper.toEntity(user)

        then:
        entity.mugs == null
        entity.createdAt == null
        entity.modifiedAt == null
    }

    def "toEntity returns null for null input"() {
        expect:
        mapper.toEntity(null) == null
    }

    def "round-trip toEntity then toModel preserves all fields"() {
        given:
        def id = UUID.randomUUID()
        def original = new User(id, "Alice", "Brown", "alice@example.com")

        when:
        UserEntity entity = mapper.toEntity(original)
        User result = mapper.toModel(entity)

        then:
        result.id == original.id
        result.firstName == original.firstName
        result.lastName == original.lastName
        result.email == original.email
    }
}
