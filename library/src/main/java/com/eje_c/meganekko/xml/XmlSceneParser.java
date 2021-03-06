/*
 * Copyright 2015 eje inc.
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
package com.eje_c.meganekko.xml;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Xml;

import com.eje_c.meganekko.Scene;
import com.eje_c.meganekko.SceneObject;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

/**
 * Load XML and add {@link SceneObject}s to {@link Scene}.
 */
public class XmlSceneParser {

    private final Context mContext;
    private final XmlSceneObjectParser mObjectParser;

    public XmlSceneParser(Context context) {
        this.mContext = context;
        this.mObjectParser = new XmlSceneObjectParser(context);
    }

    /**
     * Parse scene from XML resource.
     *
     * @param xmlRes XML resource.
     * @param scene  Root scene. It can be null.
     * @return Parsed {@code Scene}.
     * @throws XmlPullParserException
     * @throws IOException
     */
    public Scene parse(int xmlRes, Scene scene) throws XmlPullParserException, IOException {
        return parse(mContext.getResources().getXml(xmlRes), scene);
    }

    /**
     * Parse scene from {@code URL}. XML can be loaded any where.
     *
     * @param url   URL pointing to XML resource.
     * @param scene Root scene. It can be null.
     * @return Parsed {@code Scene}.
     * @throws XmlPullParserException
     * @throws IOException
     */
    public Scene parse(String url, Scene scene) throws XmlPullParserException, IOException {
        return parse(new URL(url).openStream(), scene);
    }

    /**
     * Parse scene from {@code InputStream}. XML can be loaded any where.
     *
     * @param in    {@code InputStream} of XML.
     * @param scene Root scene. It can be null.
     * @return Parsed {@code Scene}.
     * @throws XmlPullParserException
     * @throws IOException
     */
    public Scene parse(InputStream in, Scene scene) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            return parse(parser, scene);
        } finally {
            in.close();
        }
    }

    /**
     * Parse scene from {@code XmlPullParser}. This method can be used with
     * {@code Resources#getXml(int)}.
     *
     * @param parser
     * @param scene  Root scene. It can be null.
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    public Scene parse(XmlPullParser parser, Scene scene) throws XmlPullParserException, IOException {

        AttributeSet attributeSet = Xml.asAttributeSet(parser);

        // Skip to first tag
        if (parser.getEventType() == XmlPullParser.START_DOCUMENT) {
            while (parser.getEventType() != XmlPullParser.START_TAG) {
                if (parser.next() == XmlPullParser.END_DOCUMENT) {
                    return scene;
                }
            }
        }

        // Parse first tag as Scene if scene was not passed
        if (scene == null) {

            if ("scene".equals(parser.getName())) {
                String className = attributeSet.getClassAttribute();
                if (className == null) {
                    scene = new Scene();
                } else {
                    scene = createScene(parser, className);
                }
            } else {
                scene = createScene(parser, parser.getName());
            }

        }

        while (parser.next() != XmlPullParser.END_DOCUMENT) {

            switch (parser.getEventType()) {

                case XmlPullParser.START_TAG:

                    SceneObject object = mObjectParser.parse(parser);

                    if (object != null) {
                        scene.addChildObject(object);
                    }
                    break;

                case XmlPullParser.END_TAG:
                    break;
            }
        }

        return scene;
    }

    private Scene createScene(XmlPullParser parser, String className) throws XmlPullParserException {
        try {
            return ObjectFactory.newInstance(className);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException | ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
            throw new XmlPullParserException("Invalid class name " + className + ".", parser, e);
        }
    }
}
