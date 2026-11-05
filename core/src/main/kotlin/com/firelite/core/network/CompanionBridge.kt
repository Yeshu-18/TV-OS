package com.firelite.core.network

import com.firelite.core.model.RemoteCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

class TvRemoteServer(
  private val port: Int = 8765,
  private val scope: CoroutineScope
) {
  private val json = Json { ignoreUnknownKeys = true }
  private var serverJob: Job? = null
  private var serverSocket: ServerSocket? = null

  private val _commandFlow = MutableSharedFlow<RemoteCommand>(extraBufferCapacity = 64)
  val commandFlow: SharedFlow<RemoteCommand> = _commandFlow.asSharedFlow()

  private val _connectedClients = MutableStateFlow(0)
  val connectedClients: StateFlow<Int> = _connectedClients.asStateFlow()

  fun start() {
    if (serverJob?.isActive == true) return
    serverJob = scope.launch(Dispatchers.IO) {
      try {
        serverSocket = ServerSocket(port)
        while (isActive) {
          val client = serverSocket?.accept() ?: break
          launch(Dispatchers.IO) {
            handleClient(client)
          }
        }
      } catch (e: Exception) {
        // Server stopped or socket error
      }
    }
  }

  fun stop() {
    try {
      serverSocket?.close()
    } catch (_: Exception) {}
    serverJob?.cancel()
  }

  private suspend fun handleClient(socket: Socket) {
    _connectedClients.value++
    try {
      socket.use { s ->
        val reader = BufferedReader(InputStreamReader(s.getInputStream()))
        val writer = PrintWriter(s.getOutputStream(), true)

        while (scope.isActive && !s.isClosed) {
          val line = reader.readLine() ?: break
          try {
            val command = json.decodeFromString<RemoteCommand>(line)
            _commandFlow.emit(command)
          } catch (_: Exception) {}
        }
      }
    } finally {
      _connectedClients.value = (_connectedClients.value - 1).coerceAtLeast(0)
    }
  }

  companion object {
    fun getLocalIpAddress(): String {
      try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
          val iface = interfaces.nextElement()
          val addresses = iface.inetAddresses
          while (addresses.hasMoreElements()) {
            val addr = addresses.nextElement()
            val host = addr.hostAddress
            if (!addr.isLoopbackAddress && host != null && !host.contains(':')) {
              return host
            }
          }
        }
      } catch (_: Exception) {}
      return "127.0.0.1"
    }
  }
}

class CompanionRemoteClient(
  private val scope: CoroutineScope
) {
  private val json = Json { ignoreUnknownKeys = true }
  private var socket: Socket? = null
  private var writer: PrintWriter? = null
  private var connectionJob: Job? = null

  private val _isConnected = MutableStateFlow(false)
  val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

  private val _receivedCommands = MutableSharedFlow<RemoteCommand>(extraBufferCapacity = 64)
  val receivedCommands: SharedFlow<RemoteCommand> = _receivedCommands.asSharedFlow()

  fun connect(host: String, port: Int = 8765) {
    disconnect()
    connectionJob = scope.launch(Dispatchers.IO) {
      try {
        val s = Socket(host, port)
        socket = s
        writer = PrintWriter(s.getOutputStream(), true)
        _isConnected.value = true

        val reader = BufferedReader(InputStreamReader(s.getInputStream()))
        while (isActive && !s.isClosed) {
          val line = reader.readLine() ?: break
          try {
            val cmd = json.decodeFromString<RemoteCommand>(line)
            _receivedCommands.emit(cmd)
          } catch (_: Exception) {}
        }
      } catch (e: Exception) {
        _isConnected.value = false
      } finally {
        _isConnected.value = false
      }
    }
  }

  suspend fun sendCommand(cmd: RemoteCommand) = withContext(Dispatchers.IO) {
    try {
      val line = json.encodeToString(cmd)
      writer?.println(line)
    } catch (_: Exception) {}
  }

  fun disconnect() {
    connectionJob?.cancel()
    try {
      socket?.close()
    } catch (_: Exception) {}
    socket = null
    writer = null
    _isConnected.value = false
  }
}
