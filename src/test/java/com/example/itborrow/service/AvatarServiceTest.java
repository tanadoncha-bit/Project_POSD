package com.example.itborrow.service;
import com.example.itborrow.repository.AvatarRepository;
import com.example.itborrow.service.avatar.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class AvatarServiceTest {
    @Test void profileUrlReusesSignatureButNewImageGetsNewUrl() {
        var repository=mock(AvatarRepository.class); var storage=mock(ImageStorage.class);
        var service=new AvatarService(repository,storage,mock(AvatarImageProcessor.class),mock(PlatformTransactionManager.class));
        when(repository.path(1L)).thenReturn("1/old.png", "1/old.png", "1/new.png");
        when(storage.readUrl("1/old.png")).thenReturn("old-signed-url");
        when(storage.readUrl("1/new.png")).thenReturn("new-signed-url");
        assertThat(service.url(1L)).isEqualTo("old-signed-url");
        assertThat(service.url(1L)).isEqualTo("old-signed-url");
        assertThat(service.url(1L)).isEqualTo("new-signed-url");
        verify(storage,times(1)).readUrl("1/old.png");
        verify(storage,times(1)).readUrl("1/new.png");
    }
    @Test void metadataFailureRollsBackAndRemovesNewObject() {
        var repository=mock(AvatarRepository.class);var storage=mock(ImageStorage.class);
        var processor=mock(AvatarImageProcessor.class);var manager=mock(PlatformTransactionManager.class);
        var status=new SimpleTransactionStatus();when(manager.getTransaction(any())).thenReturn(status);
        when(processor.process(any())).thenReturn(new byte[]{1});
        doThrow(new IllegalStateException("Database unavailable")).when(repository).setPath(eq(1L),anyString());
        var service=new AvatarService(repository,storage,processor,manager);
        assertThatThrownBy(()->service.save(1L,new MockMultipartFile("image",new byte[]{1}))).isInstanceOf(IllegalStateException.class);
        var path=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(storage).upload(path.capture(),any()); verify(storage).delete(path.getValue()); verify(manager).rollback(status);
    }
}
