package ir.hrka.face.engine.embedding

import android.graphics.Bitmap
import ir.hrka.face.engine.model.FaceEmbedding

/**
 * Face embedding extractor contract.
 */
internal interface FaceEmbedder : AutoCloseable {

    /**
     * Generates a 512-D L2-normalized embedding from an aligned face crop.
     */
    suspend fun embed(alignedFace: Bitmap): FaceEmbedding
}
