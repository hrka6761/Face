package ir.hrka.face.recognition.api

import android.graphics.Bitmap
import ir.hrka.face.model.DetectedFace
import ir.hrka.face.model.FaceEmbedding

/**
 * Produces L2-normalized face embeddings from upright bitmaps and detected faces.
 *
 * Implementations are safe for sequential calls. Prefer calling from a background dispatcher.
 */
interface FaceEmbedder : AutoCloseable {

    /**
     * Embeds a single detected face from an upright source [bitmap] (single-scale crop).
     *
     * @param bitmap Full upright frame.
     * @param face Detected face within [bitmap].
     * @return Embedding, or `null` when the crop is invalid.
     * @throws FaceRecognitionException when the interpreter fails.
     */
    fun embed(bitmap: Bitmap, face: DetectedFace): FaceEmbedding?

    /**
     * Distance-robust embedding: runs multiple aligned crops at different scales and
     * returns their L2-normalized centroid. Prefer this for enrollment and matching.
     *
     * @param bitmap Full upright frame.
     * @param face Detected face within [bitmap].
     * @return Robust embedding, or `null` when no valid crop could be produced.
     */
    fun embedRobust(bitmap: Bitmap, face: DetectedFace): FaceEmbedding?

    /**
     * Returns every multi-scale embedding for [face] (not averaged). Useful when building
     * a multi-template gallery during enrollment.
     *
     * @param bitmap Full upright frame.
     * @param face Detected face.
     * @return One embedding per successful scale crop (may be empty).
     */
    fun embedMultiScale(bitmap: Bitmap, face: DetectedFace): List<FaceEmbedding>

    /**
     * Embeds multiple faces from the same upright [bitmap] using [embedRobust].
     *
     * @param bitmap Full upright frame.
     * @param faces Detected faces.
     * @return Map of tracking id → embedding for faces that could be cropped.
     */
    fun embedAll(bitmap: Bitmap, faces: List<DetectedFace>): Map<Int, FaceEmbedding>

    /**
     * Releases the TFLite interpreter and related resources.
     */
    override fun close()
}
