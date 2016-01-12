/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/***************************************************************************
 * JNI
 ***************************************************************************/

#include "base_texture.h"
#include "png_loader.h"
#include "util/gvr_jni.h"
#include "util/gvr_java_stack_trace.h"
#include "android/asset_manager_jni.h"

#include <png.h>

namespace mgn {
extern "C" {
JNIEXPORT jlong JNICALL
Java_com_eje_1c_meganekko_BitmapTexture_fileConstructor(JNIEnv * env,
        jobject obj, jobject asset_manager, jstring filename, jintArray jtexture_parameters);
JNIEXPORT jlong JNICALL
Java_com_eje_1c_meganekko_BitmapTexture_bareConstructor(JNIEnv * env, jobject obj, jintArray jtexture_parameters);
JNIEXPORT jboolean JNICALL
Java_com_eje_1c_meganekko_BitmapTexture_update(JNIEnv * env, jobject obj,
        jlong jtexture, jint width, jint height, jbyteArray jdata);
}
;

JNIEXPORT jlong JNICALL
Java_com_eje_1c_meganekko_BitmapTexture_fileConstructor(JNIEnv * env,
        jobject obj, jobject asset_manager, jstring filename, jintArray jtexture_parameters) {

    jint* texture_parameters = env->GetIntArrayElements(jtexture_parameters,0);

    const char* native_string = env->GetStringUTFChars(filename, 0);
    AAssetManager* mgr = AAssetManager_fromJava(env, asset_manager);
    AAsset* asset = AAssetManager_open(mgr, native_string, AASSET_MODE_UNKNOWN);
    if (NULL == asset) {
        LOGE("_ASSET_NOT_FOUND_");
        return JNI_FALSE;
    }

    PngLoader loader;
    loader.loadFromAsset(asset);

    AAsset_close(asset);
    env->ReleaseStringUTFChars(filename, native_string);

    if (loader.pOutImage.bits == NULL) {
        LOGE("PNG decoder failed");
        return JNI_FALSE;
    }

    if (loader.pOutImage.format != PngLoader::RGBAFormat) {
        LOGE("Only RGBA format supported");
        return JNI_FALSE;
    }

    int imgW = loader.pOutImage.width;
    int imgH = loader.pOutImage.height;
    unsigned char *pixels = loader.pOutImage.bits;
    jlong result = reinterpret_cast<jlong>(new BaseTexture(imgW, imgH, pixels, texture_parameters));
    env->ReleaseIntArrayElements(jtexture_parameters, texture_parameters, 0);
    return result;
}

JNIEXPORT jlong JNICALL
Java_com_eje_1c_meganekko_BitmapTexture_bareConstructor(JNIEnv * env, jobject obj, jintArray jtexture_parameters) {

    jint* texture_parameters = env->GetIntArrayElements(jtexture_parameters,0);
    jlong result =  reinterpret_cast<jlong>(new BaseTexture(texture_parameters));
    env->ReleaseIntArrayElements(jtexture_parameters, texture_parameters, 0);
    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_eje_1c_meganekko_BitmapTexture_update(JNIEnv * env, jobject obj,
        jlong jtexture, jint width, jint height, jbyteArray jdata) {
    BaseTexture* texture = reinterpret_cast<BaseTexture*>(jtexture);
    jbyte* data = env->GetByteArrayElements(jdata, 0);
    jboolean result = texture->update(width, height, data);
    env->ReleaseByteArrayElements(jdata, data, 0);
    return result;
}

}
