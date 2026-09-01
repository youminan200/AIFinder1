package kr.ac.pcu.aifinder.utils

import android.graphics.Color

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BadgeColor(val background: Int, val tint: Int)

fun getItemBadgeColor(itemName: String): BadgeColor {
    val name = itemName.lowercase()
    return when {
        name.contains("열쇠") || name.contains("key") -> BadgeColor(Color.parseColor("#FEF3C7"), Color.parseColor("#D97706"))
        name.contains("지갑") || name.contains("wallet") || name.contains("카드") || name.contains("신분증") -> BadgeColor(Color.parseColor("#D1FAE5"), Color.parseColor("#059669"))
        name.contains("폰") || name.contains("휴대폰") || name.contains("phone") || name.contains("패드") || name.contains("가전") -> BadgeColor(Color.parseColor("#E0F2FE"), Color.parseColor("#0284C7"))
        name.contains("책") || name.contains("book") || name.contains("노트") -> BadgeColor(Color.parseColor("#F3E8FF"), Color.parseColor("#7C3AED"))
        name.contains("가방") || name.contains("bag") || name.contains("쇼핑") -> BadgeColor(Color.parseColor("#FFE4E6"), Color.parseColor("#E11D48"))
        else -> BadgeColor(Color.parseColor("#F1F5F9"), Color.parseColor("#475569"))
    }
}

fun getItemIconRes(itemName: String): Int {
    val name = itemName.lowercase()
    return when {
        name.contains("열쇠") || name.contains("key") -> android.R.drawable.ic_lock_lock
        name.contains("지갑") || name.contains("wallet") || name.contains("카드") || name.contains("신분증") -> android.R.drawable.ic_menu_myplaces
        name.contains("폰") || name.contains("휴대폰") || name.contains("phone") -> android.R.drawable.ic_menu_info_details
        name.contains("책") || name.contains("book") || name.contains("노트") -> android.R.drawable.ic_menu_agenda
        name.contains("가방") || name.contains("bag") || name.contains("쇼핑") -> android.R.drawable.ic_menu_view
        else -> android.R.drawable.ic_menu_manage
    }
}

suspend fun loadItemImage(context: Context, uriString: String?, imageView: ImageView) {
    if (uriString.isNullOrEmpty()) {
        imageView.setImageBitmap(null)
        return
    }
    
    withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            withContext(Dispatchers.Main) {
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }
    }
}
