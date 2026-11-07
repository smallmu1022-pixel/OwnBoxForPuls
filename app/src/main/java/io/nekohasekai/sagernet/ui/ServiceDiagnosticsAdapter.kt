package io.nekohasekai.sagernet.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.utils.ServiceCategory
import io.nekohasekai.sagernet.utils.ServiceStatusItem
import io.nekohasekai.sagernet.utils.ServiceTestState
import io.nekohasekai.sagernet.utils.Theme

class ServiceDiagnosticsAdapter(
    private val allItems: MutableList<ServiceStatusItem>,
    private val onItemClick: (ServiceStatusItem) -> Unit,
) : RecyclerView.Adapter<ServiceDiagnosticsAdapter.ViewHolder>() {

    private var currentCategory: ServiceCategory = ServiceCategory.ALL
    private var displayedItems: List<ServiceStatusItem> = allItems.toList()

    fun filterByCategory(category: ServiceCategory) {
        currentCategory = category
        displayedItems = if (category == ServiceCategory.ALL) {
            allItems.toList()
        } else {
            allItems.filter { it.category == category }
        }
        notifyDataSetChanged()
    }

    fun updateItem(item: ServiceStatusItem) {
        val index = displayedItems.indexOfFirst { it.id == item.id }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    fun updateAll() {
        displayedItems = if (currentCategory == ServiceCategory.ALL) {
            allItems.toList()
        } else {
            allItems.filter { it.category == currentCategory }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service_diagnostics, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(displayedItems[position])
    }

    override fun getItemCount(): Int = displayedItems.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rootCard = itemView.findViewById<MaterialCardView>(R.id.card_service_root)
        private val icon = itemView.findViewById<ImageView>(R.id.service_icon)
        private val viewStatusDot = itemView.findViewById<View>(R.id.view_status_dot)
        private val tvName = itemView.findViewById<TextView>(R.id.tv_service_name)
        private val tvBadge = itemView.findViewById<TextView>(R.id.tv_service_badge)
        private val tvSub = itemView.findViewById<TextView>(R.id.tv_service_sub)
        private val tvLatency = itemView.findViewById<TextView>(R.id.tv_service_latency)
        private val tvTime = itemView.findViewById<TextView>(R.id.tv_service_time)
        private val btnRefresh = itemView.findViewById<ImageView>(R.id.btn_item_refresh)
        private val pbTesting = itemView.findViewById<ProgressBar>(R.id.pb_item_testing)

        fun bind(item: ServiceStatusItem) {
            val context = itemView.context
            val density = context.resources.displayMetrics.density

            tvName.text = item.name
            icon.setImageResource(item.iconRes)
            tvSub.text = item.subtitle
            tvTime.text = item.testTimestamp
            tvLatency.text = if (item.latencyMs > 0) "${item.latencyMs} ms" else "-"

            val isNight = Theme.usingNightMode(context)
            val isWhite = Theme.isWhiteTheme(context)

            val cardBg = when {
                isNight -> Color.parseColor("#1E1E1E")
                isWhite -> Color.parseColor("#F5F6F8")
                else -> null
            }
            if (cardBg != null) {
                rootCard.setCardBackgroundColor(cardBg)
            }
            val strokeColor = when {
                isNight -> Color.parseColor("#333333")
                isWhite -> Color.parseColor("#E0E0E0")
                else -> null
            }
            if (strokeColor != null) {
                rootCard.strokeColor = strokeColor
            }

            fun createBadge(color: Int): GradientDrawable {
                return GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 4f * density
                    setColor(color)
                }
            }

            fun setDotColor(color: Int) {
                val dot = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke((1.5f * density).toInt(), if (isNight) Color.parseColor("#1E1E1E") else Color.WHITE)
                }
                viewStatusDot.background = dot
            }

            when (item.state) {
                ServiceTestState.TESTING -> {
                    btnRefresh.visibility = View.INVISIBLE
                    pbTesting.visibility = View.VISIBLE
                    tvBadge.text = "检测中"
                    tvBadge.background = createBadge(Color.parseColor("#6B7280"))
                    setDotColor(Color.parseColor("#6B7280"))
                }
                ServiceTestState.AVAILABLE -> {
                    btnRefresh.visibility = View.VISIBLE
                    pbTesting.visibility = View.GONE
                    tvBadge.text = context.getString(R.string.service_status_available)
                    tvBadge.background = createBadge(Color.parseColor("#10B981"))
                    setDotColor(Color.parseColor("#10B981"))
                }
                ServiceTestState.RESTRICTED -> {
                    btnRefresh.visibility = View.VISIBLE
                    pbTesting.visibility = View.GONE
                    tvBadge.text = "地区受限"
                    tvBadge.background = createBadge(Color.parseColor("#F59E0B"))
                    setDotColor(Color.parseColor("#F59E0B"))
                }
                ServiceTestState.UNAVAILABLE -> {
                    btnRefresh.visibility = View.VISIBLE
                    pbTesting.visibility = View.GONE
                    tvBadge.text = context.getString(R.string.service_status_failed)
                    tvBadge.background = createBadge(Color.parseColor("#EF4444"))
                    setDotColor(Color.parseColor("#EF4444"))
                }
                ServiceTestState.IDLE -> {
                    btnRefresh.visibility = View.VISIBLE
                    pbTesting.visibility = View.GONE
                    tvBadge.text = "未测试"
                    tvBadge.background = createBadge(Color.parseColor("#9CA3AF"))
                    setDotColor(Color.parseColor("#9CA3AF"))
                }
            }

            val clickAction = View.OnClickListener {
                if (item.state != ServiceTestState.TESTING) {
                    onItemClick(item)
                }
            }
            btnRefresh.setOnClickListener(clickAction)
            rootCard.setOnClickListener(clickAction)
        }
    }
}
