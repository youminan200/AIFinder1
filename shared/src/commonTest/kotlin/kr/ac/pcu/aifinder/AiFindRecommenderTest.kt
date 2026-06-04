package kr.ac.pcu.aifinder

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

class AiFindRecommenderTest {

    private val recommender = AiFindRecommender()

    private val testAreas = listOf(
        RoomArea(1, "침대 구역"),
        RoomArea(2, "책상 구역"),
        RoomArea(3, "옷장 구역"),
        RoomArea(4, "현관 구역"),
        RoomArea(5, "선반 구역"),
        RoomArea(6, "창가 구역")
    )

    private val testItems = listOf(
        ItemRecord(
            id = "1",
            name = "현관 열쇠",
            areaId = 4,
            areaName = "현관 구역",
            timestamp = getCurrentTimeMillis() - 100000,
            photoUri = null,
            boundingBox = null,
            isFavorite = true
        ),
        ItemRecord(
            id = "2",
            name = "보조 배터리",
            areaId = 2,
            areaName = "책상 구역",
            timestamp = getCurrentTimeMillis() - 200000,
            photoUri = "content://path/to/img",
            boundingBox = null,
            isFavorite = false
        ),
        ItemRecord(
            id = "3",
            name = "우산",
            areaId = 4,
            areaName = "현관 구역",
            timestamp = getCurrentTimeMillis() - 300000,
            photoUri = null,
            boundingBox = null,
            isFavorite = false
        )
    )

    @Test
    fun testExactMatch() {
        val result = recommender.recommend("현관 열쇠", testItems, testAreas)
        assertNotNull(result)
        assertEquals(4, result.recommendedArea.id)
        assertEquals("1", result.matchedItem?.id)
        assertEquals(100, result.confidence)
    }

    @Test
    fun testContainsMatch() {
        val result = recommender.recommend("배터리", testItems, testAreas)
        assertNotNull(result)
        assertEquals(2, result.recommendedArea.id)
        assertEquals("2", result.matchedItem?.id)
        assertTrue(result.confidence >= 80)
    }

    @Test
    fun testTypoTolerance() {
        val result = recommender.recommend("열쇄", testItems, testAreas)
        assertNotNull(result)
        assertEquals(4, result.recommendedArea.id)
        assertEquals("1", result.matchedItem?.id)
        assertTrue(result.confidence > 50)
    }

    @Test
    fun testColdStartSemanticMatch() {
        val result = recommender.recommend("이불", emptyList(), testAreas)
        assertNotNull(result)
        assertEquals(1, result.recommendedArea.id)
        assertNull(result.matchedItem)
        assertEquals(70, result.confidence)
    }

    @Test
    fun testFallback() {
        val result = recommender.recommend("xyzabc", emptyList(), testAreas)
        assertNotNull(result)
        assertEquals(4, result.recommendedArea.id)
        assertNull(result.matchedItem)
        assertEquals(25, result.confidence)
    }
}
