package com.example.receipeapp2


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.receipeapp2.databinding.CustomItemimggBinding

class itemSHowAdpt(
    var context: MainActivity,
    var mlist: List<Restaurant>
) : RecyclerView.Adapter<itemSHowAdpt.ViewHolder>() {


    inner class ViewHolder(bindingg: CustomItemimggBinding) :RecyclerView.ViewHolder(bindingg.root){

        var img = bindingg.img
        var txtreceipName = bindingg.txtreceipName

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var binding = CustomItemimggBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return mlist.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var list = mlist.get(position)

        Glide.with(context).load(list.imageUrl).into(holder.img);
        holder.txtreceipName.text = list.name
    }
}