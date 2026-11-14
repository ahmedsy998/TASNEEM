package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.InterfaceBackgroundStyle
import com.example.ui.theme.LocalTasneemColors
import com.example.ui.theme.TasneemDeepCharcoal
import com.example.ui.theme.TasneemOrange
import com.example.ui.theme.TasneemSoftIvory
import kotlin.math.cos
import kotlin.math.sin

/**
 * Top Application Bar following Section 04:
 * - Tasneem logo & branding
 * - Application title ("تسنيم")
 * - Single elegant Appearance control ("المظهر")
 * - No hamburger menu, no top back button.
 */
@Composable
fun TasneemTopBar(
    onAppearanceClick: () -> Unit,
    onLogoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current

    Surface(
        color = colors.elevatedSurface.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right side (in RTL): Tasneem Logo + Application Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onLogoClick)
                    .padding(end = 8.dp, top = 2.dp, bottom = 2.dp)
                    .testTag("tasneem_brand_header")
            ) {
                TasneemGeometricLogo(sizeDp = 42)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "تَسْنِيم",
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = colors.primaryAccent,
                        lineHeight = 26.sp
                    )
                    Text(
                        text = "استوديو الفيديو القرآني",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                }
            }

            // Left side (in RTL): Single elegant Appearance button ("المظهر")
            Surface(
                shape = RoundedCornerShape(50),
                color = colors.cardBackground,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = colors.primaryAccent.copy(alpha = 0.55f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAppearanceClick)
                    .testTag("appearance_top_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "المظهر",
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "المظهر",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}

/**
 * Minimal, recognizable Tasneem logo inspired by refined Islamic 8-pointed Rub el Hizb motif
 * and an open Mushaf with a subtle play emblem.
 */
@Composable
fun TasneemGeometricLogo(
    sizeDp: Int = 44,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    val activeAccent = colors.primaryAccent

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TasneemDeepCharcoal)
            .border(1.dp, activeAccent.copy(alpha = 0.65f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.minDimension * 0.38f
            val innerR = outerR * 0.72f

            val starPath = Path()
            val points = 16
            for (i in 0 until points) {
                val angle = Math.toRadians((i * 360.0 / points) - 90.0)
                val r = if (i % 2 == 0) outerR else innerR
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
            }
            starPath.close()

            drawPath(
                path = starPath,
                color = activeAccent,
                style = Stroke(width = size.minDimension * 0.055f)
            )

            // Open Quranic pages motif inside
            val bookPath = Path().apply {
                moveTo(cx - outerR * 0.45f, cy - outerR * 0.15f)
                lineTo(cx - outerR * 0.05f, cy + outerR * 0.04f)
                lineTo(cx - outerR * 0.05f, cy + outerR * 0.42f)
                lineTo(cx - outerR * 0.45f, cy + outerR * 0.25f)
                close()

                moveTo(cx + outerR * 0.45f, cy - outerR * 0.15f)
                lineTo(cx + outerR * 0.05f, cy + outerR * 0.04f)
                lineTo(cx + outerR * 0.05f, cy + outerR * 0.42f)
                lineTo(cx + outerR * 0.45f, cy + outerR * 0.25f)
                close()
            }
            drawPath(path = bookPath, color = TasneemSoftIvory)

            // Small glowing droplet/play crown above the pages representing Tasneem spring
            drawCircle(
                color = activeAccent,
                radius = size.minDimension * 0.07f,
                center = Offset(cx, cy - outerR * 0.26f)
            )
        }
    }
}

/**
 * Optional Islamic geometric pattern background layer when InterfaceBackgroundStyle.ISLAMIC_PATTERNS is selected.
 */
@Composable
fun TasneemBackgroundContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LocalTasneemColors.current
    val activeAccent = colors.primaryAccent

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.screenBackground)
    ) {
        if (colors.backgroundStyle.hasGeometricPattern) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 96.dp.toPx()
                val cols = (size.width / step).toInt() + 2
                val rows = (size.height / step).toInt() + 2
                val strokeColor = activeAccent.copy(alpha = 0.055f)

                for (r in 0..rows) {
                    for (c in 0..cols) {
                        val cx = c * step
                        val cy = r * step
                        drawCircle(
                            color = strokeColor,
                            radius = step * 0.36f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.2f)
                        )
                        drawCircle(
                            color = strokeColor,
                            radius = step * 0.18f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }
        }
        content()
    }
}

/**
 * Appearance and Theme System Bottom Sheet (Section 14 & Harmonious Color Themes Update).
 * Provides Light Mode, Dark Mode, System Default Mode, 6 Coordinated Color Themes (Default Orange/Silver
 * + Turquoise, Warm Yellow, Deep Teal, Warm Gold, Ocean Blue), and 7 Islamic-inspired interface styles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceBottomSheet(
    currentThemeMode: AppThemeMode,
    currentBackgroundStyle: InterfaceBackgroundStyle,
    currentColorTheme: AppColorTheme = AppColorTheme.TASNEEM_ORANGE_SILVER,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectBackgroundStyle: (InterfaceBackgroundStyle) -> Unit,
    onSelectColorTheme: (AppColorTheme) -> Unit = {},
    onDismiss: () -> Unit
) {
    val colors = LocalTasneemColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.elevatedSurface,
        contentColor = colors.textPrimary
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = colors.primaryAccent
                    )
                    Column {
                        Text(
                            text = "إعدادات المظهر والثيمات اللونية",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "اختر ثيم الألوان المتناسق أو نمط إضاءة الواجهة مع معاينة فورية",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            item {
                Text(
                    text = "ثيمات الألوان المتناسقة",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryAccent
                )
                Text(
                    text = "الثيم البرتقالي والفضي الداكن هو الثيم الافتراضي، مع ٥ ثيمات إضافية متناسقة ومريحة للعين",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            items(AppColorTheme.entries) { theme ->
                val isSelected = theme == currentColorTheme
                ColorThemePreviewCard(
                    theme = theme,
                    isSelected = isSelected,
                    onClick = { onSelectColorTheme(theme) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "وضع الإضاءة",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = mode == currentThemeMode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colors.primaryAccent else colors.cardBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) colors.primaryAccent else colors.borderSubtle
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectThemeMode(mode) }
                                .testTag("theme_mode_${mode.id}")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.titleAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) colors.onPrimaryAccent else colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "أنماط خلفية الواجهة الإسلامية",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryAccent
                )
                Text(
                    text = "تطبق هذه الأنماط على خلفية الواجهة والبطاقات بتناغم مع الثيم المختار",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            items(InterfaceBackgroundStyle.entries) { style ->
                val isSelected = style == currentBackgroundStyle
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardBackground,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) colors.primaryAccent else colors.borderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onSelectBackgroundStyle(style)
                            if (style.isDarkSurface && currentThemeMode == AppThemeMode.LIGHT) {
                                onSelectThemeMode(AppThemeMode.DARK)
                            } else if (!style.isDarkSurface && currentThemeMode == AppThemeMode.DARK) {
                                onSelectThemeMode(AppThemeMode.LIGHT)
                            }
                        }
                        .testTag("bg_style_${style.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Swatch preview
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(style.primaryBackground)
                                    .border(2.dp, colors.primaryAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryAccent)
                                )
                            }
                            Column {
                                Text(
                                    text = style.titleAr,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = style.subtitleAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "مفعل",
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visual Theme Preview Card showing coordinated swatches (dark charcoal surface, elevated card surface,
 * primary accent, secondary accent, and soft neutral text) before selection.
 */
@Composable
private fun ColorThemePreviewCard(
    theme: AppColorTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalTasneemColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) theme.primaryAccent else colors.borderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("color_theme_${theme.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Mini UI Theme Visual Preview Swatch
                Box(
                    modifier = Modifier
                        .size(width = 62.dp, height = 46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.darkBackground)
                        .border(1.5.dp, theme.primaryAccent, RoundedCornerShape(10.dp))
                        .padding(5.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top bar preview strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 22.dp, height = 7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(theme.primaryAccent)
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(theme.secondaryAccent)
                            )
                        }
                        // Card & text contrast preview strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(18.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(theme.darkCardSurface)
                                .padding(horizontal = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 24.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(theme.darkTextPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(theme.primaryAccent)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.titleAr,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) theme.primaryAccent else colors.textPrimary
                    )
                    Text(
                        text = theme.subtitleAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "الثيم المفعل",
                    tint = theme.primaryAccent,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(24.dp)
                )
            }
        }
    }
}
