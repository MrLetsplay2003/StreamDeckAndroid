package me.mrletsplay.streamdeckandroid.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.Random;

import me.mrletsplay.streamdeckandroid.R;

public class DeckButton extends AppCompatButton {

	public DeckButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs);
	}

    public DeckButton(Context context) {
		this(context, null);
	}

    public DeckButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public void setBitmap(Bitmap b) {
		StateListDrawable d = new StateListDrawable();
		LayerDrawable layers = new LayerDrawable(new Drawable[] {new BitmapDrawable(getContext().getResources(), b), new ColorDrawable(Color.argb(0.5f, 0f, 0f, 0f))});
		layers.setLayerInset(0, 0, 0, 0, 0);
		layers.setLayerInset(1, 0, 0, 0, 0);
		d.addState(PRESSED_STATE_SET, layers);
		d.addState(EMPTY_STATE_SET, new BitmapDrawable(getContext().getResources(), b));
		setBackground(d);
	}

}
