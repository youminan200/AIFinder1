package kr.ac.pcu.aifinder

class AiFindRecommender {

    data class RecommendationResult(
        val recommendedArea: RoomArea,
        val matchedItem: ItemRecord?,
        val confidence: Int, // Percentage 0 - 100
        val matchReason: String
    )

    // Semantic category mappings for default areas
    private val areaKeywords = mapOf(
        1 to listOf("침대", "베개", "이불", "잠", "안대", "sleep", "pillow", "blanket", "bed", "협탁", "수면"),
        2 to listOf("노트북", "마우스", "펜", "연필", "책", "공부", "노트", "desk", "computer", "laptop", "mouse", "keyboard", "키보드", "지우개", "필기구", "충전기", "보조 배터리", "배터리", "모니터"),
        3 to listOf("양말", "바지", "티셔츠", "셔츠", "외투", "코트", "패딩", "옷", "모자", "closet", "clothes", "shirt", "pants", "socks", "벨트", "넥타이"),
        4 to listOf("신발", "우산", "열쇠", "차 키", "키", "구두", "운동화", "슬리퍼", "door", "entrance", "key", "shoes", "umbrella", "지갑", "카드", "현관", "도어락"),
        5 to listOf("영양제", "약", "화장품", "향수", "물티슈", "티슈", "휴지", "shelf", "medicine", "cosmetics", "서랍", "장식장"),
        6 to listOf("화분", "식물", "커튼", "창문", "창", "window", "plant", "창가", "베란다")
    )

    fun recommend(query: String, items: List<ItemRecord>, areas: List<RoomArea>): RecommendationResult? {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty() || areas.isEmpty()) return null

        var bestScore = 0
        var bestItem: ItemRecord? = null
        var bestAreaId = -1
        var bestReason = ""

        // 1. Evaluate registered items
        for (item in items) {
            val itemName = item.name.lowercase()
            var score = 0
            var reason = ""

            when {
                // Exact Match
                cleanQuery == itemName -> {
                    score = 100
                    reason = "보관된 물건 '${item.name}' 이름과 100% 일치합니다."
                }
                // Query contains item name
                cleanQuery.contains(itemName) -> {
                    score = 90
                    reason = "보관된 물건 '${item.name}' 이름을 포함하고 있습니다."
                }
                // Item name contains query
                itemName.contains(cleanQuery) -> {
                    score = 85
                    reason = "보관된 물건 '${item.name}' 이름의 일부가 일치합니다."
                }
                else -> {
                    // String Similarity match (typo tolerance)
                    val sim = calculateCharSimilarity(cleanQuery, itemName)
                    if (sim >= 0.5) {
                        score = (40 + (sim * 40)).toInt()
                        reason = "보관된 물건 '${item.name}' 이름과 유사합니다 (유사도 ${(sim * 100).toInt()}%)."
                    }
                }
            }

            // Semantic Category Match via item's area keywords
            val keywords = areaKeywords[item.areaId].orEmpty()
            val matchesAreaKeyword = keywords.any { cleanQuery.contains(it) || it.contains(cleanQuery) }
            if (matchesAreaKeyword && score > 0) {
                score += 10
            }

            // Recency Bonus
            // In KMP, we can use System time via expect/actual or standard system time APIs.
            // However, System.currentTimeMillis() works fine on JVM (Android) but on Kotlin/Native (iOS) we should fetch epoch time.
            // For general logic, a basic mock system time or platform-agnostic clock is ideal.
            // Let's use simple mock or ignore recency bonus, or compute via expect val.
            // Let's keep it simple: we can omit the recency bonus or use it if we define expect val currentTimeMillis.
            // Actually, we can define expect fun getEpochTime(): Long and implement it platform-specifically.
            // Let's define an expect/actual epoch time or use standard Kotlin Native time features.
            // We can define "expect fun getCurrentTimeMillis(): Long" in commonMain, and implement in android/ios.
            val isRecent = (getCurrentTimeMillis() - item.timestamp) < (24 * 60 * 60 * 1000L) // 24 hours
            if (isRecent) score += 5

            // Favorite Bonus
            if (item.isFavorite) score += 5

            // Cap score at 100
            score = score.coerceAtMost(100)

            if (score > bestScore) {
                bestScore = score
                bestItem = item
                bestAreaId = item.areaId
                bestReason = reason
            }
        }

        // 2. Evaluate cold-start semantic category keywords (if no direct item matches or score is low)
        if (bestScore < 60) {
            for (area in areas) {
                val keywords = areaKeywords[area.id].orEmpty()
                val keywordMatch = keywords.firstOrNull { cleanQuery.contains(it) || it.contains(cleanQuery) }
                if (keywordMatch != null) {
                    val score = 70
                    if (score > bestScore) {
                        bestScore = score
                        bestItem = null
                        bestAreaId = area.id
                        bestReason = "아직 등록된 '${query}' 물건은 없으나, 일반적인 보관 구역인 '${area.name}'을 추천합니다."
                    }
                }
            }
        }

        // 3. Fallback: Default to area 4 (현관 구역) or 2 (책상 구역) if no semantic match is found at all
        if (bestAreaId == -1) {
            val defaultArea = areas.firstOrNull { it.id == 4 } ?: areas.first()
            return RecommendationResult(
                recommendedArea = defaultArea,
                matchedItem = null,
                confidence = 25,
                matchReason = "일치하는 물건이나 구역 키워드를 찾지 못해 현관 구역을 기본으로 추천합니다."
            )
        }

        val recommendedArea = areas.firstOrNull { it.id == bestAreaId } ?: areas.first()
        return RecommendationResult(
            recommendedArea = recommendedArea,
            matchedItem = bestItem,
            confidence = bestScore,
            matchReason = bestReason
        )
    }

    private val CHOSEONG = listOf("ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")
    private val JUNGSEONG = listOf("ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ", "요", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ")
    private val JONGSEONG = listOf("", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")

    private fun decomposeHangul(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            val code = ch.code
            if (code in 0xAC00..0xD7A3) {
                val sIndex = code - 0xAC00
                val l = sIndex / (21 * 28)
                val v = (sIndex % (21 * 28)) / 28
                val t = sIndex % 28

                sb.append(CHOSEONG[l])
                sb.append(JUNGSEONG[v])
                if (t > 0) {
                    sb.append(JONGSEONG[t])
                }
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun calculateCharSimilarity(s1: String, s2: String): Double {
        val qDecom = decomposeHangul(s1.replace(" ", ""))
        val words = s2.split(" ")
        
        var maxSim = 0.0

        // 1. Calculate similarity against individual words
        for (word in words) {
            val wDecom = decomposeHangul(word)
            val set1 = qDecom.toSet()
            val set2 = wDecom.toSet()
            val intersect = set1.intersect(set2).size
            val union = set1.union(set2).size
            if (union > 0) {
                val sim = intersect.toDouble() / union.toDouble()
                if (sim > maxSim) maxSim = sim
            }
        }

        // 2. Calculate similarity against the whole word without spaces
        val fullDecom = decomposeHangul(s2.replace(" ", ""))
        val setWhole1 = qDecom.toSet()
        val setWhole2 = fullDecom.toSet()
        val intersectWhole = setWhole1.intersect(setWhole2).size
        val unionWhole = setWhole1.union(setWhole2).size
        if (unionWhole > 0) {
            val wholeSim = intersectWhole.toDouble() / unionWhole.toDouble()
            if (wholeSim > maxSim) maxSim = wholeSim
        }

        return maxSim
    }
}

expect fun getCurrentTimeMillis(): Long
