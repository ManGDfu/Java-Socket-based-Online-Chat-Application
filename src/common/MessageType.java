package common;

/**
 * 消息类型枚举。
 *
 * 这里把后续要做的群聊、文件、图片、语音等类型都先定义出来，
 * 但当前阶段只实现登录与私聊文字相关的处理逻辑。
 */
public enum MessageType {
    LOGIN,              // 用户登录
    REGISTER,           // 用户注册
    LOGOUT,             // 用户退出
    UPDATE_PROFILE,     // 修改用户资料

    PRIVATE_TEXT,       // 私聊文字
    PRIVATE_IMAGE,      // 私聊图片
    PRIVATE_FILE,       // 私聊文件
    PRIVATE_VOICE,      // 私聊语音

    GROUP_TEXT,         // 群聊文字
    GROUP_IMAGE,        // 群聊图片
    GROUP_FILE,         // 群聊文件
    GROUP_VOICE,        // 群聊语音

    ADD_FRIEND,         // 添加好友
    FRIEND_LIST,        // 获取好友列表
    CREATE_GROUP,       // 创建群聊
    RENAME_GROUP,       // 修改群名称
    INVITE_TO_GROUP,    // 邀请好友入群
    GROUP_LIST,         // 获取群聊列表
    GROUP_MEMBER_LIST,  // 获取群成员列表

    SERVER_NOTICE,      // 服务端通知
    ERROR               // 错误消息
}
