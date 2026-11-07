package moe.matsuri.nb4a.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ktx.getColorAttr
import io.nekohasekai.sagernet.utils.Theme
import com.google.android.material.card.MaterialCardView

class ExpandablePreferenceCategory @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceCategoryStyle,
    defStyleRes: Int = 0,
) : PreferenceCategory(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val expandedStateMap = HashMap<String, Boolean>()

        fun isCategoryExpanded(key: String?): Boolean {
            if (key == null) return false
            return expandedStateMap[key] ?: false
        }

        fun setCategoryExpanded(key: String?, expanded: Boolean) {
            if (key == null) return
            expandedStateMap[key] = expanded
        }
    }

    private val childVisibilityRules = mutableMapOf<String, () -> Boolean>()

    var isExpanded: Boolean = false
        private set

    init {
        isSelectable = true
        layoutResource = R.layout.layout_expandable_category
    }

    override fun onAttached() {
        super.onAttached()
        if (key != null && expandedStateMap.containsKey(key)) {
            isExpanded = expandedStateMap[key] == true
            applyChildrenVisibility()
        }
    }

    override fun setKey(key: String?) {
        super.setKey(key)
        if (key != null && expandedStateMap.containsKey(key)) {
            isExpanded = expandedStateMap[key] == true
            applyChildrenVisibility()
        }
    }

    override fun isSelectable(): Boolean = true
    override fun isEnabled(): Boolean = true

    override fun addPreference(preference: Preference): Boolean {
        val result = super.addPreference(preference)
        val currentExpanded = if (key != null && expandedStateMap.containsKey(key)) {
            expandedStateMap[key] == true
        } else {
            isExpanded
        }
        if (!currentExpanded) {
            preference.isVisible = false
        }
        return result
    }

    fun setExpanded(expanded: Boolean) {
        if (isExpanded == expanded) return
        isExpanded = expanded
        if (key != null) {
            expandedStateMap[key] = expanded
        }
        applyChildrenVisibility()
        notifyChanged()
    }

    fun toggle() {
        setExpanded(!isExpanded)
    }

    fun setChildVisibilityRule(key: String, rule: () -> Boolean) {
        childVisibilityRules[key] = rule
        if (isExpanded) {
            findPreference<Preference>(key)?.isVisible = rule()
        }
    }

    fun updateChildVisibility(key: String) {
        if (isExpanded) {
            val rule = childVisibilityRules[key]
            findPreference<Preference>(key)?.isVisible = rule?.invoke() ?: true
        }
    }

    fun applyChildrenVisibility() {
        for (i in 0 until preferenceCount) {
            val child = getPreference(i)
            val shouldShow = if (isExpanded) {
                childVisibilityRules[child.key]?.invoke() ?: true
            } else {
                false
            }
            child.isVisible = shouldShow
        }
    }

    override fun onClick() {
        super.onClick()
        toggle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val primaryColor = Theme.getPrimaryColor(context)

        val card = holder.itemView as? MaterialCardView
        if (card != null) {
            // 整体简约线条风：分类去卡片化——透明底、无阴影、无描边、无圆角
            card.setCardBackgroundColor(Color.TRANSPARENT)
            card.cardElevation = 0f
            card.strokeWidth = 0
            card.radius = 0f
        }

        val textPrimary = context.getColorAttr(android.R.attr.textColorPrimary)
        val textSecondary = context.getColorAttr(android.R.attr.textColorSecondary)

        val titleView = holder.findViewById(android.R.id.title) as? TextView
        titleView?.setTextColor(if (isExpanded) primaryColor else textPrimary)

        val arrow = holder.findViewById(R.id.category_arrow) as? ImageView
        if (arrow != null) {
            val arrowColor = if (isExpanded) primaryColor else textSecondary
            arrow.imageTintList = ColorStateList.valueOf(arrowColor)
            arrow.setImageResource(
                if (isExpanded) R.drawable.ic_outline_expand_less_24
                else R.drawable.ic_outline_expand_more_24
            )
        }

        holder.itemView.isClickable = true
        holder.itemView.isFocusable = true
        holder.itemView.setOnClickListener {
            toggle()
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val myState = SavedState(superState)
        myState.isExpanded = isExpanded
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) {
            super.onRestoreInstanceState(state)
            return
        }
        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)
        setExpanded(myState.isExpanded)
    }

    private class SavedState : BaseSavedState {
        var isExpanded: Boolean = false

        constructor(source: Parcel) : super(source) {
            isExpanded = source.readInt() == 1
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(if (isExpanded) 1 else 0)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}

