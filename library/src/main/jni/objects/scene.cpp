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
 * Holds scene objects. Can be used by engines.
 ***************************************************************************/

#include "Scene.h"

#include "objects/SceneObject.h"

namespace mgn {
Scene::Scene(JNIEnv * jni, jobject javaObject) :
        SceneObject(jni, javaObject), main_camera_(), frustum_flag_(
                false), dirtyFlag_(0), occlusion_flag_(false) {
}

Scene::~Scene() {
}

std::vector<SceneObject*> Scene::getWholeSceneObjects() {
    std::vector<SceneObject*> scene_objects(children());
    for (int i = 0; i < scene_objects.size(); ++i) {
        std::vector<SceneObject*> children(scene_objects[i]->children());
        for (auto it = children.begin(); it != children.end(); ++it) {
            scene_objects.push_back(*it);
        }
    }

    return scene_objects;
}

}
