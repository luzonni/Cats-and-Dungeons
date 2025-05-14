#include <windows.h>
JNIEXPORT void JNICALL Java_DPIFix_setDPI(JNIEnv *env, jclass clazz) {
    SetProcessDPIAware();
}