package com.overmild.mugs.service

import com.overmild.mugs.entity.MugEntity
import com.overmild.mugs.exception.ResourceNotFoundException
import com.overmild.mugs.mapper.MugMapper
import com.overmild.mugs.model.Mug
import com.overmild.mugs.repository.MugRepository
import spock.lang.Specification

class MugServiceSpec extends Specification {

    MugRepository repository = Mock()
    MugMapper mugMapper = Mock()
    MugService service = new MugService(repository, mugMapper)

    def "getAllMugs returns mapped list of mugs"() {
        given:
        def id = UUID.randomUUID()
        def entity = new MugEntity(id: id, displayName: "Classic Mug")
        def mug = new Mug(id, "Classic Mug", null, null)

        repository.findAll() >> [entity]
        mugMapper.toModel(entity) >> mug

        when:
        List<Mug> result = service.getAllMugs()

        then:
        result.size() == 1
        result[0].displayName == "Classic Mug"
    }

    def "getAllMugs returns empty list when no mugs exist"() {
        given:
        repository.findAll() >> []

        when:
        List<Mug> result = service.getAllMugs()

        then:
        result.isEmpty()
    }

    def "getMugById returns mug when found"() {
        given:
        def id = UUID.randomUUID()
        def entity = new MugEntity(id: id, displayName: "Travel Mug")
        def mug = new Mug(id, "Travel Mug", null, null)

        repository.findById(id) >> Optional.of(entity)
        mugMapper.toModel(entity) >> mug

        when:
        Mug result = service.getMugById(id)

        then:
        result.id == id
        result.displayName == "Travel Mug"
    }

    def "getMugById throws ResourceNotFoundException when not found"() {
        given:
        def id = UUID.randomUUID()
        repository.findById(id) >> Optional.empty()

        when:
        service.getMugById(id)

        then:
        thrown(ResourceNotFoundException)
    }

    def "getMugsByUserId returns all mugs for the user"() {
        given:
        def userId = UUID.randomUUID()
        def entity1 = new MugEntity(id: UUID.randomUUID(), displayName: "Mug A")
        def entity2 = new MugEntity(id: UUID.randomUUID(), displayName: "Mug B")
        def mug1 = new Mug(entity1.id, "Mug A", null, null)
        def mug2 = new Mug(entity2.id, "Mug B", null, null)

        repository.findAllByUserId(userId) >> [entity1, entity2]
        mugMapper.toModel(entity1) >> mug1
        mugMapper.toModel(entity2) >> mug2

        when:
        List<Mug> result = service.getMugsByUserId(userId)

        then:
        result.size() == 2
        result*.displayName.containsAll(["Mug A", "Mug B"])
    }

    def "getMugsByUserId returns empty list when user has no mugs"() {
        given:
        def userId = UUID.randomUUID()
        repository.findAllByUserId(userId) >> []

        when:
        List<Mug> result = service.getMugsByUserId(userId)

        then:
        result.isEmpty()
    }

    def "createMug persists and returns mug"() {
        given:
        def input = new Mug(null, "New Mug", null, null)
        def entity = new MugEntity(displayName: "New Mug")
        def savedEntity = new MugEntity(id: UUID.randomUUID(), displayName: "New Mug")
        def expected = new Mug(savedEntity.id, "New Mug", null, null)

        mugMapper.toEntity(input) >> entity
        repository.save(entity) >> savedEntity
        mugMapper.toModel(savedEntity) >> expected

        when:
        Mug result = service.createMug(input)

        then:
        result.displayName == "New Mug"
    }

    def "updateMug updates and returns mug"() {
        given:
        def id = UUID.randomUUID()
        def updated = new Mug(id, "Updated Mug", null, null)
        def entity = new MugEntity(id: id, displayName: "Updated Mug")
        def savedEntity = new MugEntity(id: id, displayName: "Updated Mug")
        def expected = new Mug(id, "Updated Mug", null, null)

        repository.findById(id) >> Optional.of(entity)
        mugMapper.toEntity(updated) >> entity
        repository.save(entity) >> savedEntity
        mugMapper.toModel(savedEntity) >> expected

        when:
        Mug result = service.updateMug(updated)

        then:
        result.displayName == "Updated Mug"
    }

    def "updateMug throws ResourceNotFoundException when mug does not exist"() {
        given:
        def id = UUID.randomUUID()
        def mug = new Mug(id, "Ghost Mug", null, null)
        repository.findById(id) >> Optional.empty()

        when:
        service.updateMug(mug)

        then:
        thrown(ResourceNotFoundException)
        0 * repository.save(_)
    }

    def "updateMug throws ResourceNotFoundException when id is null"() {
        given:
        def mug = new Mug(null, "No ID Mug", null, null)

        when:
        service.updateMug(mug)

        then:
        thrown(ResourceNotFoundException)
        0 * repository._
    }

    def "deleteMug calls repository deleteById"() {
        given:
        def id = UUID.randomUUID()

        when:
        service.deleteMug(id)

        then:
        1 * repository.deleteById(id)
    }
}
