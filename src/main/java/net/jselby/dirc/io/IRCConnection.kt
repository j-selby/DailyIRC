package net.jselby.dirc.io;

import java.io.*
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

// IRC connection side of bot
class IRCConnection(host : String, val nickname : String, val username : String, val realName : String) {
    val hostname: String
    val port: Int

    private var socket : Socket? = null
    private var outputStream : DataOutputStream? = null
    private var inputStream : InputStreamReader? = null

    private val incomingHandlers = CopyOnWriteArrayList<(IRCMessage) -> (Unit)>()

    init {
        hostname = host.split(":")[0]
        port = host.split(":")[1].toInt()
    }

    fun connect() {
        // TODO: TLS
        println("Connecting to $hostname:$port using nickname $nickname, username $username, realname $realName")
        socket = Socket(hostname, port)

        // Create streams
        outputStream = DataOutputStream(socket!!.outputStream)
        inputStream = DataInputStream(socket!!.inputStream).reader(charset("UTF-8"))

        // Handle login
        writeLine("NICK $nickname")
        writeLine("USER $username 0 * :$realName")

        // Create input reader
        thread {
            inputStream!!.forEachLine {
                // Parse incoming line
                val message = IRCMessage(it)

                for (handler in incomingHandlers) {
                    handler.invoke(message)
                }
            }
        }
    }

    fun addHandler(handler : (IRCMessage) -> (Unit)) {
        incomingHandlers.addIfAbsent(handler)
    }

    fun writeLine(line: String) {
        val bytes = (line + "\r\n").toByteArray(charset("UTF-8"))
        assert(bytes.size <= 512)
        println("> $line")
        outputStream!!.write(bytes)
    }

    fun disconnect() {
        socket!!.close()
    }
}