package com.example.itborrow.service;

import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.repository.*;
import com.example.itborrow.service.avatar.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class EquipmentHistoryImageTest {
    @Test
    void replacingAnImageRetainsReferencedPhotoAndAllowsHistoricalRead() {
        var processor = mock(EquipmentImageProcessor.class);
        var storage = mock(EquipmentStorage.class);
        var equipment = mock(EquipmentRepository.class);
        var history = mock(BorrowItemRepository.class);
        var current = mock(CurrentUser.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var asset = new Equipment();
        asset.setId(1L);
        asset.setImageUrl("/images/equipment/1/old.png");
        when(equipment.existsById(1L)).thenReturn(true);
        when(equipment.findLockedById(1L)).thenReturn(Optional.of(asset));
        when(equipment.findById(1L)).thenReturn(Optional.of(asset));
        when(processor.process(any())).thenReturn(new byte[] { 1, 2, 3 });
        when(history.existsBySnapshotImageUrl("/images/equipment/1/old.png")).thenReturn(true);
        when(storage.readUrl("1/old.png")).thenReturn("https://storage.test/old.png");
        var service = new EquipmentImageService(processor, equipment, current, manager, storage, history,
                mock(PersistentJobs.class));
        service.save(1L, new MockMultipartFile("image", "photo.png", "image/png", new byte[] { 1 }));
        assertThat(asset.getImageUrl()).isNotEqualTo("/images/equipment/1/old.png");
        verify(storage, never()).delete("1/old.png");
        assertThat(service.read(1L, "old.png")).isEqualTo("https://storage.test/old.png");
        assertThatThrownBy(() -> service.read(1L, "unreferenced.png"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
