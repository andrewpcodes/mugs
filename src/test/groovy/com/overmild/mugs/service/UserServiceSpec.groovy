package com.overmild.mugs.service

import com.overmild.mugs.entity.UserEntity
import com.overmild.mugs.exception.ConflictException
import com.overmild.mugs.exception.ResourceNotFoundException
import com.overmild.mugs.mapper.UserMapper
import com.overmild.mugs.model.User
import com.overmild.mugs.repository.UserRepository
import spock.lang.Specification

class UserServiceSpec extends Specification {

    UserRepository repository = Mock()
    UserMapper userMapper = Mock()
    UserService service = new UserService(repository, userMapper)

    def "getAllUsers returns mapped list of users"() {
        given:
        def id = UUID.randomUUID()
        def entity = new UserEntity(id: id, firstName: "John", lastName: "Doe", email: "john@example.com")
        def user = new User(id, "John", "Doe", "john@example.com")

        repository.findAll() >> [entity]
        userMapper.toModel(entity) >> user

        when:
        List<User> result = service.getAllUsers()

        then:
        result.size() == 1
        result[0].firstName == "John"
        result[0].email == "john@example.com"
    }

    def "getAllUsers returns empty list when no users exist"() {
        given:
        repository.findAll() >> []

        when:
        List<User> result = service.getAllUsers()

        then:
        result.isEmpty()
    }

    def "getUserById returns user when found"() {
        given:
        def id = UUID.randomUUID()
        def entity = new UserEntity(id: id, firstName: "Jane", lastName: "Smith", email: "jane@example.com")
        def user = new User(id, "Jane", "Smith", "jane@example.com")

        repository.findById(id) >> Optional.of(entity)
        userMapper.toModel(entity) >> user

        when:
        User result = service.getUserById(id)

        then:
        result.id == id
        result.firstName == "Jane"
    }

    def "getUserById throws ResourceNotFoundException when not found"() {
        given:
        def id = UUID.randomUUID()
        repository.findById(id) >> Optional.empty()

        when:
        service.getUserById(id)

        then:
        thrown(ResourceNotFoundException)
    }

    def "createUser persists and returns user"() {
        given:
        def input = new User(null, "Alice", "Brown", "alice@example.com")
        def entity = new UserEntity(firstName: "Alice", lastName: "Brown", email: "alice@example.com")
        def savedEntity = new UserEntity(id: UUID.randomUUID(), firstName: "Alice", lastName: "Brown", email: "alice@example.com")
        def expected = new User(savedEntity.id, "Alice", "Brown", "alice@example.com")

        repository.existsByEmail("alice@example.com") >> false
        userMapper.toEntity(input) >> entity
        repository.save(entity) >> savedEntity
        userMapper.toModel(savedEntity) >> expected

        when:
        User result = service.createUser(input)

        then:
        result.firstName == "Alice"
        result.email == "alice@example.com"
    }

    def "createUser throws ConflictException when email already exists"() {
        given:
        def input = new User(null, "Bob", "Jones", "existing@example.com")
        repository.existsByEmail("existing@example.com") >> true

        when:
        service.createUser(input)

        then:
        thrown(ConflictException)
        0 * repository.save(_)
    }

    def "updateUser updates and returns user"() {
        given:
        def id = UUID.randomUUID()
        def updated = new User(id, "Updated", "Name", "updated@example.com")
        def entity = new UserEntity(id: id, firstName: "Updated", lastName: "Name", email: "updated@example.com")
        def savedEntity = new UserEntity(id: id, firstName: "Updated", lastName: "Name", email: "updated@example.com")
        def expected = new User(id, "Updated", "Name", "updated@example.com")

        repository.findById(id) >> Optional.of(entity)
        repository.existsByEmailAndIdNot("updated@example.com", id) >> false
        userMapper.toEntity(updated) >> entity
        repository.save(entity) >> savedEntity
        userMapper.toModel(savedEntity) >> expected

        when:
        User result = service.updateUser(updated)

        then:
        result.firstName == "Updated"
        result.email == "updated@example.com"
    }

    def "updateUser throws ResourceNotFoundException when user does not exist"() {
        given:
        def id = UUID.randomUUID()
        def user = new User(id, "Ghost", "User", "ghost@example.com")
        repository.findById(id) >> Optional.empty()

        when:
        service.updateUser(user)

        then:
        thrown(ResourceNotFoundException)
        0 * repository.save(_)
    }

    def "updateUser throws ResourceNotFoundException when id is null"() {
        given:
        def user = new User(null, "No", "Id", "noid@example.com")

        when:
        service.updateUser(user)

        then:
        thrown(ResourceNotFoundException)
        0 * repository._
    }

    def "updateUser throws ConflictException when email is taken by another user"() {
        given:
        def id = UUID.randomUUID()
        def user = new User(id, "Tom", "Conflict", "taken@example.com")
        def entity = new UserEntity(id: id, firstName: "Tom", lastName: "Conflict", email: "taken@example.com")

        repository.findById(id) >> Optional.of(entity)
        repository.existsByEmailAndIdNot("taken@example.com", id) >> true

        when:
        service.updateUser(user)

        then:
        thrown(ConflictException)
        0 * repository.save(_)
    }

    def "deleteUser calls repository deleteById"() {
        given:
        def id = UUID.randomUUID()

        when:
        service.deleteUser(id)

        then:
        1 * repository.deleteById(id)
    }
}
