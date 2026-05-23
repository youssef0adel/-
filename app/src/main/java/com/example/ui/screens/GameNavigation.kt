package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.network.LanManager
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameNavigation(viewModel: GameViewModel) {
    val state by viewModel.roomState.collectAsState()
    val context = LocalContext.current

    // Local temporary transition screen at startup
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2200)
        showSplash = false
    }

    MysteryBackground(drawBloodDrips = showSplash || state.phase == GamePhase.LOBBY) {
        AnimatedContent(
            targetState = if (showSplash) GamePhase.LOBBY else state.phase,
            transitionSpec = {
                fadeIn() with fadeOut()
            },
            label = "PhaseTransition"
        ) { phase ->
            if (showSplash) {
                SplashScreen()
            } else {
                when (phase) {
                    GamePhase.LOBBY -> MainMenuOrLobbyScreen(viewModel, state)
                    GamePhase.ROLE_REVEAL -> RoleRevealScreen(viewModel, state)
                    GamePhase.CASE_INTRO -> CaseIntroScreen(viewModel, state)
                    GamePhase.EVIDENCE_ROUND -> EvidenceScreen(viewModel, state)
                    GamePhase.DISCUSSION -> DiscussionScreen(viewModel, state)
                    GamePhase.VOTING -> VotingScreen(viewModel, state)
                    GamePhase.VOTE_RESULT -> VoteResultScreen(viewModel, state)
                    GamePhase.JURY_ROUND -> JuryScreen(viewModel, state)
                    GamePhase.ENDGAME -> EndgameScreen(viewModel, state)
                }
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Wooden gold-beveled magnifying glass decoration icon
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(Color(0x1F2C1E14), CircleShape)
                .border(7.dp, GoldYell, CircleShape)
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Magnifier Logo",
                tint = GoldShine,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.3f)
                    .rotate(45f)
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Large high-end golden Arabic logo title
        Text(
            text = "مين فينا؟",
            color = GoldShine,
            fontSize = 52.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("app_logo_arabic")
        )
        
        // English subtitle tag
        Text(
            text = "WHO AMONG US?",
            color = PapyrusBgLight.copy(alpha = 0.7f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(70.dp))

        // Circular custom styled loading indicator
        CircularProgressIndicator(
            color = RedAccent,
            strokeWidth = 5.dp,
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "الكل متهم ......ولكن ؟",
            color = PapyrusBgLight.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// 2. MAIN MENU & LOBBY SYSTEM
// ==========================================
@Composable
fun MainMenuOrLobbyScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    var showPlayerSetup by remember { mutableStateOf(false) }
    var showLanJoinLobby by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    
    // Values for drafting new players
    var tempPlayerName by remember { mutableStateOf("") }
    val discoveredHosts by LanManager.discoveredHosts.collectAsState()
    val localIp = remember { LanManager.getLocalIpAddress() }

    if (isSettingsOpen) {
        SettingsDialog(viewModel = viewModel) { isSettingsOpen = false }
    }

    if (showPlayerSetup) {
        // LOCAL SETUP SCREEN (PASS-AND-PLAY)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ParchmentHeaderBanner(text = "إعداد اللاعبين")
            
            Spacer(modifier = Modifier.height(24.dp))

            ParchmentCard(
                modifier = Modifier.weight(1f),
                seed = 123L
            ) {
                Text(
                    text = "عدد اللاعبين: ${state.players.size}",
                    color = Color(0xFF4A1008),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "4 - 8 لاعبين (1 مجرم في 4 لاعبين، 2 مجرمين في 5+ لاعبين)",
                    color = PapyrusTextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Custom adding row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = tempPlayerName,
                        onValueChange = { tempPlayerName = it },
                        label = { Text("اسم اللاعب الجديد", fontSize = 16.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PapyrusText,
                            unfocusedTextColor = PapyrusText,
                            focusedBorderColor = DarkWoodButton,
                            unfocusedBorderColor = PapyrusTextSecondary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_name_input")
                        ,
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (tempPlayerName.isNotBlank()) {
                                if (state.players.size < 8) {
                                    viewModel.addLocalLobbyPlayer(tempPlayerName)
                                    tempPlayerName = ""
                                } else {
                                    Toast.makeText(context, "الحد الأقصى هو 8 لاعبين", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                        modifier = Modifier.testTag("add_player_button")
                    ) {
                        Icon(Icons.Default.Add, "Add player", tint = GoldShine, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة", color = GoldShine, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // List of players with scrollable layout
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.players) { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0x1F2C1E14), RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(DarkWoodButton, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = player.avatarId.toString(),
                                    color = GoldShine,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(14.dp))
                            
                            Text(
                                text = player.name,
                                color = PapyrusText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                            
                            IconButton(onClick = { viewModel.removePlayerFromLobby(player.id) }) {
                                Icon(Icons.Default.Delete, "Remove", tint = RedAccent, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedButton(
                    onClick = { showPlayerSetup = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldShine),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("رجوع", fontSize = 18.sp)
                }
                
                Button(
                    onClick = {
                        if (state.players.size < 4) {
                            Toast.makeText(context, "لازم يكون فيه 4 لاعبين عالأقل", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.startInvestigationGame()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("start_game_button"),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, "Start", tint = GoldShine, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("نبدأ القضية", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    } else if (showLanJoinLobby) {
        // LAN MULTIPLAYER DISCOVERY LOBBY
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ParchmentHeaderBanner(text = "البحث عن الغرف")

            Spacer(modifier = Modifier.height(18.dp))

            ParchmentCard(
                modifier = Modifier.weight(1f),
                seed = 456L
            ) {
                Text(
                    text = "عنوان جهازك: $localIp",
                    color = PapyrusTextSecondary,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "الغرف المتاحة عالشبكة:",
                    color = DarkWoodButton,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (discoveredHosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = DarkWoodButton, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "مستنيين حد يفتح غرفة...",
                                color = PapyrusTextSecondary,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        discoveredHosts.forEach { (ip, hostName) ->
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x0C000000), RoundedCornerShape(12.dp))
                                        .border(2.dp, GoldYell, RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.joinLanHost(ip, "لاعب ضيف")
                                        }
                                        .padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Wifi, "Wifi game", tint = RedAccent, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(hostName, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("العنوان: $ip", color = PapyrusTextSecondary, fontSize = 13.sp)
                                    }
                                    Icon(Icons.Default.ArrowForward, "Join details", tint = DarkWoodButton, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }

                // Add manual IP Join box
                var manualIp by remember { mutableStateOf("") }
                var manualName by remember { mutableStateOf("محقق شبكي") }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = { manualIp = it },
                        label = { Text("أو اكتب عنوان IP", fontSize = 15.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PapyrusText,
                            unfocusedTextColor = PapyrusText,
                            focusedBorderColor = DarkWoodButton,
                            unfocusedBorderColor = PapyrusTextSecondary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (manualIp.isNotBlank()) {
                                viewModel.joinLanHost(manualIp.trim(), manualName)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ربط", color = GoldShine, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Back button
            Button(
                onClick = {
                    LanManager.stopDiscovery()
                    showLanJoinLobby = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("الرجوع للقائمة", color = GoldShine, fontSize = 18.sp)
            }
        }
    } else {
        // MAIN MENU HOME SCREEN
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Elegant header logo area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 50.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(Color(0x1F2C1E14), CircleShape)
                        .border(5.dp, GoldYell, CircleShape)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon header",
                        tint = GoldShine,
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(45f)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                // Main Beveled Gold Title
                Text(
                    text = "مين فينا؟",
                    color = GoldShine,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "WHO AMONG US?",
                    color = PapyrusBgLight.copy(alpha = 0.7f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
            }

            // Beautiful wooden-textured Action buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pass and Play Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(5.dp, RoundedCornerShape(14.dp))
                        .clickable {
                            viewModel.setupPassAndPlayGame()
                            showPlayerSetup = true
                        }
                        .testTag("new_game_opt_button"),
                    colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, DarkWoodButton)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 22.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Pass device",
                            tint = DarkWoodButton,
                            modifier = Modifier.size(42.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 18.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "قضية جديدة",
                                color = Color(0xFF4A1008),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "جرّب اللعب بالتمرير (جهاز واحد)",
                                color = PapyrusTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go play",
                            tint = DarkWoodButton,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // WiFi LAN Multiplayer Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(5.dp, RoundedCornerShape(14.dp))
                        .clickable {
                            // Automatically start UDP discovery and open joins
                            LanManager.startDiscovery()
                            showLanJoinLobby = true
                        }
                        .testTag("lan_multiplayer_button"),
                    colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, DarkWoodButton)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 22.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "WiFi game",
                            tint = DarkWoodButton,
                            modifier = Modifier.size(42.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 18.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "شبكة محلية (LAN)",
                                color = Color(0xFF4A1008),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "العب بأجهزة منفصلة (WiFi/Hotspot)",
                                color = PapyrusTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go LAN",
                            tint = DarkWoodButton,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // LAN Host Quick Creator Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(5.dp, RoundedCornerShape(14.dp))
                        .clickable {
                            viewModel.startLanHost("مضيف التحقيق")
                            // Show player setup instantly inside LAN Room mode
                            showPlayerSetup = true
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF35120D)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, GoldYell)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddBox, "Host Game", tint = GoldShine, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "إنشاء ومشاركة غرفة (Host)",
                            color = GoldShine,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Settings & Preferences Card Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(5.dp, RoundedCornerShape(14.dp))
                        .clickable { isSettingsOpen = true }
                        .testTag("settings_button"),
                    colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, DarkWoodButton)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Icon",
                            tint = DarkWoodButton,
                            modifier = Modifier.size(34.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 18.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "الإعدادات وقواعد اللعبة",
                                color = Color(0xFF4A1008),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go settings",
                            tint = DarkWoodButton,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Bottom Footer banner slogan
            Text(
                text = "كلنا متهمين ......ولكن ؟",
                color = PapyrusBgLight.copy(alpha = 0.5f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

// ==========================================
// 3. ROLE REVEAL SCREEN
// ==========================================
@Composable
fun RoleRevealScreen(viewModel: GameViewModel, state: RoomState) {
    val activePassPlayer = state.players.getOrNull(state.activePassPlayerIndex) ?: return
    var revealed by remember(state.activePassPlayerIndex) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "كشف الملفات السرية")

        Spacer(modifier = Modifier.height(14.dp))

        // Shield warnings during offline passing device sequential reveals
        if (!revealed) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(Color(0x3B6E1C11), CircleShape)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Hide role cards",
                        tint = GoldShine,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "ادي التلفون لـ:",
                    color = PapyrusBgLight.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = activePassPlayer.name,
                    color = GoldShine,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("pass_name_reveal")
                )

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = { revealed = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldYell),
                    modifier = Modifier.testTag("reveal_role_button")
                ) {
                    Text("اعرف حقيقتك", color = Color(0xFF2C150A), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Secret Parchment Card showing role & motive details
            ParchmentCard(
                modifier = Modifier.weight(1f),
                seed = state.activePassPlayerIndex.toLong()
            ) {
                Text(
                    text = "ملف ${activePassPlayer.name}",
                    color = DarkWoodButton,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Avatar outline
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(DarkBg, CircleShape)
                        .border(3.dp, GoldYell, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, "Avatar", tint = GoldShine, modifier = Modifier.size(56.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Character Metadata
                val char = activePassPlayer.character
                if (char != null) {
                    Text("الاسم المستعار: ${char.name}", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text("العمر: ${char.age} سنة | المهنة: ${char.occupation}", color = PapyrusTextSecondary, fontSize = 15.sp)
                    Text("الصفات: ${char.traits}", color = PapyrusTextSecondary, fontSize = 15.sp, fontStyle = FontStyle.Italic)

                    Spacer(modifier = Modifier.height(14.dp))

                    Divider(color = Color(0x3B2C1E14), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    // SECRET ROLE BADGE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activePassPlayer.isMafia) RedAccent else Color(0xFF1B4332))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (activePassPlayer.isMafia) Icons.Default.Dangerous else Icons.Default.Security,
                            contentDescription = "Role Symbol",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (activePassPlayer.isMafia) "أنت: المافيا الحقيقية" else "أنت: محقق بريء",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "الدافع:",
                        color = Color(0xFF4A1008),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (activePassPlayer.isMafia) char.hiddenMotive else "حاول تدمج الأدلة وتحمي الأبرياء من اتهامات المافيا.",
                        color = PapyrusText,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action to advance
            Button(
                onClick = {
                    viewModel.confirmSecretsRevealed()
                    revealed = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_reveal_advance"),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(
                    text = if (state.activePassPlayerIndex < state.players.size - 1) "أخفي التقرير والتالي" else "استعراض تفاصيل القضية",
                    color = GoldShine,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }
    }
}

// ==========================================
// 4. CASE INTRO / DETAILS SCREEN
// ==========================================
@Composable
fun CaseIntroScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "تفاصيل الجريمة")

        Spacer(modifier = Modifier.height(14.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 9991L
        ) {
            Text(
                text = currentCase.title,
                color = Color(0xFF7A1B0C),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Info grid boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("المكان: ${currentCase.location}", color = PapyrusText, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("الضحية: ${currentCase.victim}", color = PapyrusText, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Narrative scrolling body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0x12000000), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                LazyColumn {
                    item {
                        Text(
                            text = currentCase.description,
                            color = PapyrusText,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profiles list
            Text(
                text = "المشتبه فيهم:",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentCase.characters.forEach { char ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF8C2012), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.name.split(" ").firstOrNull() ?: char.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.startCaseInvestigationIntro() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("case_details_confirm_button"),
            contentPadding = PaddingValues(18.dp)
        ) {
            Icon(Icons.Default.FindInPage, "Start Clues", tint = GoldShine, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("نبدأ التحقيق", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
    }
}

// ==========================================
// 5. EVIDENCE / PROGRESSIVE CLUES SCREEN
// ==========================================
@Composable
fun EvidenceScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase ?: return
    val clueIndex = state.currentEvidenceIndex
    val currentClue = currentCase.evidenceList.getOrNull(clueIndex) ?: "لا أدلة إضافية حالياً."
    
    var showHint by remember(clueIndex) { mutableStateOf(false) }
    var localSuspicionValue by remember(clueIndex) { mutableStateOf(0.4f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "الدليل ${clueIndex + 1} من ${currentCase.evidenceList.size}")

        Spacer(modifier = Modifier.height(16.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = (clueIndex + 10).toLong()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0x0C000000), CircleShape)
                    .border(2.dp, GoldYell, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search details info",
                    tint = Color(0xFF6E1B10),
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "دليل جديد تم اكتشافه:",
                color = Color(0xFF531E17),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0x0F000000), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentClue,
                    color = PapyrusText,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated / dynamic custom caution warning click hint
            AnimatedVisibility(visible = showHint) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF2CD))
                        .border(1.dp, Color(0xFFFFCD56), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = currentCase.hint,
                        color = Color(0xFF856404),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!showHint) {
                Button(
                    onClick = { showHint = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2A012)),
                    modifier = Modifier.testTag("clue_hint_button")
                ) {
                    Icon(Icons.Default.Warning, "Clues Alert", tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تلميح المساعد", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Suspicion Slider scale
            Text(
                text = "نسبة الشك: ${(localSuspicionValue * 100).toInt()}%",
                color = PapyrusTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Slider(
                value = localSuspicionValue,
                onValueChange = { localSuspicionValue = it },
                colors = SliderDefaults.colors(
                    thumbColor = RedAccent,
                    activeTrackColor = RedAccent,
                    inactiveTrackColor = Color(0x1F2C1E14)
                ),
                modifier = Modifier.fillMaxWidth().testTag("suspicion_slider")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.advanceFromEvidenceToDiscussion() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("evidence_reveal_advance"),
            contentPadding = PaddingValues(18.dp)
        ) {
            Icon(Icons.Default.RecordVoiceOver, "Discuss", tint = GoldShine, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("نبدأ المناقشة", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

// ==========================================
// 6. DISCUSSION SCREEN (WITH RADIAL CLOCK)
// ==========================================
@Composable
fun DiscussionScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    var suspectedByClick = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "مرحلة النقاش والمواجهة")

        // Interactive Radial Clock & Circular layout of players around
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Clock Timer in the central region
            val formattedTime = String.format("%02d:%02d", state.timerSecondsLeft / 60, state.timerSecondsLeft % 60)
            
            Canvas(modifier = Modifier.size(190.dp)) {
                // Background dark disk
                drawCircle(color = Color(0xFF1E0604), radius = size.minDimension / 2)
                
                // Red sweep progress trace
                val sweepAngle = if (state.timerTotalSeconds > 0) {
                    (state.timerSecondsLeft.toFloat() / state.timerTotalSeconds.toFloat()) * 360f
                } else 360f
                
                drawArc(
                    color = Color(0xFFE73224),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Central time texts
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("متبقي", color = GoldYell, fontSize = 14.sp)
                Text(
                    text = formattedTime,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("timer_countdown_display")
                )
                Text("للإدلاء بالاستنتاج", color = PapyrusBgLight.copy(alpha = 0.5f), fontSize = 12.sp)
            }

            // Radial distribution placement logic for players
            val alivePlayers = state.players.filter { it.isAlive }
            alivePlayers.forEachIndexed { index, player ->
                val angleRad = (2 * Math.PI * index) / alivePlayers.size
                val xOffset = (140 * cos(angleRad)).dp
                val yOffset = (140 * sin(angleRad)).dp

                val isClickSuspected = player.id in suspectedByClick

                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(72.dp)
                        .shadow(3.dp, CircleShape)
                        .background(
                            if (isClickSuspected) Color(0xFFC42512) else Color(0xFF421E14),
                            CircleShape
                        )
                        .border(
                            2.dp,
                            if (isClickSuspected) GoldShine else Color(0x3BFFFFFF),
                            CircleShape
                        )
                        .clickable {
                            if (isClickSuspected) {
                                suspectedByClick.remove(player.id)
                            } else {
                                suspectedByClick.add(player.id)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = player.name.take(6),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (player.isMafia && isClickSuspected) GoldYell else Color(0x3B000000),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isClickSuspected) "متهم" else "قيد السؤال",
                                color = if (isClickSuspected) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ParchmentCard(
            modifier = Modifier.wrapContentHeight(),
            seed = 771L
        ) {
            Text(
                text = "اضغط على لاعب لتوجيه الشبهة ضده باللون الأحمر.",
                color = PapyrusTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.advanceFromDiscussionToVoting() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voting_advance_button"),
            contentPadding = PaddingValues(18.dp)
        ) {
            Icon(Icons.Default.HowToVote, "Start Votes", tint = GoldShine, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("يلا نصوّت", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

// ==========================================
// 7. VOTING SCREEN - FIXED FOR ALL PLAYERS
// ==========================================
@Composable
fun VotingScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    
    // Get alive players and track voting progress
    val alivePlayers = state.players.filter { it.isAlive }
    
    // Determine current voter based on activePassPlayerIndex
    val currentVoter = state.players.getOrNull(state.activePassPlayerIndex)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "صندوق الاقتراع")
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // Show voting progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2C1E14)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "الدور على: ${currentVoter?.name ?: "غير معروف"}",
                color = GoldShine,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (currentVoter != null) {
            // Show voting interface for current voter
            var selectedTargetId by remember(currentVoter.id) { mutableStateOf("") }
            
            ParchmentCard(
                modifier = Modifier.weight(1f),
                seed = 33L
            ) {
                Text(
                    text = "دور ${currentVoter.name} في التصويت",
                    color = Color(0xFF6E1B10),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    "اختار الشخص اللي شاكك فيه عشان نطرده:",
                    color = PapyrusTextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Candidates List
                val eligibleCandidates = alivePlayers.filter { it.id != currentVoter.id }
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(eligibleCandidates) { candidate ->
                        val isSelected = candidate.id == selectedTargetId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color(0x3B8C2012) else Color(0x0C000000),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    2.dp,
                                    if (isSelected) RedAccent else Color(0x1F2C1E14),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTargetId = candidate.id }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(if (isSelected) RedAccent else Color(0xFF421D18), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person,
                                    contentDescription = "Pick status target",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                candidate.character?.let {
                                    Text("المشتبه: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = {
                    if (selectedTargetId.isBlank()) {
                        Toast.makeText(context, "لازم تختار حد عشان تصوت", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.submitVote(selectedTargetId)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_vote_action_button"),
                contentPadding = PaddingValues(18.dp)
            ) {
                Icon(Icons.Default.VerifiedUser, "Confirm vote", tint = GoldShine, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("أكد الصوت", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        } else {
            // No current voter (shouldn't happen, but handle gracefully)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "جاري تجهيز التصويت...",
                    color = PapyrusBgLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ==========================================
// 8. JURY ENDGAME MECHANIC SCREEN
// ==========================================
@Composable
fun JuryScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    val eliminatedPlayers = state.players.filter { !it.isAlive }
    val remainingSuspects = state.players.filter { it.isAlive }
    
    // In pass and play, we sequentialize through eliminated players as jury voters
    // Let's pick the active jury voter
    val juryVoter = eliminatedPlayers.firstOrNull { it.id !in state.juryVotes.keys }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "هيئة المحلفين العليا")

        Spacer(modifier = Modifier.height(14.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 88L
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color(0x3B6E1B10), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = "Gavel judge",
                    tint = RedAccent,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "العدالة في إيديكم!",
                color = Color(0xFF6E1D10),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "بما أنه ما تبقاش غير اتنين، اللعيبة اللي خرجوا بيرجعوا يصوتوا عشان يثبتوا إدانة المافيا.",
                color = PapyrusTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (juryVoter != null) {
                Text(
                    text = "دور المحلف: ${juryVoter.name}",
                    color = RedAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.testTag("jury_voter_title")
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(remainingSuspects) { suspect ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0C000000), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0x3B2C1E14), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(suspect.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                suspect.character?.let {
                                    Text("الشخصية: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 13.sp)
                                }
                            }
                            
                            Button(
                                onClick = { viewModel.submitJuryVote(suspect.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                            ) {
                                Text("إدانة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            } else {
                Text(
                    "خلصنا كل استنتاجات المحلفين. هنعلن النتيجة دلوقتي!",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ==========================================
// 9. ENDGAME WINNER SCREEN
// ==========================================
@Composable
fun EndgameScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase
    val isInnocentsWinner = state.winnerSide == "INNOCENTS"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "كشف أوراق القضية النهائية")

        Spacer(modifier = Modifier.height(18.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 4441L
        ) {
            // Trophy Logo Area
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(Color(0x1FA2A012), CircleShape)
                    .border(2.dp, GoldYell, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Trophy logo endgame",
                    tint = GoldYell,
                    modifier = Modifier.size(70.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // BIG WINNER BANNER
            Text(
                text = if (isInnocentsWinner) "انتصار العدالة والأبرياء" else "سقطت المدينة والمافيا انتصرت",
                color = if (isInnocentsWinner) Color(0xFF1B4332) else RedAccent,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("endgame_victory_title")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Plot reveal details scrolls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0x0C000000), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            text = if (isInnocentsWinner) {
                                "المحققين قدروا يطابقوا الأدلة ويتعقبوا الآثار عشان يحبسوا المجرمين الحقيقيين. الحكمة انتصرت!"
                            } else {
                                "المافيا قدرت تلفق الأكاذيب وتوجه الاتهامات للناس الشرفاء. الحقيقة ضاعت!"
                            },
                            color = PapyrusText,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- CRIMINAL DRAMATIC REVEAL ---
                        Text(
                            text = "الهوية الجنائية والمجرم الأكبر:",
                            color = Color(0xFF4A1008),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().testTag("dramatic_criminal_reveal_header")
                        )

                        state.players.filter { it.isMafia }.forEach { mafia ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x1CE63946)),
                                border = BorderStroke(1.dp, Color(0xFFE63946)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Text(
                                        text = "المجرم: ${mafia.name}",
                                        color = Color(0xFFD62828),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        modifier = Modifier.testTag("criminal_character_name")
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("العمر: ${mafia.character?.age ?: 30} عام | المهنة: ${mafia.character?.occupation ?: "مجهول"}", color = PapyrusText, fontSize = 15.sp)
                                    Text("المظهر والطباع: ${mafia.character?.traits ?: ""}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Text("المستوى الاجتماعي: ${mafia.character?.socialStatus ?: "متوسط الحال"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Text("علاقته بالضحية: ${mafia.character?.relationshipToVictim ?: "غامضة"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Text("علاقته بالمشتبهين: ${mafia.character?.relationshipToOtherSuspects ?: "منافسة"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Text("السجل الجنائي: ${mafia.character?.relevantHistory ?: "خالي من السوابق"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "الدافع والنية المخفية: ${mafia.character?.hiddenMotive ?: ""}",
                                        color = Color(0xFF4A1008),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(16.dp))

                        // --- CASE EXPLANATION CLOSURE ---
                        Text(
                            text = "تفاصيل وملف القضية بالكامل:",
                            color = Color(0xFF355E3B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            modifier = Modifier.fillMaxWidth().testTag("case_explanation_header")
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0x1F2A9D8F)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = currentCase?.explanation ?: "مافيش سجلات للملف ده.",
                                color = Color(0xFF1D3557),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(14.dp).testTag("case_explanation_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "كشف الهويات السرية:",
                            color = DarkWoodButton,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        state.players.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (p.isMafia) "المافيا" else "بريء",
                                    color = if (p.isMafia) RedAccent else Color(0xFF1B4332),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${p.name} (${p.character?.name ?: ""})",
                                    color = PapyrusTextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Play Again & Exit Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.playButtonClick(); viewModel.playAgain() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("play_again_button"),
                contentPadding = PaddingValues(18.dp)
            ) {
                Icon(Icons.Default.Refresh, "Play again", tint = GoldShine, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("نلعب تاني", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            
            OutlinedButton(
                onClick = { viewModel.playButtonClick(); viewModel.resetToMainMenu() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldShine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("نرجع للقايمة", fontSize = 18.sp)
            }
        }
    }
}

// ==========================================
// 10. SETTINGS & CONVENTIONS DIALOG
// ==========================================
@Composable
fun SettingsDialog(
    viewModel: GameViewModel,
    onDismissRequest: () -> Unit
) {
    val state by viewModel.roomState.collectAsState()
    var discTimeMins by remember { mutableStateOf(state.settings.discussionTimeMinutes) }
    var voteTimeMins by remember { mutableStateOf(state.settings.votingTimeMinutes) }
    var soundEnabled by remember { mutableStateOf(state.settings.isMusicEnabled) }
    var sliderVol by remember { mutableStateOf(state.settings.volume) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateSettings(discTimeMins, voteTimeMins, soundEnabled, sliderVol)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)
            ) {
                Text("حفظ التغييرات", color = GoldShine, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("تجاهل", color = PapyrusTextSecondary, fontSize = 16.sp)
            }
        },
        title = {
            Text(
                text = "الإعدادات وقواعد اللعبة",
                color = Color(0xFF4A1008),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            ParchmentCard(
                seed = 77L,
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.wrapContentHeight()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Sound switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { soundEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = RedAccent)
                            )
                            Text("المؤثرات الصوتية والموسيقى", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("مستوى الصوت: ${(sliderVol * 100).toInt()}%", color = PapyrusTextSecondary, fontSize = 13.sp)
                        Slider(
                            value = sliderVol,
                            onValueChange = { sliderVol = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider(color = Color(0x3B2C1E14))

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom timings
                        Text("وقت جولات التحقيق والمناقشة", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { if (discTimeMins > 1) discTimeMins-- },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)
                            ) { Text("-", fontSize = 16.sp) }
                            Text("$discTimeMins دقيقة", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Button(
                                onClick = { if (discTimeMins < 10) discTimeMins++ },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)
                            ) { Text("+", fontSize = 16.sp) }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("وقت جولات الاقتراع والتصويت", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { if (voteTimeMins > 1) voteTimeMins-- },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)
                            ) { Text("-", fontSize = 16.sp) }
                            Text("$voteTimeMins دقيقة", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Button(
                                onClick = { if (voteTimeMins < 5) voteTimeMins++ },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)
                            ) { Text("+", fontSize = 16.sp) }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Divider(color = Color(0x3B2C1E14))

                        Spacer(modifier = Modifier.height(8.dp))

                        // Game Rules
                        Text("قوانين اللعبة الأساسية:", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "1. اللعبة بتدعم من 4 لـ 8 لعيبة.\n" +
                                   "2. لو عدد المحققين 4، يبقى فيه مافيا واحدة بس؛ لو زاد عن كده بيبقى فيه مافيتين.\n" +
                                   "3. في نهاية الجولة لو اتبقى اتنين بس، اللي خرجوا بيرجعوا تاني عشان يصوتوا ويحكموا على المافيا.",
                            color = PapyrusTextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        },
        containerColor = PapyrusBg
    )
}

@Composable
fun VoteResultScreen(viewModel: GameViewModel, state: RoomState) {
    val isHost = state.mode == "PASS_AND_PLAY" || state.hostId == viewModel.myPlayerId.value
    
    MysteryBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .safeDrawingPadding()
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ParchmentHeaderBanner(text = "نتايج الاقتراع")
            
            Spacer(modifier = Modifier.height(28.dp))
            
            ParchmentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (state.tiedVotePlayers.isNotEmpty()) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = "Result Icon",
                        tint = if (state.tiedVotePlayers.isNotEmpty()) Color(0xFFC62828) else GoldShine,
                        modifier = Modifier.size(72.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    Text(
                        text = state.lastEliminatedResult,
                        color = Color(0xFF1C130C),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp,
                        modifier = Modifier.testTag("vote_result_text")
                    )
                    
                    if (state.tiedVotePlayers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "فيه تعادل! هنعيد التصويت بين اللي اتعادلوا بس لحد ما نوصل لأغلبية.",
                            color = Color(0xFFB71C1C),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(36.dp))
            
            if (isHost) {
                Button(
                    onClick = {
                        viewModel.playButtonClick()
                        viewModel.confirmVoteResultAndProceed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("confirm_vote_result_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (state.tiedVotePlayers.isNotEmpty()) "نبدا جولة التعادل" else "نكمل التحقيق",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x3D2C1E14)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "مستنيين المضيف يكمل القضية...",
                        color = PapyrusBgLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    )
                }
            }
        }
    }
}