package top.codings.travernet.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import top.codings.travernet.client.core.NodeClient;
import top.codings.travernet.demo.facade.HelloService;
import top.codings.travernet.model.node.bean.NodeType;
import top.codings.travernet.registry.core.NodeRegistry;
import top.codings.travernet.springboot.starter.anno.EnableTraverNet;

import java.util.Map;

@Slf4j
@EnableTraverNet(basePackages = "top.codings.travernet.demo.facade")
//@EnableTraverNet(basePackages = "top.codings.travernet.demo.impl")
@SpringBootApplication
public class SpringMain {
    public static void main(String[] args) throws Exception {
        /**
         * SpringBoot程序启动后，返回值是ConfigurableApplicationContext，它也是一个Spring容器
         * 它其实相当于原来Spring容器中的ClasspathXmlApplicationContext
         */
        //获取SpringBoot容器
        ConfigurableApplicationContext applicationContext = SpringApplication.run(SpringMain.class, args);
        Map<String, NodeRegistry> beansOfType = applicationContext.getBeansOfType(NodeRegistry.class);
        if (!beansOfType.isEmpty()) {
            return;
        }
        NodeClient client = applicationContext.getBean(NodeClient.class);
        if (client.getNetNode().getNodeType() == NodeType.PROVIDER) {
            return;
        }
        HelloService bean = applicationContext.getBean(HelloService.class);
        log.info("开始远程执行");
//        bean.helloAsync(null).whenComplete((s, throwable) -> log.error("失误", throwable));
//        bean.helloNull().whenComplete((s, throwable) -> log.error("出错了", throwable));
        String hello = bean.hello(new int[]{10000, 3394, 4342});
//        bean.hello(new int[]{10000, 3394, 4342});
        log.info("第二个结果 -> {}", hello);
        /*bean.helloAsync(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}).whenComplete((s, throwable) -> {
            if (throwable == null) {
                log.info("远程执行结果 -> {}", s);
            } else {
                log.error("执行失败", throwable);
            }
            client.close();
        });*/
    }
}
