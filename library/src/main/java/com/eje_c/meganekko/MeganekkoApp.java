package com.eje_c.meganekko;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.eje_c.meganekko.xml.XmlSceneParser;
import com.eje_c.meganekko.xml.XmlSceneParserFactory;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.ref.Reference;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The interface for your application.
 */
public abstract class MeganekkoApp {

    private static final int MAX_EVENTS_PER_FRAME = 16;

    private final Meganekko meganekko;
    private final Queue<Runnable> mRunnables = new LinkedBlockingQueue<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Scene mScene;
    private Frame frame;

    protected MeganekkoApp(Meganekko meganekko) {
        this.meganekko = meganekko;
    }

    /*
     * Life cycle methods
     */

    public void init() {
    }

    /**
     * This will be called right after entering VR mode.
     */
    public void enteredVrMode() {
    }

    /**
     * Will be called on frame update. Any animations or input handlings will be implemented in it.
     * You can override this method but you must call {@code super.update(meganekko, frame)} to work properly.
     */
    public void update() {

        // runOnGlThread handling
        for (int i = 0; !mRunnables.isEmpty() && i < MAX_EVENTS_PER_FRAME; ++i) {
            Runnable event = mRunnables.poll();
            event.run();
        }

        mScene.update(frame);

        // Delete native resources related with Garbage Collected objects
        Reference<? extends HybridObject> ref;
        while ((ref = NativeReference.sReferenceQueue.poll()) != null) {
            if (ref instanceof NativeReference) {
                ((NativeReference) ref).delete();
            }
        }
    }

    /**
     * This will be called right before leaving VR mode.
     */
    public void leavingVrMode() {
    }

    /**
     * This will be called when user exits from app.
     */
    public void shutdown() {
    }

    /**
     * Will be called when user is resumed from sleeping.
     * Note: This method is called from UI-thread. Cannot perform GL related operations.
     */
    public void onResume() {
    }

    /**
     * Will be called when device is going to sleep mode.
     * Note: This method is called from UI-thread. Cannot perform GL related operations.
     */
    public void onPause() {
    }

    public Frame getFrame() {
        return frame;
    }

    /**
     * For internal use purpose.
     *
     * @param frame
     */
    public void setFrame(Frame frame) {
        this.frame = frame;
    }

    /**
     * Enqueues a callback to be run in the GL thread.
     * This is how you take data generated on a background thread (or the main
     * (GUI) thread) and pass it to the coprocessor, using calls that must be
     * made from the GL thread (aka the "GL context").
     *
     * @param action A bit of code that must run on the GL thread
     */
    public final void runOnGlThread(@NonNull Runnable action) {
        mRunnables.add(action);
    }

    /**
     * Delayed version of {@link #runOnGlThread(Runnable)}.
     *
     * @param action      A bit of code that must run on the GL thread
     * @param delayMillis Milli seconds delay before executing action.
     */
    public final void runOnGlThread(@NonNull final Runnable action, long delayMillis) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnGlThread(action);
            }
        }, delayMillis);
    }

    /**
     * Enqueues a callback to be run in the UI thread.
     *
     * @param action A bit of code that must run on the UI thread
     */
    public final void runOnUiThread(@NonNull Runnable action) {
        meganekko.runOnUiThread(action);
    }

    /**
     * Delayed version of {@link #runOnUiThread(Runnable)}.
     *
     * @param action      A bit of code that must run on the UI thread
     * @param delayMillis Milli seconds delay before executing action.
     */
    public final void runOnUiThread(@NonNull final Runnable action, long delayMillis) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(action);
            }
        }, delayMillis);
    }

    /**
     * Request recenter. This only work with Gear VR hardware.
     */
    public void recenter() {
        meganekko.recenter();
    }

    /**
     * Run {@link Animator} on UI thread and notify end callback on GL thread.
     *
     * @param anim        {@link Animator}.
     * @param endCallback Callback for animation end. This is <b>not</b> called when animation is canceled.
     *                    If you require more complicated callbacks, use {@code AnimatorListener} instead of this.
     */
    public final void animate(@NonNull final Animator anim, @Nullable final Runnable endCallback) {

        if (anim.isRunning()) {
            cancel(anim, new Runnable() {
                @Override
                public void run() {
                    animate(anim, endCallback);
                }
            });
            return;
        }

        // Register one time animation end callback
        if (endCallback != null) {
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    anim.removeListener(this);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    anim.removeListener(this);
                    runOnGlThread(endCallback);
                }
            });
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                anim.start();
            }
        });
    }

    /**
     * Cancel {@link Animator} running.
     *
     * @param anim     {@link Animator}.
     * @param callback Callback for canceling operation was called in UI thread.
     */
    public final void cancel(@NonNull final Animator anim, @Nullable final Runnable callback) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                anim.cancel();
                if (callback != null) runOnGlThread(callback);
            }
        });
    }

    public final Context getContext() {
        return meganekko.getContext();
    }

    public boolean onKeyShortPress(int keyCode, int repeatCount) {
        return mScene.onKeyShortPress(keyCode, repeatCount);
    }

    public boolean onKeyDoubleTap(int keyCode, int repeatCount) {
        return mScene.onKeyDoubleTap(keyCode, repeatCount);
    }

    public boolean onKeyLongPress(int keyCode, int repeatCount) {
        return mScene.onKeyLongPress(keyCode, repeatCount);
    }

    public boolean onKeyDown(int keyCode, int repeatCount) {
        return mScene.onKeyDown(keyCode, repeatCount);
    }

    public boolean onKeyUp(int keyCode, int repeatCount) {
        return mScene.onKeyUp(keyCode, repeatCount);
    }

    public boolean onKeyMax(int keyCode, int repeatCount) {
        return mScene.onKeyMax(keyCode, repeatCount);
    }

    /**
     * Get current rendering scene.
     *
     * @return Current rendering scene.
     */
    public Scene getScene() {
        return mScene;
    }

    /**
     * Set current rendering scene.
     *
     * @param scene The {@link Scene} will be rendered.
     */
    public synchronized void setScene(@NonNull Scene scene) {

        if (scene == mScene)
            return;

        if (mScene != null) {
            mScene.onPause();
        }

        scene.onResume(this);

        mScene = scene;
    }

    /**
     * Create {@link Scene} from XML resource and set it to current scene.
     *
     * @param xmlRes Scene graph XML resource.
     */
    public void setSceneFromXML(int xmlRes) {
        setSceneFromXML(xmlRes, null);
    }

    /**
     * Create {@link Scene} from XML resource and set it to current scene.
     *
     * @param xmlRes Scene graph XML resource.
     * @param args   Arguments passed to scene.
     */
    public void setSceneFromXML(int xmlRes, @Nullable Bundle args) {

        XmlSceneParser parser = XmlSceneParserFactory.getInstance(meganekko.getContext()).getSceneParser();

        try {
            Scene scene = parser.parse(meganekko.getContext().getResources().getXml(xmlRes), null);
            scene.setArguments(args);
            setScene(scene);
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
    }
}
