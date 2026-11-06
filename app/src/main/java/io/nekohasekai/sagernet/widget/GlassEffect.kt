package io.nekohasekai.sagernet.widget

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager

/**
 * OneBox 液态玻璃的系统模糊能力。
 *
 * 真模糊只在 API 31+ 可用，全部调用都有版本守卫与 try/catch，低版本与
 * 不支持的 ROM 上静默跳过，玻璃质感退化为半透明填充 + 描边（见 bg_glass_*）。
 *
 * 两条路线：
 * - [applyWindowBlur]：窗口后方模糊（对话框 / 底部弹窗 / 对话框式 Activity），
 *   模糊的是窗口背后的应用内容；与 values-v31 主题里的
 *   android:windowBackgroundBlurRadius 互补（主题覆盖 AlertDialog 与 BottomSheet，
 *   代码覆盖 ThemedActivity 的对话框式 Activity 与 NodeSelectDialogActivity）。
 * - [applyBackdropBlur]：把 RenderEffect 挂在内容视图自身上，用于抽屉打开时
 *   模糊后方主界面；注意这会模糊该视图自身的渲染内容，不要挂在玻璃面板自己身上。
 */
object GlassEffect {

    /** 窗口后方模糊半径（dp），与 @dimen/glass_blur_radius 保持一致。 */
    const val WINDOW_BLUR_RADIUS_DP = 24f

    /** 抽屉打开时后方内容的模糊半径（dp），比重度窗口模糊更克制。 */
    const val BACKDROP_BLUR_RADIUS_DP = 16f

    /** 对窗口启用后方模糊；低版本或不支持时静默跳过。 */
    fun applyWindowBlur(window: Window?, radiusDp: Float = WINDOW_BLUR_RADIUS_DP) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || window == null) return
        try {
            val radiusPx = (radiusDp * window.context.resources.displayMetrics.density)
                .toInt().coerceIn(1, 100)
            window.setBackgroundBlurRadius(radiusPx)
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val attrs = window.attributes
            attrs.blurBehindRadius = radiusPx
            window.attributes = attrs
        } catch (_: Throwable) {
            // 部分 ROM 不支持窗口模糊，忽略即可，玻璃质感由半透明填充兜底。
        }
    }

    /** 清除窗口后方模糊。 */
    fun clearWindowBlur(window: Window?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || window == null) return
        try {
            window.setBackgroundBlurRadius(0)
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val attrs = window.attributes
            attrs.blurBehindRadius = 0
            window.attributes = attrs
        } catch (_: Throwable) {
        }
    }

    /** 用 RenderEffect 模糊 [view] 自身的渲染（抽屉打开时模糊后方主界面）。 */
    fun applyBackdropBlur(view: View, radiusDp: Float = BACKDROP_BLUR_RADIUS_DP) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            val radiusPx = (radiusDp * view.resources.displayMetrics.density)
                .coerceIn(1f, 100f)
            view.setRenderEffect(
                RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
            )
        } catch (_: Throwable) {
        }
    }

    /** 清除 [view] 上的 RenderEffect 模糊。 */
    fun clearBackdropBlur(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            view.setRenderEffect(null)
        } catch (_: Throwable) {
        }
    }
}
