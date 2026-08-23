// SDL JNI_OnLoad isolation for the embedded game JVM.
// Reference: https://github.com/AngelAuraMC/Amethyst-Android/commit/e4c79084d

#include <dlfcn.h>
#include <jni.h>
#include <bytehook.h>
#include <stdbool.h>
#include <string.h>

#include "environ/environ.h"
#include "logger/logger.h"
#include "utils.h"

#define SDL_LIBRARY_COUNT 2
static const char *const sdlLibraries[SDL_LIBRARY_COUNT] = {
    "libSDL3.so",
    "libSDL2.so"
};

typedef void *(*dlsym_func_t)(void *handle, const char *symbol);
typedef jint (*jni_on_load_func_t)(JavaVM *vm, void *reserved);

typedef bytehook_stub_t (*bytehook_hook_all_fn)(const char *callee_path_name, const char *symbol,
                                                  void *new_func, bytehook_hooked_t hooked, void *hooked_arg);

typedef void *(*dlopen_func_t)(const char *filename, int flags);

static void *sdlHandle;
static jni_on_load_func_t originalSdlJniOnLoad;

static bool isSdlLibrary(const char *filename) {
    if (filename == NULL) return false;
    const char *name = strrchr(filename, '/');
    name = name == NULL ? filename : name + 1;
    for (int i = 0; i < SDL_LIBRARY_COUNT; ++i) {
        if (strcmp(name, sdlLibraries[i]) == 0) return true;
    }
    return false;
}

static jint isolatedSdlJniOnLoad(JavaVM *vm, void *reserved) {
    if (originalSdlJniOnLoad != NULL && pojav_environ->dalvikJavaVMPtr == vm) {
        return originalSdlJniOnLoad(vm, reserved);
    }
    return JNI_VERSION_1_4;
}

static void *customDlopen(const char *filename, int flags) {
    void *handle = BYTEHOOK_CALL_PREV(customDlopen, dlopen_func_t, filename, flags);
    if (handle != NULL && isSdlLibrary(filename)) {
        sdlHandle = handle;
    }
    BYTEHOOK_POP_STACK();
    return handle;
}

static void *customDlsym(void *handle, const char *symbol) {
    void *result = BYTEHOOK_CALL_PREV(customDlsym, dlsym_func_t, handle, symbol);
    if (handle != NULL && handle == sdlHandle && symbol != NULL && strcmp(symbol, "JNI_OnLoad") == 0) {
        originalSdlJniOnLoad = (jni_on_load_func_t) result;
        result = (void *) isolatedSdlJniOnLoad;
    }
    BYTEHOOK_POP_STACK();
    return result;
}

void create_sdl_dlopen_hooks(bytehook_hook_all_fn hookAll) {
    if (hookAll == NULL) return;
    hookAll(NULL, "dlopen", (void *) customDlopen, NULL, NULL);
    hookAll(NULL, "dlsym", (void *) customDlsym, NULL, NULL);
}
