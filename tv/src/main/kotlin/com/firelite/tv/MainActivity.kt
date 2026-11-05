package com.firelite.tv

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.firelite.core.data.FireLiteRepository
import com.firelite.core.model.RemoteCommand
import com.firelite.core.network.TvRemoteServer
import com.firelite.tv.ui.FireLiteTheme
import com.firelite.tv.ui.FireLiteTvScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val repository = FireLiteRepository()
  private lateinit var remoteServer: TvRemoteServer

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    remoteServer = TvRemoteServer(port = 8765, scope = lifecycleScope)
    remoteServer.start()

    // Listen for companion remote commands
    lifecycleScope.launch {
      remoteServer.commandFlow.collect { command ->
        handleRemoteCommand(command)
      }
    }

    val serverIp = TvRemoteServer.getLocalIpAddress()

    setContent {
      val connectedClients by remoteServer.connectedClients.collectAsState()

      FireLiteTheme {
        FireLiteTvScreen(
          repository = repository,
          serverIp = serverIp,
          connectedClients = connectedClients
        )
      }
    }
  }

  private fun handleRemoteCommand(command: RemoteCommand) {
    when (command) {
      is RemoteCommand.PressKey -> {
        runOnUiThread {
          Toast.makeText(this, "Remote: ${command.key}", Toast.LENGTH_SHORT).show()
        }
      }
      is RemoteCommand.SendText -> {
        runOnUiThread {
          Toast.makeText(this, "Typed from phone: \"${command.text}\"", Toast.LENGTH_SHORT).show()
        }
      }
      is RemoteCommand.AddPin -> {
        repository.addPinnedItem(command.item)
        runOnUiThread {
          Toast.makeText(this, "Pinned \"${command.item.title}\" to ${command.item.category}", Toast.LENGTH_SHORT).show()
        }
      }
      is RemoteCommand.RemovePin -> {
        repository.removePinnedItem(command.itemId)
        runOnUiThread {
          Toast.makeText(this, "Removed pinned item", Toast.LENGTH_SHORT).show()
        }
      }
      is RemoteCommand.MoveCategory -> {
        repository.moveCategory(command.itemId, command.targetCategory)
        runOnUiThread {
          Toast.makeText(this, "Moved to ${command.targetCategory}", Toast.LENGTH_SHORT).show()
        }
      }
      is RemoteCommand.ReorderPins -> {
        repository.reorderPins(command.category, command.fromIndex, command.toIndex)
      }
      else -> {}
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    remoteServer.stop()
  }
}
