package com.cutsame.ui.gallery.customview.scale;

import android.graphics.PointF;
import android.util.Property;

public class GestureLayoutUtils {
    public static final Property<PathMatrix, float[]> NON_TRANSLATIONS_PROPERTY =
            new Property<PathMatrix, float[]>(float[].class, "nonTranslations") {
                @Override
                public float[] get(PathMatrix object) {
                    return null;
                }

                @Override
                public void set(PathMatrix object, float[] value) {
                    object.setValues(value);
                }
            };

    public static final Property<PathMatrix, PointF> TRANSLATIONS_PROPERTY =
            new Property<PathMatrix, PointF>(PointF.class, "translations") {
                @Override
                public PointF get(PathMatrix object) {
                    return null;
                }

                @Override
                public void set(PathMatrix object, PointF value) {
                    object.setTranslation(value);
                }
            };
}
