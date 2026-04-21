package com.overmild.mugs.controller;

import com.overmild.mugs.model.Mug;
import com.overmild.mugs.model.User;
import com.overmild.mugs.service.MugService;
import com.overmild.mugs.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller that exposes CRUD endpoints for User resources.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET    /users          - list all users</li>
 *   <li>GET    /users/{id}     - get a user by id</li>
 *   <li>GET    /users/{userId}/mugs - list all mugs for a user</li>
 *   <li>POST   /users          - create a new user</li>
 *   <li>PUT    /users          - update an existing user</li>
 *   <li>DELETE /users/{id}     - delete a user by id</li>
 * </ul>
 *
 * The controller delegates business logic to {@link UserService}.
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final MugService mugService;

    /**
     * Retrieve all users.
     *
     * @return ResponseEntity containing the list of all {@link User} objects and HTTP 200.
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUserInfo() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieve a single user by UUID.
     *
     * @param id UUID of the user to retrieve; taken from the path variable "id".
     * @return ResponseEntity containing the requested {@link User} and HTTP 200.
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Retrieve all mugs for a specific user by user UUID.
     *
     * @param userId UUID of the user whose mugs to retrieve; taken from the path variable.
     * @return ResponseEntity containing the list of {@link Mug} objects and HTTP 200.
     */
    @GetMapping("/users/{userId}/mugs")
    public ResponseEntity<List<Mug>> getMugsByUserId(@PathVariable UUID userId) {
        List<Mug> mugs = mugService.getMugsByUserId(userId);
        return ResponseEntity.ok(mugs);
    }

    /**
     * Create a new user.
     *
     * @param user User object deserialized from the request body.
     * @return ResponseEntity containing the created {@link User} and HTTP 200.
     */
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(createdUser);
    }

    /**
     * Update an existing user.
     *
     * @param user User object containing updated fields deserialized from the request body.
     * @return ResponseEntity containing the updated {@link User} and HTTP 200.
     */
    @PutMapping("/users")
    public ResponseEntity<User> updateUser(@RequestBody User user) {
        User updatedUser = userService.updateUser(user);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Delete a user by UUID.
     *
     * @param id UUID of the user to delete; taken from the path variable.
     * @return ResponseEntity with HTTP 200 and empty body on successful deletion.
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}