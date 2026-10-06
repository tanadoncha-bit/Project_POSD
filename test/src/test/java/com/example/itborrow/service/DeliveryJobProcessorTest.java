package com.example.itborrow.service;
import com.example.itborrow.repository.DeliveryJobRepository;
import com.example.itborrow.repository.EquipmentImageReferenceRepository;
import com.example.itborrow.service.jobs.*;
import com.example.itborrow.service.avatar.ImageStorage;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
class DeliveryJobProcessorTest {
    @Test void customHandlerExecutesWithoutChangingProcessor() {
        var repository=mock(DeliveryJobRepository.class);
        var handler=mock(DeliveryJobHandler.class);
        when(handler.kind()).thenReturn("CUSTOM");when(handler.available()).thenReturn(true);
        Map<String,Object> job=Map.of("id",7L,"kind","CUSTOM");
        when(repository.lockPending(false)).thenReturn(List.of(job));
        new DeliveryJobProcessor(repository,List.of(handler)).process();
        verify(handler).execute(job);verify(repository).complete(7L);
    }
    @Test void unknownJobRetriesWithoutCompletingOrExecutingAnotherHandler() {
        var repository=mock(DeliveryJobRepository.class);
        var handler=mock(DeliveryJobHandler.class);when(handler.kind()).thenReturn("STORAGE_DELETE");
        when(repository.lockPending(false)).thenReturn(List.of(Map.of("id",9L,"kind","UNKNOWN")));
        new DeliveryJobProcessor(repository,List.of(handler)).process();
        verify(repository).retry(eq(9L),isA(IllegalArgumentException.class));verify(repository,never()).complete(anyLong());verify(handler,never()).execute(any());
    }
    @Test void cleanupPreservesReferencedImages() {
        var storage=mock(ImageStorage.class);var references=mock(EquipmentImageReferenceRepository.class);
        when(references.isReferenced("asset/photo.png")).thenReturn(true);
        new StorageCleanupHandler(storage,references).execute(Map.of("payload","asset/photo.png"));
        verify(storage,never()).delete(anyString());
    }
}
