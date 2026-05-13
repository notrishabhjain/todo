package com.procrastinationkiller.domain.engine.semantic

import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.data.local.entity.LearningDataEntity
import com.procrastinationkiller.domain.engine.learning.AdaptiveWeightManager
import com.procrastinationkiller.domain.engine.learning.LearningEvent
import com.procrastinationkiller.domain.engine.learning.UserFeedbackType
import com.procrastinationkiller.domain.model.ContactPriority
import com.procrastinationkiller.domain.model.TaskPriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ContextualDisambiguatorTest {

    private lateinit var disambiguator: ContextualDisambiguator
    private lateinit var weightManager: AdaptiveWeightManager

    @BeforeEach
    fun setup() {
        weightManager = AdaptiveWeightManager(FakeLearningDataDao())
        // Boost boss sender importance by giving multiple COMPLETED_QUICKLY events
        repeat(15) {
            weightManager.updateFromFeedback(
                LearningEvent(
                    originalText = "task",
                    sender = "boss",
                    sourceApp = "com.whatsapp",
                    feedbackType = UserFeedbackType.COMPLETED_QUICKLY,
                    suggestedPriority = TaskPriority.MEDIUM,
                    keywords = emptyList()
                )
            )
        }
        disambiguator = ContextualDisambiguator(weightManager)
    }

    @Test
    fun `VIP sender gets HIGH priority`() {
        val result = disambiguator.disambiguate(
            text = "get it done",
            sender = "boss",
            contactPriority = ContactPriority.VIP
        )

        assertEquals(TaskPriority.HIGH, result.adjustedPriority)
    }

    @Test
    fun `NORMAL sender gets MEDIUM priority`() {
        val result = disambiguator.disambiguate(
            text = "get it done",
            sender = "colleague",
            contactPriority = ContactPriority.NORMAL
        )

        assertEquals(TaskPriority.MEDIUM, result.adjustedPriority)
    }

    @Test
    fun `IGNORE sender gets LOW priority`() {
        val result = disambiguator.disambiguate(
            text = "get it done",
            sender = "spammer",
            contactPriority = ContactPriority.IGNORE
        )

        assertEquals(TaskPriority.LOW, result.adjustedPriority)
    }

    @Test
    fun `HIGH_PRIORITY sender with high importance gets HIGH`() {
        val result = disambiguator.disambiguate(
            text = "get it done",
            sender = "boss",
            contactPriority = ContactPriority.HIGH_PRIORITY
        )

        // boss has boosted importance from repeated COMPLETED_QUICKLY events
        assertEquals(TaskPriority.HIGH, result.adjustedPriority)
    }

    @Test
    fun `HIGH_PRIORITY sender with normal importance gets MEDIUM`() {
        val result = disambiguator.disambiguate(
            text = "get it done",
            sender = "colleague",
            contactPriority = ContactPriority.HIGH_PRIORITY
        )

        assertEquals(TaskPriority.MEDIUM, result.adjustedPriority)
    }

    @Test
    fun `sender importance is returned in result`() {
        val result = disambiguator.disambiguate(
            text = "get it done",
            sender = "boss",
            contactPriority = ContactPriority.VIP
        )

        // Boss importance should be > 1.0 due to positive feedback
        assertTrue(result.senderContext > 1.0f)
    }

    @Test
    fun `null contact priority defaults to NORMAL`() {
        val result = disambiguator.disambiguate(
            text = "get it done",
            sender = "colleague",
            contactPriority = null
        )

        assertEquals(TaskPriority.MEDIUM, result.adjustedPriority)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.jupiter.api.Assertions.assertTrue(condition)
    }

    private class FakeLearningDataDao : LearningDataDao {
        override fun getAllLearningData(): Flow<List<LearningDataEntity>> = flowOf(emptyList())
        override fun getLearningDataByLabel(label: String): Flow<List<LearningDataEntity>> = flowOf(emptyList())
        override suspend fun insertLearningData(data: LearningDataEntity): Long = 1L
        override suspend fun deleteOldData(before: Long) {}
        override suspend fun getBySourceApp(app: String): List<LearningDataEntity> = emptyList()
        override suspend fun getBySender(sender: String): List<LearningDataEntity> = emptyList()
        override suspend fun getByFeedbackType(type: String): List<LearningDataEntity> = emptyList()
        override suspend fun getRecentDataList(limit: Int): List<LearningDataEntity> = emptyList()
        override suspend fun getCountByLabel(label: String): Int = 0
        override suspend fun deleteOldest(keepCount: Int) {}
    }
}
