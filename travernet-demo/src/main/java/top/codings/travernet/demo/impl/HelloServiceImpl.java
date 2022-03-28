package top.codings.travernet.demo.impl;

import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.common.annotation.TraverService;
import top.codings.travernet.demo.facade.HelloService;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Slf4j
@TraverService("hello")
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(int[] arrays) {
        log.info("执行方法 hello");
        return String.format("经过详细缜密的计算后，您的计算表达式(%s)结果为%s", String.join("+", Arrays.stream(arrays).mapToObj(String::valueOf).toArray(String[]::new)), Arrays.stream(arrays).reduce((left, right) -> left + right).getAsInt());
    }

    @Override
    public CompletableFuture<String> helloAsync(int[] arrays) {
        log.info("执行方法 helloAsync");
        return CompletableFuture.supplyAsync(() -> hello(arrays));
    }

    @Override
    public CompletableFuture<String> helloNull() {
        log.info("执行方法 helloNull");
        return null;
    }
}
