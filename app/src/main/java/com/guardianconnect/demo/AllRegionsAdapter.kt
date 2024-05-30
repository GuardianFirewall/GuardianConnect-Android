package com.guardianconnect.demo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.guardianconnect.GRDRegion
import com.guardianconnect.managers.GRDServerManager
import com.guardianconnect.util.Constants.Companion.GRD_AUTOMATIC_REGION

class AllRegionsAdapter(
    private val grdRegions: List<GRDRegion>?,
    private val onClickListener: IOnClickListener?,
    private val context: Context
) : RecyclerView.Adapter<AllRegionsAdapter.RegionHolder>() {

    private var selectedPosition: Int = -1

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegionHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item, parent, false)
        return RegionHolder(view)
    }

    override fun getItemCount(): Int {
        return grdRegions?.size ?: 0
    }

    override fun onBindViewHolder(holder: RegionHolder, position: Int) {
        grdRegions?.let {
            val grdRegion: GRDRegion = it[position]
            holder.bind(grdRegion, onClickListener)
            val savedRegion = GRDServerManager.getPreferredRegion()?.namePretty
            if (savedRegion.isNullOrEmpty()) {
                holder.radioButton.isChecked = position == selectedPosition
            } else {
                holder.radioButton.isChecked = savedRegion == holder.radioButton.text
            }
        }
    }

    inner class RegionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val radioButton: RadioButton

        init {
            radioButton = itemView.findViewById(R.id.radio_button)
        }

        fun bind(grdRegion: GRDRegion, onClickListener: IOnClickListener?) {
            radioButton.text = grdRegion.namePretty
            if (GRDServerManager.getPreferredRegion()?.name == null) {
                selectedPosition = 0
            }
            itemView.setOnClickListener {
                setSelectedPosition(adapterPosition)
                onClickListener?.onClick(
                    grdRegion
                )
                if (grdRegion.namePretty == GRD_AUTOMATIC_REGION) {
                    GRDServerManager.clearPreferredRegion()
                    Toast.makeText(
                        context,
                        "Preferred region cleared!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

interface IOnClickListener {
    fun onClick(grdRegion: GRDRegion?)
}
