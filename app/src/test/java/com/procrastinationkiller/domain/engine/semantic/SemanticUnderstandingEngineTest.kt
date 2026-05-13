package com.procrastinationkiller.domain.engine.semantic

import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.data.local.entity.LearningDataEntity
import com.procrastinationkiller.domain.engine.learning.AdaptiveWeightManager
import com.procrastinationkiller.domain.model.ContactPriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SemanticUnderstandingEngineTest {

    private lateinit var engine: SemanticUnderstandingEngine
    private lateinit var weightManager: AdaptiveWeightManager

    @BeforeEach
    fun setup() {
        weightManager = AdaptiveWeightManager(FakeLearningDataDao())

        val negationDetector = NegationDetector()
        val questionClassifier = QuestionVsRequestClassifier()
        val conversationFlowAnalyzer = ConversationFlowAnalyzer()
        val implicitDeadlineResolver = ImplicitDeadlineResolver()
        val contextualDisambiguator = ContextualDisambiguator(weightManager)

        engine = SemanticUnderstandingEngine(
            negationDetector = negationDetector,
            questionClassifier = questionClassifier,
            conversationFlowAnalyzer = conversationFlowAnalyzer,
            implicitDeadlineResolver = implicitDeadlineResolver,
            contextualDisambiguator = contextualDisambiguator
        )
    }

    @Test
    fun `negated message produces non-actionable result`() {
        val result = engine.analyze(
            text = "don't worry about it",
            sender = "colleague",
            sourceApp = "com.whatsapp"
        )

        assertFalse(result.isActionable)
        assertTrue(result.negationDetected)
    }

    @Test
    fun `question produces non-actionable result`() {
        val result = engine.analyze(
            text = "Did you send it?",
            sender = "boss",
            sourceApp = "com.whatsapp"
        )

        assertFalse(result.isActionable)
        assertTrue(result.isQuestion)
    }

    @Test
    fun `valid request produces actionable result`() {
        val result = engine.analyze(
            text = "Please send the report by tomorrow",
            sender = "boss",
            sourceApp = "com.whatsapp",
            contactPriority = ContactPriority.VIP
        )

        assertTrue(result.isActionable)
        assertFalse(result.negationDetected)
        assertFalse(result.isQuestion)
        assertTrue(result.semanticConfidence > 0.5f)
    }

    @Test
    fun `polite request is actionable`() {
        val result = engine.analyze(
            text = "Can you send the report?",
            sender = "boss",
            sourceApp = "com.whatsapp"
        )

        assertTrue(result.isActionable)
        assertFalse(result.isQuestion)
    }

    @Test
    fun `message with implicit deadline resolves timestamp`() {
        val result = engine.analyze(
            text = "Finish this before end of day",
            sender = "boss",
            sourceApp = "com.whatsapp"
        )

        assertTrue(result.isActionable)
        assertNotNull(result.implicitDeadline)
    }

    @Test
    fun `Hindi negation produces non-actionable result`() {
        val result = engine.analyze(
            text = "rehne do, zaroorat nahi",
            sender = "colleague",
            sourceApp = "com.whatsapp"
        )

        assertFalse(result.isActionable)
        assertTrue(result.negationDetected)
    }

    @Test
    fun `status check question is not actionable`() {
        val result = engine.analyze(
            text = "Have you finished the report?",
            sender = "boss",
            sourceApp = "com.whatsapp"
        )

        assertFalse(result.isActionable)
        assertTrue(result.isQuestion)
    }

    @Test
    fun `imperative command is actionable`() {
        val result = engine.analyze(
            text = "Send the report now",
            sender = "boss",
            sourceApp = "com.whatsapp"
        )

        assertTrue(result.isActionable)
        assertFalse(result.negationDetected)
        assertFalse(result.isQuestion)
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
