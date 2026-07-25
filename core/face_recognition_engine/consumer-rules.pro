# ONNX Runtime uses reflection / JNI; keep its public API for R8.
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**
