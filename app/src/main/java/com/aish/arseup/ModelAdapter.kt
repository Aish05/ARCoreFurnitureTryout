package com.aish.arseup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_models.view.*

class ModelAdapter(val models: List<Model>) : RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {

    val selectedFurniture = MutableLiveData<Model>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_models, parent, false)
        return ModelViewHolder(view)
    }

    override fun getItemCount(): Int {
        return models.size
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        holder.itemView.apply {
            thumbnail.setImageResource(models[position].imgResourceId)
            title.text = models[position].title

            setOnClickListener { selectFurniture(holder) }
        }
    }

    inner class ModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private fun selectFurniture(holder: ModelViewHolder) {

        selectedFurniture.value = models[holder.adapterPosition]

    }
}


