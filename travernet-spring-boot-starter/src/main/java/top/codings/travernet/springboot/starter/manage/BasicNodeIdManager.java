package top.codings.travernet.springboot.starter.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import top.codings.travernet.model.node.bean.NodeType;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class BasicNodeIdManager implements NodeIdManager {
    private final String idFileName = "_travernet.id";
    private final String prefix;

    public BasicNodeIdManager(String prefix) {
        if (StrUtil.isNotBlank(prefix)) {
            prefix = "_" + prefix;
        } else {
            prefix = "";
        }
        this.prefix = prefix.replace("-", "_").replace(".", "_");
    }

    @Override
    public String findId(NodeType nodeType) {

        String id;
        File file = new File(nodeType.name().toLowerCase() + prefix + idFileName);
        if (FileUtil.exist(file)) {
            id = FileUtil.readString(file, StandardCharsets.UTF_8);
        } else {
            id = UUID.randomUUID().toString(true);
            FileUtil.writeString(id, file, StandardCharsets.UTF_8);
        }
        return id;
    }
}
