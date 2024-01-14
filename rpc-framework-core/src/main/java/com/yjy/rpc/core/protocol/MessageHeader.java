package com.yjy.rpc.core.protocol;

import com.yjy.rpc.core.protocol.constant.ProtocolConstants;
import com.yjy.rpc.core.protocol.enums.MessageType;
import com.yjy.rpc.core.protocol.enums.SerializationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 请求协议头部信息
 * <pre>
 *   --------------------------------------------------------------------
 *  | 魔数 (4byte) | 版本号 (1byte)  | 序列化算法 (1byte)  | 消息类型 (1byte) |
 *  -------------------------------------------------------------------
 *  |  状态类型 (1byte)  |     消息序列号 (4byte)   |     消息长度 (4byte)    |
 *  --------------------------------------------------------------------
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageHeader {
    /**
     * 4字节 魔数
     */
    private byte[] magicNum;

    /**
     * 1字节 版本号
     */
    private byte version;
    /**
     * 1字节 序列化算法
     */
    private byte serializerType;

    /**
     * 1字节 消息类型：rpc请求，rpc响应，心跳检查，检查响应
     */
    private byte messageType;

    /**
     * 消息状态类型：0成功，1失败
     */
    private byte messageStatus;

    /**
     * 4字节 消息的序列号 ID
     */
    private int sequenceId;

    /**
     * 4字节 数据内容长度
     */
    private int length;

    /**
     * 根据输入的序列化算法构造一个 MessageHeader 对象
     *
     * @param serializeName 序列化算法名称
     * @return 构造指定序列化算法的默认协议头对象
     */
    public static MessageHeader build(String serializeName) {

        return MessageHeader.builder()
                .magicNum(ProtocolConstants.MAGIC_NUM)
                .version(ProtocolConstants.VERSION)
                .serializerType(SerializationType.parseByName(serializeName).getType())
                .messageType(MessageType.REQUEST.getType())
                .sequenceId(ProtocolConstants.getSequenceId()) // 添加唯一 ID 生成
                .build();
    }
}
