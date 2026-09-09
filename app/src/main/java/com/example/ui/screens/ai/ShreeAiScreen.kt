package com.example.ui.screens.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.PendingPartItem
import com.example.ai.PendingPartsReport
import com.example.ai.PendingUrgency
import com.example.data.model.Firm
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldDark
import com.example.ui.theme.AmberGoldLight
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.AutoNavyLight
import com.example.ui.theme.SaaBlue
import com.example.ui.theme.SaaBlueBg
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.SteelBlue
import com.example.ui.theme.TvsRed
import com.example.ui.theme.TvsRedBg
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.ShreeChatMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShreeAiScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStockIn: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val chatMessages by viewModel.shreeChatMessages.collectAsStateWithLifecycle()
    val pendingReport by viewModel.pendingPartsReport.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val autoSpeak by viewModel.autoSpeakEnabled.collectAsStateWithLifecycle()

    var inputQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chat, 1: Pending Parts List
    var pendingFilter by remember { mutableStateOf("ALL") } // "ALL", "ZERO", "SAA", "TVS"

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.initShreeConversation()
    }

    // Auto-scroll to bottom on new message in chat tab
    LaunchedEffect(chatMessages.size) {
        if (selectedTab == 0 && chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Speaking pulse animation for Shree Avatar
    val infiniteTransition = rememberInfiniteTransition(label = "speech_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isSpeaking) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .scale(if (isSpeaking) pulseScale else 1.0f)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AmberGold, AutoNavyDark)))
                                .border(2.dp, if (isSpeaking) StatusGreen else AmberGoldLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Shree AI Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "श्री AI (Shree)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = StatusGreen.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "100% Offline",
                                        color = StatusGreenBg,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isSpeaking) "🎙️ Speaking response..." else "Purchase & Pending Parts Tracker",
                                fontSize = 11.sp,
                                color = if (isSpeaking) AmberGold else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_shree")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Stop audio button when speaking
                    if (isSpeaking) {
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier.testTag("btn_stop_speech")
                        ) {
                            Icon(
                                Icons.Default.StopCircle,
                                contentDescription = "Stop Speech",
                                tint = AmberGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Auto-voice toggle
                    IconButton(
                        onClick = { viewModel.toggleAutoSpeak(!autoSpeak) },
                        modifier = Modifier.testTag("btn_toggle_auto_speak")
                    ) {
                        Icon(
                            imageVector = if (autoSpeak) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Voice",
                            tint = if (autoSpeak) AmberGold else Color.White.copy(alpha = 0.5f)
                        )
                    }

                    // WhatsApp Order Share
                    IconButton(
                        onClick = {
                            shareWhatsAppOrder(context, viewModel.getWhatsAppOrderText())
                        },
                        modifier = Modifier.testTag("btn_share_whatsapp_order")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Order",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AutoNavy)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Voice Speaking Banner
            AnimatedVisibility(visible = isSpeaking) {
                Surface(
                    color = AmberGoldLight.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = AutoNavyDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "श्री बोल रही हैं (Audio playing)...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AutoNavyDark
                            )
                        }
                        Text(
                            text = "रोकें (Stop)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AutoNavy,
                            modifier = Modifier
                                .clickable { viewModel.stopSpeaking() }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Tab Selector: Chat vs Pending Parts Reorder List
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AutoNavyLight,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AmberGold,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("बातचीत (Voice Chat)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "पेंडिंग पार्ट्स (${pendingReport.totalPendingCount})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }

            // Tab Content
            if (selectedTab == 0) {
                // CHAT VIEW
                ChatView(
                    chatMessages = chatMessages,
                    listState = listState,
                    isSpeaking = isSpeaking,
                    onSpeak = { text -> viewModel.speakText(text) },
                    onStopSpeak = { viewModel.stopSpeaking() },
                    onCopy = { text -> copyToClipboard(context, text) },
                    onSelectAction = { prompt -> viewModel.askShree(prompt) },
                    onViewPendingTab = { selectedTab = 1 },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                // Bottom Input Bar
                ChatInputBar(
                    query = inputQuery,
                    onQueryChange = { inputQuery = it },
                    onSend = {
                        if (inputQuery.isNotBlank()) {
                            viewModel.askShree(inputQuery)
                            inputQuery = ""
                        }
                    }
                )
            } else {
                // PENDING PARTS AUDIT & REORDER LIST VIEW
                PendingPartsListView(
                    report = pendingReport,
                    currentFilter = pendingFilter,
                    onFilterChange = { pendingFilter = it },
                    onStockInPart = onNavigateToStockIn,
                    onShareWhatsApp = {
                        shareWhatsAppOrder(context, viewModel.getWhatsAppOrderText())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ChatView(
    chatMessages: List<ShreeChatMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isSpeaking: Boolean,
    onSpeak: (String) -> Unit,
    onStopSpeak: () -> Unit,
    onCopy: (String) -> Unit,
    onSelectAction: (String) -> Unit,
    onViewPendingTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Quick Action Chips Carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                QuickPromptChip(
                    text = "📋 पेंडिंग पार्ट्स लिस्ट",
                    onClick = { onSelectAction("pending parts list dikhao") }
                )
            }
            item {
                QuickPromptChip(
                    text = "📊 परचेस व आर्डर ट्रैकिंग",
                    onClick = { onSelectAction("purchase track aur order entry dikhao") }
                )
            }
            item {
                QuickPromptChip(
                    text = "🚨 शून्य स्टॉक वाले पार्ट्स",
                    onClick = { onSelectAction("zero stock parts alert") }
                )
            }
            item {
                QuickPromptChip(
                    text = "📍 रैक सिस्टम कैसे है?",
                    onClick = { onSelectAction("rack location system guide") }
                )
            }
            item {
                QuickPromptChip(
                    text = "📶 वाई-फाई सिंक गाइड",
                    onClick = { onSelectAction("wifi sync kaise kare") }
                )
            }
            item {
                QuickPromptChip(
                    text = "🏢 SAA vs TVS स्टॉक",
                    onClick = { onSelectAction("SAA aur TVS firm ka stock") }
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(chatMessages, key = { it.id }) { message ->
                ChatMessageBubble(
                    message = message,
                    isSpeaking = isSpeaking,
                    onSpeak = { onSpeak(message.speechText.ifBlank { message.text }) },
                    onStopSpeak = onStopSpeak,
                    onCopy = { onCopy(message.text) },
                    onActionClick = onSelectAction,
                    onViewPendingTab = onViewPendingTab
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ShreeChatMessage,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onStopSpeak: () -> Unit,
    onCopy: () -> Unit,
    onActionClick: (String) -> Unit,
    onViewPendingTab: () -> Unit
) {
    if (message.isFromUser) {
        // User Bubble
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(AutoNavy)
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    } else {
        // Shree AI Bubble
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(0.96f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header with Shree Name & Speaking Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(AmberGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = AutoNavyDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "श्री AI",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AutoNavyDark
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Listen / Speak button
                            Surface(
                                color = if (isSpeaking) StatusRedBg else AmberGoldLight,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable {
                                    if (isSpeaking) onStopSpeak() else onSpeak()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                                        contentDescription = "Listen",
                                        tint = if (isSpeaking) StatusRed else AutoNavyDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSpeaking) "Stop" else "बोलकर सुनें",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSpeaking) StatusRed else AutoNavyDark
                                    )
                                }
                            }

                            // Copy button
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Slate600,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Message Text
                    Text(
                        text = message.text,
                        fontSize = 13.5.sp,
                        color = Slate800,
                        lineHeight = 20.sp
                    )

                    // Embedded Pending Parts Summary Box if attached
                    message.pendingReport?.let { report ->
                        if (report.pendingItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Slate100),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📋 ${report.totalPendingCount} पेंडिंग पार्ट्स पाए गए",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = AutoNavyDark
                                        )
                                        Text(
                                            text = "अनुमानित: ₹${report.totalEstimatedReorderCost.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = StatusGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = onViewPendingTab,
                                        colors = ButtonDefaults.buttonColors(containerColor = AutoNavy),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("पूरी पेंडिंग लिस्ट खोलें व व्हाट्सएप ऑर्डर भेजें", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Suggested Follow-up Prompts
                    if (message.suggestedActions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(message.suggestedActions) { action ->
                                Surface(
                                    color = SteelBlue.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SteelBlue.copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable { onActionClick(action) }
                                ) {
                                    Text(
                                        text = action,
                                        fontSize = 11.sp,
                                        color = SteelBlue,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPromptChip(text: String, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AutoNavyDark,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ChatInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text("श्री से पूछें (उदा. पेंडिंग पार्ट्स, रैक, सिंक...)", fontSize = 13.sp, color = Slate600)
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AutoNavy,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = Slate100,
                    unfocusedContainerColor = Slate100
                ),
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_shree_query")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (query.isNotBlank()) AutoNavy else Slate200)
                    .clickable(enabled = query.isNotBlank(), onClick = onSend)
                    .testTag("btn_send_shree"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (query.isNotBlank()) Color.White else Slate600,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PendingPartsListView(
    report: PendingPartsReport,
    currentFilter: String,
    onFilterChange: (String) -> Unit,
    onStockInPart: (String) -> Unit,
    onShareWhatsApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredItems = remember(report, currentFilter) {
        when (currentFilter) {
            "ZERO" -> report.pendingItems.filter { it.currentStock <= 0 }
            "SAA" -> report.pendingItems.filter { it.firm == Firm.SAA.code || it.firm == Firm.BOTH.code }
            "TVS" -> report.pendingItems.filter { it.firm == Firm.TVS.code || it.firm == Firm.BOTH.code }
            else -> report.pendingItems
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        // KPI Summary Dashboard Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = AutoNavy),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(AutoNavy, AutoNavyDark)
                            )
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PURCHASE & ORDER TRACKER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "पेंडिंग रीऑर्डर पार्ट्स रिपोर्ट",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Surface(
                            color = StatusRedBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${report.totalPendingCount} Items",
                                color = StatusRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniKpiTile(
                            label = "शून्य स्टॉक",
                            value = "${report.criticalZeroStockCount}",
                            containerColor = StatusRed.copy(alpha = 0.2f),
                            textColor = StatusRedBg,
                            modifier = Modifier.weight(1f)
                        )
                        MiniKpiTile(
                            label = "कम स्टॉक",
                            value = "${report.belowMinStockCount}",
                            containerColor = AmberGold.copy(alpha = 0.2f),
                            textColor = AmberGold,
                            modifier = Modifier.weight(1f)
                        )
                        MiniKpiTile(
                            label = "अनुमानित बजट",
                            value = "₹${report.totalEstimatedReorderCost.toInt()}",
                            containerColor = StatusGreen.copy(alpha = 0.2f),
                            textColor = StatusGreenBg,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Action: Share WhatsApp Order
                    Button(
                        onClick = onShareWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_whatsapp_order_sheet")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "सप्लायर को व्हाट्सएप पर आर्डर भेजें (Send WhatsApp Order)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Filter Chips Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = currentFilter == "ALL",
                        onClick = { onFilterChange("ALL") },
                        label = { Text("सभी (${report.totalPendingCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AutoNavy,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == "ZERO",
                        onClick = { onFilterChange("ZERO") },
                        label = { Text("खत्म स्टॉक (${report.criticalZeroStockCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == "SAA",
                        onClick = { onFilterChange("SAA") },
                        label = { Text("SAA फर्म (${report.saaPendingCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SaaBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == "TVS",
                        onClick = { onFilterChange("TVS") },
                        label = { Text("TVS फर्म (${report.tvsPendingCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TvsRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("कोई पेंडिंग पार्ट नहीं है!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AutoNavyDark)
                        Text(
                            "सभी स्पेयर पार्ट्स का स्टॉक निर्धारित सीमा के अंदर सुरक्षित है।",
                            fontSize = 12.sp,
                            color = Slate600,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(filteredItems, key = { it.partId }) { item ->
                PendingPartCard(
                    item = item,
                    onStockIn = { onStockInPart(item.partId) }
                )
            }
        }
    }
}

@Composable
private fun MiniKpiTile(
    label: String,
    value: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(text = label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun PendingPartCard(
    item: PendingPartItem,
    onStockIn: () -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.partName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AutoNavyDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Part No: ${item.partNumber} • ${item.category}",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Firm badge
                    Surface(
                        color = if (item.firm == Firm.TVS.code) TvsRedBg else SaaBlueBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.firm,
                            color = if (item.firm == Firm.TVS.code) TvsRed else SaaBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Urgency badge
                    val urgencyColor = when (item.urgency) {
                        PendingUrgency.CRITICAL_ZERO -> StatusRed
                        PendingUrgency.HIGH_DEMAND_LOW_STOCK -> Color(0xFFE65100)
                        PendingUrgency.BELOW_MIN_THRESHOLD -> AmberGoldDark
                        PendingUrgency.HEALTHY -> StatusGreen
                    }
                    val urgencyBg = when (item.urgency) {
                        PendingUrgency.CRITICAL_ZERO -> StatusRedBg
                        PendingUrgency.HIGH_DEMAND_LOW_STOCK -> Color(0xFFFFF3E0)
                        PendingUrgency.BELOW_MIN_THRESHOLD -> AmberGoldLight
                        PendingUrgency.HEALTHY -> StatusGreenBg
                    }

                    Surface(
                        color = urgencyBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (item.currentStock == 0) "0 स्टॉक (Zero)" else "${item.currentStock} स्टॉक",
                            color = urgencyColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stock details & Rack Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate100, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "वर्तमान स्टॉक", fontSize = 10.sp, color = Slate600)
                    Text(
                        text = "SAA: ${item.stockSaa} | TVS: ${item.stockTvs} (Min: ${item.minStock})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AutoNavyDark
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "स्थान (Rack)", fontSize = 10.sp, color = Slate600)
                    Text(
                        text = item.rackLocation,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AutoNavyDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reorder Recommendation & Supplier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "सुझावित आर्डर मात्रा: ${item.recommendedOrderQty} pcs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen
                    )
                    if (item.unitPurchasePrice > 0.0) {
                        Text(
                            text = "अनुमानित राशि: ~₹${item.estimatedOrderCost.toInt()} (@₹${item.unitPurchasePrice.toInt()}/pc)",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                    if (item.preferredSupplierName.isNotBlank()) {
                        Text(
                            text = "सप्लायर: ${item.preferredSupplierName}",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stock In 1-Tap Button
                Button(
                    onClick = onStockIn,
                    colors = ButtonDefaults.buttonColors(containerColor = AutoNavy),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stock In (परचेस करें)", fontSize = 12.sp)
                }

                // Call supplier if phone exists
                if (item.supplierMobile.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.supplierMobile}"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Shree AI Text", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "टेक्स्ट कॉपी हो गया!", Toast.LENGTH_SHORT).show()
}

private fun shareWhatsAppOrder(context: Context, orderText: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, orderText)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Send Order via WhatsApp / Message")
    context.startActivity(shareIntent)
}
