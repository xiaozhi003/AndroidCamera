package com.android.xz.util;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Size;

import androidx.exifinterface.media.ExifInterface;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageUtils {

    private static final String TAG = "ImageUtils";
    private static Context sContext;

    public static void init(Context context) {
        sContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            GALLERY_PATH = Environment.DIRECTORY_DCIM + File.separator + "Camera";
        } else {
            GALLERY_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + "Camera";
        }
    }

    private static String GALLERY_PATH = "";

    public static String getGalleryPath() {
        return GALLERY_PATH;
    }

    public static String getVideoPath() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return sContext.getExternalFilesDir("video").getAbsolutePath();
        } else {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + "Camera";
        }
    }

    private static final String[] STORE_IMAGES = {
            MediaStore.Images.Thumbnails._ID,
    };
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public static Bitmap rotateBitmap(Bitmap source, int degree, boolean flipHorizontal, boolean recycle) {
        if (degree == 0 && !flipHorizontal) {
            return source;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        if (flipHorizontal) {
            matrix.postScale(-1, 1);
        }
        Logs.d(TAG, "source width: " + source.getWidth() + ", height: " + source.getHeight());
        Logs.d(TAG, "rotateBitmap: degree: " + degree);
        Bitmap rotateBitmap = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
        Logs.d(TAG, "rotate width: " + rotateBitmap.getWidth() + ", height: " + rotateBitmap.getHeight());
        if (recycle) {
            source.recycle();
        }
        return rotateBitmap;
    }

    public static String saveImage(byte[] jpeg) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return saveImageToDCIM(jpeg);
        } else {
            return saveImageNone(jpeg);
        }
    }

    private static String saveImageToDCIM(byte[] jpeg) {
        String fileName = "IMG_" + DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".jpg";
        File outFile = new File(GALLERY_PATH, fileName);

        // 新建文件夹
        FileUtils.makeDirs(outFile.getAbsolutePath());

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        //该媒体项在存储设备中的相对路径，该媒体项将在其中保留
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, GALLERY_PATH);

        Uri uri = null;
        OutputStream outputStream = null;
        ContentResolver localContentResolver = sContext.getContentResolver();

        try {
            uri = localContentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

            outputStream = localContentResolver.openOutputStream(uri);

            // 将图片数据保存到Uri对应的数据节点中
            outputStream.write(jpeg);

            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String realImagePath = filePathByUri(sContext, uri);
        Logs.d(TAG, "saveImage. filepath: " + realImagePath);

        return realImagePath;
    }

    private static String saveImageNone(byte[] jpeg) {
        String fileName = "IMG_" + DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".jpg";
        File outFile = new File(GALLERY_PATH, fileName);
        Logs.d(TAG, "saveImage. filepath: " + outFile.getAbsolutePath());

        boolean ok = FileUtils.writeFile(outFile.getAbsolutePath(), jpeg);
        if (ok) {
            insertToDB(outFile.getAbsolutePath());
        }
        return outFile.getAbsolutePath();
    }

    public static void saveBitmap(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            saveBitmapToDCIM(bitmap);
        } else {
            saveBitmapNone(bitmap);
        }
    }

    private static void saveBitmapToDCIM(Bitmap bitmap) {
        String fileName = "IMG_" + DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".jpg";
        File outFile = new File(GALLERY_PATH, fileName);
        Logs.d(TAG, "saveImage. filepath: " + outFile.getAbsolutePath());

        // 新建文件夹
        FileUtils.makeDirs(outFile.getAbsolutePath());

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        //该媒体项在存储设备中的相对路径，该媒体项将在其中保留
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, GALLERY_PATH);

        Uri uri = null;
        OutputStream outputStream = null;
        ContentResolver localContentResolver = sContext.getContentResolver();

        try {
            uri = localContentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

            outputStream = localContentResolver.openOutputStream(uri);

            // 将bitmap图片保存到Uri对应的数据节点中
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bitmap.recycle();

            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void saveBitmapNone(Bitmap bitmap) {
        String fileName = "IMG_" + DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".jpg";
        File outFile = new File(GALLERY_PATH, fileName);
        Logs.d(TAG, "saveImage. filepath: " + outFile.getAbsolutePath());
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFile);
            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            Logs.d(TAG, "saveBitmap: " + success);
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
            Logs.d(TAG, "bitmap size " + (bitmap == null ? 0 : bitmap.getWidth()) + "x" + (bitmap == null ? 0 : bitmap.getHeight()));
        }
        cursor.close();
        return bitmap;
    }

    public static Bitmap getCorrectOrientationBitmap(byte[] bytes, Size requestSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        Matrix matrix = new Matrix();
        int rotate = getJPEGMatrix(bytes, matrix);
        if (Math.abs(rotate) == 90) {
            // 计算缩放比例
            options.inSampleSize = calculateInSampleSize(options, requestSize.getHeight(), requestSize.getWidth());
        } else {
            // 计算缩放比例
            options.inSampleSize = calculateInSampleSize(options, requestSize.getWidth(), requestSize.getHeight());
        }

        // 使用新的缩放比例在此解码图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        if (!matrix.isIdentity()) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }

    public static Bitmap getCorrectOrientationBitmap(String filePath, Size requestSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        Matrix matrix = new Matrix();
        int rotate = getJPEGMatrix(filePath, matrix);
        if (Math.abs(rotate) == 90) {
            // 计算缩放比例
            options.inSampleSize = calculateInSampleSize(options, requestSize.getHeight(), requestSize.getWidth());
        } else {
            // 计算缩放比例
            options.inSampleSize = calculateInSampleSize(options, requestSize.getWidth(), requestSize.getHeight());
        }

        // 使用新的缩放比例在此解码图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

        if (!matrix.isIdentity()) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }

    public static int getJPEGMatrix(byte[] bytes, Matrix matrix) {
        int rotate = 0;
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
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(-90);
                    matrix.postScale(-1, 1);
                    rotate = -90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(-90);
                    rotate = -90;
                    break;
                default:
                    // 无需调整
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotate;
    }

    public static int getJPEGMatrix(String filePath, Matrix matrix) {
        int rotate = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
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
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(-90);
                    matrix.postScale(-1, 1);
                    rotate = -90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(-90);
                    rotate = -90;
                    break;
                default:
                    // 无需调整
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotate;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int width = options.outWidth;
        final int height = options.outHeight;
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

    @TargetApi(Build.VERSION_CODES.O)
    private Uri saveVideoToMediaStore(Context context, File videoFile) throws IOException {
        // 创建一个 ContentValues 对象来存储视频文件的元数据
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.getName());
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES);

        // 获取外部存储中视频内容的 Uri
        Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

        // 使用 ContentResolver 插入一个新的视频条目到 MediaStore
        Uri videoUri = context.getContentResolver().insert(collection, values);

        // 使用 ContentResolver 打开一个输出流来写入视频文件
        try (OutputStream outputStream = context.getContentResolver().openOutputStream(videoUri)) {
            Files.copy(videoFile.toPath(), outputStream);
        }

        // 通知媒体扫描器扫描新文件，使其出现在相册中
        MediaScannerConnection.scanFile(context,
                new String[]{videoFile.getAbsolutePath()},
                new String[]{"video/*"},
                null);

        return videoUri;
    }

    /**
     * 通过uri 获取文件真实路径 (AndroidQ以下版本)
     *
     * @param context
     * @param uri
     * @return
     */
    public static String filePathByUri(Context context, Uri uri) {
        String imagePath = null;
        if (context != null && uri != null) {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                imagePath = cursor.getString(column_index);
            }
            cursor.close();
        }
        return imagePath;
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            InputStream inputStream = sContext.getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
