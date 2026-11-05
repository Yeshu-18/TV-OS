package com.firelite.companion

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.firelite.companion.ui.CompanionTheme
import com.firelite.companion.ui.FigmaTheme
import com.firelite.companion.ui.LiveKeyboardDialog
import com.firelite.companion.ui.PairingDialog
import com.firelite.companion.ui.PhoneAppsScreen
import com.firelite.companion.ui.PhoneMoreScreen
import com.firelite.companion.ui.PhonePinsScreen
import com.firelite.companion.ui.PhoneScreenMirrorScreen
import com.firelite.companion.ui.PhoneSearchScreen
import com.firelite.companion.ui.RemoteScreen
import com.firelite.companion.ui.getDynamicAccentColor
import com.firelite.core.data.FireLiteRepository
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import com.firelite.core.model.RemoteCommand
import com.firelite.core.model.RemoteKey
import com.firelite.core.network.CompanionRemoteClient
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val repository = FireLiteRepository()
  private lateinit var client: CompanionRemoteClient

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    client = CompanionRemoteClient(scope = lifecycleScope)
    client.connect("127.0.0.1")

    setContent {
      // 0: Remote, 1: Apps, 2: Pins, 3: Search, 4: More, 5: Screen Mirror
      var selectedTab by remember { mutableIntStateOf(0) }
      var isPaired by remember { mutableStateOf(true) }
      var showPairingDialog by remember { mutableStateOf(false) }
      var showKeyboardDialog by remember { mutableStateOf(false) }
      var isMirroring by remember { mutableStateOf(false) }

      val isConnected by client.isConnected.collectAsState()

      // Content items supporting dynamic accent demonstration:
      // Doraemon (Blue), Shinchan (Warm Red), Nature (Green), Barbie (Pink), LEGO (Yellow/Orange)
      val contentList = remember {
        listOf(
          MediaItem(
            id = "doraemon",
            title = "Doraemon",
            category = Category.KIDS,
            platform = "JioHotstar",
            subtitle = "Gadgets from the Future",
            durationOrEpisodes = "Season 12",
            accentColorHex = 0xFF00A3FF
          ),
          MediaItem(
            id = "shinchan",
            title = "Shinchan",
            category = Category.KIDS,
            platform = "JioHotstar",
            subtitle = "Nohara Family Adventures",
            durationOrEpisodes = "Season 8",
            accentColorHex = 0xFFFF4757
          ),
          MediaItem(
            id = "nature",
            title = "Planet Earth: Nature",
            category = Category.SERIES,
            platform = "Netflix",
            subtitle = "Wild Living World",
            durationOrEpisodes = "4K HDR",
            accentColorHex = 0xFF2ED573
          ),
          MediaItem(
            id = "barbie",
            title = "Barbie Dreamhouse",
            category = Category.KIDS,
            platform = "Netflix",
            subtitle = "Adventures & Fun",
            durationOrEpisodes = "Season 5",
            accentColorHex = 0xFFFF69B4
          ),
          MediaItem(
            id = "lego",
            title = "LEGO City Adventures",
            category = Category.SERIES,
            platform = "Prime Video",
            subtitle = "Building & Rescues",
            durationOrEpisodes = "Season 3",
            accentColorHex = 0xFFFFA502
          ),
          MediaItem(
            id = "harrypotter",
            title = "Harry Potter: Mystery",
            category = Category.SERIES,
            platform = "Prime Video",
            subtitle = "Hogwarts Chronicles",
            durationOrEpisodes = "2h 32m",
            accentColorHex = 0xFF8B5CF6
          )
        )
      }
      var currentContentIndex by remember { mutableIntStateOf(0) }
      val currentContent: MediaItem = contentList[currentContentIndex]

      // Smooth transition between dynamic content accent colors (300-500ms)
      val targetAccent = remember(currentContent) { getDynamicAccentColor(currentContent) }
      val animatedAccent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 400),
        label = "dynamicAccent"
      )

      // Handle Android back to always return to Remote main screen
      if (selectedTab != 0) {
        BackHandler {
          selectedTab = 0
        }
      }

      CompanionTheme(activeAccent = animatedAccent) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(FigmaTheme.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
        ) {
          // Content-derived ambient lighting layer behind primary content (subtle 5-8% intensity)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(360.dp)
              .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                  colors = listOf(
                    animatedAccent.copy(alpha = 0.08f),
                    animatedAccent.copy(alpha = 0.02f),
                    Color.Transparent
                  ),
                  center = androidx.compose.ui.geometry.Offset(x = 540f, y = 160f),
                  radius = 700f
                )
              )
          )
          // SCREEN CONTENT AREA (Leaves room for the single floating navigation dock)
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(bottom = 76.dp)
          ) {
            when (selectedTab) {
              0 -> RemoteScreen(
                client = client,
                isConnected = isConnected,
                isPaired = isPaired,
                currentContent = currentContent,
                activeAccent = animatedAccent,
                onOpenPairingDialog = { showPairingDialog = true },
                onOpenMirrorScreen = { selectedTab = 5 },
                onCycleContent = {
                  currentContentIndex = (currentContentIndex + 1) % contentList.size
                },
                modifier = Modifier.fillMaxSize()
              )
              1 -> PhoneAppsScreen(
                activeAccent = animatedAccent,
                onLaunch = { appName ->
                  Toast.makeText(this@MainActivity, "Launching $appName on TV", Toast.LENGTH_SHORT).show()
                },
                onOpenMirror = { selectedTab = 5 },
                onPowerClick = {
                  lifecycleScope.launch {
                    client.sendCommand(RemoteCommand.PressKey(RemoteKey.BACK))
                  }
                },
                modifier = Modifier.fillMaxSize()
              )
              2 -> PhonePinsScreen(
                repository = repository,
                client = client,
                activeAccent = animatedAccent,
                onOpenMirror = { selectedTab = 5 },
                onPowerClick = {
                  lifecycleScope.launch {
                    client.sendCommand(RemoteCommand.PressKey(RemoteKey.BACK))
                  }
                },
                modifier = Modifier.fillMaxSize()
              )
              3 -> PhoneSearchScreen(
                activeAccent = animatedAccent,
                currentApp = currentContent.platform,
                onSendToTv = { query ->
                  lifecycleScope.launch {
                    client.sendCommand(RemoteCommand.SendText(query))
                    Toast.makeText(this@MainActivity, "Searching for \"$query\" on TV", Toast.LENGTH_SHORT).show()
                  }
                },
                onOpenMirror = { selectedTab = 5 },
                onPowerClick = {
                  lifecycleScope.launch {
                    client.sendCommand(RemoteCommand.PressKey(RemoteKey.BACK))
                  }
                },
                modifier = Modifier.fillMaxSize()
              )
              4 -> PhoneMoreScreen(
                isConnected = isConnected,
                isPaired = isPaired,
                activeAccent = animatedAccent,
                onOpenPairDialog = { showPairingDialog = true },
                onOpenMirrorScreen = { selectedTab = 5 },
                onPowerClick = {
                  lifecycleScope.launch {
                    client.sendCommand(RemoteCommand.PressKey(RemoteKey.BACK))
                  }
                },
                modifier = Modifier.fillMaxSize()
              )
              5 -> PhoneScreenMirrorScreen(
                isMirroring = isMirroring,
                onToggleMirror = {
                  isMirroring = !isMirroring
                  Toast.makeText(
                    this@MainActivity,
                    if (isMirroring) "Screen Mirroring started" else "Screen Mirroring stopped",
                    Toast.LENGTH_SHORT
                  ).show()
                },
                activeAccent = animatedAccent,
                onPowerClick = {
                  lifecycleScope.launch {
                    client.sendCommand(RemoteCommand.PressKey(RemoteKey.BACK))
                  }
                },
                modifier = Modifier.fillMaxSize()
              )
            }
          }

          // EXACTLY ONE FIRELITE BOTTOM DOCK: Remote | Apps | Pins | Search | More
          FireLiteFloatingNavBar(
            selectedTab = if (selectedTab == 5) -1 else selectedTab,
            activeAccent = animatedAccent,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(start = 14.dp, end = 14.dp, bottom = 4.dp)
          )

          // 4-Digit TV Pairing Dialog
          if (showPairingDialog) {
            PairingDialog(
              activeAccent = animatedAccent,
              onDismiss = { showPairingDialog = false },
              onPairSuccess = {
                isPaired = true
                showPairingDialog = false
                Toast.makeText(this@MainActivity, "Remote paired with Living Room TV!", Toast.LENGTH_SHORT).show()
              }
            )
          }

          // Live TV Keyboard Dialog
          if (showKeyboardDialog) {
            LiveKeyboardDialog(
              activeAccent = animatedAccent,
              onDismiss = { showKeyboardDialog = false },
              onSend = { text ->
                lifecycleScope.launch {
                  client.sendCommand(RemoteCommand.SendText(text))
                  Toast.makeText(this@MainActivity, "Sent to TV: $text", Toast.LENGTH_SHORT).show()
                }
              }
            )
          }
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    client.disconnect()
  }
}

/**
 * EXACTLY ONE Floating Navigation Dock:
 * Remote | Apps | Pins | Search | More
 * Translucent glassy surface with delicate dynamic accent glow and subtle indicator dot.
 */
@Composable
fun FireLiteFloatingNavBar(
  selectedTab: Int,
  activeAccent: Color,
  onTabSelected: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(64.dp)
      .shadow(18.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.65f))
      .clip(RoundedCornerShape(32.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xD9101624),
            Color(0xBF080D15)
          )
        )
      )
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0x35FFFFFF),
            Color(0x10FFFFFF)
          )
        ),
        shape = RoundedCornerShape(32.dp)
      )
      .padding(horizontal = 6.dp, vertical = 5.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      val navItems = listOf(
        NavItem(0, "Remote", Icons.Outlined.Home),
        NavItem(1, "Apps", Icons.Default.GridView),
        NavItem(2, "Pins", Icons.Outlined.StarOutline),
        NavItem(3, "Search", Icons.Default.Search),
        NavItem(4, "More", Icons.Default.MoreHoriz)
      )

      navItems.forEach { item ->
        val isSelected = (selectedTab == item.index)
        val tabLift by animateDpAsState(
          targetValue = if (isSelected) (-2).dp else 0.dp,
          animationSpec = tween(durationMillis = 140),
          label = "navTabLift"
        )

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .offset(y = tabLift)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) activeAccent.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onTabSelected(item.index) },
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = item.icon,
              contentDescription = item.label,
              tint = if (isSelected) activeAccent else FigmaTheme.NavInactive,
              modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = item.label,
              fontSize = 10.sp,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
              color = if (isSelected) activeAccent else FigmaTheme.NavInactive,
              maxLines = 1
            )
            if (isSelected) {
              Spacer(modifier = Modifier.height(2.dp))
              Box(
                modifier = Modifier
                  .size(3.5.dp)
                  .clip(androidx.compose.foundation.shape.CircleShape)
                  .background(activeAccent)
              )
            }
          }
        }
      }
    }
  }
}

data class NavItem(
  val index: Int,
  val label: String,
  val icon: ImageVector
)

