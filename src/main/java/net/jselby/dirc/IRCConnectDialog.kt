package net.jselby.dirc

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane

class IRCConnectDialog : Dialog<IRCConnectDialogResult>() {
    private val host: TextField
    private val port: TextField
    private val nickname: TextField
    private val username: TextField
    private val realname: TextField

    init {
        title = "Connect..."
        dialogPane.contentText = "Enter connection details..."
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 10.0
        grid.padding = Insets(20.0, 150.0, 10.0, 10.0)

        host = TextField()
        host.promptText = "Host"
        host.textProperty().addListener({ value, oldValue, newValue -> checkInput() })
        port = TextField()
        port.promptText = "Port"
        port.textProperty().addListener({ value, oldValue, newValue -> checkInput() })
        nickname = TextField()
        nickname.promptText = "Nickname"
        nickname.textProperty().addListener({ value, oldValue, newValue -> checkInput() })
        username = TextField()
        username.promptText = "Username"
        username.textProperty().addListener({ value, oldValue, newValue -> checkInput() })
        realname = TextField()
        realname.promptText = "Real Name"
        realname.textProperty().addListener({ value, oldValue, newValue -> checkInput() })

        grid.add(Label("Host:"), 0, 0)
        grid.add(host, 1, 0)
        grid.add(Label("Port:"), 0, 1)
        grid.add(port, 1, 1)
        grid.add(Label("Nickname:"), 0, 2)
        grid.add(nickname, 1, 2)
        grid.add(Label("Username:"), 0, 3)
        grid.add(username, 1, 3)
        grid.add(Label("Real Name:"), 0, 4)
        grid.add(realname, 1, 4)

        dialogPane.content = grid
        dialogPane.lookupButton(ButtonType.OK).isDisable = true

        Platform.runLater({host.requestFocus()})

        setResultConverter { if (it == ButtonType.CANCEL) null else
            IRCConnectDialogResult(host.characters.toString(),
                        port.characters.toString().toInt(),
                        nickname.characters.toString(),
                        username.characters.toString(),
                        realname.characters.toString())
        }

        setOnCloseRequest {
            close()
        }
    }

    fun checkInput() {
        dialogPane.lookupButton(ButtonType.OK).isDisable = !isValidInput()
    }

    fun isValidInput() = host.characters.contains('.')
            && isInteger(port.characters.toString()) && nickname.characters.isNotBlank()
            && username.characters.isNotBlank() && realname.characters.isNotBlank()
}

data class IRCConnectDialogResult(val host : String, val port : Int,
                                  val nickname : String, val username : String,
                                  val realname : String)