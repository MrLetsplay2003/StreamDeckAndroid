package me.mrletsplay.streamdeckandroid.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class BitmapUtils {

	public static Bitmap newBitmap() {
		return Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
	}

	public static Bitmap text(String text, float fontSize, int textColor, int backgroundColor) {
		Bitmap b = newBitmap();
		Canvas c = new Canvas(b);

		Paint p = new Paint();
		p.setColor(backgroundColor);
		p.setStyle(Paint.Style.FILL);
		c.drawPaint(p);

		p.setColor(textColor);
		p.setAntiAlias(true);
		p.setTextSize(fontSize);

		drawLinesCentered(c, p, 256, 256, text.split("\n"));

		return b;
	}

	public static Bitmap solidColor(int backgroundColor) {
		Bitmap b = newBitmap();
		Canvas c = new Canvas(b);

		Paint p = new Paint();
		p.setColor(backgroundColor);
		p.setStyle(Paint.Style.FILL);
		c.drawPaint(p);

		return b;
	}

	private static void drawLinesCentered(Canvas c, Paint p, int centerX, int centerY, String... lines) {
		float lineHeight = p.getFontMetrics().bottom - p.getFontMetrics().top;
		Rect rect = new Rect();
		for(int i = 0; i < lines.length; i++) {
			p.getTextBounds(lines[i], 0, lines[i].length(), rect);
			c.drawText(lines[i], centerX - rect.exactCenterX(), centerY - lineHeight * (lines.length - 1) / 2 + lineHeight * i - rect.exactCenterY(), p);
		}
	}

}
