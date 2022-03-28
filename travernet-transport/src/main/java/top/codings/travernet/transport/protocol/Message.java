package top.codings.travernet.transport.protocol;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
public class Message<T> implements Serializable {
    private MessageProperty property;
    private Header header;
    private T content;
    private Map<String, Object> attachments;
}
