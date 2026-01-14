package com.overmild.mugs.service;

import com.overmild.mugs.entity.UserEntity;
import com.overmild.mugs.mapper.UserMapper;
import com.overmild.mugs.model.User;
import com.overmild.mugs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service class for managing user operations.
 * Provides business logic for CRUD operations on users.
 *
 * <p>This service acts as an intermediary between the controller layer
 * and the data access layer, handling user entities and converting them
 * to/from domain models.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final UserMapper userMapper;

    /**
     * Retrieves all users from the database.
     *
     * @return a list of all users in the system
     */
    public List<User> getAllUsers() {
        log.info("Fetching all users from the database");
        return repository.findAll()
                .stream()
                .map(userMapper::toModel)
                .toList();
    }

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the UUID of the user to retrieve
     * @return the user with the specified ID, or null if not found
     */
    public User getUserById(UUID id) {
        log.info("Fetching user with ID: {}", id);
        return repository.findById(id)
                .map(userMapper::toModel)
                .orElse(null);
    }

    /**
     * Creates a new user in the database.
     *
     * @param user the user object to create
     * @return the created user with all fields populated, including generated ID
     */
    public User createUser(User user) {
        log.info("Creating new user with id");
        UserEntity entity = userMapper.toEntity(user);
        return userMapper.toModel(repository.save(entity));
    }

    /**
     * Updates an existing user in the database.
     *
     * <p>This method will update all fields of the user. If the user does not exist
     * or has a null ID, the update will fail and return null.</p>
     *
     * @param user the user object containing updated information
     * @return the updated user, or null if the user doesn't exist or ID is null
     */
    @Transactional
    public User updateUser(User user) {
        log.info("Updating user with ID: {}", user.getId());
        if (user.getId() == null || repository.findById(user.getId()).isEmpty()) {
            return null;
        }
        var entity = userMapper.toEntity(user);
        var updatedEntity = repository.save(entity);
        return userMapper.toModel(updatedEntity);
    }

    /**
     * Deletes a user from the database by their unique identifier.
     *
     * <p>This is a hard delete operation. If the user does not exist,
     * this method will complete without throwing an exception.</p>
     *
     * @param id the UUID of the user to delete
     */
    @Transactional
    public void deleteUser(UUID id) {
        log.info("Deleting user with ID: {}", id);
        repository.deleteById(id);
    }
}
