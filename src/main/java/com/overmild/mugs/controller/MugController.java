package com.overmild.mugs.controller;

import com.overmild.mugs.model.Mug;
import com.overmild.mugs.service.MugService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MugController {

    private final MugService mugService;

    @GetMapping("/mugs")
    public ResponseEntity<List<Mug>> getAllMugs() {
        List<Mug> mugs = mugService.getAllMugs();
        return ResponseEntity.ok(mugs);
    }

    @GetMapping("/mug/{id}")
    public ResponseEntity<Mug> getMugById(@PathVariable UUID id) {
        Mug mug = mugService.getMugById(id);
        return ResponseEntity.ok(mug);
    }

    @GetMapping("/user/{userId}/mugs")
    public ResponseEntity<List<Mug>> getMugsByUserId(@PathVariable UUID userId) {
        List<Mug> mugs = mugService.getMugsByUserId(userId);
        return ResponseEntity.ok(mugs);
    }

    @PostMapping("/mug")
    public ResponseEntity<Mug> createMug(@RequestBody Mug mug) {
        Mug createdMug = mugService.createMug(mug);
        return ResponseEntity.ok(createdMug);
    }

    @PutMapping("/mug")
    public ResponseEntity<Mug> updateMug(@RequestBody Mug mug) {
        Mug updatedMug = mugService.updateMug(mug);
        return ResponseEntity.ok(updatedMug);
    }

    @DeleteMapping("/mug/{id}")
    public ResponseEntity<Void> deleteMug(@PathVariable UUID id) {
        mugService.deleteMug(id);
        return ResponseEntity.ok().build();
    }
}
