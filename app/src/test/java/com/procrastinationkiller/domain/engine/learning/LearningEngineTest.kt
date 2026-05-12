package com.procrastinationkiller.domain.engine.learning

import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.data.local.entity.LearningDataEntity
import com.procrastinationkiller.domain.model.TaskPriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LearningEngineTest {

    private lateinit var learningEngine: LearningEngine
    private lateinit var adaptiveWeightManager: AdaptiveWeightManager
    private lateinit var fakeDao: FakeLearningDataDaoForEngine

    @BeforeEach
    fun setup() {
        fakeDao = FakeLearningDataDaoForEngine()
        adaptiveWeightManager = AdaptiveWeightManager(fakeDao)
        learningEngine = LearningEngine(fakeDao, adaptiveWeightManager)
    }

    @Test
    fun `recordFeedback persists event to database`() = runBlocking {
        val event = createEvent(UserFeedbackType.APPROVED)

        learningEngine.recordFeedback(event)

        assertEquals(1, fakeDao.storedData.size)
        assertEquals("APPROVED", fakeDao.storedData[0].feedbackType)
        assertEquals("com.whatsapp", fakeDao.storedData[0].sourceApp)
        assertEquals("sender1", fakeDao.storedData[0].sender)
    }

    @Test
    fun `recordFeedback stores keywords`() = runBlocking {
        val event = createEvent(
            UserFeedbackType.APPROVED,
            keywords = listOf("deploy", "review", "urgent")
        )

        learningEngine.recordFeedback(event)

        assertEquals("deploy,review,urgent", fakeDao.storedData[0].keywords)
    }

    @Test
    fun `recordFeedback stores priority information`() = runBlocking {
        val event = LearningEvent(
            feedbackType = UserFeedbackType.EDITED,
            originalText = "test",
            sourceApp = "com.slack",
            sender = "boss",
            suggestedPriority = TaskPriority.MEDIUM,
            finalPriority = TaskPriority.HIGH,
            keywords = listOf("report"),
            confidence = 0.6f
        )

        learningEngine.recordFeedback(event)

        assertEquals("MEDIUM", fakeDao.storedData[0].prioritySuggested)
        assertEquals("HIGH", fakeDao.storedData[0].priorityFinal)
    }

    @Test
    fun `recordFeedback updates weights`() = runBlocking {
        val event = createEvent(UserFeedbackType.APPROVED, sender = "trusted_sender")

        learningEngine.recordFeedback(event)

        assertTrue(adaptiveWeightManager.getSenderImportance("trusted_sender") > 1.0f)
    }

    @Test
    fun `getAdaptedAnalysis returns boost for known good sender`() = runBlocking {
        // Build up trust for a sender
        repeat(5) {
            learningEngine.recordFeedback(createEvent(UserFeedbackType.APPROVED, sender = "vip"))
        }

        val adjustment = learningEngine.getAdaptedAnalysis("test", "vip", "com.test")

        assertTrue(adjustment.confidenceBoost > 0f)
    }

    @Test
    fun `getAdaptedAnalysis returns negative boost for rejected sender`() = runBlocking {
        repeat(5) {
            learningEngine.recordFeedback(createEvent(UserFeedbackType.REJECTED, sender = "spammer"))
        }

        val adjustment = learningEngine.getAdaptedAnalysis("test", "spammer", "com.test")

        assertTrue(adjustment.confidenceBoost < 0f)
    }

    @Test
    fun `getAdaptedAnalysis returns neutral for unknown sender`() {
        val adjustment = learningEngine.getAdaptedAnalysis("test", "unknown", "com.unknown")

        assertEquals(0f, adjustment.confidenceBoost)
        assertNull(adjustment.priorityAdjustment)
        assertFalse(adjustment.shouldAutoApprove)
    }

    @Test
    fun `shouldAutoApprove is true for highly trusted sender and app`() = runBlocking {
        // Build very high trust
        repeat(50) {
            learningEngine.recordFeedback(
                createEvent(
                    UserFeedbackType.APPROVED,
                    sender = "super_boss",
                    sourceApp = "com.trusted.app"
                )
            )
        }

        val adjustment = learningEngine.getAdaptedAnalysis("test", "super_boss", "com.trusted.app")

        assertTrue(adjustment.shouldAutoApprove)
    }

    @Test
    fun `shouldAutoApprove is false for neutral sender`() {
        val adjustment = learningEngine.getAdaptedAnalysis("test", "new_person", "com.new.app")

        assertFalse(adjustment.shouldAutoApprove)
    }

    @Test
    fun `priority adjustment suggested for very important sender`() = runBlocking {
        // Build very high importance
        repeat(50) {
            learningEngine.recordFeedback(
                createEvent(UserFeedbackType.COMPLETED_QUICKLY, sender = "ceo")
            )
        }

        val adjustment = learningEngine.getAdaptedAnalysis("test", "ceo", "com.test")

        assertNotNull(adjustment.priorityAdjustment)
    }

    @Test
    fun `pruning keeps only max records`() = runBlocking {
        // Insert more than max
        repeat(1005) { i ->
            learningEngine.recordFeedback(
                LearningEvent(
                    feedbackType = UserFeedbackType.APPROVED,
                    originalText = "text $i",
                    sourceApp = "com.test",
                    sender = "sender",
                    suggestedPriority = TaskPriority.MEDIUM,
                    keywords = listOf("keyword$i"),
                    timestamp = System.currentTimeMillis() + i,
                    confidence = 0.7f
                )
            )
        }

        assertTrue(fakeDao.storedData.size <= 1000)
    }

    @Test
    fun `decay reduces old event impact via loadFromDatabase`() = runBlocking {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

        // Add old approval event
        fakeDao.storedData.add(
            LearningDataEntity(
                id = 1,
                featureVector = "old",
                label = "APPROVED",
                confidence = 0.8f,
                timestamp = thirtyDaysAgo,
                feedbackType = "APPROVED",
                sourceApp = "com.old",
                sender = "old_sender",
                keywords = "old_kw",
                prioritySuggested = "MEDIUM",
                priorityFinal = null
            )
        )

        // Add recent approval event
        fakeDao.storedData.add(
            LearningDataEntity(
                id = 2,
                featureVector = "recent",
                label = "APPROVED",
                confidence = 0.8f,
                timestamp = System.currentTimeMillis(),
                feedbackType = "APPROVED",
                sourceApp = "com.recent",
                sender = "recent_sender",
                keywords = "recent_kw",
                prioritySuggested = "MEDIUM",
                priorityFinal = null
            )
        )

        adaptiveWeightManager.loadFromDatabase()

        val oldBoost = adaptiveWeightManager.getKeywordBoost("old_kw")
        val recentBoost = adaptiveWeightManager.getKeywordBoost("recent_kw")

        // Recent events should have greater impact
        assertTrue(recentBoost > oldBoost)
    }

    private fun createEvent(
        feedbackType: UserFeedbackType,
        sender: String = "sender1",
        sourceApp: String = "com.whatsapp",
        keywords: List<String> = listOf("test_keyword")
    ): LearningEvent {
        return LearningEvent(
            feedbackType = feedbackType,
            originalText = "test message",
            sourceApp = sourceApp,
            sender = sender,
            suggestedPriority = TaskPriority.MEDIUM,
            keywords = keywords,
            confidence = 0.7f
        )
    }
}

private class FakeLearningDataDaoForEngine : LearningDataDao {
    val storedData = mutableListOf<LearningDataEntity>()

    override fun getAllLearningData(): Flow<List<LearningDataEntity>> = flowOf(storedData)

    override fun getLearningDataByLabel(label: String): Flow<List<LearningDataEntity>> =
        flowOf(storedData.filter { it.label == label })

    override suspend fun insertLearningData(data: LearningDataEntity): Long {
        storedData.add(data)
        return storedData.size.toLong()
    }

    override suspend fun deleteOldData(before: Long) {
        storedData.removeAll { it.timestamp < before }
    }

    override suspend fun getBySourceApp(app: String): List<LearningDataEntity> =
        storedData.filter { it.sourceApp == app }

    override suspend fun getBySender(sender: String): List<LearningDataEntity> =
        storedData.filter { it.sender == sender }

    override suspend fun getByFeedbackType(type: String): List<LearningDataEntity> =
        storedData.filter { it.feedbackType == type }

    override suspend fun getRecentDataList(limit: Int): List<LearningDataEntity> =
        storedData.sortedByDescending { it.timestamp }.take(limit)

    override suspend fun getCountByLabel(label: String): Int =
        storedData.count { it.label == label }

    override suspend fun deleteOldest(keepCount: Int) {
        if (storedData.size > keepCount) {
            val sorted = storedData.sortedByDescending { it.timestamp }
            storedData.clear()
            storedData.addAll(sorted.take(keepCount))
        }
    }
}
