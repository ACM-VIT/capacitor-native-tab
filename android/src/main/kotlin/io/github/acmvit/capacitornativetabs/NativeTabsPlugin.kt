package io.github.acmvit.capacitornativetabs

import android.content.res.Configuration
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import java.util.Locale
import org.json.JSONException

@CapacitorPlugin(name = "NativeTabs")
class NativeTabsPlugin : Plugin() {
    private var tabBar: ComposeView? = null
    private val tabs = mutableListOf<JSObject>()
    private var tabBarState: MutableState<NativeTabsUiState>? = null
    private var selectedIndex = -1
    private var requestedVisible = true
    private var keyboardVisible = false
    private var themeOverrideDark: Boolean? = null

    @PluginMethod
    fun initialize(call: PluginCall) {
        val tabsArray = call.getArray("tabs")
        if (tabsArray == null || tabsArray.length() == 0) {
            call.reject("tabs are required")
            return
        }

        val parsedTabs = try {
            parseTabs(tabsArray)
        } catch (exception: JSONException) {
            call.reject("invalid tabs payload", exception)
            return
        }

        val nextSelectedIndex = normalizeIndex(call.getInt("selectedIndex", 0), parsedTabs.size)
        val nextThemeOverrideDark = readThemeOverride(call, parsedTabs)
        bridge.executeOnMainThread {
            requestedVisible = true
            applyTabs(parsedTabs, nextSelectedIndex, nextThemeOverrideDark)
            call.resolve()
        }
    }

    @PluginMethod
    fun updateTabs(call: PluginCall) {
        val tabsArray = call.getArray("tabs")
        if (tabsArray == null || tabsArray.length() == 0) {
            call.reject("tabs are required")
            return
        }

        val parsedTabs = try {
            parseTabs(tabsArray)
        } catch (exception: JSONException) {
            call.reject("invalid tabs payload", exception)
            return
        }

        bridge.executeOnMainThread {
            val nextSelectedIndex = normalizeIndex(selectedIndex, parsedTabs.size)
            val nextThemeOverrideDark = readThemeOverride(call, parsedTabs)
            applyTabs(parsedTabs, nextSelectedIndex, nextThemeOverrideDark)
            call.resolve()
        }
    }

    @PluginMethod
    fun setSelectedTab(call: PluginCall) {
        val index = call.getInt("index")
        if (index == null) {
            call.reject("index is required")
            return
        }

        bridge.executeOnMainThread {
            selectTabInternal(index, emitSelection = false, performHaptic = false)
            val result = JSObject()
            result.put("index", selectedIndex)
            call.resolve(result)
        }
    }

    @PluginMethod
    fun deselectAll(call: PluginCall) {
        bridge.executeOnMainThread {
            selectedIndex = -1
            renderTabBar()
            updateWebViewBottomInset()
            val result = JSObject()
            result.put("index", selectedIndex)
            call.resolve(result)
        }
    }

    @PluginMethod
    fun getSelectedTab(call: PluginCall) {
        val result = JSObject()
        result.put("index", selectedIndex)
        call.resolve(result)
    }

    @PluginMethod
    fun hideTabBar(call: PluginCall) {
        bridge.executeOnMainThread {
            requestedVisible = false
            applyTabBarVisibility()
            call.resolve()
        }
    }

    @PluginMethod
    fun showTabBar(call: PluginCall) {
        bridge.executeOnMainThread {
            requestedVisible = true
            applyTabBarVisibility()
            call.resolve()
        }
    }

    override fun handleOnConfigurationChanged(newConfig: Configuration) {
        super.handleOnConfigurationChanged(newConfig)
        bridge.executeOnMainThread {
            if (tabBar == null) return@executeOnMainThread
            renderTabBar()
            updateWebViewBottomInset()
        }
    }

    private fun parseTabs(tabsArray: JSArray): List<JSObject> {
        val parsedTabs = mutableListOf<JSObject>()
        for (index in 0 until tabsArray.length()) {
            parsedTabs.add(JSObject.fromJSONObject(tabsArray.getJSONObject(index)))
        }
        return parsedTabs
    }

    private fun ensureTabBar() {
        if (tabBar != null) return

        val webView = bridge.webView
        val parent = webView.parent as ViewGroup
        val nativeTabBar = ComposeView(context).apply {
            id = View.generateViewId()
            minimumHeight = dp(68)
            isClickable = true
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            isHapticFeedbackEnabled = true
            isSoundEffectsEnabled = true
            elevation = dp(12).toFloat()
            ViewCompat.setElevation(this, dp(12).toFloat())
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }

        nativeTabBar.layoutParams = if (parent is CoordinatorLayout) {
            CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.BOTTOM
            }
        } else {
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.BOTTOM
            }
        }

        val state = mutableStateOf(currentUiState())
        tabBarState = state
        nativeTabBar.setContent {
            NativeTabsNavigationBar(
                state = state.value,
                onTabSelected = { index ->
                    selectTabInternal(index, emitSelection = true, performHaptic = true)
                },
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(nativeTabBar) { view, windowInsets ->
            val systemBarsInsets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            view.setPadding(
                systemBarsInsets.left,
                dp(4),
                systemBarsInsets.right,
                systemBarsInsets.bottom + dp(4),
            )

            val nextKeyboardVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
            if (keyboardVisible != nextKeyboardVisible) {
                keyboardVisible = nextKeyboardVisible
                applyTabBarVisibility()
            } else {
                updateWebViewBottomInset()
            }

            windowInsets
        }

        nativeTabBar.addOnLayoutChangeListener { view, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) {
                updateWebViewBottomInset()
            }
        }

        parent.addView(nativeTabBar)
        tabBar = nativeTabBar
        parent.layoutTransition?.setAnimateParentHierarchy(false)
        ViewCompat.requestApplyInsets(parent)
    }

    private fun applyTabs(
        nextTabs: List<JSObject>,
        nextSelectedIndex: Int,
        nextThemeOverrideDark: Boolean?,
    ) {
        tabs.clear()
        tabs.addAll(nextTabs)
        selectedIndex = normalizeIndex(nextSelectedIndex, nextTabs.size)
        themeOverrideDark = nextThemeOverrideDark

        ensureTabBar()
        renderTabBar()
        applyTabBarVisibility()
    }

    private fun selectTabInternal(
        index: Int,
        emitSelection: Boolean,
        performHaptic: Boolean,
    ) {
        if (tabBar == null || tabs.isEmpty()) {
            selectedIndex = -1
            return
        }

        val normalizedIndex = normalizeIndex(index, tabs.size)
        if (performHaptic && normalizedIndex != selectedIndex) {
            tabBar?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        selectedIndex = normalizedIndex
        renderTabBar()

        if (emitSelection && normalizedIndex >= 0 && normalizedIndex < tabs.size) {
            val payload = JSObject()
            payload.put("index", normalizedIndex)
            payload.put("tab", tabs[normalizedIndex])
            notifyListeners("tabSelected", payload)
        }

        updateWebViewBottomInset()
    }

    private fun renderTabBar() {
        tabBarState?.value = currentUiState()
    }

    private fun currentUiState(): NativeTabsUiState {
        return NativeTabsUiState(
            tabs = tabs.map { tab ->
                NativeTabUiModel(
                    title = tab.getString("title", "Tab") ?: "Tab",
                    iconResId = resolveIcon(tab),
                )
            },
            selectedIndex = selectedIndex,
            dark = themeOverrideDark ?: isDarkTheme(),
        )
    }

    private fun applyTabBarVisibility() {
        val nativeTabBar = tabBar ?: return
        val shouldShow = requestedVisible && tabs.isNotEmpty() && !keyboardVisible
        nativeTabBar.visibility = if (shouldShow) View.VISIBLE else View.GONE
        updateWebViewBottomInset()
    }

    private fun updateWebViewBottomInset() {
        val webView = bridge.webView ?: return
        val existingLayoutParams = webView.layoutParams
        if (existingLayoutParams !is MarginLayoutParams) return

        val nativeTabBar = tabBar
        if (nativeTabBar != null && nativeTabBar.visibility == View.VISIBLE && nativeTabBar.height == 0) {
            nativeTabBar.post { updateWebViewBottomInset() }
            return
        }

        val targetBottomMargin = if (nativeTabBar != null && nativeTabBar.visibility == View.VISIBLE) {
            nativeTabBar.height
        } else {
            0
        }
        if (existingLayoutParams.bottomMargin == targetBottomMargin) return

        existingLayoutParams.bottomMargin = targetBottomMargin
        webView.layoutParams = existingLayoutParams
        webView.requestLayout()
    }

    private fun isDarkTheme(): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    private fun readThemeOverride(call: PluginCall, parsedTabs: List<JSObject>): Boolean? {
        var theme = call.getString("theme")
        if (theme.isNullOrEmpty() && parsedTabs.isNotEmpty()) {
            theme = parsedTabs[0].getString("theme", "")
        }
        return when {
            theme.equals("dark", ignoreCase = true) -> true
            theme.equals("light", ignoreCase = true) -> false
            else -> null
        }
    }

    private fun normalizeIndex(maybeIndex: Int?, size: Int): Int {
        if (size <= 0) return -1
        return if (maybeIndex == null || maybeIndex < 0 || maybeIndex >= size) 0 else maybeIndex
    }

    private fun resolveIcon(tab: JSObject): Int {
        val explicitIcon = resolveAppDrawable(tab.getString("androidIcon", ""))
        if (explicitIcon != 0) return explicitIcon

        return resolveSystemImageDrawable(tab.getString("systemImage", ""))
    }

    private fun resolveSystemImageDrawable(systemImage: String?): Int {
        val normalized = systemImage?.lowercase(Locale.US).orEmpty()

        return when {
            normalized.contains("house") || normalized.contains("home") -> R.drawable.cnt_ic_home
            normalized.contains("magnifyingglass") || normalized.contains("search") -> R.drawable.cnt_ic_search
            normalized.contains("gear") || normalized.contains("settings") -> R.drawable.cnt_ic_settings
            normalized.contains("person") || normalized.contains("profile") || normalized.contains("account") -> R.drawable.cnt_ic_person
            normalized.contains("book") || normalized.contains("bookmark") -> R.drawable.cnt_ic_book
            normalized.contains("note") || normalized.contains("pencil") || normalized.contains("edit") -> R.drawable.cnt_ic_note
            normalized.contains("list") || normalized.contains("clipboard") || normalized.contains("checkmark") -> R.drawable.cnt_ic_list
            normalized.contains("doc") || normalized.contains("text") || normalized.contains("paper") -> R.drawable.cnt_ic_document
            else -> R.drawable.cnt_ic_circle
        }
    }

    private fun resolveAppDrawable(resourceName: String?): Int {
        if (resourceName.isNullOrEmpty()) return 0

        val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        return if (resourceId != 0) resourceId else 0
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics,
        ).toInt()
    }
}

private data class NativeTabsUiState(
    val tabs: List<NativeTabUiModel> = emptyList(),
    val selectedIndex: Int = -1,
    val dark: Boolean = false,
)

private data class NativeTabUiModel(
    val title: String,
    val iconResId: Int,
)

@Composable
private fun NativeTabsNavigationBar(
    state: NativeTabsUiState,
    onTabSelected: (Int) -> Unit,
) {
    val palette = if (state.dark) DarkTabPalette else LightTabPalette
    val colorScheme = if (state.dark) {
        darkColorScheme(
            primary = palette.selected,
            surface = palette.background,
            secondaryContainer = palette.indicator,
            onSecondaryContainer = palette.selected,
            onSurfaceVariant = palette.unselected,
        )
    } else {
        lightColorScheme(
            primary = palette.selected,
            surface = palette.background,
            secondaryContainer = palette.indicator,
            onSecondaryContainer = palette.selected,
            onSurfaceVariant = palette.unselected,
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        NavigationBar(
            containerColor = palette.background,
            contentColor = palette.unselected,
            tonalElevation = 0.dp,
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        ) {
            state.tabs.forEachIndexed { index, tab ->
                val selected = index == state.selectedIndex
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(index) },
                    icon = {
                        androidx.compose.material3.Icon(
                            painter = painterResource(tab.iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = {
                        androidx.compose.material3.Text(
                            text = tab.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = palette.selected,
                        selectedTextColor = palette.selected,
                        indicatorColor = palette.indicator,
                        unselectedIconColor = palette.unselected,
                        unselectedTextColor = palette.unselected,
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = tab.title
                    },
                )
            }
        }
    }
}

private data class TabPalette(
    val background: Color,
    val selected: Color,
    val unselected: Color,
    val indicator: Color,
)

private val LightTabPalette = TabPalette(
    background = Color.White,
    selected = Color(0xFF0B57D0),
    unselected = Color(0xFF5F6368),
    indicator = Color(0xFFD3E3FD),
)

private val DarkTabPalette = TabPalette(
    background = Color(0xFF101418),
    selected = Color(0xFFA8C7FA),
    unselected = Color(0xFFC4C7C5),
    indicator = Color(0xFF1F3A5F),
)
