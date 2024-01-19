https://juejin.cn/post/6992867064952127524?searchId=2024011917043326B3F440C9D86A496B66
rpc项目简历
这个项目是我参照掘金小册中的RPC做的。此类项目已经很多了，但是在面试过程中，还是会被经常问到这个项目是如何设计的。我一般会详细说出代理层、路由层、注册中心层、异步设计等的设计思路。有的面试官可能还会问压测相关的内容。如果说要将RPC项目写到简历中，一定要清楚核心功能的设计，并且反复地尝试自己练习表达几次。


/service_registry(根节点，注册节点，持久节点)
└── myService（服务节点，持久节点，存放服务名）
├── address-000000001（地址节点，临时节点，存放服务地址）
├── address-000000002
└── ...
创建节点：zkClient.create(path, data, mode）

//replica:复制品
ceiling：天花板

final修饰字段：必须初始化，不能被修改（不能重新赋值，map可以put），修饰类：不能被继承

netty:
loop:环
/**
 * 0.ctx ：channelHandlerContext，提供了对 Channel、ChannelPipeline 和 ChannelHandler 的操作和访问
 * 0.1 ：channel类似一个套接字，提供了网络通信的基本操作，比如读取、写入、连接和关闭
 * 0.2：ChannelPipeline 是一个用于处理和拦截 Channel 传入和传出数据的处理器链
 * 由一系列的 ChannelHandler 组成，每个 Handler 负责处理特定的类型的事件。当数据通过 Channel 时，它会被传递到 ChannelPipeline 中，经过一系列的处理器处理，最终到达目的地
 * 1.writeAndFlush：写入并刷新，保证实时性，确保数据尽快到达客户端
 * 1.1 write 方法用于将数据写入通道的缓冲区，但它并不立即将数据发送到实际的对端
 * 1.2 flush 立即将数据发送出去
 * 2.添加一个监听器。如果写操作失败（比如发生异常），该监听器将关闭通道
 */

先构建metadata， 然后构建client ，client会从metadata中得到ip地址端口号 建立连接 发送消息（remoteCall）
构建metadata会用到服务发现

stub:  （discovery, rpcClient, properties） factory(加服务名) ->  handler（加方法名和参数）  -> remoteCall

https://github.com/viego1999/wxy-rpc
