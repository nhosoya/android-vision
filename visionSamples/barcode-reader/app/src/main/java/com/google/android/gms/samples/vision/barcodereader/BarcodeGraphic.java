/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.barcodereader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;

import com.google.android.gms.samples.vision.barcodereader.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.barcode.Barcode;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Graphic instance for rendering barcode position, size, and ID within an associated graphic
 * overlay view.
 */
public class BarcodeGraphic extends GraphicOverlay.Graphic {

    private static final String TAG = BarcodeGraphic.class.getSimpleName();

    private int mId;
    private Context mContext;

    private static final int COLOR_CHOICES[] = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN
    };

    private static int mCurrentColorIndex = 0;

    private Paint mRectPaint;
    private Paint mTextPaint;
    private Paint mBitmapPaint;
    private volatile Barcode mBarcode;
    private Bitmap mBitmap;
    private String mUrl;

    BarcodeGraphic(GraphicOverlay overlay, Context context) {
        super(overlay);

        mContext = context;
        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mRectPaint = new Paint();
        mRectPaint.setColor(selectedColor);
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(4.0f);

        mTextPaint = new Paint();
        mTextPaint.setColor(selectedColor);
        mTextPaint.setTextSize(36.0f);

        mBitmapPaint = new Paint();
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public Barcode getBarcode() {
        return mBarcode;
    }

    public String getUrl() {
        return mUrl;
    }

    /**
     * Updates the barcode instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateItem(Barcode barcode) {
        mBarcode = barcode;
        postInvalidate();
    }

    /**
     * Draws the barcode annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Barcode barcode = mBarcode;
        if (barcode == null) {
            return;
        }

        // Draws the bounding box around the barcode.
        RectF rect = new RectF(barcode.getBoundingBox());
        rect.left = translateX(rect.left);
        rect.top = translateY(rect.top);
        rect.right = translateX(rect.right);
        rect.bottom = translateY(rect.bottom);
        canvas.drawRect(rect, mRectPaint);

        // Draws a label at the bottom of the barcode indicate the barcode value that was detected.
        canvas.drawText(barcode.rawValue, rect.left, rect.bottom, mTextPaint);

        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, rect.left, rect.bottom, mBitmapPaint);
        }
    }

    public void fetch(Barcode item) {
        fetchItemDetail(item.displayValue);
    }

    private void fetchItemDetail(String displayValue) {
        ApiClient.getInstance().getItemDetail(displayValue)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<ApiClient.ItemDetail>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe");
                    }

                    @Override
                    public void onSuccess(ApiClient.ItemDetail value) {
                        Log.d(TAG, "onSuccess");
                        try {
                            mUrl = value.resultSet.hoge.result.item.url;
                            fetchImage(value);
                        } catch (Exception e) {
                            // Log.d()
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError");
                    }
                });
    }

    private void fetchImage(ApiClient.ItemDetail detail) {
        final Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                mBitmap = bitmap;
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                try {
                    mBitmap = ((BitmapDrawable) errorDrawable).getBitmap();
                } catch (Exception e) {

                }
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        };

        Picasso picasso = new Picasso.Builder(mContext)
                .listener(new Picasso.Listener() {
                    @Override
                    public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
                        Log.d(TAG, exception.getMessage());
                    }
                })
                .build();
        picasso.load(detail.resultSet.hoge.result.item.image.mediumUrl).into(target);

    }
}
