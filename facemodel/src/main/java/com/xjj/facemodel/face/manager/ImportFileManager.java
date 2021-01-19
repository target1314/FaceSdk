package com.xjj.facemodel.face.manager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.idl.main.facesdk.model.BDFaceSDKCommon;
import com.baidu.idl.main.facesdk.utils.ZipUtils;
import com.xjj.facemodel.face.api.FaceApi;
import com.xjj.facemodel.face.listener.OnImportListener;
import com.xjj.facemodel.face.model.ImportFeatureResult;
import com.xjj.facemodel.face.model.User;
import com.xjj.facemodel.face.utils.BitmapUtils;
import com.xjj.facemodel.face.utils.FileUtils;
import com.xjj.facemodel.face.utils.LogUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 导入相关管理类
 * Created by v_liujialu01 on 2019/5/28.
 */

public class ImportFileManager {
    private static final String TAG = "ImportFileManager";

    private Future mFuture;
    private ExecutorService mExecutorService;
    private OnImportListener mImportListener;
    // 是否需要导入
    private volatile boolean mIsNeedImport;

    private int mTotalCount;
    private int mFinishCount;
    private int mSuccessCount;
    private int mFailCount;

    private static class HolderClass {
        private static final ImportFileManager instance = new ImportFileManager();
    }

    public static ImportFileManager getInstance() {
        return HolderClass.instance;
    }

    // 私有构造，实例化ExecutorService
    private ImportFileManager() {
        if (mExecutorService == null) {
            mExecutorService = Executors.newSingleThreadExecutor();
        }
    }

    public void setOnImportListener(OnImportListener importListener) {
        mImportListener = importListener;
    }

    /**
     * 开始批量导入
     */
    public void batchImport(final boolean isUpdate) {
        // 1、获取导入目录 /sdcard/Face-Import
        File batchImportDir = FileUtils.getBatchImportDirectory();
        // 2、遍历该目录下的所有文件
        File[] picFiles = batchImportDir.listFiles();
        if (picFiles == null || picFiles.length == 0) {
            Log.i(TAG, "导入数据的文件夹没有数据");
            if (mImportListener != null) {
                mImportListener.showToastMessage("导入数据的文件夹没有数据");
            }
            return;
        }

        // 判断Face.zip是否存在
        if (FileUtils.isFileExist(batchImportDir.getPath(), "Face.zip") != null) {
            File zipFile = FileUtils.isFileExist(batchImportDir.getPath(), "Face.zip");
            if (zipFile == null) {
                LogUtils.i(TAG, "导入数据的文件夹没有Face.zip");
                if (mImportListener != null) {
                    mImportListener.showToastMessage("搜索失败，请检查操作步骤并重试");
                }
                return;
            }
            // 开启线程导入图片
            asyncImport(picFiles, batchImportDir, zipFile, isUpdate);
        }

    }

    public void setIsNeedImport(boolean isNeedImport) {
        mIsNeedImport = isNeedImport;
    }

    /**
     * 开启线程导入图片
     *
     * @param picFiles 要导入的图片集
     */
    private void asyncImport(final File[] picFiles, final File batchFaceDir, final File zipFile, final boolean isUpdate) {
        mIsNeedImport = true;     // 判断是否需要导入
        mFinishCount = 0;         // 已完成的图片数量
        mSuccessCount = 0;        // 已导入成功的图片数量
        mFailCount = 0;           // 已导入失败的图片数量

        if (mExecutorService == null) {
            mExecutorService = Executors.newSingleThreadExecutor();
        }

        mFuture = mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (picFiles == null || picFiles.length == 0) {
                        Log.i(TAG, "导入数据的文件夹没有数据");
                        if (mImportListener != null) {
                            mImportListener.showToastMessage("导入数据的文件夹没有数据");
                        }
                        return;
                    }

                    // 解压
                    if (!TextUtils.isEmpty(zipFile.getAbsolutePath()) && batchFaceDir != null) {
                        boolean zipSuccess = ZipUtils.unZipFolder(zipFile.getAbsolutePath(), batchFaceDir.toString());
                        if (!zipSuccess) {
                            if (mImportListener != null) {
                                mImportListener.showToastMessage("解压失败");
                            }
                            return;
                        }
                    }

                    // 删除zip文件
                    FileUtils.deleteFile(zipFile.getPath());
                    LogUtils.i(TAG, "解压成功");

                    // 解压成功之后再次遍历该目录是不是存在文件
                    File batchPicDir = FileUtils.getBatchImportDirectory();

                    LogUtils.i(TAG, "文件路径" + batchPicDir.getPath());
                    File[] files = batchPicDir.listFiles();

                    // 如果该目录下没有文件，则提示获取图片失败
                    if (files == null) {
                        if (mImportListener != null) {
                            mImportListener.showToastMessage("获取图片失败");
                        }
                        return;
                    }
                    // 如果该目录下有文件，则判断该文件是目录还是文件
                    File[] picFiles;   // 定义图片文件数组

                    if (files[0].isDirectory()) {
                        picFiles = files[0].listFiles();
                    } else {
                        picFiles = files;
                    }

                    // 读取图片成功，开始显示进度条
                    if (mImportListener != null) {
                        mImportListener.showProgressView();
                    }

                    Thread.sleep(400);

                    mTotalCount = picFiles.length;  // 总图片数

                    for (int i = 0; i < picFiles.length; i++) {
                        if (!mIsNeedImport) {
                            break;
                        }

                        // 3、获取图片名
                        String picName = picFiles[i].getName();
                        Log.e(TAG, "i = " + i + ", picName = " + picName);
                        // 4、判断图片后缀
                        if (!picName.endsWith(".jpg") && !picName.endsWith(".png")) {
                            Log.e(TAG, "图片后缀不满足要求");
                            mFinishCount++;
                            mFailCount++;
                            Log.e(TAG, "失败图片mFailCount===" + mFailCount);
                            // 更新进度
                            /*updateProgress(mTotalCount, mSuccessCount, mFailCount,
                                    ((float) mFinishCount / (float) mTotalCount));*/
                            continue;
                        }

                        // 5、获取不带后缀的图片名，即用户名
                        String userName = FileUtils.getFileNameNoEx(picName);

                        boolean success = false;  // 判断成功状态

                        // 6、判断姓名是否有效
                        /*String nameResult = FaceApi.getInstance().isValidName(userName);
                        if (!"0".equals(nameResult)) {
                            Log.i(TAG, nameResult);
                            mFinishCount++;
                            mFailCount++;
                            // 更新进度
                            updateProgress(mTotalCount, mSuccessCount, mFailCount,
                                    ((float) mFinishCount / (float) mTotalCount));
                            continue;
                        }*/

                        // 7、根据姓名查询数据库与文件中对应的姓名是否相等，如果相等，则直接过滤
                        if (!isUpdate) {
                            List<User> listUsers = FaceApi.getInstance().getUserListByUserName(userName);
                            if (listUsers != null && listUsers.size() > 0) {
                                Log.i(TAG, "与之前图片名称相同");
                                mFinishCount++;
                                mFailCount++;
                                Log.e(TAG, "失败图片mFailCount===" + mFailCount);
                           /*     // 更新进度
                                updateProgress(mTotalCount, mSuccessCount, mFailCount,
                                        ((float) mFinishCount / (float) mTotalCount));*/
                                continue;
                            }
                        }

                        // 8、根据图片的路径将图片转成Bitmap
                        Bitmap bitmap = BitmapFactory.decodeFile(picFiles[i].getAbsolutePath());

                        // 9、判断bitmap是否转换成功
                        if (bitmap == null) {
                            Log.e(TAG, picName + "：该图片转成Bitmap失败");
                            mFinishCount++;
                            mFailCount++;
                            // 更新进度
                            updateProgress(mTotalCount, mSuccessCount, mFailCount,
                                    ((float) mFinishCount / (float) mTotalCount));
                            continue;
                        }

                        // 图片缩放
                        if (bitmap.getWidth() * bitmap.getHeight() > 3000 * 2000) {
                            if (bitmap.getWidth() > bitmap.getHeight()) {
                                float scale = 1 / (bitmap.getWidth() * 1.0f / 1000.0f);
                                bitmap = BitmapUtils.scale(bitmap, scale);
                            } else {
                                float scale = 1 / (bitmap.getHeight() * 1.0f / 1000.0f);
                                bitmap = BitmapUtils.scale(bitmap, scale);
                            }
                        }

                        byte[] bytes = new byte[512];
                        ImportFeatureResult result;
                        // 10、走人脸SDK接口，通过人脸检测、特征提取拿到人脸特征值
                        result = FaceApi.getInstance().getFeature(bitmap, bytes,
                                BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO);

                        // 11、判断是否提取成功：128为成功，-1为参数为空，-2表示未检测到人脸
                        Log.i(TAG, "live_photo = " + result.getResult());
                        if (result.getResult() == -1) {
                            Log.e(TAG, picName + "：bitmap参数为空");
                            //失败回调
                            failCallBack(userName);
                        } else if (result.getResult() == -2) {
                            Log.e(TAG, picName + "：未检测到人脸");
                            //失败回调
                            failCallBack(userName);
                        } else if (result.getResult() == -3) {
                            Log.e(TAG, picName + "：抠图失败");
                            //失败回调
                            failCallBack(userName);
                        } else if (result.getResult() == 128) {
                            // 将用户信息保存到数据库中
                            boolean importDBSuccess = FaceApi.getInstance().registerUserIntoDBmanager(null,
                                    userName, picName, null, bytes);

                            // 保存数据库成功
                            if (importDBSuccess) {
                                success = true;
                                // 保存图片到新目录中
                               /* File facePicDir = FileUtils.getBatchImportSuccessDirectory();
                                if (facePicDir != null) {
                                    File savePicPath = new File(facePicDir, picName);
                                    if (FileUtils.saveBitmap(savePicPath, result.getBitmap())) {
                                        Log.i(TAG, "图片保存成功");
                                        success = true;
                                    } else {
                                        Log.i(TAG, "图片保存失败");
                                    }
                                }*/
                            } else {
                                Log.e(TAG, picName + "：保存到数据库失败");
                            }
                        } else {
                            Log.e(TAG, picName + "：未检测到人脸");
                            //失败回调
                            failCallBack(userName);
                        }

                        // 图片回收
                        if (!bitmap.isRecycled()) {
                            bitmap.recycle();
                        }

                        // 判断成功与否
                        if (success) {
                            mSuccessCount++;
                        } else {
                            mFailCount++;
                            Log.e(TAG, "失败图片:" + picName);
                        }
                        mFinishCount++;
                        // 导入中（用来显示进度）
                        Log.i(TAG, "mFinishCount = " + mFinishCount
                                + " progress = " + ((float) mFinishCount / (float) mTotalCount));
                        if (mImportListener != null) {
                            mImportListener.onImporting(mTotalCount, mSuccessCount, mFailCount,
                                    ((float) mFinishCount / (float) mTotalCount));
                        }
                    }

                    // 导入完成
                    if (mImportListener != null) {
                        mImportListener.endImport(mTotalCount, mSuccessCount, mFailCount);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "exception = " + e.getMessage());
                }
            }
        });
    }

    /**
     * 人脸下发失败回调
     *
     * @param userName
     */
    private void failCallBack(String userName) {
        //失败回调
        if (mImportListener != null) {
            mImportListener.registerFailCallBack(userName.split("-")[0]);
        }

    }

    private void updateProgress(int totalCount, int successCount, int failureCount, float progress) {
        if (mImportListener != null) {
            mImportListener.onImporting(totalCount, successCount, failureCount, progress);
        }
    }

    /**
     * 释放功能，用于关闭线程操作
     */
    public void release() {
        if (mFuture != null && !mFuture.isCancelled()) {
            mFuture.cancel(true);
            mFuture = null;
        }

        if (mExecutorService != null) {
            mExecutorService.shutdown();
            mExecutorService = null;
        }
    }
}
