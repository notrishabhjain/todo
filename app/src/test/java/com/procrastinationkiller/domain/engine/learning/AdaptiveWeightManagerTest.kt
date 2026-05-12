package com.procrastinationkiller.domain.engine.learning

import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.data.local.entity.LearningDataEntity
import com.procrastinationkiller.domain.model.TaskPriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AdaptiveWeightManagerTest {

    private lateinit var weightManager: AdaptiveWeightManager
    private lateinit var fakeDao: FakeLearningDataDao

    @BeforeEach
    fun setup() {
        fakeDao = FakeLearningDataDao()
        weightManager = AdaptiveWeightManager(fakeDao)
    }

    @Test
    fun `initial keyword weight is neutral`() {
        val boost = weightManager.getKeywordBoost("unknown_keyword")
        assertEquals(1.0f, boost)
    }

    @Test
    fun `initial sender importance is neutral`() {
        val importance = weightManager.getSenderImportance("new_sender")
        assertEquals(1.0f, importance)
    }

    @Test
    fun `initial app reliability is neutral`() {
        val reliability = weightManager.getAppReliability("com.new.app")
        assertEquals(1.0f, reliability)
    }

    @Test
    fun `initial confidence threshold is default`() {
        val threshold = weightManager.getConfidenceThreshold()
        assertEquals(0.5f, threshold)
    }

    @Test
    fun `approval increases keyword weight`() {
        val event = createEvent(UserFeedbackType.APPROVED, keywords = listOf("report", "send"))

        weightManager.updateFromFeedback(event)

        assertTrue(weightManager.getKeywordBoost("report") > 1.0f)
        assertTrue(weightManager.getKeywordBoost("send") > 1.0f)
    }

    @Test
    fun `rejection decreases keyword weight`() {
        val event = createEvent(UserFeedbackType.REJECTED, keywords = listOf("spam", "promo"))

        weightManager.updateFromFeedback(event)

        assertTrue(weightManager.getKeywordBoost("spam") < 1.0f)
        assertTrue(weightManager.getKeywordBoost("promo") < 1.0f)
    }

    @Test
    fun `multiple approvals increase keyword weight further`() {
        val event = createEvent(UserFeedbackType.APPROVED, keywords = listOf("deploy"))

        weightManager.updateFromFeedback(event)
        val afterOne = weightManager.getKeywordBoost("deploy")

        weightManager.updateFromFeedback(event)
        val afterTwo = weightManager.getKeywordBoost("deploy")

        assertTrue(afterTwo > afterOne)
        assertTrue(afterTwo > 1.0f)
    }

    @Test
    fun `sender importance grows with consistent approvals`() {
        val event = createEvent(UserFeedbackType.APPROVED, sender = "boss")

        weightManager.updateFromFeedback(event)
        val afterOne = weightManager.getSenderImportance("boss")

        weightManager.updateFromFeedback(event)
        val afterTwo = weightManager.getSenderImportance("boss")

        assertTrue(afterOne > 1.0f)
        assertTrue(afterTwo > afterOne)
    }

    @Test
    fun `sender importance decreases with rejections`() {
        val event = createEvent(UserFeedbackType.REJECTED, sender = "spammer")

        weightManager.updateFromFeedback(event)

        assertTrue(weightManager.getSenderImportance("spammer") < 1.0f)
    }

    @Test
    fun `app reliability increases with approvals`() {
        val event = createEvent(UserFeedbackType.APPROVED, sourceApp = "com.slack")

        weightManager.updateFromFeedback(event)

        assertTrue(weightManager.getAppReliability("com.slack") > 1.0f)
    }

    @Test
    fun `confidence threshold raises after many rejections`() {
        val initialThreshold = weightManager.getConfidenceThreshold()

        repeat(5) {
            weightManager.updateFromFeedback(createEvent(UserFeedbackType.REJECTED))
        }

        assertTrue(weightManager.getConfidenceThreshold() > initialThreshold)
    }

    @Test
    fun `confidence threshold lowers after approvals`() {
        // First raise it with rejections
        repeat(5) {
            weightManager.updateFromFeedback(createEvent(UserFeedbackType.REJECTED))
        }
        val afterRejections = weightManager.getConfidenceThreshold()

        // Now lower it with approvals
        repeat(5) {
            weightManager.updateFromFeedback(createEvent(UserFeedbackType.APPROVED))
        }

        assertTrue(weightManager.getConfidenceThreshold() < afterRejections)
    }

    @Test
    fun `keyword weights are case insensitive`() {
        val event = createEvent(UserFeedbackType.APPROVED, keywords = listOf("Report"))

        weightManager.updateFromFeedback(event)

        assertTrue(weightManager.getKeywordBoost("report") > 1.0f)
        assertTrue(weightManager.getKeywordBoost("REPORT") > 1.0f)
    }

    @Test
    fun `weight never exceeds maximum`() {
        val event = createEvent(UserFeedbackType.COMPLETED_QUICKLY, keywords = listOf("urgent"))

        repeat(100) {
            weightManager.updateFromFeedback(event)
        }

        assertTrue(weightManager.getKeywordBoost("urgent") <= 3.0f)
    }

    @Test
    fun `weight never goes below minimum`() {
        val event = createEvent(UserFeedbackType.REJECTED, keywords = listOf("junk"))

        repeat(100) {
            weightManager.updateFromFeedback(event)
        }

        assertTrue(weightManager.getKeywordBoost("junk") >= 0.1f)
    }

    @Test
    fun `loadFromDatabase rebuilds weights from stored data`() = runBlocking {
        fakeDao.storedData.add(
            LearningDataEntity(
                id = 1,
                featureVector = "test",
                label = "APPROVED",
                confidence = 0.8f,
                timestamp = System.currentTimeMillis(),
                feedbackType = "APPROVED",
                sourceApp = "com.whatsapp",
                sender = "manager",
                keywords = "deploy,review",
                prioritySuggested = "HIGH",
                priorityFinal = null
            )
        )

        weightManager.loadFromDatabase()

        assertTrue(weightManager.getKeywordBoost("deploy") > 1.0f)
        assertTrue(weightManager.getSenderImportance("manager") > 1.0f)
        assertTrue(weightManager.getAppReliability("com.whatsapp") > 1.0f)
    }

    @Test
    fun `loadFromDatabase applies decay to old events`() = runBlocking {
        val thirtyOneDaysAgo = System.currentTimeMillis() - (31L * 24 * 60 * 60 * 1000)

        fakeDao.storedData.add(
            LearningDataEntity(
                id = 1,
                featureVector = "old",
                label = "APPROVED",
                confidence = 0.8f,
                timestamp = thirtyOneDaysAgo,
                feedbackType = "APPROVED",
                sourceApp = "com.old.app",
                sender = "old_sender",
                keywords = "old_keyword",
                prioritySuggested = "HIGH",
                priorityFinal = null
            )
        )
        fakeDao.storedData.add(
            LearningDataEntity(
                id = 2,
                featureVector = "recent",
                label = "APPROVED",
                confidence = 0.8f,
                timestamp = System.currentTimeMillis(),
                feedbackType = "APPROVED",
                sourceApp = "com.new.app",
                sender = "new_sender",
                keywords = "new_keyword",
                prioritySuggested = "HIGH",
                priorityFinal = null
            )
        )

        weightManager.loadFromDatabase()

        // Recent event should have more impact than old one
        val oldKeywordBoost = weightManager.getKeywordBoost("old_keyword")
        val newKeywordBoost = weightManager.getKeywordBoost("new_keyword")
        assertTrue(newKeywordBoost > oldKeywordBoost)
    }

    private fun createEvent(
        feedbackType: UserFeedbackType,
        keywords: List<String> = listOf("test"),
        sender: String = "test_sender",
        sourceApp: String = "com.test.app"
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

private class FakeLearningDataDao : LearningDataDao {
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
