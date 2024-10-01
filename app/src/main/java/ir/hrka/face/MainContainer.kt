package ir.hrka.face

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ir.hrka.face.ui.theme.FaceTheme

@Composable
fun AppContent() {
    FaceTheme {

    }
}


@Preview(showBackground = true)
@Composable
fun AppContentPreview() {
    FaceTheme {
        AppContent()
    }
}