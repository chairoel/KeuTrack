//
// Created by Chairul Amri on 24/03/26.
//

#include <string>
#include <jni.h>

// External C linkage to avoid C++ name mangling
extern "C" {
jstring
Java_com_mascill_keutrack_core_network_utils_NetworkNativeWrapper_getBaseUrl(
        JNIEnv *env, jobject thi) {
    std::string type = TYPE;
    std::string baseUrl;

    if (type == "dev") {
        baseUrl = "https://m-smpobapi-dev.transjakarta.co.id/";
    } else {
        baseUrl = "https://m-smpobapi.transjakarta.co.id/";
    }

    return env->NewStringUTF(baseUrl.c_str());
}
}