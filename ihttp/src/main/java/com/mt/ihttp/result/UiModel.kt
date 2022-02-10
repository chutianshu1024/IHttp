package com.mt.ihttp.result

/**
 * @Description: 通用ui回调（通知）
 * @Author: CTS
 * @Date: 2020/9/17 14:47
 */
data class UiModel(
        val showLoading: Boolean,
        val showError: String?,
        val showEnd: Boolean, // 加载更多
        val isRefresh: Boolean, // 刷新
        val needLogin: Boolean? = null
)