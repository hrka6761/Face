package ir.hrka.face.presentation.ui.screens.home

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT
import com.google.mlkit.vision.face.Face

class FaceOverlayController(
    private val viewModel: HomeViewModel,
    private val orientation: Int,
) {

    fun getFaceOverlay(face: Face) =
        FaceOverlay(
            faceLeft = getOverlayLeft(face),
            faceTop = getOverlayTop(face),
            faceWith = getOverlayWidth(face),
            faceHeight = getOverlayHeight(face),
            balanceCircles = getBalanceCircles(face)
        )

    private fun getOverlayLeft(face: Face) =
        if (isImageFlipped())
            viewModel.surfaceSize.value.first -
                    face.boundingBox.centerX().toFloat() -
                    (face.boundingBox.width().toFloat() / 2)
        else
            face.boundingBox.centerX().toFloat() - (face.boundingBox.width()
                .toFloat() / 2)

    private fun getOverlayTop(face: Face) =
        face.boundingBox.centerY().toFloat() - (face.boundingBox.height().toFloat() / 2)

    private fun getOverlayWidth(face: Face) = face.boundingBox.width().toFloat()

    private fun getOverlayHeight(face: Face) = face.boundingBox.height().toFloat()

    private fun getBalanceCircles(face: Face) =
        BalanceCircles(
            horizontalBalanceCircleX = getHorizontalBalanceCircleX(face),
            horizontalBalanceCircleY = getHorizontalBalanceCircleY(face),
            verticalBalanceCircleX = getVerticalBalanceCircleX(face),
            verticalBalanceCircleY = getVerticalBalanceCircleY(face)
        )

    private fun getHorizontalBalanceCircleX(face: Face): Float {
        val overlayWidth =
            if (orientation == ORIENTATION_PORTRAIT)
                viewModel.surfaceSize.value.first
            else
                viewModel.surfaceSize.value.second

        return if (getFaceHorizontalGravity(face) == FaceGravity.Left)
            overlayWidth - 50f
        else
            50f
    }

    private fun getHorizontalBalanceCircleY(face: Face): Float {
        val overlayHeight =
            if (orientation == ORIENTATION_PORTRAIT)
                viewModel.surfaceSize.value.second
            else
                viewModel.surfaceSize.value.first

        return overlayHeight / 2
    }

    private fun getVerticalBalanceCircleX(face: Face): Float {
        val overlayWidth =
            if (orientation == ORIENTATION_PORTRAIT)
                viewModel.surfaceSize.value.first
            else
                viewModel.surfaceSize.value.second

        return overlayWidth / 2
    }

    private fun getVerticalBalanceCircleY(face: Face): Float {
        val overlayHeight =
            if (orientation == ORIENTATION_PORTRAIT)
                viewModel.surfaceSize.value.second
            else
                viewModel.surfaceSize.value.first

        return if (getFaceVerticalGravity(face) == FaceGravity.Top)
            overlayHeight - 50f
        else
            50f
    }

    private fun getFaceHorizontalGravity(face: Face): FaceGravity {
        val overlayWidth =
            if (orientation == ORIENTATION_PORTRAIT)
                viewModel.surfaceSize.value.first
            else
                viewModel.surfaceSize.value.second

        return if (isImageFlipped())
            if (face.boundingBox.centerX() <= overlayWidth / 2)
                FaceGravity.Right
            else
                FaceGravity.Left
        else
            if (face.boundingBox.centerX() >= overlayWidth / 2)
                FaceGravity.Right
            else
                FaceGravity.Left
    }

    private fun getFaceVerticalGravity(face: Face): FaceGravity {
        val overlayHeight =
            if (orientation == ORIENTATION_PORTRAIT)
                viewModel.surfaceSize.value.second
            else
                viewModel.surfaceSize.value.first

        return if (face.boundingBox.centerY() >= overlayHeight / 2)
            FaceGravity.Bottom
        else
            FaceGravity.Top
    }

    private fun isImageFlipped() = viewModel.lensFacing.value == LENS_FACING_FRONT
}