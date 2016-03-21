package net.jselby.dirc

import javafx.application.Application
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.layout.VBox
import javafx.scene.web.WebView
import javafx.stage.Stage

class DailyIRC : Application() {
    private lateinit var tabbedPane: TabPane

    override fun start(stage: Stage) {
        println("Launching dIRC...")

        // Setup our rendering platform
        stage.minWidth = 480.0
        stage.minHeight = 240.0
        stage.isResizable = true
        stage.title = "dIRC - ???"
        try {
            val icon = javaClass.getResourceAsStream("/icon.png")
            stage.icons.add(Image(icon))
            icon.close()
        } catch (e : Exception) {
            println("Failed to load icon.")
            e.printStackTrace()
        }

        // Create our GUI layout
        val welcomeContent = WebView()
        welcomeContent.engine.loadContent("Press \"Connect\" above to begin.")

        val welcomeTab = Tab("Welcome", welcomeContent)
        welcomeTab.isClosable = false

        // Create the top toolbar
        val menuConnect = Menu("Server");
        val connectButton = MenuItem("Connect...")
        val disconnectButton = MenuItem("Disconnect")
        val exitButton = MenuItem("Exit")
        menuConnect.items.addAll(connectButton, disconnectButton, exitButton)
        val toolbar = MenuBar(menuConnect)

        // Create the tab view
        tabbedPane = TabPane()
        tabbedPane.tabs.addAll(welcomeTab)

        // Add toolbar handlers
        connectButton.setOnAction {
            val dialog = IRCConnectDialog()
            dialog.showAndWait().ifPresent {
                val state = IRCState({
                    tabbedPane.tabs.remove(it)
                    updateTitle(stage)
                }, it.host + ":" + it.port, it.nickname, it.username, it.realname)
                addState(stage, state)
            }
        }

        disconnectButton.setOnAction {
            val currentTab = tabbedPane.selectionModel.selectedItem
            if (currentTab is IRCState) {
                currentTab.disconnect()
            }
        }

        exitButton.setOnAction {
            Platform.exit()
            System.exit(0)
        }

        stage.setOnCloseRequest {
            Platform.exit()
            System.exit(0)
        }


        // Enable/disable disconnect button based on state
        tabbedPane.selectionModel.selectedItemProperty().addListener({ observableValue, oldValue, newValue ->
            disconnectButton.isDisable = newValue !is IRCState
        })

        val root = VBox()
        root.children.add(toolbar)
        root.children.add(tabbedPane)

        stage.scene = Scene(root, 640.0, 480.0)

        if (parameters.raw.size == 4) {
            // We have some auto input!
            addState(stage, IRCState({
                tabbedPane.tabs.remove(it)
                updateTitle(stage)
            }, parameters.raw[0], parameters.raw[1], parameters.raw[2], parameters.raw[3]))
        } else {
            updateTitle(stage)
        }

        stage.show()
    }

    private fun addState(stage: Stage, state: IRCState) {
        tabbedPane.tabs.add(state)
        try {
            state.connect()
            tabbedPane.selectionModel.select(state)
        } catch (e : Exception) {
            e.printStackTrace()
            Alert(Alert.AlertType.ERROR, "Connect failed: " + e.message).showAndWait()
            tabbedPane.tabs.remove(state)
        }
        updateTitle(stage)
    }

    fun updateTitle(stage : Stage) {
        stage.title = "dIRC - " + if (tabbedPane.tabs.size > 1) "Connected" else "Not Connected"
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(DailyIRC::class.java, *args)
        }
    }
}