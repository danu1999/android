package com.posbah.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ButtonVariant { Filled, Outline, Tonal }

/**
 * Distinctive pill-shaped primary button with press-scale microinteraction.
 * Intentionally avoids stock M3 Button look to reinforce brand identity.
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Filled
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "btnScale"
    )

    val container: Color = when (variant) {
        ButtonVariant.Filled -> MaterialTheme.colorScheme.primary
        ButtonVariant.Outline -> Color.Transparent
        ButtonVariant.Tonal -> MaterialTheme.colorScheme.secondaryContainer
    }
    val onContainer: Color = when (variant) {
        ButtonVariant.Filled -> MaterialTheme.colorScheme.onPrimary
        ButtonVariant.Outline -> MaterialTheme.colorScheme.onBackground
        ButtonVariant.Tonal -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val shape = RoundedCornerShape(50)
    val borderMod = if (variant == ButtonVariant.Outline) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
    } else Modifier

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(if (enabled) container else container.copy(alpha = 0.4f), shape)
            .then(borderMod)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = onContainer
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = label,
                color = onContainer,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
