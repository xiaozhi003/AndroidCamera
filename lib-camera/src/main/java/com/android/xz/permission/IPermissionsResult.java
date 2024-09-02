package com.android.xz.permission;

/**
 * 权限申请的结果回调
 *
 * @author xiaozhi
 * @since 2023/5/16
 */
public interface IPermissionsResult {

    /**
     * 权限申请成功
     */
    void passPermissions();

    /**
     * 权限申请失败
     */
    void forbidPermissions();
}
