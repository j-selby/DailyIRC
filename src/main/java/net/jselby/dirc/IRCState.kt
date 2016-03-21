package net.jselby.dirc

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.web.WebView
import net.jselby.dirc.io.IRCConnection
import net.jselby.dirc.io.IRCMessageType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Represents the state of a IRC connection
 */
class IRCState(val closeCallback : (IRCState) -> Unit, val host : String, val nickname : String,
               val username : String, val realname : String): Tab(host) {
    var connection : IRCConnection? = null
    var isConnected = false
    var channelContents = HashMap<String, String>()
    var currentChannel = "Info"

    // UI
    val tabContent = VBox()
    val textArea = WebView()

    private val listItems: ObservableList<String>
    private val listSelectionModel: MultipleSelectionModel<String>

    init {
        textArea.engine.loadContent("")

        val textbox = TextField()
        textbox.font = Font.font("Times New Roman", 20.0)
        textbox.promptText = "Insert message/commands here..."
        textbox.setOnAction {
            // Send message!
            if (connection != null) {
                val message = textbox.characters.toString()
                if (currentChannel.equals("Info") || message.startsWith("/")) {
                    addSysChat(currentChannel, ": $message")
                    if (!message.startsWith("/")) {
                        addSysChat(currentChannel, "Syntax: /<command> <arguments>")
                    } else {
                        val split = message.split(" ")
                        val command = message.split(" ")[0].substring(1).toLowerCase()
                        if (command.equals("join")) { // JOIN command
                            if (split.size == 1) {
                                addSysChat(currentChannel, "Syntax: /join #<channel>,...")
                            } else {
                                val joinChannels = split.subList(1, split.size).joinToString(",")
                                addSysChat(currentChannel, "Joining channels: $joinChannels")
                                connection!!.writeLine("JOIN $joinChannels")
                            }
                        } else if (command.equals("part")) { // PART command
                            if (split.size != 2) {
                                if (!currentChannel.equals("Info")) {
                                    addSysChat(currentChannel, "Leaving channel: $currentChannel")
                                    partChannel(currentChannel)
                                } else {
                                    addSysChat(currentChannel, "Syntax: /part #<channel>")
                                }
                            } else {
                                var channel = split[1]
                                addSysChat(currentChannel, "Leaving channel: $channel")
                                partChannel(channel)
                            }
                        } else if (command.equals("msg")) { // MSG command
                            if (split.size < 2) {
                                if (!currentChannel.equals("Info")) {
                                    addSysChat(currentChannel, "Leaving channel: $currentChannel")
                                    partChannel(currentChannel)
                                } else {
                                }
                            } else {
                                addSysChat(currentChannel, "Syntax: /msg <user> <msg>")
                            }
                        } else {
                            addSysChat(currentChannel, "Unknown command.")
                        }
                    }
                } else {
                    val finalOut = message.replace("^C", "$escapeCharacter").replace("\\$escapeCharacter", "^C")
                    addChat(currentChannel, nickname, finalOut)
                    connection!!.writeLine("PRIVMSG $currentChannel :$finalOut")
                }

                textbox.text = ""
            }
        }

        // Add channels on the left
        channelContents.put("Info", "<span style=\"color: grey\">Connecting to $host...</span>")

        val list = ListView<String>();
        list.selectionModel.selectedItemProperty()
                .addListener({ observableValue, oldValue, newValue -> currentChannel = newValue; updateUI(); })
        listItems = FXCollections.observableArrayList (
                "Info"
        );
        list.items = listItems
        listSelectionModel = list.selectionModel
        listSelectionModel.select(0)
        list.minWidth = 100.0

        val hbox = HBox()
        val tabContentVertical = VBox()
        tabContentVertical.children.addAll(textArea, textbox)
        hbox.children.addAll(list, tabContentVertical)

        tabContent.children.addAll(hbox)

        this.setOnClosed { disconnect() }

        content = tabContent
    }


    fun updateUI() {
        Platform.runLater {
            textArea.engine.executeScript("document.body.innerHTML = \""
                    + escapeJavascript(channelContents[currentChannel]!!) + "\";" +
                    "window.scrollTo(0, document.body.scrollHeight);")
        }
    }

    fun addSysMessage(content : String) {
        val html = "<br /><span style=\"color: grey\">${ircMsgToHTML(content)}</span>"

        Platform.runLater {
            channelContents.put("Info", channelContents["Info"] + html)
            updateUI()
        }
    }

    fun addSysChat(channel : String, content : String) {
        val date = SimpleDateFormat("HH:mm").format(Calendar.getInstance().time)
        var html = "<span style=\"color: blue\">[$date]</span>" +
                " <span style=\"color: grey\">${ircMsgToHTML(content)}</span>"

        Platform.runLater {
            if (!channelContents.containsKey(channel)) {
                channelContents.put(channel, html)
                listItems.add(channel)
                currentChannel = channel
                listSelectionModel.select(channel)
            } else {
                channelContents.put(channel, channelContents[channel]!! + "<br />" + html)
            }
            updateUI()
        }
    }

    fun addChat(channel : String, username : String, content : String) {
        val date = SimpleDateFormat("HH:mm").format(Calendar.getInstance().time)
        var html = "<span style=\"color: blue\">[$date]</span>" +
                " <span style=\"color: red\">&lt;$username&gt;</span>&nbsp;" +
                "<span>${ircMsgToHTML(content)}</span>"

        Platform.runLater {
            if (!channelContents.containsKey(channel)) {
                channelContents.put(channel, html)
                listItems.add(channel)
                currentChannel = channel
                listSelectionModel.select(channel)
            } else {
                channelContents.put(channel, channelContents[channel]!! + "<br />" + html)
            }
            updateUI()
        }
    }

    fun partChannel(channel: String) {
        if (!listItems.contains(channel)) {
            return
        }
        connection!!.writeLine("PART $channel")
    }

    fun connect() {
        connection = IRCConnection(host, false, nickname, username, realname)
        connection!!.addHandler {
            if (it.messageType == IRCMessageType.PING) {
                connection!!.writeLine(it.rawMessage.replace("PING", "PONG"))
            }
        }
        connection!!.addHandler {
            println("< ${it.rawMessage}")

            if (it.messageType == IRCMessageType.PRIVMSG) {
                val channel = it.target
                addChat(channel, it.messageSrc.substring(1).split("!")[0], it.content.substring(1))
            } else if (it.messageType == IRCMessageType.SERVER_NOTICE) {
                if (it.messageSrc.startsWith(":ChanServ!")) {
                    val channel = it.content.split("[")[1].split("]")[0]
                    addSysChat(channel, "-ChanServ-: ${it.content.substring(1)}")
                } else {
                    addSysMessage(it.content.substring(1))
                }
            } else if (it.messageType == IRCMessageType.MOTD) {
                addSysMessage(it.content)
            } else if (it.messageType == IRCMessageType.CHANNEL_MOTD) {
                val channel = it.content.split(" ")[0]
                val split = it.content.split(" ")
                addSysChat(channel, split.subList(1, split.size).joinToString(" ").substring(1))
            } else if (it.messageType == IRCMessageType.END_NAMES) {
                val channel = it.content.split(" ")[0]
                addSysChat(channel, "Connected to $channel.")
            } else if (it.messageType == IRCMessageType.JOIN) {
                val user = it.messageSrc.split("!")[0].substring(1)
                addSysChat(it.target, "$user connected.")
            } else if (it.messageType == IRCMessageType.QUIT || it.messageType == IRCMessageType.PART) {
                val user = it.messageSrc.split("!")[0].substring(1)
                if (!it.target.startsWith(":")) {
                    addSysChat(it.target, "$user disconnected.")
                    if (user.equals(nickname)) {
                        Platform.runLater {
                            listItems.remove(it.target)
                            channelContents.remove(it.target)
                        }
                    }
                }
                // TODO: Handle connected list
            }
        }
        connection!!.connect()
    }

    fun disconnect() {
        if (connection == null) {
            return
        }

        println("Disconnecting!")

        try {
            connection!!.writeLine("QUIT")
            connection!!.disconnect()
        } catch (e : Exception) {
            // It doesn't matter anyway, force terminate the connection.
            connection!!.disconnect()
        }

        Platform.runLater {
            closeCallback.invoke(this)
        }
    }
}
