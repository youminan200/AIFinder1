package kr.ac.pcu.aifinder

fun formatTimestampForDetail(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분", java.util.Locale.KOREA)
    return format.format(date)
}
