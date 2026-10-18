package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ReplaySettings
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftGreenBg
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SoftOrangeBg
import com.example.ui.theme.SoftPurple
import com.example.ui.theme.SoftPurpleBg
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle

data class ProPlan(
    val id: Int,
    val title: String,
    val price: String,
    val originalPrice: String,
    val tag: String?,
    val description: String
)

@Composable
fun ProUpgradeDialog(
    settings: ReplaySettings,
    onUpdateSettings: (ReplaySettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPlanIndex by remember { mutableIntStateOf(1) } // Default to 季度会员 (best seller)
    var showRedeemInput by remember { mutableStateOf(false) }
    var redeemCodeText by remember { mutableStateOf("") }

    val plans = listOf(
        ProPlan(
            id = 0,
            title = "连续包月",
            price = "¥12",
            originalPrice = "¥20",
            tag = "首月特惠",
            description = "次月 ¥18 自动续费，随时可退"
        ),
        ProPlan(
            id = 1,
            title = "季度会员",
            price = "¥28",
            originalPrice = "¥58",
            tag = "热销推荐",
            description = "立省 ¥30，折合仅 ¥9.3/月"
        ),
        ProPlan(
            id = 2,
            title = "终身典藏版",
            price = "¥68",
            originalPrice = "¥168",
            tag = "一次买断",
            description = "终身免费享受所有后续 Pro 功能"
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp)),
            color = PureWhite,
            shape = RoundedCornerShape(26.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Banner: Electric Blue Gradient with Close Icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF1E293B),
                                    Color(0xFF0284C7)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF59E0B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = "Pro VIP",
                                    tint = PureWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GAME REPLAY PRO",
                                    color = PureWhite,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (settings.isProMember) "已激活：${settings.proPlanName} (尊享特权)" else "电竞专属商业尊享特权",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "释放芯片极限算力 • 功耗降低 45% • 游戏 0 丢帧",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Body Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Feature List
                    Text(
                        text = "PRO 会员尊享特权",
                        color = TextTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProBenefitRow(
                        icon = Icons.Default.Bolt,
                        iconBg = SoftOrangeBg,
                        iconTint = SoftOrange,
                        title = "硬件级低功耗极速直编",
                        subtitle = "独家 HEVC+VBR 智能调频，功耗直降 45%，发热显著降低"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ProBenefitRow(
                        icon = Icons.Default.Shield,
                        iconBg = SoftGreenBg,
                        iconTint = SoftGreen,
                        title = "对局 0 延迟保帧盾",
                        subtitle = "CPU 性能大核 100% 让位于游戏渲染线程，拒绝任何掉帧微卡"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ProBenefitRow(
                        icon = Icons.Default.Timelapse,
                        iconBg = PrimaryBlueLight,
                        iconTint = PrimaryBlue,
                        title = "180 秒超长对局回录",
                        subtitle = "完整保存整局关键团战与极限翻盘瞬间，精彩绝不错失"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ProBenefitRow(
                        icon = Icons.Default.Hd,
                        iconBg = SoftPurpleBg,
                        iconTint = SoftPurple,
                        title = "2K 120FPS 极清电竞原画",
                        subtitle = "原生点对点高帧率渲染直出，自适应码率提升 200%"
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Plan Selection Cards
                    Text(
                        text = "选择方案",
                        color = TextTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        plans.forEachIndexed { index, plan ->
                            val isSelected = selectedPlanIndex == index
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) PrimaryBlueLight else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isSelected) PrimaryBlue else BorderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPlanIndex = index }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (plan.tag != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) PrimaryBlue else Color(0xFFE2E8F0)
                                        ) {
                                            Text(
                                                text = plan.tag,
                                                color = if (isSelected) PureWhite else TextBody,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    } else {
                                        Spacer(modifier = Modifier.height(17.dp))
                                    }

                                    Text(
                                        text = plan.title,
                                        color = TextTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = plan.price,
                                        color = PrimaryBlue,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = plan.originalPrice,
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        style = androidx.compose.ui.text.TextStyle(
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = plans[selectedPlanIndex].description,
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action Button (Simulated instant checkout or free trial)
                    Button(
                        onClick = {
                            val chosenPlan = plans[selectedPlanIndex]
                            val newSettings = settings.copy(
                                isProMember = true,
                                proPlanName = chosenPlan.title,
                                bufferDurationSeconds = 180, // Unlock max duration
                                fps = 120, // Unlock 120FPS
                                gamePriorityShield = true,
                                batterySaverVbr = true,
                                autoThermalGuard = true
                            )
                            onUpdateSettings(newSettings)
                            Toast.makeText(context, "🎉 恭喜开通 ${chosenPlan.title}！已解锁 2K 120FPS 与低能耗盾", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("subscribe_pro_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = PureWhite
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (settings.isProMember) "续费 / 变更方案" else "立即开通 • 享 7 天免费试用",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Action: Redeem Code / Restore Purchases
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showRedeemInput = !showRedeemInput }
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = "Redeem", tint = TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("激活码兑换", color = TextMuted, fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = {
                                Toast.makeText(context, "购买凭证已同步，会员特权正常生效", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("恢复购买", color = TextMuted, fontSize = 12.sp)
                        }
                    }

                    // Redeem code input box
                    AnimatedVisibility(visible = showRedeemInput) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = redeemCodeText,
                                    onValueChange = { redeemCodeText = it },
                                    placeholder = { Text("输入 VIP666 或兑换码", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = BorderSubtle
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (redeemCodeText.isNotBlank()) {
                                            val newSettings = settings.copy(
                                                isProMember = true,
                                                proPlanName = "终身典藏版",
                                                bufferDurationSeconds = 180,
                                                fps = 120,
                                                gamePriorityShield = true,
                                                batterySaverVbr = true
                                            )
                                            onUpdateSettings(newSettings)
                                            Toast.makeText(context, "✨ 兑换成功！已永久激活 Pro 终身电竞典藏版", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "请输入有效的兑换码", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("兑换")
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
private fun ProBenefitRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
