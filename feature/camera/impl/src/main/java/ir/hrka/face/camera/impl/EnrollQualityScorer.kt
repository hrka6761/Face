package ir.hrka.face.camera.impl

import ir.hrka.face.model.FaceEmbedding
import kotlin.math.sqrt

/**
 * Scores a multi-pose enrollment gallery for consistency and cross-pose identity.
 */
object EnrollQualityScorer {

    /** Minimum score for [EnrollQualityGrade.Excellent] (above 90%). */
    const val EXCELLENT_MIN: Float = 0.90f

    /** Minimum score for [EnrollQualityGrade.Good] (75% inclusive; below this is Bad). */
    const val GOOD_MIN: Float = 0.75f

    /**
     * Computes a 0..1 quality score and grade from collected templates.
     *
     * Grades: Bad &lt; 75%, Good 75–90% inclusive, Excellent &gt; 90%.
     *
     * @param samples Embeddings collected in pose order (front, then left, then right).
     * @param perPose Expected samples per pose step.
     */
    fun evaluate(
        samples: List<FaceEmbedding>,
        perPose: Int = EnrollConfig.ENROLL_SAMPLES_PER_POSE,
    ): Pair<Float, EnrollQualityGrade> {
        if (samples.size < perPose * EnrollConfig.ENROLL_POSE_STEPS) {
            return 0f to EnrollQualityGrade.Bad
        }

        val front = samples.subList(0, perPose)
        val left = samples.subList(perPose, perPose * 2)
        val right = samples.subList(perPose * 2, perPose * 3)

        val consistency = listOf(
            meanPairwise(front),
            meanPairwise(left),
            meanPairwise(right),
        ).average().toFloat()

        val frontMean = meanEmbedding(front)
        val leftMean = meanEmbedding(left)
        val rightMean = meanEmbedding(right)
        val cross = (
            cosine(frontMean, leftMean) +
                cosine(frontMean, rightMean) +
                cosine(leftMean, rightMean)
            ) / 3f

        // Favor consistent poses while still requiring the same identity across poses.
        val score = (0.55f * consistency + 0.45f * cross).coerceIn(0f, 1f)
        return score to gradeFor(score)
    }

    /**
     * Scores probe embeddings against an enrollment gallery by average best-match.
     *
     * Grades: Bad &lt; 75%, Good 75–90% inclusive, Excellent &gt; 90%.
     *
     * @param gallery Templates collected during guided enrollment.
     * @param probes Embeddings captured during Test Scan at varied distances/positions.
     */
    fun evaluateProbes(
        gallery: List<FaceEmbedding>,
        probes: List<FaceEmbedding>,
    ): Pair<Float, EnrollQualityGrade> {
        if (gallery.isEmpty() || probes.isEmpty()) {
            return 0f to EnrollQualityGrade.Bad
        }

        var sum = 0f
        for (probe in probes) {
            var best = 0f
            for (template in gallery) {
                val score = cosine(probe.values, template.values)
                if (score > best) best = score
            }
            sum += best
        }
        val average = (sum / probes.size).coerceIn(0f, 1f)
        return average to gradeFor(average)
    }

    /**
     * Maps a 0..1 score to Bad / Good / Excellent.
     *
     * - Bad: below 75%
     * - Good: 75% through 90% inclusive
     * - Excellent: above 90%
     */
    fun gradeFor(score: Float): EnrollQualityGrade = when {
        score > EXCELLENT_MIN -> EnrollQualityGrade.Excellent
        score >= GOOD_MIN -> EnrollQualityGrade.Good
        else -> EnrollQualityGrade.Bad
    }

    private fun meanPairwise(group: List<FaceEmbedding>): Float {
        if (group.size < 2) return group.firstOrNull()?.let { 1f } ?: 0f
        var sum = 0f
        var count = 0
        for (i in group.indices) {
            for (j in i + 1 until group.size) {
                sum += cosine(group[i].values, group[j].values)
                count++
            }
        }
        return if (count == 0) 0f else sum / count
    }

    private fun meanEmbedding(group: List<FaceEmbedding>): FloatArray {
        val dim = group.first().values.size
        val acc = FloatArray(dim)
        for (embedding in group) {
            val v = embedding.values
            for (i in 0 until dim) acc[i] += v[i]
        }
        val n = group.size.toFloat().coerceAtLeast(1f)
        for (i in acc.indices) acc[i] /= n
        return l2Normalize(acc)
    }

    private fun cosine(a: FaceEmbedding, b: FaceEmbedding): Float =
        cosine(a.values, b.values)

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        val size = minOf(a.size, b.size)
        if (size == 0) return 0f
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in 0 until size) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        val denom = sqrt(na) * sqrt(nb)
        return if (denom == 0f) 0f else (dot / denom)
    }

    private fun l2Normalize(values: FloatArray): FloatArray {
        var sum = 0f
        for (v in values) sum += v * v
        val norm = sqrt(sum)
        if (norm == 0f) return values
        return FloatArray(values.size) { i -> values[i] / norm }
    }
}
