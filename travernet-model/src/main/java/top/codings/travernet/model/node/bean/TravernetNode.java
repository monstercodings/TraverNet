package top.codings.travernet.model.node.bean;

import java.util.concurrent.CompletableFuture;

public interface TravernetNode<Start, Close> {
    CompletableFuture<Start> start();

    CompletableFuture<Close> close();
}
