package com.example.nirapod.ui.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nirapod.R
import com.example.nirapod.data.model.HazardReport
import com.example.nirapod.databinding.ItemReportBinding
import java.text.DateFormat
import java.util.Date

class ReportAdapter(
    private val onClick: (HazardReport) -> Unit
) : ListAdapter<HazardReport, ReportAdapter.ReportViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ReportViewHolder(private val binding: ItemReportBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(report: HazardReport) {
            binding.tvCategory.text = "${report.category} • ${report.severity}"
            binding.tvDescription.text = report.description
            binding.tvStatus.text = report.status.replace('_', ' ')
            binding.tvMeta.text = "${report.confirmations} confirmations • ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(report.createdAt))}"
            if (report.imageUrl.isNotBlank()) {
                Glide.with(binding.ivReport)
                    .load(report.imageUrl)
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .into(binding.ivReport)
            } else {
                binding.ivReport.setImageResource(R.drawable.ic_app_logo)
            }
            binding.root.setOnClickListener { onClick(report) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<HazardReport>() {
        override fun areItemsTheSame(oldItem: HazardReport, newItem: HazardReport): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HazardReport, newItem: HazardReport): Boolean = oldItem == newItem
    }
}
