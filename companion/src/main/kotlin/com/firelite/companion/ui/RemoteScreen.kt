package com.firelite.companion.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firelite.companion.R
import com.firelite.core.model.MediaItem
import com.firelite.core.model.RemoteCommand
import com.firelite.core.model.RemoteKey
import com.firelite.core.network.CompanionRemoteClient
import kotlinx.coroutines.launch

@Composable
fun RemoteScreen(
  client: CompanionRemoteClient,
  isConnected: Boolean,
  isPaired: Boolean,
  currentContent: MediaItem?,
  activeAccent: Color,
  onOpenPairingDialog: () -> Unit,
  onOpenMirrorScreen: () -> Unit,
  onCycleContent: () -> Unit,
  modifier: Modifier = Modifier,
  onOpenKeyboardDialog: () -> Unit = {}
) {
  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current
  var volume by remember { mutableFloatStateOf(0.78f) }

  fun sendKey(key: RemoteKey) {
    try {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    } catch (_: Exception) {}
    scope.launch {
      client.sendCommand(RemoteCommand.PressKey(key))
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(FigmaTheme.Background)
      .padding(horizontal = 20.dp, vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // 1. TOP HEADER WITH FIRELITE BRANDING AND SCREEN MIRROR / POWER CONTROLS
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 2.dp, bottom = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "FIRELITE",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = activeAccent
      )

      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Screen Mirror icon button (subtle dynamic accent border and glow)
        Box(
          modifier = Modifier
            .size(38.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D141F))
            .border(
              width = 1.dp,
              color = activeAccent.copy(alpha = 0.5f),
              shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onOpenMirrorScreen),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Tv,
            contentDescription = "Screen Mirror",
            tint = activeAccent,
            modifier = Modifier.size(19.dp)
          )
        }

        // Power icon button (soft white/grey, NOT bright red!)
        Box(
          modifier = Modifier
            .size(38.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D141F))
            .border(
              width = 1.dp,
              color = Color(0xFF1C2533),
              shape = RoundedCornerShape(12.dp)
            )
            .clickable {
              scope.launch {
                client.sendCommand(RemoteCommand.PressKey(RemoteKey.BACK))
              }
            },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = "Power",
            tint = Color(0xFFF4F5F7),
            modifier = Modifier.size(19.dp)
          )
        }
      }
    }

    // 2. NOW PLAYING HERO: CONTENT-DRIVEN ARTWORK WITH AMBIENT ILLUMINATION
    NowPlayingHeroCard(
      currentContent = currentContent,
      activeAccent = activeAccent,
      onCycleContent = onCycleContent
    )

    // 3. COMPACT CONNECTION STATUS LINE
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onOpenPairingDialog)
        .padding(horizontal = 4.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(7.dp)
          .clip(CircleShape)
          .background(if (isPaired) FigmaTheme.GreenDot else Color(0xFFF59E0B))
          .shadow(4.dp, CircleShape, spotColor = if (isPaired) FigmaTheme.GreenDot else Color(0xFFF59E0B))
      )
      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        imageVector = Icons.Default.Tv,
        contentDescription = null,
        tint = Color(0xFF9AA4B2),
        modifier = Modifier.size(15.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = if (isPaired) "Living Room TV • Connected" else "Living Room TV • Ready to pair",
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFFC5CBD5)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        tint = Color(0xFF717686),
        modifier = Modifier.size(11.dp)
      )
    }

    // 4. REFINED TACTILE D-PAD (CONTOURED 4-LOBE PHYSICAL REMOTE POD)
    FigmaDPad(
      activeAccent = activeAccent,
      onUp = { sendKey(RemoteKey.UP) },
      onDown = { sendKey(RemoteKey.DOWN) },
      onLeft = { sendKey(RemoteKey.LEFT) },
      onRight = { sendKey(RemoteKey.RIGHT) },
      onSelect = { sendKey(RemoteKey.SELECT) }
    )

    // 5. LIGHTWEIGHT PLAYBACK CONTROLS (Rewind, Play/Pause ▶❚❚, Fast Forward)
    Row(
      modifier = Modifier.fillMaxWidth(0.85f),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Rewind circle
      FigmaPlaybackPill(
        width = 54,
        onClick = { sendKey(RemoteKey.LEFT) }
      ) {
        Icon(
          imageVector = Icons.Default.FastRewind,
          contentDescription = "Rewind",
          tint = Color(0xFFF4F5F7),
          modifier = Modifier.size(20.dp)
        )
      }

      // Play/Pause pill with ▶❚❚ icon and active accent border/glow
      FigmaPlaybackPill(
        width = 68,
        borderColor = activeAccent.copy(alpha = 0.65f),
        onClick = { sendKey(RemoteKey.PLAY_PAUSE) }
      ) {
        FigmaPlayPauseIcon(color = Color.White)
      }

      // Fast Forward circle
      FigmaPlaybackPill(
        width = 54,
        onClick = { sendKey(RemoteKey.RIGHT) }
      ) {
        Icon(
          imageVector = Icons.Default.FastForward,
          contentDescription = "Fast Forward",
          tint = Color(0xFFF4F5F7),
          modifier = Modifier.size(20.dp)
        )
      }
    }

    // 6. VOLUME CONTROL: Sleek horizontal track with accent thumb and minus/plus anchors
    FigmaVolumeControl(
      volume = volume,
      activeAccent = activeAccent,
      onVolumeChange = { volume = it },
      onVolumeDown = {
        volume = (volume - 0.05f).coerceAtLeast(0f)
        sendKey(RemoteKey.VOLUME_DOWN)
      },
      onVolumeUp = {
        volume = (volume + 0.05f).coerceAtMost(1f)
        sendKey(RemoteKey.VOLUME_UP)
      }
    )

    Spacer(modifier = Modifier.height(4.dp))
  }
}

/**
 * Visual Anchor Hero: Real artwork-led card with layered depth and subtle ambient glow.
 */
@Composable
fun NowPlayingHeroCard(
  currentContent: MediaItem?,
  activeAccent: Color,
  onCycleContent: () -> Unit,
  modifier: Modifier = Modifier
) {
  val titleLower = currentContent?.title?.lowercase() ?: "doraemon"

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(190.dp)
      .clickable(onClick = onCycleContent)
  ) {
    // Ambient atmospheric back-glow
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(
              activeAccent.copy(alpha = 0.18f),
              Color.Transparent
            ),
            center = androidx.compose.ui.geometry.Offset(x = 650f, y = 180f),
            radius = 450f
          )
        )
    )

    // Base card container
    Box(
      modifier = Modifier
        .fillMaxSize()
        .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.6f))
        .clip(RoundedCornerShape(22.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF0D1420),
              Color(0xFF080C14)
            )
          )
        )
        .border(
          width = 1.dp,
          brush = Brush.verticalGradient(
            colors = listOf(
              activeAccent.copy(alpha = 0.45f),
              Color(0xFF162130)
            )
          ),
          shape = RoundedCornerShape(22.dp)
        )
    ) {
      // Real Content Artwork (Artwork-first, replaces old radar HUD)
      if ("doraemon" in titleLower) {
        Image(
          painter = painterResource(id = R.drawable.art_doraemon_hero),
          contentDescription = "Doraemon",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
        )
      } else {
        val posterRes = when {
          "shinchan" in titleLower -> R.drawable.pin_shinchan
          "nature" in titleLower || "earth" in titleLower -> R.drawable.pin_earth
          "barbie" in titleLower -> R.drawable.pin_barbie
          "lego" in titleLower -> R.drawable.pin_lego
          "harry" in titleLower || "potter" in titleLower -> R.drawable.pin_potter
          else -> R.drawable.art_doraemon_hero
        }

        Row(
          modifier = Modifier.fillMaxSize()
        ) {
          Spacer(modifier = Modifier.weight(1f))
          Image(
            painter = painterResource(id = posterRes),
            contentDescription = currentContent?.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxHeight()
              .width(180.dp)
              .clip(RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
          )
        }
      }

      // Atmospheric gradient scrim ensuring typography readability
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.horizontalGradient(
              colors = listOf(
                Color(0xFF060A12),
                Color(0xF2060A12),
                Color(0x88060A12),
                Color.Transparent
              ),
              startX = 0f,
              endX = 480f
            )
          )
      )

      // Top-left "▶ Now Playing" Badge
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(14.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0x990A101A))
          .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = activeAccent,
            modifier = Modifier.size(11.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Now Playing",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE2E8F0)
          )
        }
      }

      // Title and Platform metadata at bottom-left
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(16.dp)
      ) {
        Text(
          text = currentContent?.title ?: "Doraemon",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${currentContent?.platform ?: "JioHotstar"} • Playing now",
          fontSize = 13.sp,
          fontWeight = FontWeight.Normal,
          color = Color(0xFFA0AAB8)
        )
      }
    }
  }
}

/**
 * Contoured 4-Lobe D-Pad matching reference physical remote control design.
 */
@Composable
fun FigmaDPad(
  activeAccent: Color,
  onUp: () -> Unit,
  onDown: () -> Unit,
  onLeft: () -> Unit,
  onRight: () -> Unit,
  onSelect: () -> Unit
) {
  val upSource = remember { MutableInteractionSource() }
  val isUpPressed by upSource.collectIsPressedAsState()
  val downSource = remember { MutableInteractionSource() }
  val isDownPressed by downSource.collectIsPressedAsState()
  val leftSource = remember { MutableInteractionSource() }
  val isLeftPressed by leftSource.collectIsPressedAsState()
  val rightSource = remember { MutableInteractionSource() }
  val isRightPressed by rightSource.collectIsPressedAsState()

  Box(
    modifier = Modifier
      .size(170.dp)
      .shadow(10.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.65f)),
    contentAlignment = Alignment.Center
  ) {
    // Horizontal contoured lobe
    Box(
      modifier = Modifier
        .size(width = 170.dp, height = 62.dp)
        .clip(RoundedCornerShape(31.dp))
        .background(
          Brush.horizontalGradient(
            colors = listOf(
              Color(0xFF0F1520),
              Color(0xFF0A0F17)
            )
          )
        )
        .border(1.dp, Color(0xFF17202E), RoundedCornerShape(31.dp))
    )

    // Vertical contoured lobe
    Box(
      modifier = Modifier
        .size(width = 62.dp, height = 170.dp)
        .clip(RoundedCornerShape(31.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF0F1520),
              Color(0xFF0A0F17)
            )
          )
        )
        .border(1.dp, Color(0xFF17202E), RoundedCornerShape(31.dp))
    )

    // Up Arrow Button
    Box(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .size(width = 58.dp, height = 52.dp)
        .clickable(interactionSource = upSource, indication = null, onClick = onUp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(
        modifier = Modifier
          .size(width = 12.dp, height = 9.dp)
          .graphicsLayer {
            val scale = if (isUpPressed) 1.2f else 1f
            scaleX = scale
            scaleY = scale
          }
      ) {
        val path = Path().apply {
          moveTo(size.width / 2f, 0f)
          lineTo(size.width, size.height)
          lineTo(0f, size.height)
          close()
        }
        drawPath(path, color = if (isUpPressed) activeAccent else Color(0xFFC8CDD8))
      }
    }

    // Down Arrow Button
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .size(width = 58.dp, height = 52.dp)
        .clickable(interactionSource = downSource, indication = null, onClick = onDown),
      contentAlignment = Alignment.Center
    ) {
      Canvas(
        modifier = Modifier
          .size(width = 12.dp, height = 9.dp)
          .graphicsLayer {
            val scale = if (isDownPressed) 1.2f else 1f
            scaleX = scale
            scaleY = scale
          }
      ) {
        val path = Path().apply {
          moveTo(0f, 0f)
          lineTo(size.width, 0f)
          lineTo(size.width / 2f, size.height)
          close()
        }
        drawPath(path, color = if (isDownPressed) activeAccent else Color(0xFFC8CDD8))
      }
    }

    // Left Arrow Button
    Box(
      modifier = Modifier
        .align(Alignment.CenterStart)
        .size(width = 52.dp, height = 58.dp)
        .clickable(interactionSource = leftSource, indication = null, onClick = onLeft),
      contentAlignment = Alignment.Center
    ) {
      Canvas(
        modifier = Modifier
          .size(width = 9.dp, height = 12.dp)
          .graphicsLayer {
            val scale = if (isLeftPressed) 1.2f else 1f
            scaleX = scale
            scaleY = scale
          }
      ) {
        val path = Path().apply {
          moveTo(size.width, 0f)
          lineTo(size.width, size.height)
          lineTo(0f, size.height / 2f)
          close()
        }
        drawPath(path, color = if (isLeftPressed) activeAccent else Color(0xFFC8CDD8))
      }
    }

    // Right Arrow Button
    Box(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .size(width = 52.dp, height = 58.dp)
        .clickable(interactionSource = rightSource, indication = null, onClick = onRight),
      contentAlignment = Alignment.Center
    ) {
      Canvas(
        modifier = Modifier
          .size(width = 9.dp, height = 12.dp)
          .graphicsLayer {
            val scale = if (isRightPressed) 1.2f else 1f
            scaleX = scale
            scaleY = scale
          }
      ) {
        val path = Path().apply {
          moveTo(0f, 0f)
          lineTo(size.width, size.height / 2f)
          lineTo(0f, size.height)
          close()
        }
        drawPath(path, color = if (isRightPressed) activeAccent else Color(0xFFC8CDD8))
      }
    }

    // Center OK Button with dynamic accent ring & subtle glow
    val okInteraction = remember { MutableInteractionSource() }
    val isOkPressed by okInteraction.collectIsPressedAsState()
    val okScale by animateFloatAsState(
      targetValue = if (isOkPressed) 0.94f else 1f,
      animationSpec = tween(durationMillis = 100),
      label = "okScale"
    )

    Box(
      modifier = Modifier
        .size(58.dp)
        .graphicsLayer {
          scaleX = okScale
          scaleY = okScale
        }
        .shadow(8.dp, CircleShape, spotColor = activeAccent.copy(alpha = 0.5f))
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors = listOf(
              Color(0xFF131A26),
              Color(0xFF090D14)
            )
          )
        )
        .border(1.8.dp, activeAccent, CircleShape)
        .clickable(interactionSource = okInteraction, indication = null, onClick = onSelect),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "OK",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
  }
}

/**
 * Playback button (rounded pill/circle with tactile press feedback)
 */
@Composable
fun FigmaPlaybackPill(
  width: Int,
  onClick: () -> Unit,
  borderColor: Color = Color(0xFF1A2230),
  content: @Composable () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.93f else 1f,
    animationSpec = tween(durationMillis = 100),
    label = "pillScale"
  )

  Box(
    modifier = Modifier
      .size(width = width.dp, height = 46.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .shadow(6.dp, RoundedCornerShape(23.dp), spotColor = Color.Black.copy(alpha = 0.5f))
      .clip(RoundedCornerShape(23.dp))
      .background(Color(0xFF0F1520))
      .border(1.dp, borderColor, RoundedCornerShape(23.dp))
      .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    content()
  }
}

/**
 * Play/Pause icon (Play triangle + Pause bars ▶❚❚)
 */
@Composable
fun FigmaPlayPauseIcon(color: Color = Color.White) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
  ) {
    Canvas(modifier = Modifier.size(width = 11.dp, height = 13.dp)) {
      val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(size.width, size.height / 2f)
        lineTo(0f, size.height)
        close()
      }
      drawPath(path, color = color)
    }

    Spacer(modifier = Modifier.width(4.dp))

    Box(
      modifier = Modifier
        .size(width = 2.5.dp, height = 12.dp)
        .background(color, RoundedCornerShape(0.5.dp))
    )
    Spacer(modifier = Modifier.width(2.5.dp))
    Box(
      modifier = Modifier
        .size(width = 2.5.dp, height = 12.dp)
        .background(color, RoundedCornerShape(0.5.dp))
    )
  }
}

/**
 * Volume Control:
 * minus on the left, thin horizontal track, circular accent-colored thumb, plus on the right.
 */
@Composable
fun FigmaVolumeControl(
  volume: Float,
  activeAccent: Color = FigmaTheme.AccentBlue,
  onVolumeChange: (Float) -> Unit,
  onVolumeDown: () -> Unit,
  onVolumeUp: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth(0.92f)
      .padding(horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Minus on the left
    Box(
      modifier = Modifier
        .clickable(onClick = onVolumeDown)
        .padding(horizontal = 8.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size(width = 10.dp, height = 2.dp)
          .clip(RoundedCornerShape(1.dp))
          .background(Color(0xFF858B96))
      )
    }

    // Thin horizontal volume track
    BoxWithConstraints(
      modifier = Modifier
        .weight(1f)
        .height(26.dp)
        .pointerInput(Unit) {
          detectTapGestures { offset ->
            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
            onVolumeChange(fraction)
          }
        }
        .pointerInput(Unit) {
          detectDragGestures { change, _ ->
            change.consume()
            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
            onVolumeChange(fraction)
          }
        },
      contentAlignment = Alignment.CenterStart
    ) {
      val totalWidth = maxWidth
      val thumbOffset = (totalWidth - 14.dp) * volume

      // Inactive track (thin 2.5dp)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.5.dp)
          .clip(RoundedCornerShape(1.5.dp))
          .background(Color(0xFF1E2532))
      )

      // Active track (thin 2.5dp)
      Box(
        modifier = Modifier
          .fillMaxWidth(volume)
          .height(2.5.dp)
          .clip(RoundedCornerShape(1.5.dp))
          .background(activeAccent)
      )

      // Circular accent thumb with subtle halo
      Box(
        modifier = Modifier
          .offset(x = thumbOffset)
          .size(14.dp)
          .shadow(elevation = 5.dp, shape = CircleShape, spotColor = activeAccent)
          .clip(CircleShape)
          .background(activeAccent)
      )
    }

    // Plus on the right
    Box(
      modifier = Modifier
        .clickable(onClick = onVolumeUp)
        .padding(horizontal = 8.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.size(9.dp)) {
        drawLine(
          color = Color(0xFF858B96),
          start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
          end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
          strokeWidth = 2.dp.toPx()
        )
        drawLine(
          color = Color(0xFF858B96),
          start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
          end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
          strokeWidth = 2.dp.toPx()
        )
      }
    }
  }
}
