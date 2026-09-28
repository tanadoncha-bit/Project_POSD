package com.example.itborrow.service;

import com.example.itborrow.service.avatar.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.assertj.core.api.Assertions.*;

class SupabaseImageStorageTest {
    @Test void uploadSignAndDeleteUseDocumentedHttpContract() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("localhost",0),0);
        var calls=new CopyOnWriteArrayList<String>();
        var bodies=new CopyOnWriteArrayList<String>();
        var keys=new CopyOnWriteArrayList<String>();
        server.createContext("/storage/v1/", exchange -> {
            calls.add(exchange.getRequestMethod()+" "+exchange.getRequestURI().getPath());
            bodies.add(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            keys.add(exchange.getRequestHeaders().getFirst("Authorization"));
            String body=exchange.getRequestURI().getPath().contains("/sign/") ? "{\"signedURL\":\"/object/sign/avatars/1/abc.png?token=test\"}" : "{}";
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(200,bytes.length);
            exchange.getResponseBody().write(bytes);exchange.close();
        });
        server.start();
        try {
            String url="http://localhost:"+server.getAddress().getPort();
            var storage=new SupabaseImageStorage(url,"test-secret","avatars");
            storage.upload("1/abc.png",new byte[]{1,2,3});
            assertThat(storage.readUrl("1/abc.png")).isEqualTo(url+"/storage/v1/object/sign/avatars/1/abc.png?token=test");
            storage.delete("1/abc.png");
            assertThat(calls).containsExactly("POST /storage/v1/object/avatars/1/abc.png","POST /storage/v1/object/sign/avatars/1/abc.png","DELETE /storage/v1/object/avatars");
            assertThat(bodies.get(1)).isEqualTo("{\"expiresIn\":3600}");
            assertThat(bodies.get(2)).isEqualTo("{\"prefixes\":[\"1/abc.png\"]}");
            assertThat(keys).containsOnly("Bearer test-secret");
            assertThatThrownBy(()->storage.upload("../bad.png",new byte[0])).isInstanceOf(StorageException.class);
        } finally { server.stop(0); }
    }
    @Test void providerErrorsDoNotExposeKeysOrResponseDetails() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("localhost",0),0);
        server.createContext("/", e -> {e.getRequestBody().readAllBytes();e.sendResponseHeaders(403,0);e.getResponseBody().write("private provider details".getBytes());e.close();});
        server.start();
        try {
            var storage=new SupabaseImageStorage("http://localhost:"+server.getAddress().getPort(),"test-secret","avatars");
            assertThatThrownBy(()->storage.upload("1/abc.png",new byte[]{1})).isInstanceOf(StorageException.class)
                .hasMessageNotContaining("test-secret").hasMessageNotContaining("private provider");
        } finally { server.stop(0); }
    }
}
