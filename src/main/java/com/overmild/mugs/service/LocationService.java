package com.overmild.mugs.service;

import com.overmild.mugs.entity.LocationEntity;
import com.overmild.mugs.mapper.LocationMapper;
import com.overmild.mugs.model.Location;
import com.overmild.mugs.repository.LocationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service class for managing location operations.
 * Provides business logic for CRUD operations on locations.
 *
 * <p>This service acts as an intermediary between the controller layer
 * and the data access layer, handling location entities and converting them
 * to/from domain models.</p>
 */
@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository repository;
    private final LocationMapper locationMapper;

    /**
     * Retrieves all locations from the database.
     *
     * @return a list of all locations in the system
     */
    public List<Location> getAllLocations() {
        log.info("Fetching all locations from the database");
        return repository.findAll()
                .stream()
                .map(locationMapper::toModel)
                .toList();
    }

    /**
     * Retrieves a location by its unique identifier.
     *
     * @param id the UUID of the location to retrieve
     * @return the location with the specified ID, or null if not found
     */
    public Location getLocationById(UUID id) {
        log.info("Fetching location with ID: {}", id);
        return repository.findById(id)
                .map(locationMapper::toModel)
                .orElse(null);
    }

    /**
     * Creates a new location in the database.
     *
     * @param location the location object to create
     * @return the created location with all fields populated, including generated ID
     */
    public Location createLocation(Location location) {
        log.info("Creating new location with id: {}", location.getId());
        LocationEntity entity = locationMapper.toEntity(location);
        return locationMapper.toModel(repository.save(entity));
    }

    /**
     * Updates an existing location in the database.
     *
     * <p>This method will update all fields of the location. If the location does not exist
     * or has a null ID, the update will fail and return null.</p>
     *
     * @param location the location object containing updated information
     * @return the updated location, or null if the location doesn't exist or ID is null
     */
    public Location updateLocation(Location location) {
        log.info("Updating location with ID: {}", location.getId());
        if (location.getId() == null || repository.findById(location.getId()).isEmpty()) {
            return null;
        }
        var entity = locationMapper.toEntity(location);
        var updatedEntity = repository.save(entity);
        return locationMapper.toModel(updatedEntity);
    }

    /**
     * Deletes a location from the database by its unique identifier.
     *
     * <p>This is a hard delete operation. If the location does not exist,
     * this method will complete without throwing an exception.</p>
     *
     * @param id the UUID of the location to delete
     */
    public void deleteLocation(UUID id) {
        log.info("Deleting location with ID: {}", id);
        repository.deleteById(id);
    }
}
