package com.procrastinationkiller.domain.engine.ml

import android.content.Context
import com.procrastinationkiller.domain.model.TaskPriority
import javax.inject.Singleton

/**
 * ONNX Runtime-based intent classifier.
 * Attempts to load and run a quantized ONNX model for intent classification inference.
 *
 * Expected model format:
 * - Input: Float tensor of shape [1, FEATURE_COUNT] where FEATURE_COUNT = TextFeatureExtractor.FEATURE_COUNT
 * - Output: Float tensor of shape [1, 6] representing logits for 6 intent classes:
 *   [TASK_REQUEST, INFORMATION, SOCIAL, QUESTION, REMINDER, MEETING]
 * - The model should be a quantized (INT8) classification model trained on labeled message data.
 * - Place the model file at: assets/intent_classifier.onnx
 *
 * When the model file is not present in assets, this classifier gracefully returns null,
 * signaling the HybridClassificationPipeline to fall back to RuleBasedIntentClassifier.
 *
 * Model inference flow:
 * 1. Create OnnxTensor from the feature FloatArray
 * 2. Run the OrtSession with the input tensor
 * 3. Parse output logits using softmax to get per-class probabilities
 * 4. Return ClassificationResult with the highest-probability intent
 */
@Singleton
class OnnxIntentClassifier : IntentClassifier {

    private var modelAvailable: Boolean = false
    private var ortEnvironment: Any? = null
    private var ortSession: Any? = null
    private var context: Context? = null

    companion object {
        /** Version of the expected model format. Increment when input/output schema changes. */
        const val MODEL_VERSION = 1

        private const val MODEL_FILE = "intent_classifier.onnx"

        /** Intent classes in the order the model outputs them */
        private val INTENT_CLASSES = arrayOf(
            IntentType.TASK_REQUEST,
            IntentType.INFORMATION,
            IntentType.SOCIAL,
            IntentType.QUESTION,
            IntentType.REMINDER,
            IntentType.MEETING
        )
    }

    /**
     * Production constructor for use with Hilt DI (via MlModule).
     * Accepts application context to load model from assets.
     */
    constructor(context: Context) {
        this.context = context
        modelAvailable = try {
            // Check if the model file exists in assets
            val assetFiles = context.assets.list("") ?: emptyArray()
            if (MODEL_FILE in assetFiles) {
                // Model file exists - attempt to initialize ONNX Runtime
                initializeOnnxSession()
            } else {
                false
            }
        } catch (e: Exception) {
            // Gracefully handle any initialization failure
            false
        }
    }

    /**
     * Test constructor that creates a classifier with no model available.
     * Used in unit tests where Android Context is not available.
     */
    constructor() {
        this.context = null
        this.modelAvailable = false
    }

    /**
     * Attempts to initialize OrtEnvironment and OrtSession.
     * Returns true if successful, false otherwise.
     *
     * NOTE: This uses reflection to avoid a compile-time dependency on the ONNX Runtime
     * native library. The tradeoff is that method signature changes in future ONNX Runtime
     * versions will fail at runtime rather than compile time. This is acceptable because
     * the app gracefully falls back to RuleBasedIntentClassifier when ONNX is unavailable.
     *
     * The model is loaded into a byte array in memory. For the expected quantized model
     * size (<10 MB), this is fine. Larger models would need a streaming approach.
     */
    private fun initializeOnnxSession(): Boolean {
        return try {
            // Use reflection to check if ONNX Runtime classes are available
            val envClass = Class.forName("ai.onnxruntime.OrtEnvironment")
            val getEnvMethod = envClass.getMethod("getEnvironment")
            val env = getEnvMethod.invoke(null)
            ortEnvironment = env

            val modelBytes = context!!.assets.open(MODEL_FILE).readBytes()

            val createSessionMethod = envClass.getMethod("createSession", ByteArray::class.java)
            val session = createSessionMethod.invoke(env, modelBytes)
            ortSession = session

            true
        } catch (e: Exception) {
            // ONNX Runtime not available or model loading failed
            ortEnvironment = null
            ortSession = null
            false
        }
    }

    override fun classify(features: FloatArray): ClassificationResult? {
        if (!modelAvailable || ortSession == null || ortEnvironment == null) {
            return null
        }

        return try {
            runInference(features)
        } catch (e: Exception) {
            // Gracefully handle any inference failure
            null
        }
    }

    /**
     * Runs ONNX inference on the feature array.
     * Uses reflection to avoid compile-time dependency on ONNX Runtime native libraries.
     */
    private fun runInference(features: FloatArray): ClassificationResult? {
        val env = ortEnvironment ?: return null
        val session = ortSession ?: return null

        return try {
            // Create input tensor: shape [1, featureCount]
            val tensorClass = Class.forName("ai.onnxruntime.OnnxTensor")
            val createTensorMethod = tensorClass.getMethod(
                "createTensor",
                Class.forName("ai.onnxruntime.OrtEnvironment"),
                Array<FloatArray>::class.java
            )
            val inputData = arrayOf(features)
            val inputTensor = createTensorMethod.invoke(null, env, inputData)

            // Run session
            val sessionClass = Class.forName("ai.onnxruntime.OrtSession")
            val runMethod = sessionClass.getMethod("run", Map::class.java)
            val inputMap = mapOf("input" to inputTensor)
            val results = runMethod.invoke(session, inputMap)

            // Parse output
            val resultClass = Class.forName("ai.onnxruntime.OrtSession\$Result")
            val getMethod = resultClass.getMethod("get", Int::class.javaPrimitiveType)
            val outputOptional = getMethod.invoke(results, 0)

            // Get the tensor value
            val optionalClass = Class.forName("java.util.Optional")
            val optionalGetMethod = optionalClass.getMethod("get")
            val outputTensor = optionalGetMethod.invoke(outputOptional)

            val getValueMethod = tensorClass.getMethod("getValue")
            val outputValue = getValueMethod.invoke(outputTensor)

            // Output is Array<FloatArray> of shape [1, 6]
            @Suppress("UNCHECKED_CAST")
            val logits = (outputValue as Array<FloatArray>)[0]

            // Close resources
            try {
                val closeMethod = resultClass.getMethod("close")
                closeMethod.invoke(results)
            } catch (_: Exception) { }

            parseLogits(logits)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts raw logits from the model output into a ClassificationResult.
     * Applies softmax to get probabilities, then selects the highest-confidence intent.
     */
    private fun parseLogits(logits: FloatArray): ClassificationResult {
        val probabilities = softmax(logits)

        var maxIndex = 0
        var maxProb = probabilities[0]
        for (i in 1 until probabilities.size) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIndex = i
            }
        }

        val intent = if (maxIndex < INTENT_CLASSES.size) {
            INTENT_CLASSES[maxIndex]
        } else {
            IntentType.INFORMATION
        }

        val isActionable = intent in listOf(
            IntentType.TASK_REQUEST,
            IntentType.REMINDER,
            IntentType.MEETING
        )

        val suggestedPriority = when {
            !isActionable -> null
            maxProb > 0.9f -> TaskPriority.HIGH
            maxProb > 0.7f -> TaskPriority.MEDIUM
            else -> TaskPriority.LOW
        }

        return ClassificationResult(
            intent = intent,
            confidence = maxProb,
            isActionable = isActionable,
            suggestedPriority = suggestedPriority
        )
    }

    /**
     * Applies softmax normalization to convert logits to probabilities.
     */
    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.max()
        val exps = FloatArray(logits.size) { i ->
            kotlin.math.exp((logits[i] - maxLogit).toDouble()).toFloat()
        }
        val sumExps = exps.sum()
        return FloatArray(exps.size) { i -> exps[i] / sumExps }
    }
}
