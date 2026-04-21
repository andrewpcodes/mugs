package com.overmild.mugs.controller;

import com.overmild.mugs.model.Location;
import com.overmild.mugs.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller that exposes CRUD endpoints for Location resources.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET    /locations        - list all locations</li>
 *   <li>GET    /locations/{id}   - get a location by id</li>
 *   <li>POST   /locations        - create a new location</li>
 *   <li>PUT    /locations        - update an existing location</li>
 *   <li>DELETE /locations/{id}   - delete a location by id</li>
 * </ul>
 *
 * The controller delegates business logic to {@link LocationService}.
 */
@RestController
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    /**
     * Retrieve all locations.
     *
     * @return ResponseEntity containing the list of all {@link Location} objects and HTTP 200.
     */
    @GetMapping("/locations")
    public ResponseEntity<List<Location>> getAllLocations() {
        List<Location> locations = locationService.getAllLocations();
        return ResponseEntity.ok(locations);
    }

    /**
     * Retrieve a single location by UUID.
     *
     * @param id UUID of the location to retrieve; taken from the path variable "id".
     * @return ResponseEntity containing the requested {@link Location} and HTTP 200.
     */
    @GetMapping("/locations/{id}")
    public ResponseEntity<Location> getLocationById(@PathVariable UUID id) {
        Location location = locationService.getLocationById(id);
        return ResponseEntity.ok(location);
    }

    /**
     * Create a new location.
     *
     * @param location Location object deserialized from the request body.
     * @return ResponseEntity containing the created {@link Location} and HTTP 200.
     */
    @PostMapping("/locations")
    public ResponseEntity<Location> createLocation(@Valid @RequestBody Location location) {
        Location createdLocation = locationService.createLocation(location);
        return ResponseEntity.ok(createdLocation);
    }

    /**
     * Update an existing location.
     *
     * @param location Location object containing updated fields deserialized from the request body.
     * @return ResponseEntity containing the updated {@link Location} and HTTP 200.
     */
    @PutMapping("/locations")
    public ResponseEntity<Location> updateLocation(@Valid @RequestBody Location location) {
        Location updatedLocation = locationService.updateLocation(location);
        return ResponseEntity.ok(updatedLocation);
    }

    /**
     * Delete a location by UUID.
     *
     * @param id UUID of the location to delete; taken from the path variable.
     * @return ResponseEntity with HTTP 200 and empty body on successful deletion.
     */
    @DeleteMapping("/locations/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable UUID id) {
        locationService.deleteLocation(id);
        return ResponseEntity.ok().build();
    }
}
