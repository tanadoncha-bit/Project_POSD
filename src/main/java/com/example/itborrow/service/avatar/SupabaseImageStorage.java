package com.example.itborrow.service.avatar;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

@Component
@org.springframework.context.annotation.Primary
public class SupabaseImageStorage implements ImageStorage {
    private final String baseUrl, key, bucket;
    private final HttpClient client;
    private final ObjectMapper json = new ObjectMapper();
    public SupabaseImageStorage(@Value("${app.storage.url:}") String url,
            @Value("${app.storage.service-key:}") String key,
            @Value("${app.storage.bucket:avatars}") String bucket) {
        this.baseUrl=url.replaceAll("/+$",""); this.key=key; this.bucket=bucket;
        this.client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    private String object(String path) {
        if(!bucket.matches("[A-Za-z0-9_-]+") || !path.matches("[0-9]+/[a-f0-9-]+[.]png")) throw new StorageException();
        return bucket+"/"+path;
    }
    private HttpRequest.Builder request(String path) {
        if(baseUrl.isBlank() || key.isBlank()) throw new StorageException();
        var uri=URI.create(baseUrl);
        if(!"https".equals(uri.getScheme()) && !("http".equals(uri.getScheme()) && "localhost".equals(uri.getHost()))) throw new StorageException();
        return HttpRequest.newBuilder(URI.create(baseUrl+"/storage/v1"+path)).timeout(Duration.ofSeconds(15))
            .header("apikey",key).header("Authorization","Bearer "+key);
    }
    private String send(HttpRequest request) {
        try {
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()<200 || response.statusCode()>=300) throw new StorageException();
            return response.body();
        } catch(InterruptedException ex) { Thread.currentThread().interrupt(); throw new StorageException(); }
        catch(java.io.IOException ex) { throw new StorageException(); }
    }
    public void upload(String path,byte[] png) {
        send(request("/object/"+object(path)).header("Content-Type","image/png").header("Cache-Control","max-age=3600")
            .POST(HttpRequest.BodyPublishers.ofByteArray(png)).build());
    }
    public String readUrl(String path) {
        String result=send(request("/object/sign/"+object(path)).header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"expiresIn\":3600}")).build());
        try {
            String signed=json.readTree(result).path("signedURL").asText();
            if(!signed.startsWith("/object/sign/"+object(path)+"?")) throw new StorageException();
            return baseUrl+"/storage/v1"+signed;
        } catch(RuntimeException ex) { throw new StorageException(); }
    }
    public void delete(String path) {
        object(path); // Validate before including the path in the request body.
        send(request("/object/"+bucket).header("Content-Type","application/json")
            .method("DELETE",HttpRequest.BodyPublishers.ofString("{\"prefixes\":[\""+path+"\"]}")).build());
    }
}
