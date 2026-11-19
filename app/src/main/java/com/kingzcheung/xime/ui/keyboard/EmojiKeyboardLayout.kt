package com.kingzcheung.xime.ui.keyboard

import com.kingzcheung.xime.i18n.localizeData

import com.kingzcheung.xime.R

import com.kingzcheung.xime.i18n.tr

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingzcheung.xime.clipboard.ClipboardManager
import com.kingzcheung.xime.data.EmojiCategory
import com.kingzcheung.xime.data.EmojiData
import com.kingzcheung.xime.data.RecentUsageStore

@Composable
fun EmojiKeyboardLayout(
    onEmojiSelect: (String) -> Unit,
    onImageEmojiSelect: ((String) -> Unit)? = null,
    onBack: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    accentColor: Color,
    bottomPaddingDp: Int = 0,
    modifier: Modifier = Modifier,
    /** 分类 tab 切换的振动钩子（emoji 点击/删除经 onEmojiSelect 由调用方统一振动）。 */
    onHapticFeedback: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboardManager = remember { ClipboardManager.getInstance(context) }

    // 图标按钮容器色：surface 与 primary 的混合色调（带种子色但不过于强烈）
    val iconButtonContainer = androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.primary,
        0.15f
    )

    // 默认落在第一项表情分类（不再落在「最近使用」页，见 recentCategory）
    var selectedSubCategoryIndex by remember {
        mutableStateOf(if (RecentUsageStore.get(context, RecentUsageStore.KEY_RECENT_EMOJIS).isEmpty()) 0 else 1)
    }

    // 内置表情分类（插件表情分类已随插件体系移除）
    val builtinCategories = EmojiData.categories

    // 最近使用（LRU）：惰性排序——面板打开期间点按任何 emoji 只持久化使用记录，
    // 不重排当前 UI（最近使用页位置稳定，便于连续输入）；面板关闭后组合状态丢弃，
    // 下次打开重新读取持久化结果，即为最新顺序。
    val recentEmojis = remember {
        RecentUsageStore.get(context, RecentUsageStore.KEY_RECENT_EMOJIS)
    }
    val recentCategory = EmojiCategory(name = tr(R.string.s_zuijinshiyong), icon = "🕘", emojis = recentEmojis)
    // 「最近使用」为空时不占位：面板一打开就是第一项表情分类
    val displayBuiltinCategories = remember(builtinCategories, recentEmojis) {
        if (recentEmojis.isEmpty()) builtinCategories else listOf(recentCategory) + builtinCategories
    }

    val currentSubCategories = displayBuiltinCategories

    // 所有页面的扁平索引（用于动画过渡）
    val currentPageIndex = selectedSubCategoryIndex
        .coerceIn(0, maxOf(0, displayBuiltinCategories.lastIndex))
    val totalPages = displayBuiltinCategories.size

    // 布局按父容器真实宽度自适应（悬浮卡片/键盘收窄/分屏的容器宽 ≠ 屏幕宽），
    // 不再读屏幕方向：宽容器（横屏全屏）用大边距与更多列，其余按竖屏形态
    BoxWithConstraints(modifier = modifier) {
        val isWide = maxWidth >= WIDE_CONTAINER_WIDTH
        val emojiColumns = gridColumnCount(maxWidth, targetCellWidth = 50.dp, minColumns = 8, maxColumns = 15)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 导航区：返回按钮 + 顶层 Tab（Emoji / 插件）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                // 头部按钮与候选栏 logo 同位（8dp），不再随宽容器加大缩进
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconButtonContainer)
                        .tolerantClick { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = tr(R.string.s_fanhui),
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 顶层 Tab（ClipboardView 样式）
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(iconButtonContainer)
                        .padding(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Emoji 主 Tab
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    accentColor.copy(0.4f)
                                )
                                .tolerantClick {
                                    onHapticFeedback?.invoke()
                                    selectedSubCategoryIndex = 0
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "😊",
                                fontSize = 14.sp
                            )
                        }

                    }
                }
            }
        }

        val pagerState = rememberPagerState(
            initialPage = currentPageIndex,
            pageCount = { totalPages }
        )

        // 外部切换分类时同步到 Pager
        LaunchedEffect(currentPageIndex) {
            pagerState.animateScrollToPage(currentPageIndex)
        }

        // Pager 滑动时同步到外部状态
        LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
            val page = pagerState.currentPage
            if (!pagerState.isScrollInProgress && page != currentPageIndex) {
                selectedSubCategoryIndex = page
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = if (isWide) 50.dp else 4.dp)
                .padding(bottom = 4.dp)
        ) { pageIndex ->
            val category = displayBuiltinCategories[pageIndex]

            if (category.emojis.isEmpty()) {
                // 最近使用为空时的占位提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tr(R.string.s_zanwuzuijinshiyong),
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(category.emojis.chunked(emojiColumns)) { rowEmojis ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            rowEmojis.forEach { emoji ->
                                EmojiButton(
                                    emoji = emoji,
                                    onClick = {
                                        // 惰性排序：任何页（含最近使用页）点按都只持久化使用记录、
                                        // 不重排当前 UI（最近使用页位置稳定，便于连续输入）；
                                        // 面板关闭后下次打开重新读取 store，即为最新顺序。
                                        RecentUsageStore.record(
                                            context, RecentUsageStore.KEY_RECENT_EMOJIS, emoji
                                        )
                                        onEmojiSelect(emoji)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(emojiColumns - rowEmojis.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 底部：子分类 Tab 或留空 + 删除按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = if (isWide) 50.dp else 4.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentSubCategories.isNotEmpty()) {
                // 显示当前顶层 tab 的子分类
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    currentSubCategories.forEachIndexed { index, category ->
                        EmojiCategoryTab(
                            icon = category.icon,
                            isSelected = index == selectedSubCategoryIndex,
                            onClick = {
                                onHapticFeedback?.invoke()
                                selectedSubCategoryIndex = index
                            },
                            backgroundColor = backgroundColor,
                            textColor = textColor,
                            selectedBackgroundColor = accentColor,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            KeyButton(
                text = tr(R.string.s_shanchu),
                onClick = { onEmojiSelect("delete") },
                backgroundColor = backgroundColor,
                textColor = textColor,
                modifier = Modifier.width(48.dp),
                fontSize = 12.sp
            )
        }

        // 底部留空
        Spacer(modifier = Modifier.height(if (isWide) 15.dp else bottomPaddingDp.dp))
    }
    }
}

@Composable
fun EmojiCategoryTab(
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    selectedBackgroundColor: Color = textColor.copy(alpha = 0.15f),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))

            .background(
                if (isSelected) selectedBackgroundColor.copy(0.4f)
                else backgroundColor
            )
            .padding(horizontal = 5.dp)
            .tolerantClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmojiButton(
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .tolerantClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}
