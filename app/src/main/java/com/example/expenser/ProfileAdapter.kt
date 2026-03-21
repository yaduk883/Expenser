package com.example.expenser

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ProfileAdapter(
    private var profiles: List<Profile>,
    private val onProfileClick: (Profile) -> Unit,
    private val onProfileLongClick: (Profile) -> Unit // ഡിലീറ്റ് ചെയ്യാനുള്ള ലോങ്ങ് പ്രസ്സ് ഫീച്ചർ
) : RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    private var selectedPosition = 0

    inner class ProfileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardProfile: MaterialCardView = view.findViewById(R.id.cardProfile)
        val ivProfileIcon: ImageView = view.findViewById(R.id.ivProfileIcon)
        val tvProfileName: TextView = view.findViewById(R.id.tvProfileName)
        val tvProfileType: TextView = view.findViewById(R.id.tvProfileType)

        fun bind(profile: Profile, position: Int) {
            tvProfileName.text = profile.name
            tvProfileType.text = profile.type.name

            // സെലക്ട് ചെയ്ത പ്രൊഫൈലിന് ബോർഡർ നൽകുന്നു
            if (selectedPosition == position) {
                cardProfile.strokeColor = Color.parseColor("#6200EE")
                cardProfile.strokeWidth = 4
            } else {
                cardProfile.strokeColor = Color.parseColor("#E0E0E0")
                cardProfile.strokeWidth = 1
            }

            // ഐക്കൺ മാറ്റം (Type അനുസരിച്ച്)
            when (profile.type) {
                ProfileType.USER -> ivProfileIcon.setImageResource(android.R.drawable.ic_menu_myplaces)
                ProfileType.FAMILY -> ivProfileIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                ProfileType.PET -> ivProfileIcon.setImageResource(android.R.drawable.ic_menu_view)
            }

            // സിംഗിൾ ക്ലിക്ക് (Select ചെയ്യാൻ)
            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onProfileClick(profile)
            }

            // ലോങ്ങ് പ്രസ്സ് (ഡിലീറ്റ് ഡയലോഗ് വരാൻ)
            itemView.setOnLongClickListener {
                onProfileLongClick(profile)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile, parent, false)
        return ProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(profiles[position], position)
    }

    override fun getItemCount() = profiles.size

    fun updateProfiles(newProfiles: List<Profile>) {
        this.profiles = newProfiles
        notifyDataSetChanged()
    }
}