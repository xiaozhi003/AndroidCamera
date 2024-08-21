package com.android.xz.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageUtils {

    private static final String TAG = "ImageUtils";
    private static Context sContext;

    public static void init(Context context) {
        sContext = context;
    }

    private static final String GALLERY_PATH = Environment.getExternalStoragePublicDirectory(Environment
            .DIRECTORY_DCIM) + File.separator + "Camera";

    private static final String[] STORE_IMAGES = {
            MediaStore.Images.Thumbnails._ID,
    };
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public static Bitmap rotateBitmap(Bitmap source, int degree, boolean flipHorizontal, boolean recycle) {
        if (degree == 0 && !flipHorizontal) {
            return source;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        if (flipHorizontal) {
            matrix.postScale(-1, 1);
        }
        Log.d(TAG, "source width: " + source.getWidth() + ", height: " + source.getHeight());
        Log.d(TAG, "rotateBitmap: degree: " + degree);
        Bitmap rotateBitmap = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
        Log.d(TAG, "rotate width: " + rotateBitmap.getWidth() + ", height: " + rotateBitmap.getHeight());
        if (recycle) {
            source.recycle();
        }
        return rotateBitmap;
    }

    public static void saveImage(byte[] jpeg) {
        String fileName = DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".jpg";
        File outFile = new File(GALLERY_PATH, fileName);
        Log.d(TAG, "saveImage. filepath: " + outFile.getAbsolutePath());
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFile);
            os.write(jpeg);
            os.flush();
            os.close();
            insertToDB(outFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void saveBitmap(Bitmap bitmap) {
        String fileName = DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".jpg";
        File outFile = new File(GALLERY_PATH, fileName);
        Log.d(TAG, "saveImage. filepath: " + outFile.getAbsolutePath());
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFile);
            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            Log.d(TAG, "saveBitmap: " + success);
            if (success) {
                insertToDB(outFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void insertToDB(String picturePath) {
        ContentValues values = new ContentValues();
        ContentResolver resolver = sContext.getContentResolver();
        values.put(MediaStore.Images.ImageColumns.DATA, picturePath);
        values.put(MediaStore.Images.ImageColumns.TITLE, picturePath.substring(picturePath.lastIndexOf("/") + 1));
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static Bitmap getLatestThumbBitmap() {
        Bitmap bitmap = null;
        // 按照时间顺序降序查询
        Cursor cursor = MediaStore.Images.Media.query(sContext.getContentResolver(), MediaStore.Images.Media
                .EXTERNAL_CONTENT_URI, STORE_IMAGES, null, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC");
        boolean first = cursor.moveToFirst();
        if (first) {
            long id = cursor.getLong(0);
            bitmap = MediaStore.Images.Thumbnails.getThumbnail(sContext.getContentResolver(), id, MediaStore.Images
                    .Thumbnails.MICRO_KIND, null);
            Log.d(TAG, "bitmap width: " + bitmap.getWidth());
            Log.d(TAG, "bitmap height: " + bitmap.getHeight());
        }
        cursor.close();
        return bitmap;
    }

    public static Bitmap getCorrectOrientationBitmap(byte[] bytes, Size requestSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        // 计算缩放比例
        options.inSampleSize = calculateInSampleSize(options, requestSize.getWidth(), requestSize.getHeight());

        // 使用新的缩放比例在此解码图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        Matrix matrix = getJPEGMatrix(bytes);
        if (!matrix.isIdentity()) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }

    public static Matrix getJPEGMatrix(byte[] bytes) {
        Matrix matrix = new Matrix();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                InputStream inputStream = new ByteArrayInputStream(bytes);
                ExifInterface exifInterface = new ExifInterface(inputStream);
                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                Logs.i(TAG, "orientation: " + orientation);

                switch (orientation) {
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                        matrix.setScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.setRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        matrix.setScale(1, -1);
                        break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        matrix.setRotate(90);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.setRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        matrix.setRotate(-90);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.setRotate(-90);
                        break;
                    default:
                        // 无需调整
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return matrix;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // 计算最大的inSampleSize值，该值是2的幂，并保持高度和宽度大于所需高度和宽度的一半。
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * 保留图像Exif信息
     *
     * @param oldFilePath
     * @param newFilePath
     * @throws Exception
     */
    public static void saveExif(String oldFilePath, String newFilePath) throws Exception {
        ExifInterface oldExif = new ExifInterface(oldFilePath);
        ExifInterface newExif = new ExifInterface(newFilePath);
        Class<ExifInterface> cls = ExifInterface.class;
        Field[] fields = cls.getFields();
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fields[i].getName();
            if (!TextUtils.isEmpty(fieldName) && fieldName.startsWith("TAG")) {
                String fieldValue = fields[i].get(cls).toString();
                String attribute = oldExif.getAttribute(fieldValue);
                if (attribute != null) {
                    newExif.setAttribute(fieldValue, attribute);
                }
            }
        }
        newExif.saveAttributes();
    }
}
