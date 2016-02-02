package it.amonshore.comikkua.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

/**
 * Created by Narsenico on 17/10/2015.
 */
class ComicsImageTransformation implements Transformation<Bitmap> {

    private BitmapPool mBitmapPool;
    private float mHeight;
    private int mColor0;
    private int mColor1;

    public ComicsImageTransformation(Context context, float height, int color0, int color1) {
        this(Glide.get(context).getBitmapPool(), height, color0, color1);
    }

    private ComicsImageTransformation(BitmapPool pool, float height, int color0, int color1) {
        mBitmapPool = pool;
        mHeight = height;
        mColor0 = color0;
        mColor1 = color1;
    }

    @Override
    public Resource<Bitmap> transform(Resource<Bitmap> resource, int outWidth, int outHeight) {
        Bitmap source = resource.get();

        int width = source.getWidth();
        int height = source.getHeight();

        Bitmap.Config config =
                source.getConfig() != null ? source.getConfig() : Bitmap.Config.ARGB_8888;
        Bitmap bitmap = mBitmapPool.get(width, height, config);
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, config);
        }

        LinearGradient shader = new LinearGradient(0, 0, 0, mHeight,
                mColor0, mColor1, Shader.TileMode.CLAMP);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);
        canvas.drawBitmap(source, 0,0, null);
        canvas.drawPaint(paint);

        return BitmapResource.obtain(bitmap, mBitmapPool);
    }

    @Override
    public String getId() {
        return String.format("ComicsImageTransformation(%s, %s, %s)",
                mHeight, mColor0, mColor1);
    }
}
