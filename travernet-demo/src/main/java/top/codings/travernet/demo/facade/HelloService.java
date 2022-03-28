package top.codings.travernet.demo.facade;

import top.codings.travernet.common.annotation.TraverService;

import java.util.concurrent.CompletableFuture;

@TraverService
public interface HelloService {
    String  hello(int[] arrays);

    CompletableFuture<String> helloAsync(int[] arrays);

    CompletableFuture<String> helloNull();
}
