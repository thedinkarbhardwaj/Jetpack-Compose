import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receipeapp2.CategoryResponse
import com.example.receipeapp2.MainActivity
import com.example.receipeapp2.Restaurant
import com.example.receipeapp2.databinding.CustomItemBinding
import com.example.receipeapp2.itemSHowAdpt

class itemadpt(
    private val context: MainActivity,
    private var mlist: MutableList<CategoryResponse>
) : RecyclerView.Adapter<itemadpt.ViewHolder>(), Filterable {

    private var originalList: List<CategoryResponse> = ArrayList(mlist)
    private var filteredList: List<CategoryResponse> = ArrayList(mlist)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CustomItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val list = filteredList[position]
        holder.day.text = list.name

        if (list.restaurant.isNotEmpty()) {
            recItemShow(holder.recNotificationList, list.restaurant)
        }
    }

    inner class ViewHolder(binding: CustomItemBinding) : RecyclerView.ViewHolder(binding.root) {
        var day = binding.txtName
        var recNotificationList = binding.recNotification
    }

    fun recItemShow(rec: RecyclerView, restaurant: List<Restaurant>) {
        val notirec = rec
        val mLayoutManagerGrpClas = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        notirec.layoutManager = mLayoutManagerGrpClas
        notirec.itemAnimator = DefaultItemAnimator()
        val notificationAdpt = itemSHowAdpt(context, restaurant)
        notirec.adapter = notificationAdpt
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint.toString().lowercase()
                filteredList = if (query.isEmpty()) {
                    originalList
                } else {
                    originalList.filter { item ->
                        item.name.lowercase().contains(query)
                    }
                }
                return FilterResults().apply { values = filteredList }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as List<CategoryResponse>
                notifyDataSetChanged()
            }
        }
    }
}
