/*
 * DiPlay — Zeekr adaptation. GPLv3 (see LICENSE).
 *
 * JNI shim for the Zeekr HUD (HAB) frame channel. Our own implementation: it
 * dlopen()s the vendor /system/lib64/libHabCamera.so and forwards an
 * AHardwareBuffer to it. This replaces the third-party libhabmanager; no code
 * from that binary is used here.
 *
 * STATUS: skeleton. The vendor entry points below are placeholders — resolve
 * the real symbols/signatures from the device's libHabCamera.so (nm -D /
 * readelf), e.g. the com::driving::parking::HabCameraListener frame-submit path,
 * then wire create/open/send/close to them.
 */
#include <jni.h>
#include <android/log.h>
#include <android/hardware_buffer.h>
#include <android/hardware_buffer_jni.h>
#include <dlfcn.h>
#include <stdlib.h>

#define TAG "zeekr_hab_jni"
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

static void *g_lib = NULL;

/* TODO(device): real vendor signatures from libHabCamera.so. */
typedef void *(*hab_create_fn)(void);
typedef int   (*hab_open_fn)(void *ctx, int w, int h, int fmt);
typedef int   (*hab_send_fn)(void *ctx, AHardwareBuffer *buf);
typedef int   (*hab_close_fn)(void *ctx);

typedef struct { void *ctx; hab_send_fn send; hab_close_fn close; } hab_handle;

static void *load_lib(void) {
    if (!g_lib) {
        g_lib = dlopen("/system/lib64/libHabCamera.so", RTLD_NOW | RTLD_LOCAL);
        if (!g_lib) LOGW("dlopen libHabCamera failed: %s", dlerror());
    }
    return g_lib;
}

JNIEXPORT jlong JNICALL
Java_com_shilapi_xcertplay_hud_zeekr_ZeekrHabNative_open(
        JNIEnv *env, jobject thiz, jint w, jint h, jint fmt) {
    (void) env; (void) thiz;
    if (!load_lib()) return -1;
    /* TODO(device): resolve + call the real create/open symbols. */
    hab_create_fn create = (hab_create_fn) dlsym(g_lib, "/*TODO create symbol*/");
    hab_open_fn open_fn   = (hab_open_fn)   dlsym(g_lib, "/*TODO open symbol*/");
    if (!create || !open_fn) { LOGW("HAB symbols unresolved (fill TODOs)"); return -2; }
    hab_handle *hdl = calloc(1, sizeof(hab_handle));
    hdl->ctx = create();
    if (!hdl->ctx || open_fn(hdl->ctx, w, h, fmt) < 0) { free(hdl); return -3; }
    hdl->send  = (hab_send_fn)  dlsym(g_lib, "/*TODO send symbol*/");
    hdl->close = (hab_close_fn) dlsym(g_lib, "/*TODO close symbol*/");
    LOGI("HAB opened %dx%d fmt=%d", w, h, fmt);
    return (jlong) (intptr_t) hdl;
}

JNIEXPORT jint JNICALL
Java_com_shilapi_xcertplay_hud_zeekr_ZeekrHabNative_sendHardwareBuffer(
        JNIEnv *env, jobject thiz, jlong handle, jobject jbuf) {
    (void) thiz;
    hab_handle *hdl = (hab_handle *) (intptr_t) handle;
    if (!hdl || !hdl->send) return -1;
    AHardwareBuffer *buf = AHardwareBuffer_fromHardwareBuffer(env, jbuf);
    if (!buf) return -2;
    return hdl->send(hdl->ctx, buf);
}

JNIEXPORT jint JNICALL
Java_com_shilapi_xcertplay_hud_zeekr_ZeekrHabNative_close(
        JNIEnv *env, jobject thiz, jlong handle) {
    (void) env; (void) thiz;
    hab_handle *hdl = (hab_handle *) (intptr_t) handle;
    if (!hdl) return -1;
    int rc = hdl->close ? hdl->close(hdl->ctx) : 0;
    free(hdl);
    return rc;
}
