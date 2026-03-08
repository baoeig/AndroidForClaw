package com.xiaomo.androidforclaw.util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.xiaomo.androidforclaw.core.MyApplication;

import java.util.HashMap;
import java.util.Map;


public class WakeLockManager {

    private static final String TAG = "WakeLockManager";

    public static final long PROCESS_WAKELOCK_TIMEOUT = 30 * 1000;

    // 屏幕唤醒锁的 key
    public static final String SCREEN_WAKE_LOCK_KEY = "screen_wake_lock";
    // 定期刷新间隔：4分钟（WakeLock 默认超时是 5 分钟）
    private static final long REFRESH_INTERVAL = 4 * 60 * 1000; // 4分钟
    // 定期唤醒屏幕间隔：30秒（通过反射调用userActivity防止锁屏）
    private static final long WAKE_SCREEN_INTERVAL = 30 * 1000; // 30秒
    
    private static PowerManager sPowerManager = getSystemService(Context.POWER_SERVICE);
    private static final Map<String, WakeLock> sWakeLockMap = new HashMap<String, WakeLock>();
    
    // 定期刷新 WakeLock 的 Handler
    private static Handler sRefreshHandler = new Handler(Looper.getMainLooper());
    // 负责定期刷新WakeLock的回调任务
    private static Runnable sRefreshRunnable = null;
    // 负责定期唤醒屏幕的回调任务
    private static Runnable sWakeScreenRunnable = null;
    // 标记屏幕唤醒锁是否处于激活状态
    private static boolean sScreenWakeLockActive = false;

    /**
     * 获取系统服务
     */
    public static void acquire(@NonNull String key, long timeOut) {
        Log.i(TAG, String.format("acquire wakeLock: %s for %d", key, timeOut));
        try {
            WakeLock wakeLock = sPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    WakeLockManager.class.getCanonicalName() + "/" + key);
            wakeLock.acquire(timeOut);

            WakeLock oldWakeLock;
            synchronized (sWakeLockMap) {
                oldWakeLock = sWakeLockMap.get(key);
                sWakeLockMap.put(key, wakeLock);
                if (oldWakeLock != null && oldWakeLock.isHeld()) {
                    oldWakeLock.release();  //因每个WakeLock有相关的超时，新旧之间不等同，须释放旧的，用新的代替
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "exception when aquire wakelock " + key + ": " + e.toString());
        }
    }

    /**
     * 释放系统服务
     */
    public static void release(@NonNull String key) {
        try {
            WakeLock oldWakeLock;
            synchronized (sWakeLockMap) {
                oldWakeLock = sWakeLockMap.remove(key);
                if (oldWakeLock != null && oldWakeLock.isHeld()) {
                    Log.i(TAG, "release wakeLock: " + key);
                    oldWakeLock.release();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "exception when release wakelock " + key + ": " + e.toString());
        }
    }

    /**
     * 获取屏幕唤醒锁，防止设备锁屏
     * 
     * 多层次防锁屏方案：
     * 1. 使用 PARTIAL_WAKE_LOCK 保持 CPU 运行（所有 Android 版本都支持）
     * 2. 结合 FLAG_KEEP_SCREEN_ON 在 Window 层面（需要在 Activity/Service 中设置）
     * 3. 定期刷新 WakeLock，确保不会因超时失效
     * 4. 使用 ACQUIRE_CAUSES_WAKEUP 确保能唤醒屏幕
     */
    public static void acquireScreenWakeLock() {
        Log.i(TAG, "acquire screen wakeLock to prevent screen lock - 使用多层次防锁屏方案");
        
        synchronized (WakeLockManager.class) {
            if (sScreenWakeLockActive) {
                Log.i(TAG, "屏幕唤醒锁已激活，跳过重复获取");
                return;
            }
            sScreenWakeLockActive = true;
        }
        
        try {
            // 🔥 关键修复：PARTIAL_WAKE_LOCK 不能防止锁屏！
            // 需要组合使用多种方案：
            // 1. PARTIAL_WAKE_LOCK 保持 CPU 运行
            // 2. 尝试使用 SCREEN_DIM_WAKE_LOCK（即使 API 27+ 被废弃，某些设备仍可能有效）
            // 3. 结合 FLAG_KEEP_SCREEN_ON 在窗口层面
            
            // 方案1：获取 PARTIAL_WAKE_LOCK 保持 CPU 运行
            // 🔥 使用无超时时间，确保不会自动释放
            int partialFlags = PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
            WakeLock partialWakeLock = sPowerManager.newWakeLock(partialFlags,
                    WakeLockManager.class.getCanonicalName() + "/" + SCREEN_WAKE_LOCK_KEY + "_partial");
            partialWakeLock.setReferenceCounted(false);
            // 使用无超时时间，通过定期刷新来维持
            partialWakeLock.acquire();
            
            // 方案2：尝试获取 SCREEN_DIM_WAKE_LOCK 防止锁屏（即使被废弃）
            WakeLock screenWakeLock = null;
            try {
                // 使用反射尝试获取 SCREEN_DIM_WAKE_LOCK（即使被废弃）
                int screenFlags = PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
                screenWakeLock = sPowerManager.newWakeLock(screenFlags,
                        WakeLockManager.class.getCanonicalName() + "/" + SCREEN_WAKE_LOCK_KEY + "_screen");
                screenWakeLock.setReferenceCounted(false);
                // 使用无超时时间，通过定期刷新来维持
                screenWakeLock.acquire();
                Log.i(TAG, "成功获取 SCREEN_DIM_WAKE_LOCK（防止锁屏）");
            } catch (Exception e) {
                Log.w(TAG, "获取 SCREEN_DIM_WAKE_LOCK 失败（可能被系统忽略）: " + e.getMessage());
                // 如果失败，尝试 SCREEN_BRIGHT_WAKE_LOCK
                try {
                    int brightFlags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
                    screenWakeLock = sPowerManager.newWakeLock(brightFlags,
                            WakeLockManager.class.getCanonicalName() + "/" + SCREEN_WAKE_LOCK_KEY + "_screen");
                    screenWakeLock.setReferenceCounted(false);
                    // 使用无超时时间，通过定期刷新来维持
                    screenWakeLock.acquire();
                    Log.i(TAG, "成功获取 SCREEN_BRIGHT_WAKE_LOCK（防止锁屏）");
                } catch (Exception e2) {
                    Log.w(TAG, "获取 SCREEN_BRIGHT_WAKE_LOCK 也失败: " + e2.getMessage());
                }
            }

            WakeLock oldWakeLock;
            synchronized (sWakeLockMap) {
                // 保存两个 WakeLock
                oldWakeLock = sWakeLockMap.get(SCREEN_WAKE_LOCK_KEY);
                sWakeLockMap.put(SCREEN_WAKE_LOCK_KEY, partialWakeLock);
                if (screenWakeLock != null) {
                    sWakeLockMap.put(SCREEN_WAKE_LOCK_KEY + "_screen", screenWakeLock);
                }
                if (oldWakeLock != null && oldWakeLock.isHeld()) {
                    oldWakeLock.release();
                }
            }
            
            Log.i(TAG, "屏幕唤醒锁获取成功 - PARTIAL_WAKE_LOCK（CPU）" + 
                    (screenWakeLock != null ? " + SCREEN_WAKE_LOCK（屏幕）" : "（屏幕锁可能被系统忽略）"));
            
            // 🔥 尝试使用 WRITE_SECURE_SETTINGS 权限修改屏幕超时（如果可能）
            trySetScreenTimeoutNever();
            
            // 启动定期刷新机制
            startRefreshRoutine();
            
            // 启动定期唤醒屏幕机制（通过反射调用userActivity防止锁屏）
            startWakeScreenRoutine();
            
        } catch (Exception e) {
            Log.e(TAG, "获取屏幕唤醒锁失败: " + e.toString(), e);
            synchronized (WakeLockManager.class) {
                sScreenWakeLockActive = false;
            }
        }
    }
    
    /**
     * 定期刷新 WakeLock，确保不会因超时失效
     * 每 4 分钟刷新一次（WakeLock 默认超时是 5 分钟）
     */
    private static void startRefreshRoutine() {
        // 停止旧的刷新任务
        stopRefreshRoutine();
        
        sRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!sScreenWakeLockActive) {
                    Log.d(TAG, "屏幕唤醒锁已释放，停止刷新");
                    return;
                }
                
                try {
                    synchronized (sWakeLockMap) {
                        // 刷新 PARTIAL_WAKE_LOCK
                        WakeLock currentPartialLock = sWakeLockMap.get(SCREEN_WAKE_LOCK_KEY);
                        if (currentPartialLock != null && currentPartialLock.isHeld()) {
                            currentPartialLock.release();
                            
                            int partialFlags = PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
                            WakeLock newPartialLock = sPowerManager.newWakeLock(partialFlags,
                                    WakeLockManager.class.getCanonicalName() + "/" + SCREEN_WAKE_LOCK_KEY + "_partial");
                            newPartialLock.setReferenceCounted(false);
                            // 使用无超时时间
                            newPartialLock.acquire();
                            sWakeLockMap.put(SCREEN_WAKE_LOCK_KEY, newPartialLock);
                        }
                        
                        // 刷新 SCREEN_WAKE_LOCK（如果存在）
                        WakeLock currentScreenLock = sWakeLockMap.get(SCREEN_WAKE_LOCK_KEY + "_screen");
                        if (currentScreenLock != null && currentScreenLock.isHeld()) {
                            currentScreenLock.release();
                            
                            try {
                                int screenFlags = PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
                                WakeLock newScreenLock = sPowerManager.newWakeLock(screenFlags,
                                        WakeLockManager.class.getCanonicalName() + "/" + SCREEN_WAKE_LOCK_KEY + "_screen");
                                    newScreenLock.setReferenceCounted(false);
                                    // 使用无超时时间
                                    newScreenLock.acquire();
                                    sWakeLockMap.put(SCREEN_WAKE_LOCK_KEY + "_screen", newScreenLock);
                            } catch (Exception e) {
                                Log.w(TAG, "刷新 SCREEN_WAKE_LOCK 失败: " + e.getMessage());
                            }
                        }
                        
                        if (currentPartialLock == null || !currentPartialLock.isHeld()) {
                            Log.w(TAG, "当前 WakeLock 未持有，重新获取");
                            // 如果 WakeLock 丢失，重新获取
                            acquireScreenWakeLock();
                            return;
                        }
                        
                        Log.d(TAG, "屏幕唤醒锁已刷新");
                    }
                    
                    // 安排下一次刷新
                    sRefreshHandler.postDelayed(this, REFRESH_INTERVAL);
                } catch (Exception e) {
                    Log.e(TAG, "刷新屏幕唤醒锁失败: " + e.toString(), e);
                    // 即使刷新失败，也尝试继续
                    sRefreshHandler.postDelayed(this, REFRESH_INTERVAL);
                }
            }
        };
        
        // 延迟 REFRESH_INTERVAL 后开始第一次刷新
        sRefreshHandler.postDelayed(sRefreshRunnable, REFRESH_INTERVAL);
        Log.i(TAG, "已启动屏幕唤醒锁定期刷新机制，间隔: " + (REFRESH_INTERVAL / 1000) + "秒");
    }
    
    /**
     * 停止定期刷新
     */
    private static void stopRefreshRoutine() {
        if (sRefreshRunnable != null) {
            sRefreshHandler.removeCallbacks(sRefreshRunnable);
            sRefreshRunnable = null;
            Log.d(TAG, "已停止屏幕唤醒锁刷新机制");
        }
    }
    
    /**
     * 定期唤醒屏幕，防止锁屏（通过反射调用userActivity）
     */
    private static void startWakeScreenRoutine() {
        // 停止旧的唤醒任务
        stopWakeScreenRoutine();
        
        sWakeScreenRunnable = new Runnable() {
            @Override
            public void run() {
                if (!sScreenWakeLockActive) {
                    Log.d(TAG, "屏幕唤醒锁已释放，停止定期唤醒");
                    return;
                }
                
                try {
                    // 使用反射调用 userActivity() 告诉系统有用户活动，防止锁屏
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            java.lang.reflect.Method userActivityMethod = sPowerManager.getClass()
                                    .getMethod("userActivity", long.class, int.class, int.class);
                            // USER_ACTIVITY_EVENT_OTHER = 0
                            int userActivityEventOther = 0;
                            userActivityMethod.invoke(sPowerManager, System.currentTimeMillis(), 
                                    userActivityEventOther, 0);
                            Log.d(TAG, "已通过反射调用 userActivity() 防止锁屏");
                        }
                    } catch (NoSuchMethodException e) {
                        Log.d(TAG, "userActivity() 方法不存在，跳过");
                    } catch (Exception e) {
                        Log.w(TAG, "userActivity() 调用失败: " + e.getMessage());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "定期唤醒屏幕失败: " + e.toString(), e);
                }
                
                // 安排下一次唤醒
                sRefreshHandler.postDelayed(this, WAKE_SCREEN_INTERVAL);
            }
        };
        
        // 延迟开始第一次唤醒
        sRefreshHandler.postDelayed(sWakeScreenRunnable, WAKE_SCREEN_INTERVAL);
        Log.i(TAG, "已启动定期唤醒屏幕机制，间隔: " + (WAKE_SCREEN_INTERVAL / 1000) + "秒");
    }
    
    /**
     * 停止定期唤醒屏幕
     */
    private static void stopWakeScreenRoutine() {
        if (sWakeScreenRunnable != null) {
            sRefreshHandler.removeCallbacks(sWakeScreenRunnable);
            sWakeScreenRunnable = null;
            Log.d(TAG, "已停止定期唤醒屏幕机制");
        }
    }

    /**
     * 释放屏幕唤醒锁
     */
    public static void releaseScreenWakeLock() {
        synchronized (WakeLockManager.class) {
            if (!sScreenWakeLockActive) {
                Log.d(TAG, "屏幕唤醒锁未激活，跳过释放");
                return;
            }
            sScreenWakeLockActive = false;
        }
        
        // 停止刷新机制
        stopRefreshRoutine();
        
        // 停止定期唤醒屏幕机制
        stopWakeScreenRoutine();
        
        // 释放所有 WakeLock
        release(SCREEN_WAKE_LOCK_KEY); // PARTIAL_WAKE_LOCK
        release(SCREEN_WAKE_LOCK_KEY + "_screen"); // SCREEN_WAKE_LOCK
        
        // 恢复屏幕超时设置（可选）
        // restoreScreenTimeout();
        
        Log.i(TAG, "screen wakeLock released");
    }
    
    /**
     * 检查屏幕唤醒锁是否已激活
     * @return true=已激活, false=未激活
     */
    public static boolean isScreenWakeLockActive() {
        synchronized (WakeLockManager.class) {
            return sScreenWakeLockActive;
        }
    }
    
    /**
     * 为 Window 设置 FLAG_KEEP_SCREEN_ON
     * 这是防止锁屏的另一个重要层面，需要在 Activity/Service 中调用
     */
    public static void setKeepScreenOn(Window window) {
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Log.d(TAG, "已为 Window 设置 FLAG_KEEP_SCREEN_ON");
        }
    }
    
    /**
     * 清除 Window 的 FLAG_KEEP_SCREEN_ON
     */
    public static void clearKeepScreenOn(Window window) {
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Log.d(TAG, "已清除 Window 的 FLAG_KEEP_SCREEN_ON");
        }
    }

    
    /**
     * 尝试使用 WRITE_SECURE_SETTINGS 权限设置屏幕超时为永不锁屏
     * 仅在有权限时尝试设置，不请求权限或跳转设置页面
     */
    private static void trySetScreenTimeoutNever() {
        try {
            Context context = MyApplication.application.getApplicationContext();
            
            // 检查是否有 WRITE_SECURE_SETTINGS 权限（通过尝试写入测试）
            boolean hasPermission = false;
            try {
                Settings.Secure.putString(context.getContentResolver(), 
                        "test_write_secure_settings", "test");
                Settings.Secure.putString(context.getContentResolver(), 
                        "test_write_secure_settings", null);
                hasPermission = true;
            } catch (Exception e) {
                hasPermission = false;
            }
            
            if (hasPermission) {
                // 设置屏幕超时为最大值（2147483647 毫秒，约 24 天）
                // 注意：SCREEN_OFF_TIMEOUT 在 Settings.System 中，不在 Secure 中
                try {
                    // 方法1：尝试通过 System 设置屏幕超时（需要 WRITE_SETTINGS 权限）
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.System.canWrite(context)) {
                            Settings.System.putInt(context.getContentResolver(), 
                                    Settings.System.SCREEN_OFF_TIMEOUT, Integer.MAX_VALUE);
                            Log.i(TAG, "已通过 WRITE_SETTINGS 设置屏幕超时为最大值");
                        } else {
                            Log.d(TAG, "没有 WRITE_SETTINGS 权限，无法通过 System 设置屏幕超时");
                        }
                    } else {
                        // API 23 以下可以直接设置
                        Settings.System.putInt(context.getContentResolver(), 
                                Settings.System.SCREEN_OFF_TIMEOUT, Integer.MAX_VALUE);
                        Log.i(TAG, "已设置屏幕超时为最大值（API < 23）");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "通过 System 设置屏幕超时失败: " + e.getMessage());
                }
                
                // 方法2：尝试通过 Secure 设置（某些设备可能支持）
                try {
                    // 使用反射尝试设置 secure 中的屏幕超时
                    java.lang.reflect.Method putIntMethod = Settings.Secure.class
                            .getMethod("putInt", android.content.ContentResolver.class, String.class, int.class);
                    // 尝试设置屏幕超时（某些设备可能在 Secure 中有相关设置）
                    putIntMethod.invoke(null, context.getContentResolver(), 
                            "screen_off_timeout", Integer.MAX_VALUE);
                    Log.i(TAG, "已尝试通过 Secure 设置屏幕超时");
                } catch (Exception e) {
                    Log.d(TAG, "通过 Secure 设置屏幕超时失败（可能不支持）: " + e.getMessage());
                }
            } else {
                Log.d(TAG, "没有 WRITE_SECURE_SETTINGS 权限，跳过设置屏幕超时（不跳转设置页面）");
            }
        } catch (Exception e) {
            Log.w(TAG, "尝试设置屏幕超时失败: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getSystemService(String name) {
        return (T) MyApplication.application
                .getApplicationContext()
                .getSystemService(name);
    }
}