package net.jselby.dirc.io

class IRCMessage(val rawMessage : String) {
    val messageType : IRCMessageType
    val messageSrc: String
    val target: String
    val content: String

    init {
        val messageSplit = rawMessage.split(" ")
        messageSrc = messageSplit[0]
        val messageID = messageSplit[1]

        if (messageSplit.size > 2) {
            target = messageSplit[2]
            content = messageSplit.subList(3, messageSplit.size).joinToString(separator = " ")
        } else {
            target = ""
            content = ""

        }

        val type = IRCMessageType.values()
                .filter {
                    (it.netName.length > 0 && messageID.equals(it.netName, ignoreCase = true))
                            || (it.id != -1 && messageID.equals("${it.id}", ignoreCase = true))
                }
                .singleOrNull()

        if (messageSrc.equals("PING")) {
            messageType = IRCMessageType.PING
        } else if (type == null) {
            println("Failed to find type: $messageID")
            messageType = IRCMessageType.UNKNOWN
        } else {
            messageType = type
        }
    }
}

enum class IRCMessageType(val id: Int, val netName: String) {
    SERVER_NOTICE(-1, "NOTICE"),
    MODE(-1, "MODE"),
    JOIN(-1, "JOIN"),
    QUIT(-1, "QUIT"),
    PART(-1, "PART"),
    MOTD(372, ""),
    END_MOTD(376, ""),
    PRIVMSG(-1, "PRIVMSG"),
    CHANNEL_MOTD(332, ""),
    PING(-1, "PING"),
    NAMES(353, ""),
    END_NAMES(366, ""),
    UNKNOWN(-1, "")
}