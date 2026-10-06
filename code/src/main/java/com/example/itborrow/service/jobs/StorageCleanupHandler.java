package com.example.itborrow.service.jobs;
import com.example.itborrow.service.avatar.ImageStorage;
import com.example.itborrow.repository.EquipmentImageReferenceRepository;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.Map;
@Component
public class StorageCleanupHandler implements DeliveryJobHandler {
    private final ImageStorage storage;
    private final EquipmentImageReferenceRepository references;
    public StorageCleanupHandler(@Qualifier("equipmentStorage") ImageStorage storage,EquipmentImageReferenceRepository references) {this.storage=storage;this.references=references;}
    public String kind() {return "STORAGE_DELETE";}
    public void execute(Map<String,Object> job) {String path=(String)job.get("payload");if(!references.isReferenced(path))storage.delete(path);}
}
