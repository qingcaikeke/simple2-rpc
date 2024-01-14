package com.yjy.rpc.core.loadbalance.impl;

import com.yjy.rpc.core.common.RpcRequest;
import com.yjy.rpc.core.common.ServiceInfo;
import com.yjy.rpc.core.loadbalance.LoadBalance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一致性哈希负载均衡算法
 */
public class ConsistentHashLoadBalance implements LoadBalance {
    /**
     * 构建缓存：服务名 -- 哈希环
     */
    private final Map<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();
    @Override
    public ServiceInfo select(List<ServiceInfo> invokers, RpcRequest request) {
        // 得到请求的方法名称
        String method = request.getMethod();
        // 构建对应的 key 值，key = 全限定类名 + "." + 方法名，比如 com.xxx.DemoService.sayHello
        String key = request.getServiceName() + "." + method;
        // 获取 invokers 原始的 hashCode , 节点信息没变，哈希环就没变
        int identityHashCode = System.identityHashCode(invokers);
        // 从 map 从获取对应的 selector
        ConsistentHashSelector selector = selectors.get(key);
        // 如果为 null，表示之前没有缓存过，如果 hashcode 不一致，表示缓存的服务列表发生变化
        if (selector == null || selector.identityHashCode != identityHashCode) {
            // 创建新的 selector 并缓存
            selectors.put(key, new ConsistentHashSelector(invokers, 160, identityHashCode));
            selector = selectors.get(key);
        }
        // 调用 ConsistentHashSelector 的 select 方法选择 Invoker
        String selectKey = key;
        // 将 key 与 方法参数进行 hash 运算，因此 ConsistentHashLoadBalance 的负载均衡逻辑只受参数值影响，
        // 具有相同参数值的请求将会被分配给同一个服务提供者。ConsistentHashLoadBalance 不关心权重
        if (request.getParameterValues() != null && request.getParameterValues().length > 0) {
            selectKey += Arrays.stream(request.getParameterValues());
        }
        return selector.select(selectKey);
    }

    //final:表示该内部类是一个不可继承的最终类,确保内部类的稳定性和不可修改性。
    private static final class ConsistentHashSelector{
        /**
         * 使用 TreeMap 存储虚拟节点（virtualInvokers
         * 需要提供高效的查询操作，因此选用 TreeMap 作为存储结构（底层为红黑树）
         */
        //final：字段不可修改，必须初始化，线程安全
        private final TreeMap<Long, ServiceInfo> virtualInvokers;

        /**
         * invokers 的原始哈希码，用于判断哈希环是否变化，是否需要构造新的哈希环
         */
        private final int identityHashCode;

        /**
         * 构建一个 ConsistentHashSelector 对象
         * 虚拟节点数 = 40*4（key改为原始key+i），每个key能生成4个不同的哈希（摘要数组16位，每次取4位生成哈希）
         * @param invokers         存储虚拟节点
         * @param replicaNumber    虚拟节点数，默认为 160
         * @param identityHashCode invokers 的原始哈希码
         */
        public ConsistentHashSelector(List<ServiceInfo> invokers, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            for(ServiceInfo invoker : invokers){
                String address = invoker.getAddress();
                //replica:复制品
                for (int i = 0; i < replicaNumber / 4; i++) {
                    // 对 address + i 进行 md5 运算，得到一个长度为16的字节数组
                    byte[] digest = md5(address + i);
                    // 对 digest 部分字节进行4次 hash 运算，得到四个不同的 long 型正整数
                    for (int h = 0; h < 4; h++) {
                        // h = 0 时，取 digest 中下标为 0 ~ 3 的4个字节进行位运算
                        // h = 1 时，取 digest 中下标为 4 ~ 7 的4个字节进行位运算
                        // h = 2, h = 3 时过程同上
                        long hash = hash(digest, h);
                        // 将 hash 到 invoker 的映射关系存储到 virtualInvokers 中
                        virtualInvokers.put(hash, invoker);
                    }
                }
            }
        }

        /**
         * 进行 md5 运算，返回摘要字节数组
         * 根据摘要生成 hash 值
         * 找到第一个大于等于 hash 值的服务信息，若没有则返回第一个
         */
        public ServiceInfo select(String key){
            byte[] digest = md5(key);
            long hash = hash(digest,0);
            Map.Entry<Long, ServiceInfo> entry = virtualInvokers.ceilingEntry(hash);
            if(entry == null){
                entry = virtualInvokers.firstEntry();
            }
            return entry.getValue();
        }

        /**
         * 进行 md5 运算，返回摘要字节数组
         *
         * @param key 编码字符串 key
         * @return 编码后的摘要内容，长度为 16 的字节数组
         */
        private byte[] md5(String key){
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            return md.digest();
        }
        /**
         * 根据摘要生成 hash 值，摘要是一个16为的字节数组
         * h = 0 时，取 digest 中下标为 0 ~ 3 的4个字节进行位运算
         * h = 1 时，取 digest 中下标为 4 ~ 7 的4个字节进行位运算
         * h = 2, h = 3 时过程同上
         * @param digest md5摘要内容
         * @param number 当前索引数
         * @return hash 值
         */
        private long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[number * 4] & 0xFF))
                    & 0xFFFFFFFFL;
        }
    }
}
