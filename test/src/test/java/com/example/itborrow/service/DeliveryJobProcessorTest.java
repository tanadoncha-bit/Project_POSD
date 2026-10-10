package com.example.itborrow.service;

import static org.mockito.Mockito.*;

import com.example.itborrow.repository.DeliveryJobRepository;
import com.example.itborrow.repository.EquipmentImageReferenceRepository;
import com.example.itborrow.service.jobs.*;
import com.example.itborrow.service.storage.ImageStorage;

import org.junit.jupiter.api.Test;

import java.util.*;

class DeliveryJobProcessorTest {
    @Test
    void customHandlerExecutesWithoutChangingProcessor() {
        var repository = mock(DeliveryJobRepository.class);
        var handler = mock(DeliveryJobHandler.class);
        when(handler.kind()).thenReturn("CUSTOM");
        when(handler.available()).thenReturn(true);
        Map<String, Object> job =
                Map.of("id", 7L, "kind", "CUSTOM", "lease_token", "lease", "attempts", 1);
        when(repository.claim(false)).thenReturn(job, Map.of());
        new DeliveryJobProcessor(repository, List.of(handler)).process();
        verify(handler).execute(job);
        verify(repository).complete(7L, "lease");
    }

    @Test
    void unknownJobRetriesWithoutCompletingOrExecutingAnotherHandler() {
        var repository = mock(DeliveryJobRepository.class);
        var handler = mock(DeliveryJobHandler.class);
        when(handler.kind()).thenReturn("STORAGE_DELETE");
        when(repository.claim(false))
                .thenReturn(
                        Map.of("id", 9L, "kind", "UNKNOWN", "lease_token", "lease", "attempts", 1),
                        Map.of());
        new DeliveryJobProcessor(repository, List.of(handler)).process();
        verify(repository).retry(eq(9L), eq("lease"), eq(1), isA(IllegalStateException.class));
        verify(repository, never()).complete(anyLong(), anyString());
        verify(handler, never()).execute(any());
    }

    @Test
    void cleanupPreservesReferencedImages() {
        var storage = mock(ImageStorage.class);
        var references = mock(EquipmentImageReferenceRepository.class);
        when(references.isReferenced("asset/photo.png")).thenReturn(true);
        new StorageCleanupHandler(storage, references)
                .execute(Map.of("payload", "asset/photo.png"));
        verify(storage, never()).delete(anyString());
    }
}
